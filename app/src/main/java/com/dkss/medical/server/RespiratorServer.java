package com.dkss.medical.server;


import com.dkss.medical.device.rer.Respirator;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class RespiratorServer implements Runnable {
    public volatile Respirator respirator = null;

    private static  HashMap<String,byte[]> configMap = null;
    private int localPort;

    private Map<String,Object> commonCfgMap;
    private Map<String,Object> rerIDMap;


    public RespiratorServer(Map<String, Object> commonCfgMap, Map<String, Object> rerIDMap) {
        this.commonCfgMap = commonCfgMap;
        this.rerIDMap = rerIDMap;

    }

    @Override
    public void run() {
        ArrayList<Socket> socketList = new ArrayList<Socket>();
        ServerSocket ss = null;
        HashMap<String,String> rerCfgMap = (HashMap)commonCfgMap.get("rer");
        this.localPort = Integer.parseInt(rerCfgMap.get("port"));
        System.out.println("监听端口:"+localPort);
        try {
            ss = new ServerSocket(localPort);
        } catch (IOException e) {
            e.printStackTrace();
        }

        while(true)
        {
            System.out.println("服务器等待链接……");
            // 此行代码会阻塞，将一直等待别人的连接
            Socket s = null;// 程序阻塞
            try {
                s = ss.accept();
            } catch (IOException e) {
                e.printStackTrace();
            }
            socketList.add(s);
            System.out.println("socket连接 +1 ，一共 ：" + socketList.size());
            // 每当客户端连接后启动一条ServerThread线程为该客户端服务

            try {
                respirator = new Respirator(s);
                respirator.init(commonCfgMap,rerIDMap);
                new Thread(respirator).start();
                System.out.println("新建线程，处理连接");

            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("thread + 1");
        }
    }

}