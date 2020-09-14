package com.dkss.medical.device;

import com.dkss.medical.server.ServerInfo;
import com.dkss.medical.util.BufferQueue;
import com.dkss.medical.util.DkssUtil;
import com.dkss.medical.util.Payload;
import com.dkss.medical.util.SocketUtil;

import java.util.Map;

public class BasicDevice implements Device,Runnable{
    @Override
    public boolean init(Map<String, Object> cfgMap) {
        return false;
    }

    @Override
    public void parse(byte[] packet, BufferQueue queue) {


    }

    @Override
    public byte[] receive() {
        return null;
    }

    @Override
    public void send(byte[] cmd) {

    }

    @Override
    public void flushBuf(ServerInfo info, BufferQueue queue) {
        while(queue.size()>0){
            byte[] ret = SocketUtil.deliveryDataToServer(info,queue.get(0));
            if(ret ==null){
                return ;
            }
            queue.remove(0);;
        }
    }

    @Override
    public void registDev(ServerInfo info, Payload payload) {
        byte[] packet = DkssUtil.constructPacket(DkssUtil.DKSS_VERSION,DkssUtil.DKSS_CMD_REGISTER_DEV,
                payload.getNum(),payload.getData());

        while(true){
            byte[] ret = SocketUtil.deliveryDataToServer(info,packet);
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
    public void run() {

    }
}
