package com.dkss.medical.device.pmr;

import android.util.Log;

import com.dkss.medical.util.DkssUtil;
import com.dkss.medical.util.SocketUtil;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class PatientMonitor_v0 implements Runnable {
	public volatile boolean exit = false;
	private static final String TAG = "pmr";

	private String pmrHost;
	private int pmrPort;
	private int pmrReadTimeout;

	private String remoteHost;
	private int remotePort;
	private int remoteReadTimeout;
	private int remoteConnectTimeout;

	private String QRFQRD;
	private String ACK;

	private String lastData = "";
	private String regexMsg; // 信息分割符
	private String regexSeg; // 段分隔符

	private HashMap<String, byte[]> dataIDMap; // 标识

	private ArrayList<byte[]> payloadList; // 负载列表，存储一轮的数据内容

	private ArrayList<byte[]> bufferDataQueue; // 缓存，保存发送服务器失败的数据队列
	private int bufferDataQueueLen;

	private byte[] boxIDPayload; // 盒子ID
	private byte[] boxTypePayload;   //盒子类型
	private byte[] devTypePayload;   //设备类型
	private byte[] devIDPayload;
	private boolean fake = false;

	private boolean packetFlag = false;

	public PatientMonitor_v0() {

		String SB = new String(new byte[] { 0x0b });
		String EB = new String(new byte[] { 0x1c });
		String CR = new String(new byte[] { 0x0d });

		this.regexMsg = EB + CR;
		this.regexSeg = CR;
		// 生成查询和ACK命令
		this.QRFQRD = SB + "MSH|^~\\&|||||||ORU^R02|QY01|P|2.3.1|\r" + "QRD||R|I|QCM2010|||||RES|\r"
				+ "QRF|CMS||||0&0^1^1^0^1|\r" + regexMsg;
		this.ACK = SB + "MSH|^~\\&|||||||ORU^R01|HR01|P|2.3.1|\r" + regexMsg;

		this.payloadList = new ArrayList<>();

		this.bufferDataQueue = new ArrayList<>();
		this.dataIDMap = new HashMap<>();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public boolean init(Map<String, Object> commonCfgMap, Map<String, Object> pmIDMap) {

		// 填补标识映射
		HashMap<String, String> tempMap = (HashMap) pmIDMap.get("patient_monitor_id");
		byte[] value = null;
		for (String key : tempMap.keySet()) {
			value = DkssUtil.hexStringToByteArr(tempMap.get(key));
			if (value == null) {
				System.err.println("配置文件出错，错误的值字段");
				return false;
			}
			dataIDMap.put(key, value);
		}

		// 监护仪设备设置
		tempMap = (HashMap) commonCfgMap.get("patient_monitor");
		this.pmrHost = tempMap.get("host");
		this.pmrPort = Integer.parseInt(tempMap.get("port"));
		this.pmrReadTimeout = Integer.parseInt(tempMap.get("read_timeout"));
		this.bufferDataQueueLen = Integer.parseInt(tempMap.get("buffer_data_queue_len"));
		devTypePayload = DkssUtil.constructPayload(new byte[]{0x00,0x05},tempMap.get("dev_type"));
		devIDPayload = DkssUtil.constructPayload(new byte[]{0x00,0x01},tempMap.get("dev_id"));

		//远程服务器配置
		tempMap = (HashMap) commonCfgMap.get("server_host");
		this.remoteHost = tempMap.get("host");
		this.remotePort = Integer.parseInt(tempMap.get("port"));
		this.remoteReadTimeout = Integer.parseInt(tempMap.get("read_timeout"));
		this.remoteConnectTimeout = Integer.parseInt(tempMap.get("connect_timeout"));

		// 盒子
		tempMap = (HashMap) commonCfgMap.get("box");
		boxIDPayload = DkssUtil.constructPayload(new byte[] { 0x00, 0x01 }, tempMap.get("box_id"));
		boxTypePayload = DkssUtil.constructPayload(new byte[] {0x00,0x04}, tempMap.get("box_type"));

		return true;
	}

	private boolean parseData(String data) {

		data = lastData + data;
		String[] messageArr = data.split(regexMsg);
		int i = 0;

		for (i = 0; i < messageArr.length - 1; i++) {
			String message = messageArr[i].substring(1);
//			if(parseMessage(message)){
//				return true;
//			}
			parseMessage(message);
		}
		// 数据分割后，最后一个数据可能不完整，因此保留到lastData中，留到下次
		lastData = messageArr[i];
		return true;
	}

	// 任何一个message(即hl7数据包，第一个段均为MSH)
	private boolean parseMessage(String message) {

		String[] segmentArr = message.split(regexSeg);
		String[] fields = segmentArr[0].split("\\|"); // 这是一个MSH段
		String MSHType = null;
		try {
			 MSHType = fields[9];
		}catch (ArrayIndexOutOfBoundsException e){
			Log.i(TAG,"message = "+message);
			return false;
		}

		// 构建包，并将数据加入发送缓冲区
		if (MSHType.equals("HR01") && payloadList.size() >0 ) {

			payloadList.add(0, DkssUtil.getTimePayload());
			payloadList.add(0,devTypePayload);
			payloadList.add(0, boxIDPayload);
			//payloadList.add(0, devIDPayload);
			payloadList.add(0, boxTypePayload);



			byte[] procotolPacket = DkssUtil.constructPacket(DkssUtil.DKSS_VERSION, DkssUtil.DKSS_CMD_SEND_DATA,
					payloadList.size(), DkssUtil.mergeByte(payloadList));

			if (bufferDataQueue.size() >= bufferDataQueueLen) {
				bufferDataQueue.remove(0);
			}
			bufferDataQueue.add(procotolPacket);
			payloadList.clear();
			return true;
		}

		int i = 1;
		String segment = "";
		byte[] idByte = null;
		int segArrLen = segmentArr.length;

		// 固定的病人信息
		if (MSHType.equals("PA01")) {
			for (i = 1; i < segArrLen; i++) {
				segment = segmentArr[i];
				fields = segmentArr[i].split("\\|");
				if (fields[0].equals("PID")) {
					payloadList.add(DkssUtil.constructPayload(dataIDMap.get("patient_id"), fields[3]));// 病历号

					String[] name = fields[5].split("\\^");

					payloadList.add(DkssUtil.constructPayload(dataIDMap.get("f_name"), name[0]));// 名
					payloadList.add(DkssUtil.constructPayload(dataIDMap.get("s_name"), name[1])); // 姓

					payloadList.add(DkssUtil.constructPayload(dataIDMap.get("sex"), fields[8].charAt(0)));// 性别
				} else if (fields[0].equals("PV1")) {
					String[] room = fields[3].split("\\^");
					payloadList.add(DkssUtil.constructPayload(dataIDMap.get("room_no"), room[1]));// 房间号

					String BedNO = room[2].split("\\&")[1];
					payloadList.add(
							DkssUtil.constructPayload(dataIDMap.get("bed_no"), BedNO.substring(0, BedNO.length() - 1)));// 床位

					payloadList.add(DkssUtil.constructPayload(dataIDMap.get("type"), fields[18].charAt(0)));
				} else if (fields[0].equals("OBX")) {

					if ((idByte = dataIDMap.get(fields[3])) == null) {
						continue;
					}
					if (fields[2].equals("NM")) {
						payloadList.add(DkssUtil.constructPayload(idByte, Short.parseShort(fields[5])));
					} else if (fields[2].equals("CE")) {
						payloadList.add(DkssUtil.constructPayload(idByte, fields[5]));
					}
				}
			}
		} else if (MSHType.equals("RP01")) { // 测量的数据
			for (i = 1; i < segArrLen; i++) {
				segment = segmentArr[i];
				fields = segment.split("\\|");
				if (fields[0].equals("OBX")) {
					String argID = fields[4].split("\\^")[0];
					if ((idByte = dataIDMap.get(fields[3] + "-" + argID)) == null) {
						continue;
					}
					if (fields[2].equals("NM")) {
						payloadList.add(DkssUtil.constructPayload(idByte, Short.parseShort(fields[5])));
					} else if (fields[2].equals("CE")) {
						payloadList.add(DkssUtil.constructPayload(idByte, fields[5]));
					}
				}
			}
		} else if (MSHType.equals("AP01") || MSHType.equals("AT01")) { // 报警信息
			for (i = 1; i < segArrLen; i++) {
				segment = segmentArr[i];
				fields = segment.split("\\|");
				if (fields[0].equals("OBX")) {
					int alarmCode = 0;
					int alarmID = Integer.parseInt(fields[5]);   //报警编号
					String[] IDs = fields[3].split("\\^"); // 数组第一个为模块ID，第二个为子ID(参数ID)
					int mID = Integer.parseInt(IDs[0]);
					int sID = Integer.parseInt(IDs[1]);


					// 构建报警码
					alarmCode = 1*(int)Math.pow(16,6) + mID*(int)Math.pow(16,4) + sID*(int)Math.pow(16,2) + alarmID;
					if(mID == 1 && sID == 72 ){
						System.out.println("ECG导连:"+alarmCode);
					}

					payloadList.add(DkssUtil.constructPayload(new byte[]{0x00,0x09},alarmCode));
				}
			}

		}
		return false;

	}

	private void flushBufferQueue() {
		// 把发送缓冲区中的数据传输完成
		bufferDataQueue.clear();
		while (bufferDataQueue.size() > 0) {
            System.out.println("发送监护仪数据:");
            DkssUtil.parsePacket(bufferDataQueue.get(0));
            DkssUtil.printByte(bufferDataQueue.get(0));
			byte[] ret = SocketUtil.deliveryDataToServer(remoteHost, remotePort, remoteReadTimeout,
					remoteConnectTimeout, bufferDataQueue.get(0));
			if(ret == null ){
				return;
			}
			System.out.println("监护仪接收到返回：");
			DkssUtil.printByte(ret);
			bufferDataQueue.remove(0);
		}
	}

	private boolean registDev(){

		// 注册设备
		byte[] registerPacket = DkssUtil.constructPacket(
				DkssUtil.DKSS_VERSION,DkssUtil.DKSS_CMD_REGISTER_DEV,4,
				DkssUtil.mergeByte(boxTypePayload,devIDPayload,devTypePayload,DkssUtil.getTimePayload()));
		DkssUtil.printByte(registerPacket);

		while (true && !exit) {
			System.out.println("注册监护仪");
			byte[] ret = SocketUtil.deliveryDataToServer(remoteHost, remotePort, remoteReadTimeout, remoteConnectTimeout,
					registerPacket);
			if(ret == null){
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				continue;
			}

			Log.i(TAG,"注册监护仪，服务器返回：");
			DkssUtil.printByte(ret);
			if(DkssUtil.parseReply(ret)){
				break;
			}
		}
		return !exit;
	}

	@Override
	public void run() {
		//注册
		if(!registDev()){
			Log.i(TAG,"pm Thread died");
			return;
		}

		char[] data = new char[1024];
		PrintWriter printWriter = null;
		BufferedReader bufferedReader = null;

		// 连接监护仪
		Socket socket = null;

		while((printWriter = SocketUtil.getPrintWriter(socket)) == null ||
				(bufferedReader = SocketUtil.getBufferedReader(socket,"gb2312")) == null){

			socket = SocketUtil.getSocket(pmrHost, pmrPort, pmrReadTimeout);
			if(exit){
				break;
			}
		}
		// 发送QRDQRF
		SocketUtil.sendToStream(QRFQRD, printWriter);

		while (!exit) {

			//Log.i(TAG,"准备读取监护仪数据");
			if (!SocketUtil.receiveFromStream(data, bufferedReader)) {
				Log.i(TAG,"监护仪数据读取失败");
				SocketUtil.closeStream(printWriter, bufferedReader);
				SocketUtil.closeSocket(socket);
				//重新连接监护仪
				while((printWriter = SocketUtil.getPrintWriter(socket)) == null ||
						(bufferedReader = SocketUtil.getBufferedReader(socket,"gb2312")) == null){

					socket = SocketUtil.getSocket(pmrHost, pmrPort, pmrReadTimeout);
					if(exit){
						break;
					}
				}
				SocketUtil.sendToStream(QRFQRD, printWriter);
				continue;
			}
			// 数据处理
			if(parseData(String.valueOf(data))){
				flushBufferQueue();  //发送缓冲数据
//				String dataString = "ffd10000000900082c18e001056d086001016f3d1b3d1b3d1b3d103d103d103d143d143d143d253d253d253d333d333d333d323d323d323d2c3d2c3d2c3d2f3d2f3d2f3d3c3d3c3d3c3d493d493d493d483d483d483d443d443d443d483d483d483d533d533d533d5e3d5e3d5e3d5e3d5e3d5e3d5c3d5c3d5c3d5f3d5f3d5f3d693d693d693d713d713d713d733d733d733d723d723d723d753d753d753d7d3d7d3d7d3e043e043e043e063e063e063e053e053e053e093e093e093e103e103e103e163e163e163e183e183e183e193e193e193e1b3e1b3e1b3e223e223e223e273e273e273e2a3e2a3e2a3e2a3e2a3e2a3e2d3e2d3e2d3e333e333e333e373e373e373e3a3e3a3e3a3e3a3e3a3e3a3e3d3e3d3e3d3e423e423e423e473e473e473f723f723f72417941794179431643164316c35c435c435c436d436d436d435343534353432843284328431243124312431f431f431f433043304330432643264326430a430a430a427b427b427b430343034303430f430f430f430643064306427042704270426342634263426942694269427142714271426942694269425742574257424c424c424c424f424f424f425442544254424f424f424f424042404240423642364236423942394239423c423c423c423542354235422a422a422a422142214221422342234223422442244224421f421f421f421542154215420d420d420d420d420d420d420e420e420e420942094209420142014201417b417b417b417941794179417a417a417a417641764176416e416e416e416841684168416841684168416641664166416341634163415c415c415c415841584158415741574157415541554155415241524152414c414c414c414741474147414741474147414541454145414141414141413d413d413d4130413041303f493f493f493d553d553d55bc543c543c54bc1c3c1c3c1c3c163c163c163c383c383c383c623c623c623c6c3c6c3c6c3c5c3c5c3c5c3c513c513c513c613c613c613c7c3c7c3c7c3d043d043d043c783c783c783c713c713c713d003d003d00fe000d0000000000010001140401fe16e4320003047f3fe4330003047f3fe3040067000c0004020402040204020402040204010401040104010401040104010401040104000400040004000400040004000400040004000400040004000400040004000400040004000400040004000400040004000400040004000400040004010401040104010401e102011800ff0000007f00007f00007f00007f00007f00007f00007f00007f00007f00007f00007f00007f00007f00007f00007f00007f00007f00007f00007f00007f00007f00007f00007f00007f00007f00007f00007f00007f00007f00007f00007f00007f00007f00007f00007f00007f00007f00007f00007f00007f00007f00007f00007f00007f00007f00007f00007f00007f00007f0000e20b00036f0101e503000a10010000803f763f763f";
//
//				parsePacketD1(DkssUtil.hexStringToByteArr(dataString));
			}

			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			SocketUtil.sendToStream(ACK,printWriter);
		}
		Log.i(TAG,"pmr thread died");
	}

}
