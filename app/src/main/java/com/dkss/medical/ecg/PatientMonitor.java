package com.dkss.medical.ecg;

import android.util.Log;

import com.dkss.medical.util.DkssUtil;
import com.dkss.medical.util.SocketUtil;

public class PatientMonitor implements Runnable{
    private boolean isAlive = false;   //监护仪状态是否在线
    private byte cMachineNum  = 0x09;

    private boolean registDev(){

        // 注册设备
        byte[] boxIDPayload = DkssUtil.constructPayload(new byte[] { 0x00, 0x01 },"c3653127-08b3-488d-9b80-77b7e6082910");
        byte[] boxTypePayload = DkssUtil.constructPayload(new byte[] {0x00,0x04},"medicalbox");
        byte[] devTypePayload = DkssUtil.constructPayload(new byte[]{0x00,0x05},"pmr");
        byte[] registerPacket = DkssUtil.constructPacket(
                DkssUtil.DKSS_VERSION,DkssUtil.DKSS_CMD_REGISTER_DEV,4,
                DkssUtil.mergeByte(boxTypePayload,boxIDPayload,devTypePayload,DkssUtil.getTimePayload()));
        DkssUtil.printByte(registerPacket);

        while (true ) {
            System.out.println("注册监护仪----");

            byte[] ret = SocketUtil.deliveryDataToServer("47.107.85.10",20905,3000,5000,
                    registerPacket);
            if(ret == null){
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }

            Log.i("p","注册监护仪，服务器返回：");
            DkssUtil.printByte(ret);
            if(DkssUtil.parseReply(ret)){
                break;
            }
        }
        return true;
    }
    @Override
    public void run() {
        registDev();
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
