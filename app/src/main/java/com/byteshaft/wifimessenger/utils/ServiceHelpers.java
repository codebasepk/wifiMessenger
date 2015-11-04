package com.byteshaft.wifimessenger.utils;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.byteshaft.wifimessenger.R;
import com.byteshaft.wifimessenger.activities.CallActivity;
import com.byteshaft.wifimessenger.activities.ChatActivity;
import com.byteshaft.wifimessenger.database.MessagesDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;


public class ServiceHelpers {

    private static boolean BRUTE_STOP;

    public static final int DISCOVER_PORT = 50003;
    public static boolean DISCOVER;
    private static Thread discoverThread;
    private static DatagramSocket sDataSocket;
    private static boolean cleanLoopStart;

    private final static int BROADCAST_INTERVAL = 1000;
    public final static int BROADCAST_PORT = 50001;
    private static final int BROADCAST_BUF_SIZE = 10240;
    private static boolean BROADCAST;
    private static boolean DISCOVERY;

    private static Thread broadcastThread;
    private static Thread discoveryThread;

    private static DatagramSocket mSocket;

    private static HashMap<String, String> peersMap = new HashMap<>();
    public static ArrayList<HashMap> peersArray = new ArrayList<>();

    private static String LOG_TAG = "wifiMessenger";

    public static ArrayList<HashMap> getPeersList() {
        return peersArray;
    }

    public static boolean isPeerListEmpty() {
        return peersArray.isEmpty();
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
        peersArray.clear();
        peersMap.clear();
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
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
//                        sDataSocket.setSoTimeout(100);
                        sDataSocket.receive(packet);
                        String data = new String(buffer, 0, packet.getLength());
                        String ip = packet.getAddress().getHostAddress();
                        String action = data.substring(0, 4);
                        if (action.equals("ADD:")) {
                            System.out.println(data);
                            System.out.println(packet.getAddress().getHostAddress());
                            String nameData = data.substring(4, data.length());
                            JSONObject object = new JSONObject(nameData);
                            String name = (String) object.get("name");
                            String deviceId = (String) object.get("id");
                            if (peersMap.get("name") == null && !isSelf(ip)) {
                                peersMap.put("name", name);
                                peersMap.put("ip", ip);
                                peersMap.put("device_id", deviceId);
                                peersMap.put("user_table", name+"_"+deviceId);
                                peersArray.add(peersMap);
                            }
                        }
                    }

                    ArrayList<String> peerNames = new ArrayList<>();
                    for (HashMap map: peersArray) {
                        peerNames.add((String) map.get("name"));
                    }

