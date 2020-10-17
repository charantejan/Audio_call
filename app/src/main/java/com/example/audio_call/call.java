package com.example.audio_call;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class call extends AppCompatActivity implements PopupMenu.OnMenuItemClickListener {

    private AudioCall call;
    private int host = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent extras=getIntent();
        host = extras.getIntExtra("Host", 0);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);
        try {
            InetAddress address = InetAddress.getByName("localhost");
            call = new AudioCall(address, true);
            call.startCall();
            check();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
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

    public void mutemic(View view){
        ImageView mic_image = findViewById(R.id.mic);
        String tag = (String) mic_image.getTag();
        Log.d("myTag", tag);
        if( tag.equals("mute") ) {
            call.unmuteMic();
            mic_image.setImageResource(R.drawable.unmute);
            mic_image.setTag("unmute");
        }
        else{
            call.muteMic();
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
                return true;
            default:
                return false;
        }
    }
}