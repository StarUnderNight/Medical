package com.dkss.medical.util;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;


public class DkssUtil {

    public static final byte[] DKSS_STRING = new byte[] {0x01};
    public static final byte[] DKSS_SHORT = new byte[] {0x02};
    public static final byte[] DKSS_INT = new byte[]{0x03};
    public static final byte[] DKSS_FLOAT = new byte[] {0x04};
    public static final byte[] DKSS_UCHAR = new byte[] {0x05};
    public static final byte[] DKSS_UINT64 = new byte[] {0x06};
    public static final byte[] DKSS_BYTE_ARR = new byte[]{0x07};

    public static final byte[] DKSS_LEN_1 = new byte[] {0x00,0x01};
    public static final byte[] DKSS_LEN_2 = new byte[] {0x00,0x02};
    public static final byte[] DKSS_LEN_4 = new byte[] {0x00,0x04};
    public static final byte[] DKSS_LEN_8 = new byte[] {0x00,0x08};

    public static final byte[] DKSS_CMD_REGISTER_DEV = new byte[] {0x00,0x10};
    public static final byte[] DKSS_CMD_CLOSE_DEV = new byte[] {0x00,0x11};
    public static final byte[] DKSS_CMD_SEND_DATA = new byte[] {0x00,0x40};

    public static final byte[] DKSS_VERSION = new byte[] {0x00};

    //构建payload
    public static byte[] constructPayload(byte[] id,String data) {
        byte[] dataByte = null;
        short len = -1;
        dataByte = getByteFromUTF8(data);
        len = (short) dataByte.length;
        return mergeByte(id,DKSS_STRING,baseToByte(len),dataByte);
    }
    public static byte[] constructPayload(byte[] id,short data){
        return mergeByte(id,DKSS_SHORT,DKSS_LEN_2,baseToByte(data));
    }
    public static byte[] constructPayload(byte[] id,int data){
        return mergeByte(id,DKSS_INT,DKSS_LEN_4,baseToByte(data));
    }
    public static byte[] constructPayload(byte[] id,float data){
        return mergeByte(id,DKSS_FLOAT,DKSS_LEN_4,baseToByte(data));
    }
    public static byte[] constructPayload(byte[] id,char data){
        return mergeByte(id,DKSS_UCHAR,DKSS_LEN_1,baseToByte(data));
    }
    public static byte[] constructPayload(byte[] id,double data){
        return mergeByte(id,DKSS_UINT64,DKSS_LEN_8,baseToByte(data));
    }
    public static byte[] constructPayload(byte[] id,ArrayList<Byte> data){
        short len = (short)data.size();
        byte[] temp = new byte[len];
        for(int i=0;i<len;i++){
            temp[i] = data.get(i);
        }
        //DkssUtil.printByte(mergeByte(id,DKSS_BYTE_ARR,baseToByte(len),temp));
        return mergeByte(id,DKSS_BYTE_ARR,baseToByte(len),temp);
    }
    public static byte[] constructPayload(byte[] id,byte[] data){
        short len = (short)data.length;
        return mergeByte(id,DKSS_BYTE_ARR,baseToByte(len),data);
    }

    //构建数据包
    public static byte[] constructPacket(byte[] version, byte[] cmd, int payloadNum, byte[] data) {
//        if(payloadNum>255 || data.length>2042) {
//            System.err.println("数据包长度过长或者负载数量过多");
//            return null;
//        }
        byte[] num = new byte[1];
        num[0] = (byte)payloadNum;
        byte[] msgBody = mergeByte(cmd,num,data);
        return mergeByte(version,baseToByte((short)msgBody.length),msgBody);
    }


    //基本类型 转换成byte[]数组
    public static byte[] baseToByte(double arg) {
        long value = Double.doubleToRawLongBits(arg);
        int size =8;
        byte[] ret = new byte[size];

        for(int i = 0;i<size;i++) {
            ret[i] = (byte)((value>>size*(size-1-i))&0xff);
        }
        return ret;
    }

    public static byte[] baseToByte(float arg) {
        int value = Float.floatToRawIntBits(arg);
        return baseToByte(value);
    }

