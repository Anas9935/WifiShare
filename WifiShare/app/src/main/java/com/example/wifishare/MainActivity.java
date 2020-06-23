package com.example.wifishare;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.PathUtils;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.icu.util.Output;
//import android.net.Uri;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;
import static java.lang.System.in;
import static java.lang.System.out;

public class MainActivity extends AppCompatActivity {

    Button create,close,select,send,create2,close2;
    TextView tv,tv2;

    private Socket socket,socket2;
    private ServerSocket serverSocket;
    Handler updateConvHandler;
    Thread serverThread=null;
    static final int PortNo=5000;
    private static  String My_IP="192.168.43.140";
    private static int selectCode=101;
    String path_of_file="";
    Uri fileUri;
    private static String TAG="MAIN ACTVIITY.";
    private static int MAX_BUFFER_SIZE=65536;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        create=findViewById(R.id.btn);
        close=findViewById(R.id.btn2);
        create2=findViewById(R.id.btn5);
        close2=findViewById(R.id.btn6);
        select=findViewById(R.id.btn3);
        send=findViewById(R.id.btn4);
        tv=findViewById(R.id.textview);
        tv2=findViewById(R.id.textview2);
        Button server,client;
        server=findViewById(R.id.serverBtn);
        client=findViewById(R.id.clientBtn);

