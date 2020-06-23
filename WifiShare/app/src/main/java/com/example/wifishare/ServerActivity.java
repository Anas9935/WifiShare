package com.example.wifishare;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

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
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

import static com.example.wifishare.MainActivity.PortNo;

public class ServerActivity extends AppCompatActivity {

    Button search,close,choose,send,receive;
    TextView status,percent,sending,receiving;

    Socket socket;

    int connInt=0,sendInt=0,receiveInt=0;

    Uri[] fileuris;

    String TAG="SERVERACTIVITY";
    int currentPort=UtilityClass.getPortAddress();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);

        initializeViews();

        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createServer();
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
        choose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY,true);
                startActivityForResult(intent,101);

            }
        });
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        receive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });
    }


    private void receiveFileAlter(final String filename,final long size,final int port,final int iter){

        File file=new File(getExternalFilesDir(UtilityClass.BASEPATH),filename);

        try{
            InetAddress addr=InetAddress.getByName(UtilityClass.getIpAddress());
            ServerSocket serverSocket=new ServerSocket(port);
            Socket subsock=serverSocket.accept();
//            Socket subsock=new Socket(addr,port);
            Log.e(TAG, "receiveFileAlter: "+"Client Connected"+iter );

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
        int num_files=fileuris.length;
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
                    String filename = UtilityClass.getFileName(ServerActivity.this, fileuris[i]);
                    AssetFileDescriptor afd = getContentResolver().openAssetFileDescriptor(fileuris[i], "r");
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

                        final String filename = UtilityClass.getFileName(ServerActivity.this, fileuris[i]);
                        AssetFileDescriptor afd = getContentResolver().openAssetFileDescriptor(fileuris[i], "r");
                        final long fileSize = afd.getLength();
                        afd.close();
                        final int finalI = i;
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                //calling new thread for each file
                                sendFileAlter(fileuris[finalI], ports[finalI], finalI, filename, fileSize);
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
            InetAddress addr=InetAddress.getByName(UtilityClass.getIpAddress());
            //Socket subSock=new Socket(addr,port);
            ServerSocket serverSocket=new ServerSocket(port);
            Socket subSock=serverSocket.accept();
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
        }
    }


    private void createServer() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    status.setText("WAITING");
                    ServerSocket serverSocket = new ServerSocket(UtilityClass.getPortAddress());
                    Log.e(TAG, "run: " + "Connection Started");
                    socket = serverSocket.accept();
                    Log.e(TAG, "run: " + "Socket connected");
                    connInt = 1;
                    data_exchange_check();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }).start();
    }

    private void initializeViews() {
        search=findViewById(R.id.server_search);
        close=findViewById(R.id.server_closeBtn);
        choose=findViewById(R.id.server_selectFile);
        send=findViewById(R.id.server_sendfile);
        receive=findViewById(R.id.server_recv_file);

        status=findViewById(R.id.server_status);
        percent=findViewById(R.id.server_percent);
        sending=findViewById(R.id.server_data_excng_send);
        receiving=findViewById(R.id.server_data_excng_recv);

    }


    private void data_exchange_check() {
                try {
                    DataOutputStream dos=new DataOutputStream(socket.getOutputStream());
                    dos.write(UtilityClass.CONNECTION_ESTABLISHED_SERVER.getBytes());
                    BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String receive=input.readLine();
                    receive+="\n";
                    Log.e(TAG, "run: "+receive );
                    if(receive.equals(UtilityClass.RECEIVING_ACK)){
                        //uploading success
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                sending.setText("ALLOWED");
                                sendInt=1;
                                boolean b=checkUpsNDowns();
                            }
                        });

                    }else{
                        Log.e(TAG, "run: "+"Recv Didn't Acknowledged" );
                    }

                   // BufferedReader input=new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String recv=input.readLine();
                    recv+="\n";
                    if(recv.equals(UtilityClass.CONNECTION_ESTABLISHED_CLIENT)){
                        //send connection establish

                        dos.write(UtilityClass.RECEIVING_ACK.getBytes());
                        //ackowledged
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                receiving.setText("ALLOWED");
                                receiveInt=1;
                                Boolean b=checkUpsNDowns();
                            }
                        });

                    }else{
                        Log.e(TAG, "run: "+"Client Didn't Acknowledged" );
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }


    }

    private Boolean checkUpsNDowns(){
        if(connInt==1){
            if(sendInt==1 && receiveInt==1){
                receive.setVisibility(View.VISIBLE);
                return true;
            }
        }
        receive.setVisibility(View.GONE);
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==101){
            if(resultCode==RESULT_OK){
                if(data!=null){
                    if(data.getClipData()!=null){
                        fileuris=new Uri[data.getClipData().getItemCount()];
                        for(int  i=0;i<data.getClipData().getItemCount();i++){
                            fileuris[i]=data.getClipData().getItemAt(i).getUri();
                        }
                    }else{
                        fileuris=new Uri[1];
                        fileuris[0]=data.getData();
                    }
                }
            }
        }

    }
}
