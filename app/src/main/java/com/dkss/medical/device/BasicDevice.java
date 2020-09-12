package com.dkss.medical.device;

import com.dkss.medical.util.BufferQueue;
import com.dkss.medical.util.DkssUtil;
import com.dkss.medical.util.Payload;
import com.dkss.medical.util.SocketUtil;

public class BasicDevice implements Device,Runnable{
    @Override
    public void parse(byte[] packet, BufferQueue queue) {


    }

    @Override
    public void registDev(String sIP, int sPort, int sReadTimeout, int sConnectTimeout, Payload payload) {
        byte[] packet = DkssUtil.constructPacket(DkssUtil.DKSS_VERSION,DkssUtil.DKSS_CMD_REGISTER_DEV,
                payload.getNum(),payload.getData());

        while(true){
            byte[] ret = SocketUtil.deliveryDataToServer(sIP,sPort,sReadTimeout,sConnectTimeout,packet);
            if(ret == null){
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }

            if(DkssUtil.parseReply(ret)){
                System.out.println("Regist dev success");
                break;
            }
        }

    }

    @Override
    public void flushBuf(String sIP, int sPort, int sReadTimeout, int sConnectTimeout, BufferQueue queue) {

    }


    @Override
    public void run() {

    }
}