        server.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(MainActivity.this,ServerActivity.class);
                startActivity(intent);
            }
        });
        client.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(MainActivity.this,ClientActivity.class);
                startActivity(intent);
            }
        });





        create.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                WifiManager wifiManager=(WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
                WifiInfo info=wifiManager.getConnectionInfo();
                String inf=info.toString();

                int ip=info.getIpAddress();
                String ipAddress = Formatter.formatIpAddress(ip);


                tv.setText(inf+" "+ipAddress);
                    //UtilityClass.getAvailableConnections(MainActivity.this);
                    createClient();
         //       createServer();
            }
        });
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeClient();
            }
        });
        select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(Intent.ACTION_GET_CONTENT);
                if (android.os.Build.MANUFACTURER.equalsIgnoreCase("samsung")) {
                    intent = new Intent("com.sec.android.app.myfiles.PICK_DATA");
                    intent.putExtra("CONTENT_TYPE", "*/*");
                    intent.addCategory(Intent.CATEGORY_DEFAULT);
                } else {


                    intent = new Intent(Intent.ACTION_GET_CONTENT); // or ACTION_OPEN_DOCUMENT
                    intent.setType("*/*");
                  //  intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);

                }
                startActivityForResult(intent,selectCode);

            }
        });
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!path_of_file.equals("")){
                    if(socket!=null){
                        sendFileNameAndSize();
//                        recieveAck();
                        //sendFile();
                    }
                }
            }
        });
        create2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createClient2();
            }
        });
        close2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkAvailableConnections();
            }
        });
    }

    private void checkAvailableConnections() {
        Log.e(TAG, "checkAvailableConnections: " );
        final ArrayList<String> ips=UtilityClass.getIpsFromHotspot(false);
        if(ips.size()!=0){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for(final String i:ips){
                        try {
                            //Log.e(TAG, "run: "+i+"ip addr" );
                            InetAddress addr = InetAddress.getByName(i);
                            Socket socket=new Socket(addr,UtilityClass.getPortAddress());
                            if(socket.isConnected()){
                                Log.e(TAG, "run: socket is connected to "+i );
                                break;
                            }
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        } catch (SocketException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                }
            }).start();

        }else {

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void recieveFileName() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    BufferedReader input=new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String recv=input.readLine();
                    recv+="\n";
                    if(recv.equals(UtilityClass.REQUEST_TO_SEND)){
                        Log.e(TAG, "run: Receivvdd : "+recv );
                        OutputStream os=socket.getOutputStream();
                        DataOutputStream dos=new DataOutputStream(os);
                        if(os!=null){
                            os.write(UtilityClass.ALLOW_TO_RECV.getBytes());
                            //receiving filename
                            String recvName=input.readLine();
                            final String []file=recvName.split(UtilityClass.SEPARATOR);
                            Log.e(TAG, "run: "+file[0]+" "+file[1] );
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    saveFile(file[0],Integer.parseInt(file[1]));
                                    Log.e(TAG, "run: goes to save the file" );
                                }
                            });

                            os.flush();
                        }else{
                            Log.e(TAG, "run: "+"stream is null 162" );
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void saveFile(final String name, final int size) {
        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)||
                !Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED_READ_ONLY)){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {

                        File file = new File(getExternalFilesDir("WifiShare/Pictures"), name);
                        if(file.length()>0){
                            Boolean del=file.delete();
                            file=new File(getExternalFilesDir("WifiShare/Pictures"),name);
                        }
                        //BufferedReader is = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        InputStream is=socket.getInputStream();
                        OutputStream os = new FileOutputStream(file);
                        DataInputStream dos=new DataInputStream(is);
                        int b=UtilityClass.BUFFER_SIZE_MEDIUM;
                        socket.setReceiveBufferSize(b);
                        byte[] data = new byte[UtilityClass.BUFFER_SIZE_MEDIUM];
                        int total=0;
                        int ocunt=0;
                        //617
                        int bytesRead = 0;//is.read(data, 0, data.length);

                        Log.e(TAG, "run: start: "+System.currentTimeMillis() );
                        while((bytesRead=dos.read(data,0,data.length))!=-1){
                            os.write(data,0,bytesRead);
                            ocunt+=bytesRead;
                           // Log.e(TAG, "saveFile: recved"+bytesRead+"Tot: "+total+" "+ocunt);
                            total++;

                        }
                        Log.e(TAG, "run: end: "+System.currentTimeMillis() );
                       // Log.e(TAG, "run: "+Environment.DIRECTORY_DOWNLOADS );

                        //is.read(data);
                        //os.write(data);
                        //bis.close();
                        //bos.close();
                        is.close();
                        os.close();

                        Log.e(TAG, "saveFile: "+file.length());
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        }
    }

    private void recvFromBoth() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    File file = new File(getExternalFilesDir("WifiShare/Pictures"), "Something");
                    InputStream is1 = socket.getInputStream();
                    InputStream is2 = socket2.getInputStream();
                    OutputStream os = new FileOutputStream(file);
                    DataInputStream dos1 = new DataInputStream(is1);
                    DataInputStream dos2 = new DataInputStream(is2);
                    //int b=UtilityClass.BUFFER_SIZE_MEDIUM;
                    //socket.setReceiveBufferSize(b);
                    byte[] data = new byte[UtilityClass.BUFFER_SIZE_MEDIUM];
                    int total = 0;
                    int ocunt = 0;
                    //617
                    int bytesRead = 0;//is.read(data, 0, data.length);

                    Log.e(TAG, "run: start: " + System.currentTimeMillis());
                    while ((bytesRead = dos1.read(data, 0, data.length)) != -1) {
                        os.write(data, 0, bytesRead);
                        if ((bytesRead = dos2.read(data, 0, data.length)) != -1) {
                            os.write(data, 0, bytesRead);
                        } else {
                            break;
                        }

                    }
                    Log.e(TAG, "run: end: " + System.currentTimeMillis());

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }
    private void recieveAck() {
        //String receive=null;
        Log.e(TAG, "recieveAck: "+"you  ar n ack" );
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //Log.e(TAG, "run: HERE 1" );
                    BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    Log.e(TAG, "run:receviving..." );
                    final String receive=input.readLine();
                    Log.e(TAG, "run: ACK"+receive );
                    if(receive.equals("FILENAME_RECVD")){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                sendFile();
                            }
                        });
                    }else{
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MainActivity.this,"ACK Not RECVD [code:]"+receive,Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "run: "+"ioe" );
                    //return false;
                }
            }
        }).start();


    }

    private void sendFile() {
        Log.e(TAG, "sendFile: "+"You are in sendfile" );

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    OutputStream os=null;
                    InputStream is=getContentResolver().openInputStream(fileUri);
                    //first send filename and fileSize
                    //sendFileNameAndSize();

                    byte[] mybytearray = new byte[MAX_BUFFER_SIZE];//(int) myFile.length()];
                    // fis = new FileInputStream(myFile);
                    //bis = new BufferedInputStream(fis);
                    int br=0;
                    os = socket.getOutputStream();
                    while((br=is.read(mybytearray,0,MAX_BUFFER_SIZE))!=-1){
                        os.write(mybytearray,0,MAX_BUFFER_SIZE);
                        Log.e(TAG, "sendFile: "+br );
                        //              Thread.sleep(1000);
                    }
                    Log.e(TAG, "sendFile: "+"END" );
                    //os.write("END_OF_FILE".getBytes());
                    is.close();

                    os.flush();


                }
                catch (FileNotFoundException e){
                    e.printStackTrace();
                }
                catch (IOException e){
                    e.printStackTrace();

                }
            }
        }).start();



    }

    private void sendFileNameAndSize() {
        new Thread(new Runnable() {
            @Override
            public void run() {
//                    final String sendData="Hello from Server";
                String filename=getFileName(fileUri);

                long fileSize=0;
                try {
                    AssetFileDescriptor afd = getContentResolver().openAssetFileDescriptor(fileUri, "r");
                    fileSize = afd.getLength();
                    afd.close();
                }catch (Exception e){
                    e.printStackTrace();
                }

                OutputStream stream = null;
                try {
                    stream = socket.getOutputStream();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                DataOutputStream commStream=new DataOutputStream(stream);
                String temp=filename+"-_-"+fileSize;


                if (stream != null) {

                    Log.e("THis", "run: "+"output generated" );
                    try {
                        commStream.write(temp.getBytes());
                        recieveAck();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                Log.e("THis", "run: "+"filename send" );
            }
        }).start();

    }

    private void createServer() {
        this.serverThread=new Thread(new ServerThread());
        this.serverThread.start();
        Log.e("Main", "createServer: Listening" );

    }

    private void closeClient() {
        try {
            socket.close();
            Log.e("This", "closeClient: "+"Socket closed" );
        } catch (IOException e) {
            e.printStackTrace();

        }
    }


    private void createClient() {
        Log.e("This", "createClient: Client Is up" );
     //   new Thread(new ClientThread()).start();
        //chcek if android is higer than 6.0 or not
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.M){
            getWifiPermissions();
        }else{
            doGetWifi();
        }


    }
    private void createClient2(){
        Log.e(TAG, "createClient2: Client 2 is starting" );
       // new Thread(new Client2Thread()).start();
        openHotpsotSetting();

    }
    private void openHotpsotSetting(){
        final Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        final ComponentName cn = new ComponentName("com.android.settings", "com.android.settings.TetherSettings");
        intent.setComponent(cn);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity( intent);


    }

    /*
    private void setHotspotEnabled(){

        boolean isHotspotEnabled=false;
        WifiManager wifiManager=(WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiConfiguration wifiConfig = null;
        try{
            Method method=wifiManager.getClass().getMethod("getWifiApConfiguration");
            wifiConfig=(WifiConfiguration)method.invoke(wifiManager);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        try {
            if (isHotspotEnabled) { //disables wifi hotspot if it's already enabled
                wifiManager.setWifiEnabled(false);
            }

            Method method = wifiManager.getClass()
                    .getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            isHotspotEnabled= (Boolean) method.invoke(wifiManager, wifiConfig, isHotspotEnabled);
        } catch (Exception e) {
            Log.e(this.getClass().toString(), "", e);
            isHotspotEnabled=false;
        }
    }
    private     boolean checkWritePermissions(){
        if(Build.VERSION.SDK_INT< Build.VERSION_CODES.O){
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M) {

                if (!Settings.System.canWrite(this)) {
                    Log.v("DANG", " " + !Settings.System.canWrite(this));
                    Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
                    intent.setData(Uri.parse("package:" + this.getPackageName()));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    this.startActivity(intent);
                    return false;
                }else{
                    return true;
                }
            }else{
                return true;
            }

        }else{
            Log.e(TAG, "createClient2: Version Already Giver The System Settings," );
            return true;
        }
    }
    */



    class Client2Thread implements  Runnable{

        @Override
        public void run() {
            try {
                //Log.e("Here", "run: "+"Trying to reach" );
                InetAddress serverAddr = InetAddress.getByName(My_IP);
                socket2 = new Socket(serverAddr,6000);
                Log.e(TAG, "run: "+"Client 2 is up" );
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tv2.setText("Client 2 is Up");
                        recvFromBoth();
                    }
                });

            } catch (UnknownHostException e1) {
                e1.printStackTrace();
                Log.e("This", "run: uhe" );
            } catch (IOException e1) {
                e1.printStackTrace();
                Log.e("This", "run: ioe" );
            }
        }
    }

    class ClientThread implements Runnable {

        @Override
        public void run() {

            try {
                //Log.e("Here", "run: "+"Trying to reach" );
                InetAddress serverAddr = InetAddress.getByName(My_IP);
                socket = new Socket(serverAddr,PortNo);
                //socket.connect();

                Log.e("TAG", "run: "+"Successful"+socket.getLocalAddress() );

                OutputStream stream = socket.getOutputStream();
                DataOutputStream commStream = new DataOutputStream(stream);
                commStream.write("Hello From Client".getBytes());

                final String receive;
                BufferedReader input = null;
                try {
                    input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    receive=input.readLine();
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            // Stuff that updates the UI
                            tv.setText(receive);
                            recieveFileName();
                        }
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                }



            } catch (UnknownHostException e1) {
                e1.printStackTrace();
                Log.e("This", "run: uhe" );
            } catch (IOException e1) {
                e1.printStackTrace();
                Log.e("This", "run: ioe" );
            }

        }

    }
    class ServerThread implements Runnable{
        Socket socket = null;
        String send,receive;
        @Override
        public void run() {

            try {
                serverSocket = new ServerSocket(PortNo);
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (!Thread.currentThread().isInterrupted()) {

                try {

                    socket = serverSocket.accept();
                    Log.e("Main", "run: Connected" );

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            BufferedReader input = null;
                            try {
                                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                                receive=input.readLine();
                                runOnUiThread(new Runnable() {

                                    @Override
                                    public void run() {

                                        // Stuff that updates the UI
                                        tv.setText(receive);
                                    }
                                });
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                    }).start();

                    Log.e("Main", "run: "+"Input closed" );


                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            final String sendData="Hello from Server";
                            OutputStream stream = null;
                            try {
                                stream = socket.getOutputStream();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            final PrintWriter output;
                            if (stream != null) {
                                output = new PrintWriter(stream);
                                Log.e("THis", "run: "+"output generated" );
                                output.print(sendData);
                                output.flush();
                            }

                            Log.e("THis", "run: "+"output sent" );
                        }
                    }).start();
                    //output.write(sendData);
                    //output.flush();
//                    DataOutputStream commStream = new DataOutputStream(stream);
                    //commStream.write(sendData.getBytes());





                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
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

    private void getWifiPermissions(){

        if (ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 102);
        } else {
            doGetWifi(); // the actual wifi scanning
        }
    }
    private void doGetWifi(){
        if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},103);
        }else {

            //TODO need to implement for higher android versions above 29
            final WifiManager manager = (WifiManager) MainActivity.this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                manager.setWifiEnabled(true);
                if (manager.isWifiEnabled()) {
                    Log.e(TAG, "getAvailableConnections: " + "Wifi On");
                }
            } else {
                Toast.makeText(this, "Wifi Is not  Enabled. Enable Now", Toast.LENGTH_SHORT).show();
            }

            BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    boolean success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
                    if (success) {
                        //scan is success
                        List<ScanResult> result = manager.getScanResults();
                        Log.e(TAG, "onReceive: " + result.size());
                        for (ScanResult i : result) {
                            Log.e("Utility class", "onReceive: " + i.toString());
                        }
                    } else {
                        Toast.makeText(context, "Listener failed", Toast.LENGTH_SHORT).show();
                    }
                }
            };
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            registerReceiver(broadcastReceiver, intentFilter);
            boolean success = manager.startScan();
            if (!success) {
                Toast.makeText(MainActivity.this, "Scanning Failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == selectCode) {
            if (resultCode == RESULT_OK) {
//                path_of_file=data.getDataString();
                fileUri = data.getData();
                path_of_file = fileUri.getPath();
                tv2.setText(fileUri.getPath());
                //URI u= null;


            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==102){
            if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                doGetWifi();
            }else{
                Log.e(TAG, "onRequestPermissionsResult: "+"Permission not available" );
            }
        }else if(requestCode==103){
            if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                doGetWifi();
            }else{
                Log.e(TAG, "onRequestPermissionsResult: "+"Permission Fine Locn not available" );
            }
        }
    }
}
