package com.mamehub.client.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UDPReflectionServer {
	private final static Logger logger = LoggerFactory.getLogger(UDPReflectionServer.class.getName());
	
	private Thread serverThread;
	private boolean finished = false;

	public UDPReflectionServer(int port) {
		try {
			final DatagramSocket serverSocket = new DatagramSocket(port);
			serverSocket.setSoTimeout(2000);
			serverThread = new Thread(new Runnable(){
				@Override
				public void run() {
					try {
						byte[] receiveData = new byte[1024];
						byte[] sendData = new byte[1024];
						logger.info("SERVER: Waiting for packet");
						while(!finished)
						{
							DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
							try {
								serverSocket.receive(receivePacket);
							} catch(SocketTimeoutException ste) {
								continue;
							}
							String sentence = new String( receivePacket.getData());
							System.out.println("SERVER RECEIVED: " + sentence);
							InetAddress IPAddress = receivePacket.getAddress();
							int port = receivePacket.getPort();
							String capitalizedSentence = sentence.toUpperCase();
							sendData = capitalizedSentence.getBytes();
							DatagramPacket sendPacket =
									new DatagramPacket(sendData, sendData.length, IPAddress, port);
							serverSocket.send(sendPacket);
							Thread.sleep(1);
						}
					} catch(IOException ioe) {
						ioe.printStackTrace();
					} catch (InterruptedException e) {
						logger.info("Thread interrupted");
					}
					logger.info("Server shutting down");
					serverSocket.close();
				}
			});
			serverThread.start();
		} catch(SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}
	
	public void shutdown() {
		logger.info("Shutting down");
		finished = true;
		serverThread.interrupt();
		try {
			serverThread.join();
		} catch (InterruptedException e) {
			throw new RuntimeException("OOPS");
		}
	}
}
