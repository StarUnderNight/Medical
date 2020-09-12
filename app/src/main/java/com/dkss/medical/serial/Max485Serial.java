package com.dkss.medical.serial;

public class Max485Serial {
    public native int open(int Port, int Rate, int nBits, char nEvent, int nStop);
    public native int close();
    public native int[] read();
    public native int write(int[] buffer,int len,float sleepTime);

}
