package com.byteshaft.wifimessenger.utils;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

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

    public static void startCall(final String ip, final int port) {
        int SAMPLE_RATE = 8000; // Hertz
        final String header = "PICK";
        final AudioRecord audioRecorder = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                AudioRecord.getMinBufferSize(SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT) * 10);

        callThread = new Thread(new Runnable() {
            @Override
            public void run() {
                int bytes_read;
                byte[] buf = new byte[BUF_SIZE];
                try {
                    // Create a socket and start recording
                    Log.i(LOG_TAG, "Packet destination: " + ip);
                    DatagramSocket socket = new DatagramSocket();
                    InetAddress local = InetAddress.getByName(ip);
                    audioRecorder.startRecording();
                    while(MIC) {
                        // Capture audio from the MIC and transmit it
                        bytes_read = audioRecorder.read(buf, 0, BUF_SIZE);
                        DatagramPacket packet = new DatagramPacket(buf, bytes_read, local, port);
                        socket.send(packet);
                        Thread.sleep(SAMPLE_INTERVAL, 0);
                    }
                    // Stop recording and release resources
                    audioRecorder.stop();
                    audioRecorder.release();
                }
                catch(InterruptedException e) {

                    Log.e(LOG_TAG, "InterruptedException: " + e.toString());
                    MIC = false;
                }
                catch(SocketException e) {

                    Log.e(LOG_TAG, "SocketException: " + e.toString());
                    MIC = false;
                }
                catch(UnknownHostException e) {

                    Log.e(LOG_TAG, "UnknownHostException: " + e.toString());
                    MIC = false;
                }
                catch(IOException e) {

                    Log.e(LOG_TAG, "IOException: " + e.toString());
                    MIC = false;
                }
            }
        });
        callThread.start();
    }
    
    public static void endCall() {
        MIC = false;
        if (callThread != null) {
            callThread.interrupt();
            callThread = null;
        }
    }
}
