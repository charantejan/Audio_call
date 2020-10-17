package com.example.audio_call;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class createRoom extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_room);
    }

    public void createClicked(View view) {
        // Do something in response to button
        Intent intent = new Intent(this, call.class);
        intent.putExtra("Host", 1);
        startActivity(intent);
    }
}