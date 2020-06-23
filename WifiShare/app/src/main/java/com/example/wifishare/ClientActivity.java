package com.example.wifishare;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class ClientActivity extends AppCompatActivity {

    Button search,recieve,choose,send,close,hotpsot;
    TextView status,data_exchng_send,data_exchng_recv;

    ProgressBar pbar;


    Socket socket;
    int conn=0,recv=0,sent=0;

    private String TAG="CLIENT_ACTIVITY";
    int currentPort=UtilityClass.getPortAddress();


    Uri[] fileUris;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        initializeViews();

        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                status.setText("WAITING");
                checkAvailableConnections();

            }
        });
        recieve.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                receiveFileAlter();
            }
        });
        choose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(intent,101);
            }
        });
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(fileUris.length!=0){

                    new Thread(new Runnable() {
                        @Override
                        public void run() {

                            sendFileAlter();
                        }
                    }).start();
                }
            }
        });
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(socket!=null){
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
            }
        });
        hotpsot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!checkHotspotState()){
                    final Intent intent = new Intent(Intent.ACTION_MAIN, null);
                    intent.addCategory(Intent.CATEGORY_LAUNCHER);
                    final ComponentName cn = new ComponentName("com.android.settings", "com.android.settings.TetherSettings");
                    intent.setComponent(cn);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity( intent);
                }else{
                    Toast.makeText(ClientActivity.this, "Hotspot is Activated", Toast.LENGTH_SHORT).show();
                }


            }
        });
    }
    private boolean checkHotspotState(){
        if(Build.VERSION.SDK_INT< Build.VERSION_CODES.O) {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            try {
                Method method = wifiManager.getClass().getDeclaredMethod("getWifiApState");
                method.setAccessible(true);
                int actualState = (Integer) method.invoke(wifiManager, (Object[]) null);
                if(actualState==12 || actualState==13){
                    return true;
                }else{
                    return false;
                }

            } catch (NoSuchMethodException e) {
                e.printStackTrace();
                return false;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                return false;
            } catch (InvocationTargetException e) {
                e.printStackTrace();
                return false;
            }
        }else{
            return false;
        }
    }

    private void checkAvailableConnections() {
        int count=5;
        while(count>0) {
            Log.e(TAG, "checkAvailableConnections: ");
            final ArrayList<String> ips = UtilityClass.getIpsFromHotspot(false);
            if (ips.size() != 0) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        for (final String i : ips) {
                            try {
                                //Log.e(TAG, "run: "+i+"ip addr" );
                                InetAddress addr = InetAddress.getByName(i);
                                socket = new Socket(addr, UtilityClass.getPortAddress());
                                if (socket.isConnected()) {
                                    Log.e(TAG, "run: socket is connected to " + i);
                                    UtilityClass.setIpAddress(i);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            status.setText("CONNECTION ESTABLISHED");
                                            conn = 1;
                                            boolean b = checkUpsNDowns();
                                            data_exchange_check();
                                        }
                                    });
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

            } else {

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        count--;
        }


    }
    /*
    private void sendFile(String fileName,long filesize,final Uri fileUri){

                try {
                    InetAddress addr = InetAddress.getByName(UtilityClass.getIpAddress());
                    currentPort += 2;
                    Socket subSock = new Socket(addr, currentPort);

                    InputStream is = getContentResolver().openInputStream(fileUri);
                    OutputStream os = subSock.getOutputStream();
                    byte[] data = new byte[UtilityClass.BUFFER_SIZE_MEDIUM];
                    int br = 0;
                    while ((br = is.read(data, 0, data.length)) != -1) {
                        os.write(data, 0, br);
                        Log.e(TAG, "run: Sending " + br);
                    }
                    Log.e(TAG, "run: Sending Complete");
                    subSock.close();
                    currentPort-=2;
                    is.close();
                    os.flush();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

    }


    private void sendFile(final Uri fileUri) {
        final String filename=UtilityClass.getFileName(this,fileUri);
        long fileSize=0;
        try {
            AssetFileDescriptor afd = getContentResolver().openAssetFileDescriptor(fileUri, "r");
            fileSize = afd.getLength();
            afd.close();
        }catch (Exception e){
            e.printStackTrace();
        }
        if(fileSize!=0){
            final long finalFileSize = fileSize;
                    try{
                        String filenameandsize=filename+UtilityClass.SEPARATOR+ finalFileSize;
                        OutputStream stream=socket.getOutputStream();
                        DataOutputStream dos=new DataOutputStream(stream);
                        dos.write(UtilityClass.REQUEST_TO_SEND.getBytes());
                        //request sent

                        BufferedReader input=new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        String recv=input.readLine();
                        recv+="\n";
                        if(recv.equals(UtilityClass.ALLOW_TO_RECV)){
                            //request accepted
                            dos.write(filenameandsize.getBytes());
                            sendFile(filename,finalFileSize,fileUri);
                        }



                    } catch (IOException e) {
                        e.printStackTrace();
                    }


        }
    }

    private void receiveFile(final String filename,final long filesize){

                //creating new socket for file recving
                File file=new File(getExternalFilesDir(UtilityClass.BASEPATH),filename);
                try{
                    InetAddress address=InetAddress.getByName(UtilityClass.getIpAddress());
                    currentPort+=2;
                    Log.e(TAG, "run: checking for port "+currentPort );
                    Socket s=new Socket(address,currentPort);
                    Log.e(TAG, "run: "+"Client  for file generated" );

                    InputStream is=s.getInputStream();
                    OutputStream os=new FileOutputStream(file);
                    DataInputStream dis=new DataInputStream(is);
                    byte[] data=new byte[UtilityClass.BUFFER_SIZE_MEDIUM];
                    int bytes_read=0;
                    int percent=0;
                    int prevPerc=0;
                    long total=0;
                    while((bytes_read=dis.read(data,0,data.length))!=-1){
                        os.write(data,0,bytes_read);
                        Log.e(TAG, "run: Recving "+bytes_read );
                        total+=bytes_read;
                        percent=(int)((total)*100/filesize);
                        if(percent!=prevPerc){
                            prevPerc=percent;
                            final int finalPercent = percent;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    percentTv.setText(finalPercent +" %");
                                }
                            });
                        }
                    }
                    currentPort-=2;
                    s.close();
                    Log.e(TAG, "run: File receiving complete" );
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

    }
    */


    private void receiveFileAlter(final String filename,final long size,final int port,final int iter){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                pbar.setVisibility(View.VISIBLE);
            }
        });
        File file=new File(getExternalFilesDir(UtilityClass.BASEPATH),filename);

        try{
            InetAddress addr=InetAddress.getByName(UtilityClass.getIpAddress());
            Socket subsock=new Socket(addr,port);
            Log.e(TAG, "receiveFileAlter: "+"Server Connected"+iter );

            BufferedReader inp=new BufferedReader(new InputStreamReader(subsock.getInputStream()));
            String req=inp.readLine();
            String[] parts=req.split(UtilityClass.SEPARATOR);
            parts[0]+="\n";
            if(parts[0].equals(UtilityClass.REQUEST_TO_SEND) && parts[1].equals(String.valueOf(iter))){
                //correct file requested
                DataOutputStream dos=new DataOutputStream(subsock.getOutputStream());
                String toSend="ALLOW_TO_RECV"+UtilityClass.SEPARATOR+iter+"\n";
                dos.write(toSend.getBytes());

                //ready to receive file
                DataInputStream dis=new DataInputStream(subsock.getInputStream());
                OutputStream os=new FileOutputStream(file);
                byte []data=new byte[UtilityClass.BUFFER_SIZE_MEDIUM];
                int bytes_read=0;
                while((bytes_read=dis.read(data,0,data.length))!=-1){
                    os.write(data,0,bytes_read);
                }
                Log.e(TAG, "receiveFileAlter: File "+iter+" Saving Complete" );
                subsock.close();

            }else{
                subsock.close();
            }

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    pbar.setVisibility(View.GONE);
                }
            });
        }

    }
    private void receiveFileAlter(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    BufferedReader inp=new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String req=inp.readLine();
                    String[] l=req.split(UtilityClass.SEPARATOR);
                    int num_files=Integer.parseInt(l[1]);
                    Log.e(TAG, "run: "+num_files );
                    DataOutputStream dos=new DataOutputStream(socket.getOutputStream());
                    dos.write(UtilityClass.RECEIVING_ACK.getBytes());
                    String []fileData=new String[num_files];
                    for(int i=0;i<num_files;i++){
                        fileData[i]=inp.readLine();
                    }
                    dos.write(UtilityClass.RECEIVING_ACK.getBytes());
                    //fileinfo got, now create new sockets for each files
                    for(int i=0;i<num_files;i++){
                       String[] fl=fileData[i].split(UtilityClass.SEPARATOR);
                       final String filename=fl[0];
                       final long filesize=Long.parseLong(fl[1]);
                       final int port=Integer.parseInt(fl[2]);
                        final int finalI = i;
                        new Thread(new Runnable() {
                           @Override
                           public void run() {

                               receiveFileAlter(filename,filesize,port, finalI);
                           }
                       }).start();
                    }


                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    private void sendFileAlter(){
        int num_files=fileUris.length;
        try {
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            String msg = "LENGTH" + UtilityClass.SEPARATOR + num_files + "\n";
            dos.write(msg.getBytes());
            Log.e(TAG, "sendFileAlter: " + "Send" + num_files);
            BufferedReader inp = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line = inp.readLine();
            line += "\n";
            int p = UtilityClass.getPortAddress();
            final int[] ports = new int[num_files];
            if (line.equals(UtilityClass.RECEIVING_ACK)) {
                for (int i = 0; i < num_files; i++) {
                    p += 2;
                    ports[i] = p;
                    String filename = UtilityClass.getFileName(ClientActivity.this, fileUris[i]);
                    AssetFileDescriptor afd = getContentResolver().openAssetFileDescriptor(fileUris[i], "r");
                    long fileSize = afd.getLength();
                    afd.close();
                    String fileI = filename + UtilityClass.SEPARATOR + String.valueOf(fileSize) + UtilityClass.SEPARATOR
                            + String.valueOf(p) + "\n";
                    dos.write(fileI.getBytes());


                }
                String l = inp.readLine();
                l += "\n";
                if (l.equals(UtilityClass.RECEIVING_ACK)) {
                    for (int i = 0; i < num_files; i++) {

                        final String filename = UtilityClass.getFileName(ClientActivity.this, fileUris[i]);
                        AssetFileDescriptor afd = getContentResolver().openAssetFileDescriptor(fileUris[i], "r");
                        final long fileSize = afd.getLength();
                        afd.close();
                        final int finalI = i;
                        pbar.setVisibility(View.VISIBLE);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                //calling new thread for each file
                                sendFileAlter(fileUris[finalI], ports[finalI], finalI, filename, fileSize);
                            }
                        }).start();
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendFileAlter(final Uri fileuri,final int port,final int iter,final String filename,final long filesize){
        try{
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    pbar.setVisibility(View.VISIBLE);
                }
            });
            InetAddress addr=InetAddress.getByName(UtilityClass.getIpAddress());
            Socket subSock=new Socket(addr,port);
            Log.e(TAG, "sendFileAlter: Socket connected for "+iter );
            DataOutputStream dos=new DataOutputStream(subSock.getOutputStream());
            String msg="REQUEST_TO_SEND"+UtilityClass.SEPARATOR+iter+"\n";
            dos.write(msg.getBytes());
            BufferedReader inp=new BufferedReader(new InputStreamReader(subSock.getInputStream()));
            String recv=inp.readLine();
            String[] r=recv.split(UtilityClass.SEPARATOR);
            r[0]+="\n";
            if(r[0].equals(UtilityClass.ALLOW_TO_RECV) && Integer.parseInt(r[1])==iter) {
                Log.e(TAG, "sendFileAlter: Request Accepted " + iter);

                InputStream is = getContentResolver().openInputStream(fileuri);
                OutputStream os = subSock.getOutputStream();
                byte[] data = new byte[UtilityClass.BUFFER_SIZE_MEDIUM];
                int br = 0;
                while ((br = is.read(data, 0, data.length)) != -1) {
                    os.write(data, 0, br);
    //                Log.e(TAG, "run: Sending " + br);
                }
                Log.e(TAG, "run: Sending Complete");
                subSock.close();
                currentPort -= 2;
                is.close();
                os.flush();

            }else{
                Log.e(TAG, "sendFileAlter: REJECTED"+recv );
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    pbar.setVisibility(View.GONE);
                }
            });
        }
    }

    /*

    private void receiveFile() {
        //accept request
        Log.e(TAG, "receiveFile: Here" );
        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    BufferedReader input=new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String recq=input.readLine();
                    String[] l=recq.split(UtilityClass.SEPARATOR);
                    int num_files=Integer.parseInt(l[1]);
                    Log.e(TAG, "run: "+num_files );
                    DataOutputStream dos=new DataOutputStream(socket.getOutputStream());
                    dos.write(UtilityClass.RECEIVING_ACK.getBytes());
                    for(int i=0;i<num_files;i++) {
                        String rec=input.readLine();
                        rec += "\n";
                        Log.e(TAG, "run: " + "Request to send recieved");
                        if (rec.equals(UtilityClass.REQUEST_TO_SEND)) {
                            OutputStream stream = socket.getOutputStream();
                            DataOutputStream commstream = new DataOutputStream(stream);
                            commstream.write(UtilityClass.ALLOW_TO_RECV.getBytes());

                            rec = input.readLine();
                            String[] fl = rec.split(UtilityClass.SEPARATOR);
                            String filename = fl[0];
                            long filesize = Long.parseLong(fl[1]);
                            receiveFile(filename, filesize);
                        } else {
                            Log.e(TAG, "run: Recieve didn't acknowledge");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        //send ack
    }

    private void recieveBigFile(final String filename, long filesize) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //creating new socket for file recving
                File file=new File(getExternalFilesDir(UtilityClass.BASEPATH),filename);
                try{
                    InetAddress address=InetAddress.getByName(UtilityClass.getIpAddress());
                    currentPort+=2;
                   // Log.e(TAG, "run: checking for port "+currentPort );
                    Socket s1=new Socket(address,currentPort);
                    Log.e(TAG, "run: "+"Client1  for file generated" );
                    currentPort+=2;
                    Socket s2=new Socket(address,currentPort);
                    Log.e(TAG, "run: "+"Client2  for file generated" );

                    InputStream is1=s1.getInputStream();
                    OutputStream os=new FileOutputStream(file);
                    DataInputStream dis1=new DataInputStream(is1);

                    InputStream is2=s2.getInputStream();
                    DataInputStream dis2=new DataInputStream(is2);

                    byte[] data=new byte[UtilityClass.BUFFER_SIZE_MEDIUM];
                    byte[] temp1=new byte[UtilityClass.BUFFER_SIZE_MEDIUM];
                    byte[] temp2=new byte[UtilityClass.BUFFER_SIZE_MEDIUM];
                    int bytes_read=0;
                    int a=0,b=0;
                    Log.e(TAG, "run: start "+System.currentTimeMillis() );
                    while((bytes_read=dis1.read(data,0,data.length))!=-1){
                        for(byte i:data){
                            temp1[a++]=i;
                        }
                        if(a==UtilityClass.BUFFER_SIZE_MEDIUM){
                            a=0;
                            os.write(temp1,0,UtilityClass.BUFFER_SIZE_MEDIUM);
                            Log.e(TAG, "run: 1" );
                        }
                        if((bytes_read=dis2.read(data,0,data.length))!=-1){
                            for(byte i:data){
                                temp2[b++]=i;
                            }
                            if(b==UtilityClass.BUFFER_SIZE_MEDIUM){
                                b=0;
                                os.write(temp2,0,UtilityClass.BUFFER_SIZE_MEDIUM);
                                Log.e(TAG, "run: 2" );
                            }
                        }else{
                            if(b!=0){
                                os.write(temp2,0,temp2.length);
                            }
                            break;
                        }
                    }
                    if(a!=0){
                        os.write(temp1,0,temp1.length);
                    }

                    /*

                    while((bytes_read=dis1.read(data,0,data.length))!=-1){
                        os.write(data,0,bytes_read);
                        int a=bytes_read;
                        if((bytes_read=dis2.read(data,0,data.length))!=-1){
                            os.write(data,0,bytes_read);
                        }else{
                            break;
                        }
                        a+=bytes_read;
                        //Log.e(TAG, "run: Recving "+a );
                    }
                    */
    /*
                    Log.e(TAG, "run: end "+System.currentTimeMillis() );
                    s1.close();
                    s2.close();
                    Log.e(TAG, "run: File receiving complete" );
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    */

    private void data_exchange_check() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String receive=input.readLine();
                    receive+="\n";
                    Log.e(TAG, "run: "+receive );
                    if(receive.equals(UtilityClass.CONNECTION_ESTABLISHED_SERVER)){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                data_exchng_recv.setText("ALLOWED");
                                recv=1;
                                boolean b=checkUpsNDowns();
                            }
                        });

                        OutputStream stream = socket.getOutputStream();
                        DataOutputStream commStream = new DataOutputStream(stream);
                        commStream.write(UtilityClass.RECEIVING_ACK.getBytes());
                    }else{
                        Log.e(TAG, "run: "+"Recv Didn't Acknowledged" );
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
                //now check for sending data
                try{
                    OutputStream stream = socket.getOutputStream();
                    DataOutputStream commStream = new DataOutputStream(stream);
                    commStream.write(UtilityClass.CONNECTION_ESTABLISHED_CLIENT.getBytes());

                    BufferedReader input=new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String recv=input.readLine();
                    recv+="\n";
                    if(recv.equals(UtilityClass.RECEIVING_ACK)){
                        //the sent packet is acknowledged
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                data_exchng_send.setText("ALLOWED");
                                sent=1;
                                boolean b=checkUpsNDowns();
                            }
                        });

                    }else{
                        Log.e(TAG, "run: "+"Send Didn't Acknowledged" );
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }).start();


    }

    private Boolean checkUpsNDowns(){
        if(conn==1){
            if(sent==1 && recv==1){
                recieve.setVisibility(View.VISIBLE);
                return true;
            }
        }
        recieve.setVisibility(View.GONE);
        return false;
    }
    private void initializeViews() {
        search=findViewById(R.id.client_search);
        status=findViewById(R.id.client_status);
        data_exchng_send=findViewById(R.id.client_data_excng_send);
        data_exchng_recv=findViewById(R.id.client_data_excng_recv);
        recieve=findViewById(R.id.client_recv_file);
        choose=findViewById(R.id.client_selectFile);
        send=findViewById(R.id.client_sendfile);
        pbar=findViewById(R.id.client_progress);
        close=findViewById(R.id.client_closeBtn);
        hotpsot=findViewById(R.id.client_hotspot);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==101){
            if(resultCode==RESULT_OK){
                if(data!=null){
                    if(data.getClipData()!=null){
                        fileUris=new Uri[data.getClipData().getItemCount()];
                        for(int i=0;i<data.getClipData().getItemCount();i++){
                            Uri u=data.getClipData().getItemAt(i).getUri();
                            fileUris[i]=u;
                        }
                    }
                    else{
                        fileUris=new Uri[1];
                        fileUris[0]=data.getData();
                    }
                }
            }
        }
    }
}