                    final ArrayAdapter adapter = new ArrayAdapter(
                            AppGlobals.getContext(),
                            R.layout.list_layout,
                            R.id.tv_peer_list,
                            peerNames
                    );

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (BRUTE_STOP) {
                                BRUTE_STOP = false;
                                return;
                            }
                            peersList.setAdapter(null);
                            peersList.setAdapter(adapter);
                            restartDiscovery(5000, activity, peersList);
                        }
                    });

                } catch (SocketException e) {
                    DISCOVER = false;
                    e.printStackTrace();
                } catch (IOException e) {
                    if (BRUTE_STOP) {
                        BRUTE_STOP = false;
                        return;
                    }
                    DISCOVER = false;
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            peersList.setAdapter(null);
                            restartDiscovery(5000, activity, peersList);
                        }
                    });
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        discoverThread.start();
    }

    public static void stopDiscover() {
        BRUTE_STOP = true;
        DISCOVER = false;
        if (discoverThread != null) {
            discoverThread.interrupt();
            discoverThread = null;
        }
    }

    public static void startListeningForCommands() {
        System.out.println("Listening");
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
//                        mSocket.setSoTimeout(15000);
                        mSocket.receive(packet);
                        InetAddress ip = packet.getAddress();
                        final String data = new String(buffer, 0, packet.getLength());
                        String action = data.substring(0, 4);
                        System.out.println(data);
                        switch (action) {
                            case "MSG:":
                                String message = data.substring(4, data.length());
                                try {
                                    JSONObject object = new JSONObject(message);
                                    String deviceId = (String) object.get("device_id");
                                    String sender = (String) object.get("sender");
                                    String messageText = (String) object.get("text");
                                    String messageTime = (String) object.get("time");
                                    MessagesDatabase database = new MessagesDatabase(AppGlobals.getContext());
                                    database.addNewMessageToThread(
                                            sender + "_" + deviceId, messageText, "1", messageTime);

                                    IntentFilter filter = new IntentFilter("sms_notification");
                                    AppGlobals.getContext().registerReceiver(notificationReceiver, filter);
                                    Intent intent = new Intent("sms_notification");
                                    intent.putExtra("sender", sender);
                                    intent.putExtra("message", messageText);
                                    intent.putExtra("time", messageTime);
                                    intent.putExtra("unique_id", sender+"_"+deviceId);
                                    intent.putExtra("ip_address", packet.getAddress().getHostAddress());
                                    AppGlobals.getContext().sendBroadcast(intent);

                                    if (ChatActivity.isRunning()) {
                                        if (ChatActivity.getInstance().isChatVisibleForContact(sender)) {
                                            final HashMap<String, String> mapTemp = new HashMap<>();
                                            mapTemp.put("direction", "1");
                                            mapTemp.put("body", messageText);
                                            if (ChatActivity.getInstance().adapter != null) {
                                                ChatActivity.getInstance().runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        ChatActivity.getInstance().updateAdapter(mapTemp);
                                                    }
                                                });
                                            }
                                        }
                                    }
                                } catch (JSONException e) {
                                    startListeningForCommands();
                                    e.printStackTrace();
                                }
                                break;
                            case "CAL:":
                                String nameCAL = data.substring(4, data.length());
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
                                if (CallActivity.isVisible() && CallActivity.isRunning()) {
                                    CallActivity.getInstance().setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
                                }
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
                            case "ADD:":
                                System.out.println("Discovery:" +data);
                                break;
                        }
                    }
                } catch (SocketException e) {
                    startListeningForCommands();
                    e.printStackTrace();
                } catch (IOException e) {
                    startListeningForCommands();
                    e.printStackTrace();
                }
            }
        });
        discoveryThread.start();
    }

    public static void restartDiscovery(int restartTime, final Activity activty,
                                        final ListView peersList) {
//        restartDiscovery();
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                System.out.println("Restarting discovery");
                discover(activty, peersList);
            }
        }, restartTime);
    }

    public static boolean isSelf(String foundIp) {
        int ipAddress = getSelfIp();
        String realIP = String.format("%d.%d.%d.%d",
                (ipAddress & 0xff),
                (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff),
                (ipAddress >> 24 & 0xff));
        return realIP.equals(foundIp);
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

    private static BroadcastReceiver notificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String sender = intent.getExtras().getString("sender");
            String message = intent.getExtras().getString("message");
            String uniqueId = intent.getExtras().getString("unique_id");
            String ipAddress = intent.getExtras().getString("ip_address");
            try {
                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Ringtone r = RingtoneManager.getRingtone(AppGlobals.getContext(), notification);
                r.play();
            } catch (Exception e) {
                e.printStackTrace();
            }
            showNotification(sender, message, uniqueId, ipAddress);
        }
    };

    private static void showNotification(String title, String content, String uniqueId, String ip) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(AppGlobals.getContext());
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentTitle("Message from " + title);
        builder.setContentText(content);
        builder.setAutoCancel(true);

        Intent resultIntent = new Intent(AppGlobals.getContext(), ChatActivity.class);
        resultIntent.putExtra("CONTACT_NAME", title);
        resultIntent.putExtra("IP_ADDRESS", ip);
        resultIntent.putExtra("user_table", uniqueId);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                AppGlobals.getContext(), 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);


        NotificationManager manager = (NotificationManager) AppGlobals.getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(998, builder.build());
    }
}
