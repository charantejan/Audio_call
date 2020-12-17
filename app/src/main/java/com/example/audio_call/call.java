package com.example.audio_call;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class call extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {

    private AudioCall call;
    private int host = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        Log.d("port123", String.valueOf(host
//        Log.d("port123", String.valueOf(host));

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);
        Intent extras=getIntent();
        host = extras.getIntExtra("port", 0); //Get the port number
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) //Permission for using mic
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    1234);
        }
        while (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) //Permission for using mic
                != PackageManager.PERMISSION_GRANTED){}
        //Use the helper functions from AudioCall.java
        call = new AudioCall(host, true, getApplicationContext());
        Log.d("port123", String.valueOf(host));
        call.startCall(); //Start the call
        //Check if endcall is clicked
        check();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Menu for leave call/End call
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.tcp_udp, menu);
        return true;
    }

    private void check(){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(call.getendcall){}
                View myView = findViewById(R.id.leave);
                myView.performClick();
            }
        });
        thread.start();
    }

    public void mutemic(View view){ //Mute and unmute mic
        ImageView mic_image = findViewById(R.id.mic);
        String tag = (String) mic_image.getTag();
        Log.d("myTag", tag);
        if( tag.equals("mute") ) {
            call.muteMic();
            mic_image.setImageResource(R.drawable.unmute);
            mic_image.setTag("unmute");
        }
        else{
            call.unmuteMic();
            mic_image.setImageResource(R.drawable.mute);
            mic_image.setTag("mute");
        }
    }

    public void LeaveCall(View view){
        PopupMenu popup = new PopupMenu(this, view);
        popup.setOnMenuItemClickListener(this);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.leave, popup.getMenu());
        popup.show();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.leave:
                call.endCall();
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                return true;
            case R.id.End_call:
                //Should implement
                return true;
            default:
                return false;
        }
    }
}