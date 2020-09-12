package com.dkss.medical.util;

import com.dkss.medical.util.DkssUtil;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;

public class PMUtil {

    //ff d0 00 08 00 fe 00 09 34 01 02 03 04 0b 32 33 50
    public static HashMap<String, Byte> parsePacketD0(byte[] data) {
        return null;
    }

    public static void parsePacketD1(byte[] data) {

    }

    public static void parsePacketD3_7(byte[] data) {

    }

    public static void parsePacketD4(byte[] data) {
        int offset = 19;
        byte[] temp = new byte[2];
        temp[0] = data[17];
        temp[1] = data[18];
        int packetLen = DkssUtil.byteToShort(temp, 0);
//	while(offset<data.length) {
//	    switch (key) {
//	    case value:
//
//		break;
//
//	    default:
//		break;
//	    }
//
//	}


    }

    /*
     * 	心律失常，上下行配置信息，与命令报文合并一起发出(命令
     */
    public static int parsePacketDA(byte[] data) {

        byte[] temp = new byte[2];
        temp[1] = data[7];
        int Len = data.length - DkssUtil.byteToShort(temp, 0)+8;



        temp[1] = (byte) ((data[9] << 7) | data[10]);
        temp[0] = (byte) (data[9] >> 1 & 0x03);
        short cmdNum = DkssUtil.byteToShort(temp, 0);
        temp[0] = data[9];
        temp[1] = data[10];
        switch (cmdNum) {
            case 512://200
                //System.out.println("从机收到了我的数据请求");
                DkssUtil.printByte(temp);
                return 200;
            case 513://201
                //System.out.println("请求重发的数据不存在");
                return 201;
            case 514://202
               // System.out.println("上传大气压力,需要回复");
                return 202;
            case 515://203
               // System.out.println("收入病人，更新病人信息，需要回复");
                respondMonitor("192.168.1.119",8002,temp);
                return 203;
            case 516://204
                //System.out.println("接触病人并待机，需要回复");
                respondMonitor("192.168.1.119",8002,temp);
                return 204;
            case 517://205
                //System.out.println("心律失常数据包，需要回复");
                respondMonitor("192.168.1.119",8002,temp);
                //TODO处理心律失常

                return 205;
            case 518://206
                //System.out.println("从机上传配置信息，需要回复");
                respondMonitor("192.168.1.119",8002,temp);
                parsePacketD4(data);
                return 206;
            default:
                //System.out.println("默认");
                DkssUtil.printByte(data);
                break;
        }
        return -1;
    }

    public static void respondMonitor(String ip,int port,byte[] cmd) {
        byte[] data = DkssUtil.mergeByte(new byte[] {(byte)0xff,(byte)0xda,(byte)0x7f,0x09,
                0x00,0x08,0x00,0x03,0x0a},cmd);
        sendUDP(ip, port, data);
    }

    public static void sendUDP(String ip,int port,byte[] data) {

        try {
            InetAddress addr = InetAddress.getByName(ip);
            DatagramPacket packet = new DatagramPacket(data,data.length,addr,port);
            DatagramSocket socket;
            socket = new DatagramSocket();
            socket.send(packet);
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    public static byte[] receiveUDP(int port, int soTimeout) {
        byte[] data = null;
        DatagramSocket socket = null;
        DatagramPacket packet = null;

        try {
            byte[] buf = new byte[4096];
            packet = new DatagramPacket(buf, buf.length);
            socket = new DatagramSocket(port);
            socket.setSoTimeout(soTimeout);
            socket.receive(packet);
            socket.close();
            int len = packet.getLength();
            data = new byte[len];
            System.arraycopy(buf, 0, data, 0, len);
        } catch (Exception e) {
            System.err.println("读取超时");
            socket.close();
        }
        return data;
    }

    public static int createPort() {
        int i = 0;
        DatagramSocket socket = null;
        for (i = 50000; i < 60000; i++) {
            try {
                socket = new DatagramSocket(i);
            } catch (Exception e) {

            }
            socket.close();
            return i;
        }

        return 0;

    }
    public static byte[] portToByte(int port) {

        byte[] ret = new byte[3];
        ret[2] = (byte) (port & 0x7f);
        ret[1] = (byte) ((port >> 7) & 0x7f);
        ret[0] = (byte) ((port >> 14) & 0x7f);
        return ret;
    }

}
