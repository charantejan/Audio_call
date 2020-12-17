package com.example.audio_call;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Application;
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
//The same file is used to create the screen and functionality for both create and join screen.
public class createRoom extends AppCompatActivity {
    private String CorJ; //Is it from Create room or Join room screen
    private Socket socket = null;
    private int port = 65536;
    private String roomName = "";
    private String passwd = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent extras=getIntent();
        String cj = extras.getStringExtra("CorJ"); //Get the request type
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
            Log.d("testing", "kk");
        }
    }

    public void createClicked(View view) {
        // Do something in response to button
//        try {
            EditText mEdit = findViewById(R.id.roomName); //Get the entered details
            EditText mEdit2 = findViewById(R.id.roomPassword);
            roomName = mEdit.getText().toString();
            passwd = mEdit2.getText().toString();
            //Do some sanity checks
            if(roomName == null || roomName.isEmpty() || passwd == null || passwd.isEmpty()){
                Toast.makeText(getApplicationContext(),"Username or Password cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            Log.d("testing", "charan");
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.d("testing", "Inside thread");
                    try {
                        Log.d("testing", "123456789");
                        socket = new Socket("192.168.1.4", 9998);//Initialize here server's address
                        //The following are the buffers required to send/receive data from the server.
                        Log.d("testing", "Here12345678");
                        OutputStream out = socket.getOutputStream();
                        InputStream in = socket.getInputStream();
                        BufferedOutputStream buff = new BufferedOutputStream(out); //out1 is the socket's outputStream
                        DataOutputStream dataOutputStreamInstance = new DataOutputStream(buff);
                        BufferedInputStream buff_in = new BufferedInputStream(in);
                        DataInputStream dataInputStreamInstance = new DataInputStream(buff_in);
                        //Format: "Create Room_name Password" or "Join Room_name Password"
                        String msg = CorJ+ roomName+ " " + passwd;
                        Log.d("testing", msg);
                        //The message sent is padded with length(4 bytes), as the server recursively calls recv until the whole message is received. Look multiplex.py for implementation
                        byte[] byteArrray = msg.getBytes();
                        byte[] len = ByteBuffer.allocate(4).order(BIG_ENDIAN).putInt(msg.length()).array();
                        byte[] data = new byte[4+byteArrray.length];
                        System.arraycopy(len, 0, data, 0, 4);
                        System.arraycopy(byteArrray, 0, data, 4, byteArrray.length);
                        //Send the message
                        dataOutputStreamInstance.write(data);
                        dataOutputStreamInstance.flush();
                        byte[] buf = new byte[8];
                        //Read the response into the buf array
                        dataInputStreamInstance.read(buf);
                        byte[] buf1 = Arrays.copyOfRange(buf, 4, 8); //Remove the 4 bytes of length (Refer multiplex.py for send function used)
//                        Log.d("port123", Arrays.toString(buf1));
                        port = ByteBuffer.wrap(buf1).getInt();
                        Log.d("testing", String.valueOf(port));
                        if (port > 65535){ //If any error the server sends the port number to be 65536
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    Toast.makeText(getApplicationContext(), "Error", Toast.LENGTH_SHORT).show();
                                }
                            });
                            Log.d("testing", "toast working");
                            return;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.d("testing", e.getMessage());
                    }
                }
            });
            thread.start();
            try {
                //Wait for above functionality to complete then continue.
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
                Log.d("testing", "Errror");
            }
//            Log.d("port123", String.valueOf(port));
        //Call the call screen with input as the port number.
            Intent intent = new Intent(this, call.class);
            intent.putExtra("port", port);
            startActivity(intent);
    }
}