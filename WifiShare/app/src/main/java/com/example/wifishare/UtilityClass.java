package com.example.wifishare;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;

public class UtilityClass {
    public static int BUFFER_SIZE_SMALL=4096;
    public static int BUFFER_SIZE_MEDIUM=16384;
    public static int BUFFER_SIZE_LARGE=65536;

    public static String FILE_NAME_ACK="FILENAMEACK\n";
    public static String REQUEST_TO_SEND="REQUEST_TO_SEND\n";
    public static String ALLOW_TO_RECV="ALLOW_TO_RECV\n";
    public static String NOT_ALLOW_TO_RECV="NOT_ALLOW_TO_RECV\n";
    public static String CONNECTION_ESTABLISHED_CLIENT="CONNECTION_ESTABLISHED_CLIENT_ACK\n";
    public static String CONNECTION_ESTABLISHED_SERVER="CONNECTION_ESTABLISHED_SERVER_ACK\n";
    public static String RECEIVING_ACK="RECEIVING_ACK\n";


    public static String BASEPATH="WifiDocs";
    private static String IP_ADDR="";
    public static String SEPARATOR="-_-";
    public static void setIpAddress(String ip){
        IP_ADDR=ip;
    }
    public static String getIpAddress(){
        return IP_ADDR;
    }
    public static  int getPortAddress(){
        return 5000;
    }
    public static ArrayList<String> getIpsFromHotspot(Boolean onlyReachables){

            BufferedReader bufRead = null;
            ArrayList<String> result = null;

            try {
                result = new ArrayList<String>();
                bufRead = new BufferedReader(new FileReader("/proc/net/arp"));
                String fileLine;
                while ((fileLine = bufRead.readLine()) != null) {

                    String[] splitted = fileLine.split(" +");

                    if ((splitted != null) && (splitted.length >= 4)) {

                        String mac = splitted[3];
                        if (mac.matches("..:..:..:..:..:..")) {
                            boolean isReachable = pingCmd(splitted[0]);/**
                             * Method to Ping  IP Address
                             * @return true if the IP address is reachable
                             */
                            if (!onlyReachables || isReachable) {
                                result.add(splitted[0]);
                            }
                        }
                    }
                }
            } catch (Exception e) {

            } finally {
                try {
                    bufRead.close();
                } catch (IOException e) {

                }
            }

            return result;
    }




    private static boolean pingCmd(String addr){
        try {
            String ping = "ping  -c 1 -W 1 " + addr;
            Runtime run = Runtime.getRuntime();
            Process pro = run.exec(ping);
            try {
                pro.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            int exit = pro.exitValue();
            if (exit == 0) {
                return true;
            } else {
                //ip address is not reachable
                return false;
            }
        }
        catch (IOException e) {
        }
        return false;
    }


    public static void getSocket(final String ipAddr,final int portAddr,final InterfacesClass.getClientSocket sockInterface){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    InetAddress address=InetAddress.getByName(ipAddr);
                    Socket s=new Socket(address,portAddr);
                    sockInterface.onClientSocketRecvd(s);
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                    sockInterface.onClientSocketRecvd(null);
                } catch (IOException e) {
                    e.printStackTrace();
                    sockInterface.onClientSocketRecvd(null);
                }
            }
        }).start();

    }
    public static String getFileName(Context context,Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

}
