package com.example.audio_call;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void callClicked(View view) {
        // Do something in response to button
        Intent intent = new Intent(this, createRoom.class);
        intent.putExtra("CorJ", "C");
        startActivity(intent);
    }

    public void joinClicked(View view) {
        Intent intent = new Intent(this, createRoom.class);
        intent.putExtra("CorJ", "J");
        startActivity(intent);
    }
}