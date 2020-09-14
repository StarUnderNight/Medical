package com.dkss.medical.device.max485;

import android.util.Log;

import com.dkss.medical.serial.Max485Serial;
import com.dkss.medical.util.Protocol;
import com.dkss.medical.util.DkssUtil;
import com.dkss.medical.util.Payload;
import com.dkss.medical.util.SerialUtil;
import com.dkss.medical.util.SocketUtil;

import java.util.ArrayList;

public class Max485 implements  Runnable{

    public volatile boolean exit = false;



    private ArrayList<byte[]> bufferDataQueue;
    private int bufferDataQueueLen;

    private byte[] boxIDPayload; // 盒子ID
    private byte[] boxTypePayload;   //盒子类型
    private byte[] vDevTypePayload;   //电量仪设备类型负载
    private byte[] oDevTypePayload;   //氧气设备类型负载

    private static final int VOLTAGE_RANGE = 250; //单位V
    private static final int CURRENT_RANGE = 10;  //单位A

    public Max485() {
        this.bufferDataQueue = new ArrayList<>();
        this.boxIDPayload = DkssUtil.constructPayload(Protocol.ID_BOX_ID, Protocol.boxId);
        this.boxTypePayload = DkssUtil.constructPayload(Protocol.ID_BOX_TYPE, Protocol.BOX_TYPE);
        this.oDevTypePayload = DkssUtil.constructPayload(Protocol.ID_DEV_TYPE, Protocol.O_TYPE);
        this.vDevTypePayload = DkssUtil.constructPayload(Protocol.ID_DEV_TYPE, Protocol.V_TYPE);
        this.bufferDataQueueLen = Protocol.maxBufQueueLen;
    }

    private int[] readFromSerial(Max485Serial serial,int[] cmd,int readLen,int faultTolerant,float sleepTime){
        int count = 0;
        while(count++ < faultTolerant){

            serial.write(cmd,cmd.length,sleepTime);
            int[] buffer = serial.read();

            if(buffer ==null){
                System.out.println("buffer is null");
                continue;
            }

            //判断校验和是否对不对
            if(!SerialUtil.getCRC(buffer).equals("0")){
                System.out.println("crc error");
                continue;
            }
            return buffer;
        }
        return null;
    }

    private boolean parseVolta(Max485Serial serial, int[] cmd, int readLen, Payload payload){
        int[] data = readFromSerial(serial,cmd,readLen, Protocol.vFTT, Protocol.vSleepTime);
        if(data == null){
            System.out.println("data is null");
            return false;
        }

        byte[][] id = {Protocol.ID_VOR_0, Protocol.ID_VOR_1, Protocol.ID_VOR_2, Protocol.ID_VOR_3, Protocol.ID_VOR_4, Protocol.ID_VOR_5, Protocol.ID_VOR_6,
                Protocol.ID_VOR_7, Protocol.ID_VOR_8, Protocol.ID_VOR_9, Protocol.ID_VOR_10, Protocol.ID_VOR_11, Protocol.ID_VOR_12, Protocol.ID_VOR_13};

        int len = 14;
        float[] vArr = new float[len];
        //电压
        vArr[0] = ( (float)(data[3]*256+data[4]) * VOLTAGE_RANGE ) / 10000;
        //电流
        vArr[1] = ( (float)(data[5]*256+data[6]) * CURRENT_RANGE ) / 10000;
        //有功功率
        vArr[2] = ( (float)(data[7]*256+data[8]) * CURRENT_RANGE * VOLTAGE_RANGE ) / 10000;
        //无功功率
        vArr[3] = ( (float)(data[9]*256+data[10] ) * CURRENT_RANGE * VOLTAGE_RANGE ) / 10000;
        //功率因数
        vArr[4] = ( (float)(data[11]*256+data[12]) ) / 10000;
        //F频率
        vArr[5] = ( (float)(data[13]*256+data[14]) ) / 100;
        //正向有功电度
        vArr[6] = ( (float)(data[15]*256+data[16]) * VOLTAGE_RANGE * CURRENT_RANGE ) / (1000*3600);
        //正向无功电度
        vArr[7] = ( (float)(data[17]*256+data[18]) * VOLTAGE_RANGE * CURRENT_RANGE ) / (1000*3600);
        //反向有功电度
        vArr[8] = ( (float)(data[19]*256+data[20]) * VOLTAGE_RANGE * CURRENT_RANGE ) / (1000*3600);
        //反向无功电度
        vArr[9] = ( (float)(data[21]*256+data[22]) * VOLTAGE_RANGE * CURRENT_RANGE ) / (1000*3600);
        //视在功率
        vArr[10] = ( (float)(data[23]*256+data[24] ) * CURRENT_RANGE * VOLTAGE_RANGE ) / 10000;
        //谐波有功功率
        vArr[11] = ( (float)(data[25]*256+data[26] ) * CURRENT_RANGE * VOLTAGE_RANGE ) / 10000;
        //基波有功功率
        vArr[12] = ( (float)(data[27]*256+data[28] ) * CURRENT_RANGE * VOLTAGE_RANGE ) / 10000;
        //基波无功功率
        vArr[13] = ( (float)(data[29]*256+data[30] ) * CURRENT_RANGE * VOLTAGE_RANGE ) / 10000;

        for(int i=0;i<len;i++){
            payload.add(DkssUtil.constructPayload(id[i],vArr[i]));
        }

        return true;
    }