    public static byte[] baseToByte(short arg) {
        byte[] ret = new byte[2];
        int size = 2;
        for(int i = 0;i<size;i++) {
            ret[i] = (byte)((arg>>8*(size-1-i))&0xff);
        }
        return ret;
    }

    public static byte[] baseToByte(int arg) {
        int size = 4;
        byte[] ret = new byte[size];
        for(int i = 0;i<size;i++) {
            ret[i] = (byte)((arg>>8*(size-1-i))&0xff);
        }
        return ret;
    }
    public static byte[] baseToByte(char arg) {
        byte[] ret = new byte[1];
        ret[0] = (byte)arg;
        return ret;
    }

    //字节转其他类型
    public static String byteToChar(byte[] arg,int index){
        byte[] t = new byte[1];
        t[0] = arg[index];
        return "0x"+byteArrToHexString(t);
    }

    public static short byteToShort(byte[] arg,int index){
        return (short)(0xff00 & arg[index] << 8 | (0xff & arg[index+1]));
    }

    public static int byteToInt(byte[] arg,int index){

        return  (0xff000000 & (arg[index+0] << 24))|
                (0x00ff0000 & (arg[index+1] << 16))|
                (0x0000ff00 & (arg[index+2] << 8))|
                (0x000000ff & (arg[index+3]));
    }

    public static float byteToFloat(byte[] arg,int index){
        return Float.intBitsToFloat(byteToInt(arg,index));
    }

    public static long byteToLong(byte[] arg,int index){
        return (0xff00000000000000L & ((long)arg[index+0] << 56)) |
                (0x00ff000000000000L & ((long)arg[index+1] << 48)) |
                (0x0000ff0000000000L & ((long)arg[index+2] << 40)) |
                (0x000000ff00000000L & ((long)arg[index+3] << 32)) |
                (0x00000000ff000000L & ((long)arg[index+4] << 24)) |
                (0x0000000000ff0000L & ((long)arg[index+5] << 16)) |
                (0x000000000000ff00L & ((long)arg[index+6] << 8)) |
                (0x00000000000000ffL & ((long)arg[index+7]));
    }

    public static double byteToDouble(byte[] arg,int index){
        return Double.longBitsToDouble(byteToLong(arg,index));
    }

