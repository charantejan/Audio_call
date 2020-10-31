package com.example.audio_call;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.util.LogPrinter;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import static java.nio.ByteOrder.BIG_ENDIAN;

public class createRoom extends AppCompatActivity {
    private String CorJ;
    private Socket socket = null;
    private int port = 65536;
    private String roomName = "";
    private String passwd = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent extras=getIntent();
        String cj = extras.getStringExtra("CorJ");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_room);
        Button mButton = (Button) findViewById(R.id.button);
        if (cj.equals("C")){
            CorJ = "Create ";
            mButton.setText("Create Room");
        }
        else {
            CorJ = "Join ";
            mButton.setText("Join Room");
        }
    }
    private boolean haveNetworkConnection() {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        for (NetworkInfo ni : netInfo) {
            if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                if (ni.isConnected()) {
                    haveConnectedWifi = true;
                    Log.d("charan", "wifi");
                }
            if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                if (ni.isConnected())
                    haveConnectedMobile = true;
        }
        return haveConnectedWifi || haveConnectedMobile;
    }
    public void createClicked(View view) {
        // Do something in response to button
//        try {
            EditText mEdit = findViewById(R.id.roomName);
            EditText mEdit2 = findViewById(R.id.roomPassword);
            roomName = mEdit.getText().toString();
            passwd = mEdit2.getText().toString();
            Log.d("charan", roomName+passwd);
            if(roomName == null || roomName.isEmpty() || passwd == null || passwd.isEmpty()){
                Toast.makeText(getApplicationContext(),"Username or Password cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            Log.d("charan", roomName+passwd+"123");
//            if(haveNetworkConnection()) {
//                Socket socket = new Socket("192.168.1.3", 9999);
//            }

            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
//                        EditText mEdit = findViewById(R.id.roomName);
//                        EditText mEdit2 = findViewById(R.id.roomPassword);
//                        String roomName = mEdit.getText().toString();
//                        String passwd = mEdit2.getText().toString();
//                        Log.d("charan", roomName+passwd);
//                        if(roomName == null || roomName.isEmpty() || passwd == null || passwd.isEmpty()){
//                            Toast.makeText(getApplicationContext(),"Username or Password cannot be empty", Toast.LENGTH_SHORT).show();
//                            return;
//                        }
//                        Log.d("charan", roomName+passwd+"123");
                        Log.d("Charan", "here");
                        socket = new Socket("192.168.1.4", 9998);
                        Log.d("Charan", "here1");
                        OutputStream out = socket.getOutputStream();
                        InputStream in = socket.getInputStream();
                        BufferedOutputStream buff = new BufferedOutputStream(out); //out1 is the socket's outputStream
                        DataOutputStream dataOutputStreamInstance = new DataOutputStream(buff);
                        BufferedInputStream buff_in = new BufferedInputStream(in);
                        DataInputStream dataInputStreamInstance = new DataInputStream(buff_in);
                        String msg = CorJ+ roomName+ " " + passwd;

                        byte[] byteArrray = msg.getBytes();
                        byte[] len = ByteBuffer.allocate(4).order(BIG_ENDIAN).putInt(msg.length()).array();
                        byte[] data = new byte[4+byteArrray.length];
                        System.arraycopy(len, 0, data, 0, 4);
                        System.arraycopy(byteArrray, 0, data, 4, byteArrray.length);
                        dataOutputStreamInstance.write(data);
                        dataOutputStreamInstance.flush();
                        byte[] buf = new byte[8];
                        dataInputStreamInstance.read(buf);
                        byte[] buf1 = Arrays.copyOfRange(buf, 4, 8);
//                        Log.d("port123", Arrays.toString(buf1));
                        port = ByteBuffer.wrap(buf1).getInt();
                        if (port > 65535){
                            Toast.makeText(getApplicationContext(),"Error", Toast.LENGTH_SHORT).show();
                            return;
                        }
//                        Intent intent = new Intent(this, call.class);
//                        intent.putExtra("port", port);
//                        startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            thread.start();
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
//            Log.d("port123", String.valueOf(port));
            Intent intent = new Intent(this, call.class);
            intent.putExtra("port", port);
            startActivity(intent);
    }
}