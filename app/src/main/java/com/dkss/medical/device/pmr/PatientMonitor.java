package com.dkss.medical.device.pmr;

import android.util.Log;

import com.dkss.medical.device.BasicDevice;
import com.dkss.medical.server.ServerInfo;
import com.dkss.medical.util.BufferQueue;
import com.dkss.medical.util.PMUtil;
import com.dkss.medical.util.DkssUtil;
import com.dkss.medical.util.SocketUtil;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

public class PatientMonitor extends BasicDevice {
    private boolean isAlive = false;   //监护仪状态是否在线
    private byte pMachineNum;
    private ServerInfo sInfo;
    private BufferQueue queue;
    private int pReadTimeout;

    public boolean init(Map<String,Object> cfgMap){

        try{
            HashMap<String,String> tempMap = (HashMap)cfgMap.get("server_host");
            String sIp = tempMap.get("host");
            int sPort = Integer.parseInt(tempMap.get("port"));
            int sReadTimeout = Integer.parseInt(tempMap.get("read_timeout"));
            int sConnectTimeout = Integer.parseInt(tempMap.get("connect_timeout"));

            tempMap = (HashMap<String, String>)cfgMap.get("pmr");
            pReadTimeout = Integer.parseInt(tempMap.get("read_timeout"));
            int bufQueueLen = Integer.parseInt(tempMap.get("buf_queue_len"));
            pMachineNum = Byte.parseByte(tempMap.get("machine_num"));

            if(sIp ==null){
                throw  new NumberFormatException();
            }

            sInfo = new ServerInfo(sIp,sPort,sReadTimeout,sConnectTimeout);
            queue = new BufferQueue(bufQueueLen);

        }catch (NumberFormatException e){
            System.err.println("pmr配置文件有误");
            return false;
        }
        return true;
    }

    @Override
    public void run() {

        ServerInfo info = new ServerInfo("47.107.85.10",20905,3000,5000);

        byte mMachineNum  = 0x00;
        String mIP = null;
        byte mProtocolVersion = 0x00;

        ListenBroadcast broadcast = new ListenBroadcast("255.255.255.255",(byte) 0x08);
        ListenMonitor monitor = new ListenMonitor();

        //1、创建线程，监听8002端口
        System.out.println("启动，监听8002端口");
        new Thread(broadcast).start();

        System.out.println("启动，监听51818端口");
        //2、创建线程，监听与监护仪协定的端口
        new Thread(monitor).start();

        //3、创建线程，广播自己的存在
        System.out.println("启动，广播自己存在");
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    byte[] data = new byte[] { (byte) 0xff, (byte) 0xd0, (byte) 0x7f, 0x09, 0x00, (byte) 0xfe, 0x00, 0x01,
                            0x34 };
//		String[] ipArr = InetAddress.getLocalHost().getHostAddress().split("//.");
//		int type = Integer.parseInt(ipArr[0]);

                    while (true) {
                       // System.out.println("广播---自己");
                        PMUtil.sendUDP("192.168.1.255", 8002, data);
                        Thread.sleep(1000);
                    }
                }catch (Exception e) {
                    System.err.println("广播异常");
                }
            }
        }).start();

        //4、主线程，监听IP地址等是否变化，并进行及时更新
        while(!broadcast.isData) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        monitor.isData = true;
    }


}
