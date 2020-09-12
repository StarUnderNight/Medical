package com.dkss.medical.device.rer;

import android.util.Log;

import com.dkss.medical.util.DkssUtil;
import com.dkss.medical.util.SocketUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Thread.sleep;

public class Respirator  implements  Runnable {
    public volatile boolean exit = false;

    // 定义当前线程所处理的Socket
    private Socket socket = null;


    private String remoteHost;
    private int remotePort;
    private int remoteReadTimeout;
    private int remoteConnectTimeout;
    // 该线程所处理的Socket所对应的输入流
    private ArrayList<byte[]> bufferDataQueue; // 缓存，保存发送服务器失败的数据队列
    private int bufferDataQueueLen;

    private PrintWriter printWriter;
    private BufferedReader bufferedReader;

    public static HashMap<String, byte[]> dataIDMap; // 标识

    private byte[] boxIDPayload; // 盒子ID
    private byte[] boxTypePayload;   //盒子类型
    private byte[] devTypePayload;   //设备类型

    public Respirator(Socket socket) throws IOException {
        this.socket = socket;
        this.dataIDMap = new HashMap<>();
        this.printWriter = SocketUtil.getPrintWriter(socket);
        this.bufferedReader = SocketUtil.getBufferedReader(socket,"utf-8");
        this.bufferDataQueue = new ArrayList<>();
    }

    public boolean init(Map<String, Object> commonCfgMap, Map<String, Object> rerIDMap){

        //标识
        HashMap<String,String> tempMap = (HashMap)rerIDMap.get("dkss_rer_id");
        byte[] value = null;
        for (String key : tempMap.keySet()) {
            value = DkssUtil.hexStringToByteArr(tempMap.get(key));
            if (value == null) {
                System.err.println("配置文件出错，错误的值字段");
                return false;
            }
            this.dataIDMap.put(key, value);
        }

        // 呼吸机设备设置
        tempMap = (HashMap) commonCfgMap.get("respirator");
        this.devTypePayload = DkssUtil.constructPayload(new byte[]{0x00,0x05},tempMap.get("dev_type"));
        this.bufferDataQueueLen = Integer.parseInt(tempMap.get("buffer_data_queue_len"));


        //远程服务器配置
        tempMap = (HashMap) commonCfgMap.get("remote_host");
        this.remoteHost = tempMap.get("host");
        this.remotePort = Integer.parseInt(tempMap.get("port"));
        this.remoteReadTimeout = Integer.parseInt(tempMap.get("read_timeout"));
        this.remoteConnectTimeout = Integer.parseInt(tempMap.get("connect_timeout"));

        tempMap = (HashMap) commonCfgMap.get("box");
        this.boxIDPayload = DkssUtil.constructPayload(new byte[] { 0x00, 0x01 }, tempMap.get("box_id"));
        this.boxTypePayload = DkssUtil.constructPayload(new byte[] {0x00,0x04}, tempMap.get("box_type"));

        return true;

    }

    // 定义读取客户端数据的方法
    private String readFromClient () {
        try {
//            System.out.println("开始读取数据");
            return bufferedReader.readLine();
        }
        // 如果捕捉到异常，表明该Socket对应的客户端已经关闭
        catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("返回null");
        return null;
    }
    //
    /***
     * 发送缓冲队列数据
     */
    private void flushBufferQueue() {
        // 把发送缓冲区中的数据传输完成
        while (bufferDataQueue.size() > 0) {
            System.out.println("准备发送呼吸机数据为：");
            DkssUtil.parsePacket(bufferDataQueue.get(0));
            byte[] ret = SocketUtil.deliveryDataToServer(remoteHost, remotePort, remoteReadTimeout,
                    remoteConnectTimeout, bufferDataQueue.get(0));
            if(ret == null){
                return;
            }
            System.out.println("呼吸机接收到：");
            DkssUtil.printByte(ret);
            bufferDataQueue.remove(0);
        }
    }

    /**
     * 注册设备
     */
    private boolean registDev(){

        // 注册设备
        byte[] registerPacket = DkssUtil.constructPacket(
                DkssUtil.DKSS_VERSION,DkssUtil.DKSS_CMD_REGISTER_DEV,4,
                DkssUtil.mergeByte(boxTypePayload,boxIDPayload,devTypePayload,DkssUtil.getTimePayload()));
        DkssUtil.printByte(registerPacket);

        while (true && !exit) {
            byte[] ret = SocketUtil.deliveryDataToServer(remoteHost, remotePort, remoteReadTimeout, remoteConnectTimeout,
                    registerPacket);

            try {
                System.out.println("接收注册返回为：");
                if(ret == null){
                    Thread.sleep(5000);
                    continue;
                }
                DkssUtil.printByte(ret);
                if(DkssUtil.parseReply(ret)){
                    break;
                }

            } catch (InterruptedException e) {
                System.err.println("连接远程服务器失败");
            }
        }
        return !exit;
    }



