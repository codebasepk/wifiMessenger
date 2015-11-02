package com.byteshaft.wifimessenger.utils;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.byteshaft.wifimessenger.CallActivity;
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

    private static final int DISCOVER_PORT = 50003;
    private static boolean DISCOVER;
    private static Thread discoverThread;
    private static DatagramSocket sDataSocket;
    private static boolean cleanLoopStart;

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
                    DatagramPacket packet = new DatagramPacket(message, message.length, broadcastIP, DISCOVER_PORT);
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

    public static void discover(final Activity activity, final ListView peersList) {
        DISCOVER = true;
        final ArrayList<String> peers = new ArrayList<>();
        long start = System.currentTimeMillis();
        final long end = start + 10*1000; // 10 seconds * 1000 ms/sec
        discoverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (sDataSocket == null) {
                        sDataSocket = new DatagramSocket(DISCOVER_PORT);
                        sDataSocket.setReuseAddress(true);
                    }
                    byte[] buffer = new byte[BROADCAST_BUF_SIZE];
                    while (DISCOVER && System.currentTimeMillis() < end) {
                        DatagramPacket packet = new DatagramPacket(buffer, BROADCAST_BUF_SIZE);
                        sDataSocket.setSoTimeout(15000);
                        sDataSocket.receive(packet);
                        InetAddress ip = packet.getAddress();
                        String data = new String(buffer, 0, packet.getLength());
                        String action = data.substring(0, 4);
                        if (action.equals("ADD:")) {
                            String name = data.substring(4, data.length());
                            if (peersMap.get(name) == null && !isSelf(ip)) {
                                peersMap.put(name, ip);
                            }
                        }
                    }

                    for (String key: peersMap.keySet()) {
                        peers.add(key);
                    }

                    final ArrayAdapter adapter = new ArrayAdapter(
                            AppGlobals.getContext(),
                            R.layout.list_layout,
                            R.id.tv_peer_list,
                            peers
                    );

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            peersList.setAdapter(null);
                            peersList.setAdapter(adapter);
                            stopDiscovery(5000, activity, peersList);
                        }
                    });

                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (IOException e) {

                    System.out.println("Faced IOException");
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            peersList.setAdapter(null);
                            stopDiscovery(5000, activity, peersList);
                        }
                    });
                    e.printStackTrace();
                }
            }
        });
        discoverThread.start();
    }

    public static void stopDiscover() {
        DISCOVER = false;
    }

    public static void startPeerDiscovery() {
        DISCOVERY = true;
        discoveryThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (mSocket == null) {
                        mSocket = new DatagramSocket(BROADCAST_PORT);
                        mSocket.setReuseAddress(true);
                    }
                    byte[] buffer = new byte[BROADCAST_BUF_SIZE];
                    while (DISCOVERY) {
                        DatagramPacket packet = new DatagramPacket(buffer, BROADCAST_BUF_SIZE);
                        mSocket.setSoTimeout(15000);
                        mSocket.receive(packet);
                        InetAddress ip = packet.getAddress();
                        final String data = new String(buffer, 0, packet.getLength());
                        String action = data.substring(0, 4);
                        switch (action) {
                            case "MSG:":
                                String message = data.substring(4, data.length());
                                System.out.println(message);
                                break;
                            case "ADD:":
                                String name = data.substring(4, data.length());
                                if (peersMap.get(name) == null && !isSelf(ip)) {
                                    peersMap.put(name, ip);
                                }
                                break;
                            case "CAL:":
                                String nameCAL = data.substring(4, data.length());
//                                Intent intent = new Intent(AppGlobals.getContext(), CallActivity.class);
//                                intent.putExtra("CONTACT_NAME", nameCAL);
//                                intent.putExtra("CALL_STATE", "INCOMING");
//                                intent.putExtra("IP_ADDRESS", ip.getHostAddress());
                                IntentFilter filter = new IntentFilter("com.call");
                                AppGlobals.getContext().registerReceiver(receiver, filter);
                                Intent intent = new Intent("com.call");
                                intent.putExtra("CONTACT_NAME", nameCAL);
                                intent.putExtra("CALL_STATE", "INCOMING");
                                intent.putExtra("IP_ADDRESS", ip.getHostAddress());
                                AppGlobals.getContext().sendBroadcast(intent);
                                Log.i("CAL", "Incoming Call");
                                break;
                            case "ACC:":
                                AudioCall callACC = AudioCall.getInstance(ip);
                                callACC.startCall();
                                CallActivity.IN_CALL = true;
                                Log.i("ACC", "started calling");
                                break;
                            case "REJ:":
                                if (CallActivity.isRunning()) {
                                    CallActivity.getInstance().finish();
                                }
                                Log.i("REJ", "Call Rejected");
                                break;
                            case "END:":
                                AudioCall callEND = AudioCall.getInstance(ip);
                                callEND.endCall();
                                if (CallActivity.isRunning()) {
                                    CallActivity.getInstance().finish();
                                }
                                CallActivity.IN_CALL = false;
                                Log.i("END", "Call Ended");
                                break;
                        }
                    }
                } catch (SocketException e) {
                    startPeerDiscovery();
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
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
//        stopDiscovery();
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                System.out.println("Restarting discovery");
                discover(activty, peersList);
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

    private static BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Intent callIntent = new Intent(AppGlobals.getContext(), CallActivity.class);
            callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            callIntent.putExtra("CONTACT_NAME", intent.getExtras().getString("CONTACT_NAME"));
            callIntent.putExtra("CALL_STATE", intent.getExtras().getString("CALL_STATE"));
            callIntent.putExtra("IP_ADDRESS", intent.getExtras().getString("IP_ADDRESS"));
            AppGlobals.getContext().startActivity(callIntent);
        }
    };
}