    public static String byteToString(byte[] arg,int index,int len){
        byte[] ret = new byte[len];
        System.arraycopy(arg,index,ret,0,len);
        String s = null;
        try {
            s = new String(ret,"utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return s;
    }


    public static byte[] getByteFromUTF8(String arg) {
        if(arg==null) {
            return null;
        }
        byte[] ret = null;
        try {
            ret = arg.getBytes("utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return ret;
    }



    //合并字节数组
    public static byte[] mergeByte(byte[]... args) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(2048);
        int size = 0;
        int argsLen = args.length;
        int i=0;
        try {


            for (i = 0; i < argsLen; i++) {
                try {
                    baos.write(args[i]);
                    size += args[i].length;
                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println("错误的空指针"+i);

                }
            }
        }catch (Exception e){
            e.printStackTrace();
            System.err.println("空指针异常:"+i);
        }
        byte[] res = new byte[size];

        System.arraycopy(baos.toByteArray(), 0, res, 0, size);
        return res;
    }

    public static byte[] mergeByte(ArrayList<byte[]> argList) {
        if(argList.size()==0) {
            return null;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream(2048);
        int size = 0;
        Iterator<byte[]> iterator = argList.iterator();
        try {
            while (iterator.hasNext()) {
                byte[] temp = iterator.next();
                baos.write(temp);
                size += temp.length;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] res = new byte[size];
        System.arraycopy(baos.toByteArray(), 0, res, 0, size);
        return res;
    }



    //16进制字符串转换成其对应字节数组
    public static byte[] hexStringToByteArr(String hexString) {
        if (hexString==null || hexString.isEmpty() || (hexString.length()%2!=0)) {
            return null;
        }
        hexString = hexString.toLowerCase();
        final byte[] byteArray = new byte[hexString.length() / 2];
        int k = 0;
        for (int i = 0; i < byteArray.length; i++) {//因为是16进制，最多只会占用4位，转换成字节需要两个16进制的字符，高位在先
            byte high = (byte) (Character.digit(hexString.charAt(k), 16) & 0xff);
            byte low = (byte) (Character.digit(hexString.charAt(k + 1), 16) & 0xff);
            byteArray[i] = (byte) (high << 4 | low);
            k += 2;
        }
        return byteArray;
    }

    public static String byteArrToHexString(byte[] arg){
        char[] hexChar = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        int len = arg.length;
        StringBuffer sb = new StringBuffer();
        for(int i=0;i<len;i++){
            short d = (short)(arg[i] & 0x00ff);
            sb.append(hexChar[d/16]);
            sb.append(hexChar[d%16]);
        }
        return sb.toString();
    }


    //构建时间包
    public static byte[] getTimePayload() {
        return constructPayload(new byte[] {0x00,0x02},
                (""+System.currentTimeMillis()));
    }

    //解析云服务器返回的应答
    public static boolean parseReply(byte[] reply){

        if(reply == null){
            return false;
        }

        switch (reply[4]){
            case 0x12:
                break;
            case 0x41:
                break;
        }

        return true;
    }


    //打印字节数组,方便测试
    public static void printByte(byte[] b) {
        System.out.println("字节数组长度:"+b.length);
        for( int i=0;i<b.length;i++) {
            System.out.printf("%02x ",b[i]);
        }
        System.out.println();
    }
    public static void printByte(int[] b){
        System.out.println("字节数组长度:"+b.length);
        for( int i=0;i<b.length;i++){
            System.out.printf("%02x ",(byte)b[i]);
        }
        System.out.println();
    }

    //解析协议包

    public static ArrayList<String> parsePacket(byte[] data){
        ArrayList<String> list = new ArrayList<>();
        int payloadNum = 0;
        payloadNum = (int)data[5];
        int offset = 6;
        byte[] temp = new byte[2];
        for(int i = 0;i < payloadNum;i++){
            temp[0] = data[offset];
            temp[1] = data[offset+1];
            int valueLen = byteToShort(data,offset+3);
            String id = byteArrToHexString(temp);
            String value = "";

            switch (data[offset+2]){
                case 0x01:
                    value = byteToString(data,offset+5,valueLen);
                    break;
                case 0x02:
                    value = "" + byteToShort(data,offset+5);
                    break;
                case 0x03:
                    value = "" + byteToInt(data,offset+5);
                    break;
                case 0x04:
                    value = "" + byteToFloat(data,offset+5);
                    break;
                case 0x05:
                    value = "" + byteToChar(data,offset+5);
                    break;
                case 0x06:
                    value = "" + byteToDouble(data,offset+5);
                    break;
                case 0x07:
                    StringBuffer sb = new StringBuffer();
                    byte[] temp1 = new byte[2];
                    temp1[0] = 0;
                    for(int j=0;j<valueLen;j++){
                        temp1[1] = data[offset+5+j];
                        sb.append(" "+DkssUtil.byteToShort(temp1,0));
                    }
                    value = sb.toString();
                    System.out.println("len = "+valueLen+" "+"value = "+value);

                default:
                    System.err.printf("解析协议包，未知的数据类型 %x",data[offset+2]);
                    break;
            }

            list.add(id+":"+value);

            offset += 5 + valueLen;
        }

        Iterator iterator = list.iterator();
//        while(iterator.hasNext()){
//            System.out.println(iterator.next());
//        }

        return list;
    }

    public static void parsePayload(byte[] payload){
        byte[] temp = new byte[2];
        System.arraycopy(payload,0,temp,0,2);
        String id = byteArrToHexString(temp);
        short len = byteToShort(payload,3);
        switch (payload[2]){
            case 0x01:
                String sg = new String(payload,5,len);
                System.out.println(id+":"+sg);
                break;
            case 0x02:
                short st = byteToShort(payload,5);
                System.out.println(id+":"+st);
                break;
            case 0x03:
                int it = byteToInt(payload,5);
                System.out.println(id+":"+it);
                break;
            case 0x04:
                float ft = byteToFloat(payload,5);
                System.out.println(id+":"+ft);
                break;
            case 0x05:
                byte be = payload[5];
                System.out.println(id+":"+be);
                break;
            case 0x06:

                break;
        }
    }



}
