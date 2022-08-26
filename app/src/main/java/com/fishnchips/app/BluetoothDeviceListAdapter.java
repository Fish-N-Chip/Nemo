package com.fishnchips.app;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class BluetoothDeviceListAdapter extends RecyclerView.Adapter<BluetoothDeviceListAdapter.ViewHolder> {

    private final Context context;
    private OnDeviceSelectedListener deviceSelectedListener;
    ArrayList<BluetoothDevice> devices;

    public BluetoothDeviceListAdapter(ArrayList<BluetoothDevice> devices, Context context) {
        this.devices = devices;
        this.context = context;

        try {
            deviceSelectedListener = (OnDeviceSelectedListener) context;
        } catch (ClassCastException exception) {
            throw new UnsupportedOperationException(context + " must implement OnDeviceSelectedListener");
        }

    }

    @NonNull
    @Override
    public BluetoothDeviceListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Create a new view, which defines the UI of the list item
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycler_bluetooth_device, parent, false);

        return new BluetoothDeviceListAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BluetoothDevice device = devices.get(position);
        holder.deviceName.setText(device.getName());
        holder.deviceMacAddress.setText(device.getAddress());

//        holder.deviceName.setText(devices.get(position));
        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BluetoothDevice device = devices.get(position);
                String name = device.getName();
                String macAddress = device.getAddress();
                deviceSelectedListener.onDeviceSelected(name, macAddress);
//                Toast.makeText(context, String.valueOf(position), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    public void setDevices(ArrayList<BluetoothDevice> devices) {
        this.devices = devices;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView deviceName, deviceMacAddress;
        CardView cardView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.bluetooth_device_card_view);
            deviceName = itemView.findViewById(R.id.bluetooth_device_name);
            deviceMacAddress = itemView.findViewById(R.id.bluetooth_device_mac_address);
        }
    }

    public interface OnDeviceSelectedListener {
        void onDeviceSelected(String name, String macAddress);
    }

}
