package com.fishnchips.app;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

public class BluetoothConnector {

    private final BluetoothMessageReceiver messageReceiver;
    private final String TAG = "bluetooth_status";
    private final Context context;
    private Handler handler;

    private BluetoothSocket mmSocket;
    private final BluetoothConnector connector;
    private BluetoothConnector.ConnectedThread connectedThread;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothConnector.CreateConnectThread createConnectThread;

    private static final int DEVICE_CONNECTED = 0;
    private static final int UNABLE_TO_CONNECT = 1;
    private static final int MESSAGE_FROM_DEVICE = 2;

    public BluetoothConnector(Context context) {
        connector = this;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.context = context;

        if (context instanceof BluetoothMessageReceiver) {
            messageReceiver = (BluetoothMessageReceiver) context;
        } else {
            throw new UnsupportedOperationException(context.toString() + " must implement BluetoothMessageReceiver");
        }

        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                int status = msg.getData().getInt("status");
                String message = msg.getData().getString("message");
                messageReceiver.onMessageReceived(status, message);
            }
        };
    }

    public ArrayList<BluetoothDevice> getDevicesList() {
//        Log.d(TAG, bluetoothAdapter.getBondedDevices().toString());
        return new ArrayList<>(bluetoothAdapter.getBondedDevices());
    }

    public void connectToDeviceMac(String macAddress) {
//        Returns true if connection is successful, false otherwise
        createConnectThread = new BluetoothConnector.CreateConnectThread(bluetoothAdapter, macAddress);
        createConnectThread.start();
    }

    public void disconnect() {
        if (connectedThread != null) {
            connectedThread.cancel();
        }
    }

    public void sendMessageToHandler(int status, String message) {
        Log.d(TAG + " MTH", message);

        Message msg = handler.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putString("message", message);
        bundle.putInt("status", status);
        msg.setData(bundle);
        handler.sendMessage(msg);
    }

    public interface BluetoothMessageReceiver {
        void onMessageReceived(int status, String message);
    }

    public class CreateConnectThread extends Thread {

        public CreateConnectThread(BluetoothAdapter bluetoothAdapter, String address) {
            /*
            Use a temporary object that is later assigned to mmSocket
            because mmSocket is final.
             */
            BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
            BluetoothSocket tmp = null;
            UUID uuid = bluetoothDevice.getUuids()[0].getUuid();

            try {
                /*
                Get a BluetoothSocket to connect with the given BluetoothDevice.
                Due to Android device varieties,the method below may not work fo different devices.
                You should try using other methods i.e. :
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
                 */
                tmp = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(uuid);

            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            bluetoothAdapter.cancelDiscovery();
            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
                connector.sendMessageToHandler(DEVICE_CONNECTED, "Device connected");
            } catch (IOException connectException) {
                connectException.printStackTrace();
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                    connector.sendMessageToHandler(UNABLE_TO_CONNECT, "Unable to connect");
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            connectedThread = new BluetoothConnector.ConnectedThread(mmSocket);
            connectedThread.run();
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

    public void sentDataToServer(String data) {
        // TODO

        String url = "";

        JSONObject jsonBody = null;
        try {
            jsonBody = new JSONObject("{\"type\":\"example\"}");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Request<JSONObject> request = new JsonObjectRequest(url, jsonBody, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });
    }

    /* =============================== Thread for Data Transfer =========================================== */
    public class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes = 0; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    /*
                    Read from the InputStream from Arduino until termination character is reached.
                    Then send the whole String message to GUI Handler.
                     */
                    buffer[bytes] = (byte) mmInStream.read();
                    String readMessage;
                    if (buffer[bytes] == '\n') {
                        readMessage = new String(buffer, 0, bytes);

                        Log.d("IOT_DEVICE_MESSAGE", readMessage);

//                        Function for sending data to MainActivity
                        connector.sendMessageToHandler(MESSAGE_FROM_DEVICE, readMessage);
                        bytes = 0;
                    } else {
                        bytes++;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String input) {
            byte[] bytes = input.getBytes(); //converts entered String into bytes
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e("Send Error", "Unable to send message", e);
            }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }
}
