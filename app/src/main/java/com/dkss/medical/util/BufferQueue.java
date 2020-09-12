package com.dkss.medical.util;

import java.util.ArrayList;

public class BufferQueue {

    private ArrayList<byte[]> bufList = new ArrayList<>();
    private int capacity = 10;

    public BufferQueue(){

    }

    public BufferQueue(int capacity){
        this.capacity = capacity;
    }

    public boolean add(byte[] data){
        if(bufList.size()>= capacity){
            bufList.remove(0);
            bufList.add(data);
            return false;
        }

        bufList.add(data);
        return true;
    }

    public int size(){
        return bufList.size();
    }

    public byte[] get(int index){
        if(index>=bufList.size()){
            return null;
        }
        return bufList.get(index);
    }
    public void remove(int index){
        if(index>=bufList.size()){
            return;
        }
        bufList.remove(index);
    }
}