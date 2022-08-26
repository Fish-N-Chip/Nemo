package com.fishnchips.app;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;


public class BluetoothDeviceSelectorDialogFragment extends DialogFragment {

    private RecyclerView recyclerView;
    private BluetoothDeviceListAdapter deviceListAdapter;
    private ArrayList<BluetoothDevice> devices;

    public BluetoothDeviceSelectorDialogFragment(ArrayList<BluetoothDevice> deviceList) {
        // Empty constructor is required for DialogFragment
        // Make sure not to add arguments to the constructor
        // Use `newInstance` instead as shown below
        this.devices = deviceList;
    }

    public static BluetoothDeviceSelectorDialogFragment newInstance(String title, ArrayList<BluetoothDevice> deviceList) {
        BluetoothDeviceSelectorDialogFragment frag = new BluetoothDeviceSelectorDialogFragment(deviceList);
        Bundle args = new Bundle();
        args.putString("title", title);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.dialog_bluetooth_device_selector, container);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Get field from view
        // Fetch arguments from bundle and set title
        ArrayList<String> temp = new ArrayList<>();
        temp.add("Adwait");
        temp.add("Bhope");
//        deviceListAdapter = new BluetoothDeviceListAdapter(temp, getContext());

//        recyclerView = view.findViewById(R.id.recyclerView);
//        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
//        recyclerView.setAdapter(deviceListAdapter);

        for (BluetoothDevice device : devices) {
            Log.d("TestBluetoothDevices", device.toString());
        }

//        deviceListAdapter.notifyDataSetChanged();

    }
}
