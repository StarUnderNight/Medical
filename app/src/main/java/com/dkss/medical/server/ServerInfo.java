package com.dkss.medical.server;

public class ServerInfo {
    private String ip;
    private int port;
    private int readTimeout;
    private int connectTimeout;

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public ServerInfo(String ip, int port, int readTimeout, int connectTimeout) {
        this.ip = ip;
        this.port = port;
        this.readTimeout = readTimeout;
        this.connectTimeout = connectTimeout;
    }

    public ServerInfo(String ip, int port) {
        this.ip = ip;
        this.port = port;
        this.readTimeout = 5000;
        this.connectTimeout = 5000;
    }

}
