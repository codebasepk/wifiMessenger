package com.byteshaft.wifimessenger.utils;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.byteshaft.wifimessenger.R;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;


public class ServiceHelpers {

    private final static int BROADCAST_INTERVAL = 1000;
    public final static int BROADCAST_PORT = 50001;
    private static final int BROADCAST_BUF_SIZE = 1024;
    private static boolean BROADCAST;
    private static boolean DISCOVERY;
    private static Thread broadcastThread;
    private static Thread discoveryThread;

    private static DatagramSocket mSocket;

    private static HashMap<String, InetAddress> peersMap = new HashMap<>();

    private static String LOG_TAG = "wifiMessenger";

    public static HashMap<String, InetAddress> getPeersList() {
        return peersMap;
    }

    public static boolean isPeerListEmpty() {
        return peersMap.isEmpty();
    }

    public static void broadcastName(final String action, final String name,
                                     final InetAddress broadcastIP) {
        // Broadcasts the name of the device at a regular interval
        Log.i(LOG_TAG, "Broadcasting started!");
        BROADCAST = true;
        broadcastThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String request = action + name;
                    byte[] message = request.getBytes();
                    DatagramSocket socket = new DatagramSocket();
                    socket.setBroadcast(true);
                    DatagramPacket packet = new DatagramPacket(message, message.length, broadcastIP, BROADCAST_PORT);
                    while(BROADCAST) {
                        socket.send(packet);
                        Log.i(LOG_TAG, "Broadcast packet sent: " + packet.getAddress().toString());
                        Thread.sleep(BROADCAST_INTERVAL);
                    }
                    Log.i(LOG_TAG, "Broadcaster ending!");
                    socket.disconnect();
                    socket.close();
                }
                catch(SocketException e) {
                    Log.e(LOG_TAG, "SocketExceltion in broadcast: " + e);
                    Log.i(LOG_TAG, "Broadcaster ending!");
                }
                catch(IOException e) {
                    Log.e(LOG_TAG, "IOException in broadcast: " + e);
                    Log.i(LOG_TAG, "Broadcaster ending!");
                }
                catch(InterruptedException e) {
                    Log.e(LOG_TAG, "InterruptedException in broadcast: " + e);
                    Log.i(LOG_TAG, "Broadcaster ending!");
                }
            }
        });
        broadcastThread.start();
    }

    public static void stopNameBroadcast() {
        BROADCAST = false;
        if (broadcastThread != null) {
            broadcastThread.interrupt();
            broadcastThread = null;
        }
    }


    public static InetAddress getBroadcastIp() {
        // Function to return the broadcast address, based on the IP address of the device
        try {
            int ipAddress = getSelfIp();
            String addressString = toBroadcastIp(ipAddress);
            return InetAddress.getByName(addressString);
        }
        catch(UnknownHostException e) {
            Log.e(LOG_TAG, "UnknownHostException in getBroadcastIP: " + e);
            return null;
        }
    }

    private static String toBroadcastIp(int ip) {
        // Returns converts an IP address in int format to a formatted string
        return (ip & 0xFF) + "." +
                ((ip >> 8) & 0xFF) + "." +
                ((ip >> 16) & 0xFF) + "." +
                "255";
    }

    public static void startPeerDiscovery(final Activity activty, final ListView peersList) {
        final ArrayList<String> peers = new ArrayList<>();
        long start = System.currentTimeMillis();
        final long end = start + 5*1000; // 10 seconds * 1000 ms/sec
        DISCOVERY = true;
        discoveryThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mSocket == null) {
                        mSocket = new DatagramSocket(BROADCAST_PORT);
                        mSocket.setReuseAddress(true);
                    }
                    mSocket.setReuseAddress(true);
                    byte[] buffer = new byte[BROADCAST_BUF_SIZE];
                    while (DISCOVERY && System.currentTimeMillis() < end) {
                        DatagramPacket packet = new DatagramPacket(buffer, BROADCAST_BUF_SIZE);
                        mSocket.setSoTimeout(5000);
                        mSocket.receive(packet);
                        final String data = new String(buffer, 0, packet.getLength());
                        String action = data.substring(0, 4);
                        if (action.equals("MSG:")) {
                            activty.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    String message = data.substring(4, data.length());
                                    Toast.makeText(
                                            activty.getApplicationContext(),
                                            message, Toast.LENGTH_LONG).show();
                                }
                            });
                        } else if (action.equals("ADD:")) {
                            InetAddress ip = packet.getAddress();
                            String name = data.substring(4, data.length());
                            if (peersMap.get(name) == null && !isSelf(ip)) {
                                peersMap.put(name, ip);
                            }
                        }
                    }

                    for (String key : peersMap.keySet()) {
                        peers.add(key);
                        final ArrayAdapter adapter = new ArrayAdapter(
                                AppGlobals.getContext(), R.layout.list_layout, R.id.tv_peer_list, peers);
                        activty.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                peersList.setAdapter(null);
                                peersList.setAdapter(adapter);
                                peersMap.clear();
                                stopDiscovery(5000, activty, peersList);
                            }
                        });
                    }
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    activty.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            peersList.setAdapter(null);
                            peersMap.clear();
                            stopDiscovery(5000, activty, peersList);
                        }
                    });
                }
            }
        });
        discoveryThread.start();
    }

    public static void stopDiscovery() {
        DISCOVERY = false;
        if (discoveryThread != null) {
            discoveryThread.interrupt();
            discoveryThread = null;
        }
    }

    public static void stopDiscovery(int restartTime, final Activity activty,
                                     final ListView peersList) {
        stopDiscovery();
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                System.out.println("Restarting discovery");
                startPeerDiscovery(activty, peersList);
            }
        }, restartTime);
    }

    public static boolean isSelf(InetAddress inetAddress) {
        int ipAddress = getSelfIp();
        String realIP = String.format("%d.%d.%d.%d",
                (ipAddress & 0xff),
                (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff),
                (ipAddress >> 24 & 0xff));
        return realIP.equals(inetAddress.getHostAddress());
    }

    private static int getSelfIp() {
        Context context = AppGlobals.getContext();
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        return wifiInfo.getIpAddress();
    }
}
