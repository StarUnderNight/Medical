package com.dkss.medical.device;

import com.dkss.medical.server.ServerInfo;
import com.dkss.medical.util.BufferQueue;
import com.dkss.medical.util.Payload;

import java.util.ArrayList;

public interface Device {
    void  parse(byte[] packet, BufferQueue queue);
    void registDev(ServerInfo info,Payload payload);
    byte[] receive();
    void send(byte[] cmd);
    void flushBuf(ServerInfo info, BufferQueue queue);
}
