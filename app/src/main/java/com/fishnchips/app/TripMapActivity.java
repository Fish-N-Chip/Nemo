package com.fishnchips.app;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

public class TripMapActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_map);
        setTitle("Trip History");
    }
}