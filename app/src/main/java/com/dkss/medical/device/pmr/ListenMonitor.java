package com.dkss.medical.device.pmr;

import android.util.Log;

import com.dkss.medical.device.BasicDevice;
import com.dkss.medical.server.ServerInfo;
import com.dkss.medical.util.PMUtil;
import com.dkss.medical.util.DkssUtil;
import com.dkss.medical.util.Payload;
import com.dkss.medical.util.Protocol;
import com.dkss.medical.util.SocketUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class ListenMonitor extends BasicDevice {

    private ArrayList<byte[]> bufferDataQueue = new ArrayList<>();

    public volatile boolean isData = false;

    private static  byte[] boxIDPayload = DkssUtil.constructPayload(Protocol.ID_BOX_ID,Protocol.boxId);
    private static  byte[] boxTypePayload = DkssUtil.constructPayload(Protocol.ID_BOX_TYPE,Protocol.BOX_TYPE);
    private static  byte[] devTypePayload = DkssUtil.constructPayload(Protocol.ID_DEV_TYPE,Protocol.P_TYPE);
    private static byte[] timePayload = null;


    @Override
    public void run() {
        int i = 0;

        int listenPort = PMUtil.createPort();
        byte[] portByteArr = PMUtil.portToByte(listenPort);
        while (true) {

            try {
                if (i >= 16) {
                    i = 0;
                }
                byte requestNum = (byte) (i << 3);
                i++;
                byte[] requestByte = new byte[] { (byte) 0xff, (byte) 0xda, (byte) 0x7f, 0x09, 0x00, 0x08, 0x00, 0x06,
                        0x02, 0x08, 0x01, portByteArr[0], portByteArr[1], portByteArr[2] };
                byte[] timeSynByte = DkssUtil.mergeByte(new byte[] {(byte) 0xff, (byte) 0xda, (byte) 0x7f, 0x09, 0x00, 0x08, 0x00,0x0c,
                        0x02,0x08,0x0d},getTimeSyn());
                byte[] cfgByte = new byte[] {(byte) 0xff, (byte) 0xda, (byte) 0x7f, 0x09, 0x00, 0x08, 0x00, 0x03,
                        0x02,0x08,0x04};

                requestByte[9] = requestNum;
                timeSynByte[9] = requestNum;
                cfgByte[9] = requestNum;

                byte[] data = PMUtil.receiveUDP(listenPort, 5000);
                if(data ==null ) {
                    System.out.println("发送数据请求包,端口:"+listenPort);
                    PMUtil.sendUDP("192.168.1.119", 8002, requestByte);
                    DkssUtil.printByte(requestByte);

                }
                if(isData && data ==null) {
                    System.out.println("发送时间同步");
                    PMUtil.sendUDP("192.168.1.119", 8002, timeSynByte);
                    DkssUtil.printByte(timeSynByte);
                    System.out.println("发送配置请求包");
                    PMUtil.sendUDP("192.168.1.119", 8002, cfgByte);
                    DkssUtil.printByte(cfgByte);
                }
                if(data !=null) {
                    System.out.println("接收到监听端口的数据");
                   // System.err.println("接收到的数据为");
                    parsePacketD1(data);
                }
            } catch (Exception e) {
                System.out.println("监听监护仪发生异常");
                e.printStackTrace();
            }

        }

    }
    public static byte[] getTimeSyn() {
        SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
        Date data = new Date(System.currentTimeMillis());
        int year = data.getYear();
        int month = data.getMonth();
        int day = data.getDay();
        int hour = data.getHours();
        int min = data.getMinutes();
        int sec = data.getSeconds();

        byte[] time = new byte[9];
        time[0] = (byte)(year>>7);
        time[1] = (byte)(year&0x7f);
        time[2] = (byte)(month);
        time[3] = (byte)(day);
        time[4] = (byte)(hour);
        time[5] = (byte)(min);
        time[6] = (byte)(sec);
        return time;

    }
    public  void parsePacketD1(byte[] data) {

        Payload payload = new Payload();
        timePayload = DkssUtil.getTimePayload();
        int packetLen = data[7]*128 +data[8]+9;
        int modLen = -1;   //各个模块的模块总长度
        int offset = 10;

        if(data.length!=packetLen){
            System.out.println("D1数据包长度检验不通过");
            return;
        }

        payload.add(boxTypePayload,boxIDPayload,devTypePayload,timePayload);

        while (offset<packetLen) {

            modLen = data[offset+2]*128+data[offset+3]+4;
            System.err.println(String.format("%02x",data[offset]));
            // ECG数据块
            if (data[offset] == (byte)0xe0) {

                if(!parseEcg(data,offset,modLen,payload)){
                    System.out.println("ECG解析false");
                    DkssUtil.printByte(payload.getData());
                    bufferDataQueue.add(DkssUtil.constructPacket(DkssUtil.DKSS_VERSION,DkssUtil.DKSS_CMD_SEND_DATA,payload.getNum(),payload.getData()));
                    payload.clear();
                    payload.add(boxTypePayload,boxIDPayload,devTypePayload,timePayload);
                    if(!parseEcg(data,offset,modLen,payload)){
                        System.err.println("ECG模块中的数据超出协议规定的数据包长度，需要修改协议");
                    }
                }
            }else if(data[offset] == (byte)0xe1){  //SpO2
                if(!parseSpO2(data,offset,modLen,payload)){
                    bufferDataQueue.add(DkssUtil.constructPacket(DkssUtil.DKSS_VERSION,DkssUtil.DKSS_CMD_SEND_DATA,payload.getNum(),payload.getData()));
                    payload.clear();
                    payload.add(boxTypePayload,boxIDPayload,devTypePayload,timePayload);
                    if(!parseSpO2(data,offset,modLen,payload)){
                        System.err.println("SpO2模块中的数据超出协议规定的数据包长度，需要修改协议");
                    }
                }
            }else if(data[offset] == (byte)0xe2){   //Pulse
                if(!parsePulse(data,offset,modLen,payload)){
                    System.out.println("Pulse解析false");
                    bufferDataQueue.add(DkssUtil.constructPacket(DkssUtil.DKSS_VERSION,DkssUtil.DKSS_CMD_SEND_DATA,payload.getNum(),payload.getData()));
                    payload.clear();
                    payload.add(boxTypePayload,boxIDPayload,devTypePayload,timePayload);
                    if(!parsePulse(data,offset,modLen,payload)){
                        System.err.println("Pulse模块中的数据超出协议规定的数据包长度，需要修改协议");
                    }
                }
            }else if(data[offset] == (byte)0xe3){  //RESP
                if(!parseResp(data,offset,modLen,payload)){
                    System.out.println("Resp解析false");
                    bufferDataQueue.add(DkssUtil.constructPacket(DkssUtil.DKSS_VERSION,DkssUtil.DKSS_CMD_SEND_DATA,payload.getNum(),payload.getData()));
                    payload.clear();
                    payload.add(boxTypePayload,boxIDPayload,devTypePayload,timePayload);
                    if(!parseResp(data,offset,modLen,payload)){
                        System.err.println("RESP模块中的数据超出协议规定的数据包长度，需要修改协议");
                    }
                }
            }else if(data[offset] == (byte)0xe4){   //Temp
                if(!parseTemp(data,offset,modLen,payload)){
                    System.out.println("Temp解析false");
                    bufferDataQueue.add(DkssUtil.constructPacket(DkssUtil.DKSS_VERSION,DkssUtil.DKSS_CMD_SEND_DATA,payload.getNum(),payload.getData()));
                    payload.clear();
                    payload.add(boxTypePayload,boxIDPayload,devTypePayload,timePayload);
                    if(!parseTemp(data,offset,modLen,payload)){
                        System.err.println("Temp模块中的数据超出协议规定的数据包长度，需要修改协议");
                    }
                }
            }else if(data[offset] == (byte)0xe5){   //NIBP

                if(!parseNIBP(data,offset,modLen,payload)){
                    System.out.println("NIBP解析false");
                    bufferDataQueue.add(DkssUtil.constructPacket(DkssUtil.DKSS_VERSION,DkssUtil.DKSS_CMD_SEND_DATA,payload.getNum(),payload.getData()));
                    payload.clear();
                    payload.add(boxTypePayload,boxIDPayload,devTypePayload,timePayload);
                    if(!parseNIBP(data,offset,modLen,payload)){
                        System.err.println("NIBP模块中的数据超出协议规定的数据包长度，需要修改协议");
                    }
                }
            }else if(data[offset] == (byte)0xe6){   //IBP

                if(!parseIBP(data,offset,modLen,payload)){
                    System.out.println("IBP解析false");
                    bufferDataQueue.add(DkssUtil.constructPacket(DkssUtil.DKSS_VERSION,DkssUtil.DKSS_CMD_SEND_DATA,payload.getNum(),payload.getData()));
                    payload.clear();
                    payload.add(boxTypePayload,boxIDPayload,devTypePayload,timePayload);
                    if(!parseIBP(data,offset,modLen,payload)){
                        System.err.println("IBP模块中的数据超出协议规定的数据包长度，需要修改协议");
                    }
                }
            }else if(data[offset] == (byte)0xe7){

            }else if(data[offset] == (byte)0xe8){

            }else if(data[offset] == (byte)0xe9){

            }else if(data[offset] == (byte)0xea){

            }else if(data[offset] == (byte)0xeb){

            }else if(data[offset] == (byte)0xfc){

            }else if(data[offset] == (byte)0xfd){

            }else if(data[offset] == (byte)0xfe){
                modLen = data[offset+1]*128+data[offset+2]+3;
                System.out.println("出现FE模块");
            }

            offset +=modLen;
        }
        byte[] pData = payload.getData();
        if(pData==null){
            System.err.println("剩余0");
        }else {
            bufferDataQueue.add(DkssUtil.constructPacket(DkssUtil.DKSS_VERSION,DkssUtil.DKSS_CMD_SEND_DATA,payload.getNum(),payload.getData()));
        }

        //System.out.println("缓存数量:"+bufferDataQueue.size()+"; 缓存长度");

        while (bufferDataQueue.size() > 0) {

//            if(bufferDataQueue.get(0).length==1984){
//                byte[] a = new byte[1300];
//                int l = data.length-1300;
//                byte[] b= new byte[l];
//                System.arraycopy(data,0,a,0,1300);
//                System.arraycopy(data,1300,b,0,l);
//                DkssUtil.printByte(a);
//                DkssUtil.printByte(b);
//
//            }
           // System.out.println("准备发送监护仪数据,数据长度:"+bufferDataQueue.get(0).length);
            ServerInfo info = new ServerInfo("47.107.85.10",20905,3000,5000);
            byte[] ret = SocketUtil.deliveryDataToServer(info, bufferDataQueue.get(0));
            if(ret == null ){
                System.out.println("监护仪返回null，重新发送数据");
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }

            bufferDataQueue.remove(0);
        }

    }

    private boolean parseEcg(byte[] data, int offset, int modLen, Payload payload){

        ArrayList<Short> ecgShortListI = new ArrayList<>();
        ArrayList<Short> ecgShortListII = new ArrayList<>();
        ArrayList<Short> ecgShortListIII = new ArrayList<>();
        ArrayList<Short> ecgShortListAVR = new ArrayList<>();
        ArrayList<Short> ecgShortListAVL = new ArrayList<>();
        ArrayList<Short> ecgShortListAVF = new ArrayList<>();
        ArrayList<Short> ecgShortListV4 = new ArrayList<>();
        ArrayList<Short> ecgShortListV1 = new ArrayList<>();
        ArrayList<Short> ecgShortListV2 = new ArrayList<>();
        ArrayList<Short> ecgShortListV3 = new ArrayList<>();
        ArrayList<Short> ecgShortListV5 = new ArrayList<>();
        ArrayList<Short> ecgShortListV6 = new ArrayList<>();

        //判断几导
        int lead = 0;
        int waveSize = 0;
        int fixedLen = 9;

        //处理固定参数的数据
        short ecgHr =  (short)((data[offset+6]&0x03)*128+data[offset+8]);
        byte[] test = DkssUtil.constructPayload(new byte[]{0x10,0x20},ecgHr);
//        if(ecgHr !=511 ){
//            if(payload.add(DkssUtil.constructPayload(new byte[]{0x10,0x20},ecgHr))){
//                return false;
//            }
//        }

        switch ((data[offset + 5] & 0x03)){
            case 0x00:
                lead = 5;
                waveSize = (modLen-fixedLen)/6;
                break;
            case 0x02:
                lead = 3;
                waveSize = (modLen-fixedLen)/3;
                System.out.println("还没有做3导");
                break;
            case 0x01:
                lead = 12;
                waveSize = (modLen-fixedLen)/16;
                break;
        }

        byte[] ecgByteArrI = new byte[waveSize];
        byte[] ecgByteArrII = new byte[waveSize];
        byte[] ecgByteArrIII = new byte[waveSize];
        byte[] ecgByteArrAVR = new byte[waveSize];
        byte[] ecgByteArrAVL = new byte[waveSize];
        byte[] ecgByteArrAVF = new byte[waveSize];
        byte[] ecgByteArrV4 = new byte[waveSize];
        byte[] ecgByteArrV1 = new byte[waveSize];
        byte[] ecgByteArrV2 = new byte[waveSize];
        byte[] ecgByteArrV3 = new byte[waveSize];
        byte[] ecgByteArrV5 = new byte[waveSize];
        byte[] ecgByteArrV6 = new byte[waveSize];

        int i = 0;
        boolean ret = false;

        switch (lead) {
            case 3:
                System.out.println("还没有做3导");
                break;
            case 5:
                for (i = 0; i < (modLen-fixedLen)/6 ; i++) {
                    short dataII = (short)((data[offset+fixedLen+i*6]&0x7f)*128 + (data[offset+fixedLen+i+1]&0x7f));
                    short dataI = (short)((data[offset+fixedLen+i*6+2]&0x7f)*128 + (data[offset+fixedLen+i+3]&0x7f));
                    short dataV4 = (short)((data[offset+fixedLen+i*6+4]&0x7f)*128 + (data[offset+fixedLen+i+5]&0x7f));

                    short dataIII  = (short)(dataII-dataI +8192);
                    short dataAVR = (short)(8192*2-(dataI+dataII)/2);
                    short dataAVL = (short)((dataI-dataIII)/2 +8192);
                    short dataAVF = (short)((dataII+dataIII)/2);

                    ecgShortListI.add(dataI);
                    ecgShortListII.add(dataII);
                    ecgShortListIII.add(dataIII);
                    ecgShortListAVR.add(dataAVR);
                    ecgShortListAVL.add(dataAVL);
                    ecgShortListAVF.add(dataAVF);
                    ecgShortListV4.add(dataV4);
                }

                for(int j=0;j<waveSize;j++){
                    byte wTemp = 0;
                    wTemp = (byte)(ecgShortListI.get(j)/64);
                    ecgByteArrI[j] = wTemp;
                    wTemp = (byte)(ecgShortListII.get(j)/64);
                    ecgByteArrII[j] = wTemp;
                    wTemp = (byte)(ecgShortListIII.get(j)/64);
                    ecgByteArrIII[j] = wTemp;
                    wTemp = (byte)(ecgShortListAVR.get(j)/64);
                    ecgByteArrAVR[j] = wTemp;
                    wTemp = (byte)(ecgShortListAVL.get(j)/64);
                    ecgByteArrAVL[j] = wTemp;
                    wTemp = (byte)(ecgShortListAVF.get(j)/64);
                    ecgByteArrAVF[j] = wTemp;
                    wTemp = (byte)(ecgShortListV4.get(j)/64);
                    ecgByteArrV4[j] = wTemp;
                }
                //加入payload
                ret =  payload.add(
                        test,
                        DkssUtil.constructPayload(new byte[]{0x10,0x36},(short)waveSize),
                        DkssUtil.constructPayload(new byte[]{0x10,0x23},ecgByteArrI),
                        DkssUtil.constructPayload(new byte[]{0x10,0x24},ecgByteArrII),
                        DkssUtil.constructPayload(new byte[]{0x10,0x25},ecgByteArrIII),
                        DkssUtil.constructPayload(new byte[]{0x10,0x26},ecgByteArrAVR),
                        DkssUtil.constructPayload(new byte[]{0x10,0x27},ecgByteArrAVL),
                        DkssUtil.constructPayload(new byte[]{0x10,0x28},ecgByteArrAVF),
                        DkssUtil.constructPayload(new byte[]{0x10,0x29},ecgByteArrV4)
                );

                break;
            case 12:
                for (i = 0; i < modLen-fixedLen ; i = i + 16) {
                    short dataII = (short)((data[offset+fixedLen+i]&0x7f)*128 + (data[offset+fixedLen+i+1]&0x7f));
                    short dataI = (short)((data[offset+fixedLen+i+2]&0x7f)*128 + (data[offset+fixedLen+i+3]&0x7f));
                    short dataV4 = (short)((data[offset+fixedLen+i+4]&0x7f)*128 + (data[offset+fixedLen+i+5]&0x7f));
                    short dataV1 = (short)((data[offset+fixedLen+i+6]&0x7f)*128 + (data[offset+fixedLen+i+7]&0x7f));
                    short dataV2 = (short)((data[offset+fixedLen+i+8]&0x7f)*128 + (data[offset+fixedLen+i+9]&0x7f));
                    short dataV3 = (short)((data[offset+fixedLen+i+10]&0x7f)*128 + (data[offset+fixedLen+i+11]&0x7f));
                    short dataV5 = (short)((data[offset+fixedLen+i+12]&0x7f)*128 + (data[offset+fixedLen+i+13]&0x7f));
                    short dataV6 = (short)((data[offset+fixedLen+i+14]&0x7f)*128 + (data[offset+fixedLen+i+15]&0x7f));

                    short dataIII  = (short)(dataII-dataI +8192);
                    short dataAVR = (short)(8192*2-(dataI+dataII)/2);
                    short dataAVL = (short)((dataI-dataIII)/2 +8192);
                    short dataAVF = (short)((dataII+dataIII)/2);

                    ecgShortListI.add(dataI);
                    ecgShortListII.add(dataII);
                    ecgShortListIII.add(dataIII);
                    ecgShortListAVR.add(dataAVR);
                    ecgShortListAVL.add(dataAVL);
                    ecgShortListAVF.add(dataAVF);
                    ecgShortListV4.add(dataV4);
                    ecgShortListV1.add(dataV1);
                    ecgShortListV2.add(dataV2);
                    ecgShortListV3.add(dataV3);
                    ecgShortListV5.add(dataV5);
                    ecgShortListV6.add(dataV6);
                }

                for(int j=0;j<waveSize;j++){
                    byte wTemp = 0;
                    wTemp = (byte)(ecgShortListI.get(j)/64);
                    ecgByteArrI[j] = wTemp;
                    wTemp = (byte)(ecgShortListII.get(j)/64);
                    ecgByteArrII[j] = wTemp;
                    wTemp = (byte)(ecgShortListIII.get(j)/64);
                    ecgByteArrIII[j] = wTemp;
                    wTemp = (byte)(ecgShortListAVR.get(j)/64);
                    ecgByteArrAVR[j] = wTemp;
                    wTemp = (byte)(ecgShortListAVL.get(j)/64);
                    ecgByteArrAVL[j] = wTemp;
                    wTemp = (byte)(ecgShortListAVF.get(j)/64);
                    ecgByteArrAVF[j] = wTemp;
                    wTemp = (byte)(ecgShortListV4.get(j)/64);
                    ecgByteArrV4[j] = wTemp;
                    wTemp = (byte)(ecgShortListV1.get(j)/64);
                    ecgByteArrV1[j] = wTemp;
                    wTemp = (byte)(ecgShortListV2.get(j)/64);
                    ecgByteArrV2[j] = wTemp;
                    wTemp = (byte)(ecgShortListV3.get(j)/64);
                    ecgByteArrV3[j] = wTemp;
                    wTemp = (byte)(ecgShortListV5.get(j)/64);
                    ecgByteArrV5[j] = wTemp;
                    wTemp = (byte)(ecgShortListV6.get(j)/64);
                    ecgByteArrV6[j] = wTemp;
                }
                //加入payload
                  ret =  payload.add(
                          test,
                        DkssUtil.constructPayload(new byte[]{0x10,0x36},(short)waveSize),
                        DkssUtil.constructPayload(new byte[]{0x10,0x23},ecgByteArrI),
                        DkssUtil.constructPayload(new byte[]{0x10,0x24},ecgByteArrII),
                        DkssUtil.constructPayload(new byte[]{0x10,0x25},ecgByteArrIII),
                        DkssUtil.constructPayload(new byte[]{0x10,0x26},ecgByteArrAVR),
                        DkssUtil.constructPayload(new byte[]{0x10,0x27},ecgByteArrAVL),
                        DkssUtil.constructPayload(new byte[]{0x10,0x28},ecgByteArrAVF),
                        DkssUtil.constructPayload(new byte[]{0x10,0x29},ecgByteArrV4),
                        DkssUtil.constructPayload(new byte[]{0x10,0x30},ecgByteArrV1),
                        DkssUtil.constructPayload(new byte[]{0x10,0x31},ecgByteArrV2),
                        DkssUtil.constructPayload(new byte[]{0x10,0x32},ecgByteArrV3),
                        DkssUtil.constructPayload(new byte[]{0x10,0x33},ecgByteArrV5),
                        DkssUtil.constructPayload(new byte[]{0x10,0x34},ecgByteArrV6)
                        );

                break;
            default:
                System.err.println("错误的导联");
                break;
        }

        return ret;
    }

    private boolean parseSpO2(byte[] data,int offset,int modLen,Payload payload){
        int fixedLen = 9;
        short spo2Value = data[offset+5];
        //spo2Value = spo2Value == (short)65535?0:spo2Value;

        //处理波形数据
        int waveSize = (modLen-fixedLen)/3;
        byte[] spo2ByteArrBO = new byte[waveSize];

        int i=0;
        if(waveSize!=(modLen-fixedLen)/3){
            System.out.println("Spo2 波形数据长度有错 waveSize="+waveSize+";"+"数据长度:"+(modLen-fixedLen)/3);
        }

        for(i=0;i<waveSize;i++){
            spo2ByteArrBO[i] = (byte)((data[offset+fixedLen+i*3+2]>>2)*128+data[offset+fixedLen+i*3]);
        }

        boolean ret =payload.add(
                DkssUtil.constructPayload(new byte[]{0x10,0x78},spo2ByteArrBO),
                DkssUtil.constructPayload(new byte[]{0x10,0x70},spo2Value)
                );
        return ret;
    }

    private boolean parsePulse(byte[] data, int offset, int modLen, Payload payload){

        short pulsePR = (short)(data[offset+5]*128+data[offset+4]);
//        if(pulsePR == (short)65535 || ((data[offset+5]&0x04)==0x04)){
//            pulsePR =0;
//        }
        boolean ret = payload.add(DkssUtil.constructPayload(new byte[]{0x11,0x70},pulsePR));
        return ret;
    }

    private boolean parseResp(byte[] data, int offset, int modLen, Payload payload){
        int fixedLen = 7;
        int waveSize = (modLen-fixedLen)/2;
        ArrayList<Short> respShortList = new ArrayList<>();
        byte[] respByteArr = new byte[waveSize];
        short respRR = data[offset+4];
//        if((data[offset+5]&0x01)==0 && (data[offset+4] != (byte)0xff)){
//            respRR = data[offset+4];
//        }

        for(int i=0;i<waveSize;i++){
            respShortList.add((short)((data[offset+fixedLen+i*2]>>2)*128+data[offset+fixedLen+i*2+1]));
        }
        for(int i=0;i<waveSize;i++){
            respByteArr[i] = (byte)(respShortList.get(i)/2);
        }
        boolean ret = payload.add(
                DkssUtil.constructPayload(Protocol.ID_RESP_RR,respRR),
                DkssUtil.constructPayload(new byte[]{0x10,(byte)0x92},respByteArr));
        return ret;
    }

    private boolean parseTemp(byte[] data,int offset,int modLen,Payload payload){

        float tempValue = (float)((data[offset+6]*128+data[offset+5])*0.1);
        //判断是否有体温，当体温所对应的位全为1时，表示无体温值
//        if(data[offset+5] ==(byte)0x7f && data[offset+6] == (byte)0x3f){
//            tempValue = 0;
//        }

        byte[][] id = new byte[][]{
                {0x12,0x30},
                {0x12,0x33},
                {0x12,0x36},
                {0x12,0x39},
                {0x12,0x42},
                {0x12,0x35},
                {0x12,0x48},
                {0x12,0x51}
        } ;
        int index = -1;
        switch (data[offset+1]){
            case 50: index = 0;break;
            case 51: index = 1;break;
            case 52: index = 2;break;
            case 53: index = 3;break;
            case 54: index = 4;break;
            case 55: index = 5;break;
            case 56: index = 6;break;
            case 57: index = 7;break;
        }

        boolean ret = payload.add(DkssUtil.constructPayload(id[index],tempValue));
        return ret;
    }

    private boolean parseNIBP(byte[] data,int offset,int modLen,Payload payload){
        if((data[offset+5]&0xf3) != 1){
            return true;
        }

        short nibpSys = (short)((data[offset+9]*128+data[offset+8])*0.1);
        short nibpMean = (short)((data[offset+11]*128+data[offset+10])*0.1);
        short nibpDia = (short)((data[offset+13]*128+data[offset+12])*0.1);
//        if(nibpSys==793 && nibpMean==818 && nibpDia ==818){
//            nibpSys = 0;
//            nibpMean = 0;
//            nibpDia = 0;
//        }

        boolean ret = payload.add(
                DkssUtil.constructPayload(new byte[]{0x10,(byte)0x80},nibpSys),
                DkssUtil.constructPayload(new byte[]{0x10,(byte)0x81},nibpMean),
                DkssUtil.constructPayload(new byte[]{0x10,(byte)0x82},nibpDia));

        return ret;
    }

    private boolean parseIBP(byte[] data,int offset,int modLen,Payload payload){
        //                                    P1           P2               ART               CVP               PA           RAP        LAP         ICP
        byte[][] sysID  = new byte[][]{{0x11,(byte)0x80},{0x11,(byte)0x85},{0x11,(byte)0x90},{0x11,(byte)0x95},{0x12,0x00},{0x12,0x05},{0x12,0x10},{0x12,0x15}} ;
        byte[][] meanID = new byte[][]{{0x11,(byte)0x81},{0x11,(byte)0x86},{0x11,(byte)0x91},{0x11,(byte)0x96},{0x12,0x01},{0x12,0x06},{0x12,0x11},{0x12,0x16}} ;
        byte[][] diaID  = new byte[][]{{0x11,(byte)0x82},{0x11,(byte)0x87},{0x11,(byte)0x92},{0x11,(byte)0x97},{0x12,0x02},{0x12,0x07},{0x12,0x12},{0x12,0x17}} ;
        byte[][] prID   = new byte[][]{{0x11,(byte)0x83},{0x11,(byte)0x88},{0x11,(byte)0x93},{0x11,(byte)0x98},{0x12,0x03},{0x12,0x08},{0x12,0x13},{0x12,0x18}} ;
        byte[][] waveID = new byte[][]{{0x11,(byte)0x84},{0x11,(byte)0x89},{0x11,(byte)0x94},{0x11,(byte)0x99},{0x12,0x04},{0x12,0x09},{0x12,0x14},{0x12,0x19}} ;
        int index = -1;
        int fixedLen = 11;
        int waveSize = (modLen-fixedLen)/2;
        byte[] ibpWaveByteArr = new byte[waveSize];
        short[] ibpWaveShortArr = new short[waveSize];
        switch (data[offset+1]){
            case 20:
                index = 0;
                break;
            case 21:
                index = 1;
                break;
            case 22:
                index = 2;
                break;
            case 23:
                index = 3;
                break;
            case 24:
                index = 4;
                break;
            case 25:
                index = 5;
            case 26:
                index = 6;
                break;
            case 27:
                index = 7;
                break;
        }

        short ibpSys = (short)((data[offset + 6] * 128 + data[offset + 5])*0.1-100);
        short ibpMean = (short)((data[offset + 8] * 128 + data[offset + 7])*0.1-100);
        short ibpDia = (short)((data[offset + 10] * 128 + data[offset + 9])*0.1-100);

        for(int i=0;i<waveSize;i++){
            ibpWaveShortArr[i] = (short)(data[offset+fixedLen+i*2+1]*128+data[offset+fixedLen+i*2]);
            ibpWaveByteArr[i] = (byte)(ibpWaveShortArr[i]>>5);
        }

        boolean ret = payload.add(
                DkssUtil.constructPayload(sysID[index],ibpSys),
                DkssUtil.constructPayload(meanID[index],ibpMean),
                DkssUtil.constructPayload(diaID[index],ibpDia),
                DkssUtil.constructPayload(waveID[index],ibpWaveByteArr)
                );

        return ret;
    }

    private byte[] parseCO2(byte[] data,int offset,int modLen){
        return null;
    }

    private byte[] parseO2(byte[] data,int offset,int modLen){
        return null;
    }

    private byte[] parseN2O(byte[] data,int offset,int modLen){
        return null;
    }

    private byte[] parseAA(byte[] data,int offset,int modLen){
        return null;
    }

    private byte[] parseICG(byte[] data,int offset,int modLen){
        return null;
    }

    private byte[] parseFC(byte[] data,int offset,int modLen){
        return null;
    }

    private byte[] parseFD(byte[] data,int offset,int modLen){
        return null;
    }

    private byte[] parseFE(byte[] data,int offset,int modLen){
        return null;
    }
}
