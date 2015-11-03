package com.byteshaft.wifimessenger.utils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class MessagingHelpers {
    
    private static Thread callThread;
    private static final int SAMPLE_INTERVAL = 20; // Milliseconds
    private static final int SAMPLE_SIZE = 2; // Bytes
    private static final int BUF_SIZE = SAMPLE_INTERVAL * SAMPLE_INTERVAL * SAMPLE_SIZE * 2; //Bytes
    private static final String LOG_TAG = "CALL";
    private static boolean MIC;

    public static void sendMessage(final String msg, final String ip, final int port) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                sendMsg(msg, ip, port);
            }
        }).start();
    }

    public static void sendCallRequest(final String name, final String ip, final int port) {
        final String content = "CAL:";
        new Thread(new Runnable() {
            @Override
            public void run() {
                sendMsg(content, ip, port);
            }
        }).start();
    }

    private static void sendMsg(final String msg, final String ip, final int port) {
        try {
            System.out.println("IP: " + ip + " PORT: " + port);
            DatagramSocket datagramSocket = new DatagramSocket();
            InetAddress local = InetAddress.getByName(ip);
            byte[] message = msg.getBytes();
            DatagramPacket p = new DatagramPacket(message, msg.length(), local, port);
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
}
