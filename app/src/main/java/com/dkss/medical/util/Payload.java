package com.dkss.medical.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class Payload {
    private int num;
    private static final int LIMIT = 10000;
    private byte[] data = new byte[LIMIT];
    private int offset = 0;

    public int getNum() {
        return num;
    }

    public byte[] getData() {
        if(offset==0){
            return null;
        }
        byte[] temp = new byte[offset];
        System.arraycopy(data,0,temp,0,offset);
        return temp;
    }

    public boolean add(byte[] payload){
        int size = payload.length;
        if(offset+size>LIMIT){
            return false;
        }
        System.arraycopy(payload,0,data,offset,size);
        offset += size;
        num++;
        return true;
    }

    public boolean add(int index, byte[] payload) {
        int size = payload.length;
        if (offset + size > LIMIT) {
            return false;
        }
        byte[] temp = new byte[LIMIT];
        System.arraycopy(payload, 0, temp, 0, size);
        System.arraycopy(data, 0, temp, size, size);
        data = temp;
        num++;
        offset += size;
        return true;
    }

    public boolean add(byte[] ... args){
        int argsLen = args.length;
        int size = 0;
        ByteArrayOutputStream baos = new ByteArrayOutputStream(LIMIT);
        for(int i=0;i<argsLen;i++){
            try {
                baos.write(args[i]);
                size += args[i].length;
                if(offset+size>LIMIT){
                    return false;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.arraycopy(baos.toByteArray(),0,data,offset,size);
        offset += size;
        num +=argsLen;
        return true;
    }

    public void clear(){
        this.data = new byte[LIMIT];
        this.num = 0;
        this.offset = 0;
    }


}