    public void run() {
        //注册设备
        if(!registDev()){
            Log.i("service","regist thread died");
            return ;
        }

        try{
            System.out.println("enter");
            printWriter.write(RespiratorData.CMD_An);   //心跳包
            printWriter.flush();
            //InputStream is = s.getInputStream();
            //byte[] bf = new byte[1024];
            //int length = is.read(bf);
            //得到和解析呼吸机数据
            ArrayList<byte[]> arrayListMk = null;
            ArrayList<byte[]> arrayListMi = null;
            ArrayList<byte[]> arrayListMj = null;
            ArrayList<byte[]> arrayListMo = null;
            ArrayList<byte[]> arrayListMn = null;
            ArrayList<byte[]> arrayListMh = null;

            Boolean flagMh = false;
            Boolean flagMi = false;
            Boolean flagMj = false;
            Boolean flagMk = false;
            Boolean flagMn = false;
            Boolean flagMo = false;
            String content = null;
            System.out.println("ready to go into while");
            int a = 0;
            //不断获取呼吸机数据
            while ((content = readFromClient()) != null && !exit) {
//                System.out.println("contet 数据：" + new String(content.getBytes()));

//                System.out.println("循环次数" + a++);
                try {
                    if (!flagMh) {
                         printWriter.write(RespiratorData.CMD_Ah);
                        printWriter.flush();
                        if ((arrayListMh = RespiratorData.data_Mh(content.getBytes())) != null) {
//                            System.out.println("got Mh");
                            flagMh = true;
                        }
                    }
                    if (!flagMk) {
                        printWriter.write(RespiratorData.CMD_An);
                        printWriter.flush();
                        if ((arrayListMk =RespiratorData.data_Mk(content.getBytes())) != null) {
                            flagMk = true;
//                            System.out.println("got Mk");
                        }
                    }
                    if (!flagMi) {
                        printWriter.write(RespiratorData.CMD_Ai);
                        printWriter.flush();
                        if ((arrayListMi =RespiratorData.data_Mi(content.getBytes())) != null) {
                            flagMi = true;
//                            System.out.println("got Mi");
                        }
                    }
                    if (!flagMo) {
                        printWriter.write(RespiratorData.CMD_An);
                        printWriter.flush();
                        if ((arrayListMo =RespiratorData.data_Mo(content.getBytes())) != null) {
                            flagMo = true;
//                            System.out.println("got Mo");
                        }
                    }
                    if (!flagMj) {
                        printWriter.write(RespiratorData.CMD_Aj);
                        printWriter.flush();
                        if ((arrayListMj =RespiratorData.data_Mj(content.getBytes()) )!= null) {
                            flagMj = true;
//                            System.out.println("got Mj");
                        }
                    }
                    if (!flagMn) {
                        printWriter.write(RespiratorData.CMD_An);
                        printWriter.flush();
                        if ((arrayListMn =RespiratorData.data_Mn(content.getBytes())) != null) {
                            flagMn = true;
//                            System.out.println("got Mn");
                        }
                    }
                } catch (Exception e) {
                    System.err.println("解析异常");
                }

//                System.out.println(flagMh+":"+flagMi+":"+flagMj+":"+flagMo+":"+flagMk+":"+flagMh);

                if ((flagMh && flagMi && flagMj && flagMk && flagMn && flagMo)) {

                    System.out.println("Done");
                    //构建包，准备发送
                    byte[] dataMk = DkssUtil.mergeByte(arrayListMk);
//                    System.out.println(" Mk = " + new String(dataMk)+"\n" + RespiratorData.bytes2hex(dataMk));
                    byte[] dataMi = DkssUtil.mergeByte(arrayListMi);
//                    System.out.println(" Mi = " + new String(dataMi)+"\n" + RespiratorData.bytes2hex(dataMi));
                    byte[] dataMo = DkssUtil.mergeByte(arrayListMo);
//                    System.out.println(" Mo = " + new String(dataMo)+"\n" + RespiratorData.bytes2hex(dataMo));
                    byte[] dataMj = DkssUtil.mergeByte(arrayListMj);
//                    System.out.println(" Mj = " + new String(dataMj)+"\n" + RespiratorData.bytes2hex(dataMj));
                    byte[] dataMn = DkssUtil.mergeByte(arrayListMn);
//                    System.out.println(" Mn = " + new String(dataMn)+"\n" + RespiratorData.bytes2hex(dataMn));
                    byte[] dataMh = DkssUtil.mergeByte(arrayListMh);
//                    System.out.println(" Mh = " + new String(dataMh)+"\n" + RespiratorData.bytes2hex(dataMh));

                    int data_lenth = arrayListMh.size() + arrayListMi.size() + arrayListMj.size() + arrayListMk.size() + arrayListMn.size() + arrayListMo.size();

                    byte[] data = DkssUtil.mergeByte(boxTypePayload,boxIDPayload,devTypePayload,DkssUtil.getTimePayload(),
                            dataMh, dataMi, dataMj, dataMk, dataMn, dataMo);
                    byte[] dataPacket = DkssUtil.constructPacket(DkssUtil.DKSS_VERSION, DkssUtil.DKSS_CMD_SEND_DATA, data_lenth, data);


                    if (bufferDataQueue.size() >= bufferDataQueueLen) {
                        bufferDataQueue.remove(0);
                    }
                    bufferDataQueue.add(dataPacket);

                    arrayListMk.clear();
                    arrayListMi .clear();
                    arrayListMj .clear();
                    arrayListMo .clear();
                    arrayListMn .clear();
                    arrayListMh .clear();
                    //注册设备

                    //发送包
                    flushBufferQueue();  //清除发送缓冲区

                    //休眠5秒，在读取发送
                    sleep(5000);
                    flagMh = false;
                    flagMi = false;
                    flagMj = false;
                    flagMk = false;
                    flagMn = false;
                    flagMo = false;
                    a=0;

                }



            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i("service","rs thread died");
    }
}
