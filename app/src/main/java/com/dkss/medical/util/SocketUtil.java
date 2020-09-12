package com.dkss.medical.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

public class SocketUtil {

	public static Socket getSocket(String host,int port,int readTimeout) {

		Socket socket = null;
		while (true) {

			try {
				System.out.println("连接 "+host+":"+port+" 中......");
				socket = new Socket(host, port);
			} catch (Exception e) {
				System.err.println("连接 "+host+":"+port+" 超时....");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				continue;
			}

			try {
				socket.setSoTimeout(readTimeout);
			} catch (SocketException e) {
				e.printStackTrace();
			}
			System.out.println("连接 "+host+":"+port+" 成功.");
			return socket;
		}
	}

	public static void closeSocket(Socket socket) {
		if(socket == null || socket.isClosed()) {
			return ;
		}
		try {
			socket.close();
		} catch (IOException e) {
			System.err.println("关闭socket时，抛出异常");
			e.printStackTrace();
		}
	}

	//从Socket服务器接收数据流
	public static boolean receiveFromStream(char[] data,BufferedReader bufferedReader){

		try {
			if(bufferedReader.read(data)<0) {
				System.out.println("读取数据失败");
			}
		} catch (IOException e) {
			System.err.println("读取数据超时");
			return false;
		}
		return true;
	}

	//发送数据到Socket服务器
	public static void sendToStream(String data,PrintWriter pr) {
		if(pr == null){
			return ;
		}
		pr.write(data);
		pr.flush();
	}

	//获取PrintWriter
	public static PrintWriter getPrintWriter(Socket socket) {
		PrintWriter pw = null;
		if(socket == null) {
			System.err.println("socket为null指针");
			return null;
		}
		try {
			pw = new PrintWriter(new OutputStreamWriter(
					new BufferedOutputStream(socket.getOutputStream())));
		} catch (IOException e) {
			//socket的getOutputStream方法抛出异常，socket关闭，sockek未连接，输入或输出流关闭
			e.printStackTrace();
			return null;
		}
		return pw;
	}

	public static BufferedReader getBufferedReader(Socket socket,String code) {
		BufferedReader bufferedReader = null;
		if(socket == null) {
			System.err.println("socket为null指针");
			return null;
		}
		try {
			bufferedReader = new BufferedReader(new InputStreamReader(
					new BufferedInputStream(socket.getInputStream()),code));
		} catch (IOException e) {
			//socket的getOutputStream方法抛出异常，socket关闭，sockek未连接，输入或输出流关闭
			e.printStackTrace();
			return null;
		}
		return bufferedReader;
	}

	//关闭输入输出流
	public static void closeStream(PrintWriter printWriter,BufferedReader bufferedReader) {
		if(printWriter!=null) {
			printWriter.close();
		}
		try {
			if(bufferedReader != null)
				bufferedReader.close();
		} catch (Exception e) {
			System.err.println("关闭文件流失败");
		}
	}

	// 工具类，发送data到host:port，并接受服务器回复，读超时readTimeout，连接超时connectTimeout
	public static byte[] deliveryDataToServer(String host, int port, int readTimeout, int connectTimeout, byte[] data) {

		Socket socket = new Socket();
		byte[] buffer = new byte[1024];
		int len = -1;
		BufferedInputStream bis = null;
		byte[] ret = null;


		try {
			socket.setSoTimeout(readTimeout);
			socket.connect(new InetSocketAddress(host, port), connectTimeout);


			bis = new BufferedInputStream(socket.getInputStream());
			BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());

			bos.write(data);
			bos.flush();

			len = bis.read(buffer);

			if (len < 0) {
				System.out.println("deliveryDataToServer:读取数据失败");
			}else{
				ret = new byte[len];
				System.arraycopy(buffer,0,ret,0,len);
			}

		} catch (IOException e) {
			System.err.println("服务器无返回");
		}

		try {
			if(socket!=null)
				socket.close();
			if(bis!=null)
				bis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return ret;
	}

}