    private boolean parseOxygen(Max485Serial serial, int[] cmd, int readLen, Payload payload){
        int[] data = readFromSerial(serial,cmd,readLen, Protocol.oFTT, Protocol.oSleepTime);
        if(data == null){
            return false;
        }
        float oxy = ((float)(data[3]*256+data[4])) /100;

        return true;
    }

    private void addToBufferQueue(byte[] data){
        if(bufferDataQueue.size() >= bufferDataQueueLen){
            bufferDataQueue.remove(0);
        }
        bufferDataQueue.add(data);
    }

    private void flushBufferQueue(){

        while(bufferDataQueue.size() > 0){
            DkssUtil.parsePacket(bufferDataQueue.get(0));
            byte[] ret = SocketUtil.deliveryDataToServer(null,bufferDataQueue.get(0));

            if(ret == null || !DkssUtil.parseReply(ret)){
                break;
            }
            bufferDataQueue.remove(0);
        }
    }

    private boolean registDev(){

        // 注册电量仪设备
        byte[] vRegisterPacket = DkssUtil.constructPacket(
                DkssUtil.DKSS_VERSION,DkssUtil.DKSS_CMD_REGISTER_DEV,4,
                DkssUtil.mergeByte(boxTypePayload,boxIDPayload,vDevTypePayload,DkssUtil.getTimePayload()));

        while (true && !exit) {
            System.out.println("注册电量仪");
            byte[] ret = SocketUtil.deliveryDataToServer(null,
                    vRegisterPacket);
            if(ret == null){
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }
            System.out.println("注册电量仪，收到回复");
            DkssUtil.printByte(ret);

            if(DkssUtil.parseReply(ret)){
                break;
            }
        }

        // 注册氧气设备
        byte[] oRegisterPacket = DkssUtil.constructPacket(
                DkssUtil.DKSS_VERSION,DkssUtil.DKSS_CMD_REGISTER_DEV,4,
                DkssUtil.mergeByte(boxTypePayload,boxIDPayload,vDevTypePayload,DkssUtil.getTimePayload()));

        while (true && !exit) {
            System.out.println("注册氧气");
            byte[] ret = SocketUtil.deliveryDataToServer(null,
                    oRegisterPacket);

            if(ret == null){
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }
            System.out.println("注册氧气，收到回复");
            DkssUtil.printByte(ret);

            if(DkssUtil.parseReply(ret)){
                break;
            }
        }

        return   !exit;
    }



    @Override
    public void run() {
        System.loadLibrary("Max485Serial");
        Max485Serial serial = new Max485Serial();

        int ret = -1;
        try {
            ret = serial.open(1, 9600, 8, 'N', 1);
        }catch (Exception e){
            System.err.println("无法打开seroen");
        }
        if(ret<0){
            Log.i("serial485","串口打开失败");
            return;
        }

        registDev();

        int[] vCmd = {3,3,0,0,0,15,4,44};   //03 03 00 00 00 0f 04 2c
        int[][] oCmd = {{1,3,0,1,0,1,213,202},//01 03 00 01 00 01 d5 ca
                {2,3,0,1,0,1,213,249}   //02 03 00 01 00 01 d5 f9
        };

        int vDataLen = 35;
        int oDataLen = 8;


        while(!exit){
            Payload payload = new Payload();
            //读取电量仪数据
            parseVolta(serial,vCmd,vDataLen,payload);
            if(payload.getNum()!=0){
                payload.add(0, DkssUtil.getTimePayload());
                payload.add(0,vDevTypePayload);
                payload.add(0,boxIDPayload);
                payload.add(0,boxTypePayload);
                addToBufferQueue(payload.getData());
                payload.clear();
            }

            for(int i=0;i<oCmd.length;i++){
                parseOxygen(serial,oCmd[0],oDataLen,payload);
            }

            if(payload.getNum()!=0){
                payload.add(0, DkssUtil.getTimePayload());
                payload.add(0,oDevTypePayload);
                payload.add(0,boxIDPayload);
                payload.add(0,boxTypePayload);

                addToBufferQueue(payload.getData());
                payload.clear();
            }

            flushBufferQueue();
        }
        serial.close();
        Log.i("service","max485  died");
    }




}
