package com.byteshaft.wifimessenger.utils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class MessagingHelpers {

    public static void sendMessage(final String msg, final String ip, final int port) {
        final String header = "MSG:";
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    System.out.println("IP: " + ip + " PORT: " + port);
                    DatagramSocket datagramSocket = new DatagramSocket();
                    InetAddress local = InetAddress.getByName(ip);
                    String content = header + msg;
                    System.out.println(content);
                    byte[] message = content.getBytes();
                    DatagramPacket p = new DatagramPacket(message, content.length(), local, port);
                    datagramSocket.send(p);
                    datagramSocket.disconnect();
                    datagramSocket.close();
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
