package com.fishnchips.app;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;

public class SelectBluetoothDeviceActivity extends AppCompatActivity implements BluetoothDeviceListAdapter.OnDeviceSelectedListener, BluetoothConnector.BluetoothMessageReceiver {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        setTitle("Select your device");

        BluetoothDeviceListAdapter adapter = new BluetoothDeviceListAdapter(null, this);

        RecyclerView recyclerView = findViewById(R.id.testRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        BluetoothConnector bluetoothConnector = new BluetoothConnector(this);
        ArrayList<BluetoothDevice> devices = bluetoothConnector.getDevicesList();
        adapter.setDevices(devices);
    }

    @Override
    public void onDeviceSelected(String name, String macAddress) {
        Log.d("BlueTooth Selected", macAddress);
        Intent intent = new Intent();
        intent.putExtra("name", name);
        intent.putExtra("mac_address", macAddress);
        setResult(1, intent);
        finish();
    }

    @Override
    public void onMessageReceived(int status, String message) {
        Log.e("WTF", "This message should not have been shown.");
    }

}