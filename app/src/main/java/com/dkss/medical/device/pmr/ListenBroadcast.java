package com.dkss.medical.device.pmr;

import com.dkss.medical.util.PMUtil;

public class ListenBroadcast implements Runnable{
    public volatile boolean isData = false;
    private String ip;
    private int port;
    private byte mMachineNum;
    private byte mProtocolVersion;

    public ListenBroadcast(String ip,byte mMachineNum) {
        this.ip = ip;
        this.port = 8002;
        this.mMachineNum  = mMachineNum;
    }

    @Override
    public void run() {

        byte[] data = null;

        while (true) {

            data = PMUtil.receiveUDP(8002, 0);
            if(data[1] == (byte)0xD0) {
                mProtocolVersion = data[8];
            }else if(data[1] == (byte)0xDA) {
                int ret = PMUtil.parsePacketDA(data);
                if(ret ==200) {
                    isData = true;
                }
            }
        }
    }


}
