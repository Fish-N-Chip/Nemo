package com.fishnchips.app;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.room.Room;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.esri.arcgisruntime.ArcGISRuntimeEnvironment;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.PointCollection;
import com.esri.arcgisruntime.geometry.Polyline;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.layers.ArcGISMapImageLayer;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.BasemapStyle;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.LocationDisplay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.Permission;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

import static java.lang.Math.sqrt;

public class MainActivity extends AppCompatActivity implements BluetoothConnector.BluetoothMessageReceiver {

    private MapView mapView;
    private BluetoothConnector bluetoothConnector;
    private Button tripButton;

    private SharedPreferences pref;
    private SharedPreferences blackBoxPref;
    private SharedPreferences.Editor editor;

    // Do not convert this to a local variable, listener will have a higher chance to get garbage collected
    private SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener;

    private ArcGISMap map;

    private static final int SELECT_BLUETOOTH_DEVICE = 0;

    private static final int DEVICE_CONNECTED = 0;
    private static final int UNABLE_TO_CONNECT = 1;
    private static final int MESSAGE_FROM_DEVICE = 2;

    private static final int ON = 1;
    private static final int OFF = -1;

    private int tripStatus = OFF;

    private ProfileFragment profileFragment;
    private FrameLayout fragmentArea;

    private static int MMSI;
    private Random random = new Random();

    private AppDatabase db = Room.databaseBuilder(GlobalVariables.getContext(),
            AppDatabase.class, "database-name")
            .fallbackToDestructiveMigration()
            .build();

    private final AISMessageDao aisMessageDao = db.aisMessageDao();
    private final TripDao tripDao = db.tripDao();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView navView = findViewById (R.id.main_activity_bottom_nav);
        navView.setSelectedItemId(R.id.navigation_home);
        navView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_dashboard, R.id.navigation_home, R.id.navigation_notifications)
                .build();

        fragmentArea = findViewById(R.id.home_screen_fragment_area);
        profileFragment = new ProfileFragment();

//        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
//        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
//        NavigationUI.setupWithNavController(navView, navController);

        blackBoxPref = getSharedPreferences("black_box_signals", Context.MODE_PRIVATE);
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        if (pref.getBoolean("stop", true)) {
            editor = pref.edit();
            editor.putBoolean("stop", true);
            editor.apply();
        }

        MMSI = pref.getInt("mmsi", -1);
        if (MMSI == -1) {
            int randomMMSI = random.nextInt(1000000);
            editor = pref.edit();
            editor.putInt("mmsi", randomMMSI);
            editor.apply();
            Log.d("Generating MMSI", String.valueOf(randomMMSI));
        } else {
            Log.d("Using old MMSI", String.valueOf(MMSI));
        }

        mapView = findViewById(R.id.mapView);
        tripButton = findViewById(R.id.trip_button);
        tripButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tripButtonClicked();
            }
        });

        setupMap();

        bluetoothConnector = new BluetoothConnector(this);
    }

    private void setupMap() {
        ArcGISRuntimeEnvironment.setApiKey("AAPK19a9fa26ad314ad7ba4425798bcf51baj-GIkTwNnjTcBjOGcVdF8DS9js7hlSCAJFZK0PHuSdUPz2DkZ9or7KLqApgAMkxT");
        map = new ArcGISMap(BasemapStyle.ARCGIS_TOPOGRAPHIC);

        showEezLayer();

        mapView.setMap(map);
        mapView.setViewpoint(new Viewpoint(18, 80, 7200000.0));

        showCurrentLocation();
    }

    private void showCurrentLocation() {
        // TODO: Ask for location permission
        // Reference: https://developers.arcgis.com/net/android/sample-code/display-device-location/

        LocationDisplay locationDisplay = mapView.getLocationDisplay();
        locationDisplay.startAsync();
        locationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.RECENTER);
    }

    private void showEezLayer() {
        // Find the homepage at https://www.arcgis.com/home/item.html?id=9c707fa7131b4462a08b8bf2e06bf4ad

        ServiceFeatureTable serviceFeatureTable = new ServiceFeatureTable("https://oceans1.arcgis.com/arcgis/rest/services/World_Exclusive_Economic_Zone_Boundaries/FeatureServer/0");
        FeatureLayer featureLayer = new FeatureLayer(serviceFeatureTable);

        // create the feature layer using the service feature table
        map.getOperationalLayers().add(featureLayer);
    }

    private void showDeviceSelector() {
        // show popup selector for device

//        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
//        ViewGroup viewGroup = findViewById(android.R.id.content);
//        View dialogView = LayoutInflater.from(MainActivity.this).inflate(R.layout.dialog_bluetooth_device_selector, viewGroup, false);
//        builder.setView(dialogView);
//        AlertDialog alertDialog = builder.create();
//        alertDialog.show();

//        FragmentManager fm = getSupportFragmentManager();
//        BluetoothDeviceSelectorDialogFragment editNameDialogFragment = BluetoothDeviceSelectorDialogFragment.newInstance("Bluetooth Device Selector", devices);
//        editNameDialogFragment.show(fm, "bluetooth_device_selector_fragment");

        Intent intent = new Intent(MainActivity.this, SelectBluetoothDeviceActivity.class);
        startActivityForResult(intent, SELECT_BLUETOOTH_DEVICE);
    }

    private void listenForNewAISMessage() {
        sharedPreferenceChangeListener = (sharedPreferences, key) -> {
            long messageId = sharedPreferences.getLong("ais_message_id", -1);
            Log.d("MainActivity AIS", String.valueOf(messageId));
            if (messageId != -1) {
                // TODO: Fetch coordinates from Room and display
                new FetchLatestPointFromDB().execute(messageId);
                // then plot it on the map
            }
        };

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        pref.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    }

    private void attemptToUploadToServer(AISMessage message) {
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://asean-hackathon2021.herokuapp.com/ais-create/";

        Gson gson = new Gson();
        String jsonString = gson.toJson(message);

        Log.d("JSON String", jsonString);

        JSONObject jsonBody = null;
        try {
            jsonBody = new JSONObject(jsonString);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        jsonBody.remove("id");
        jsonBody.remove("uploaded");
        jsonBody.remove("bb_timestamp");

        try {
            String date = jsonBody.getString("timestamp");
            Log.d("Current Date", date);

            SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//dd/MM/yyyy
            Date now = new Date();
            String strDate = sdfDate.format(now);
            Log.d("DATE", strDate);

            jsonBody.remove("timestamp");
            jsonBody.put("timestamp", strDate);

            jsonBody.put("mmsi", String.valueOf(pref.getInt("mmsi", -1)));
            jsonBody.put("fleet_id", 3);
        } catch (JSONException e) {
            e.printStackTrace();
        }

//        Log.d("JSON String Updated", jsonBody.toString());
//        Log.d("abc", abc.toString());

        Request<JSONObject> request = new JsonObjectRequest(url, jsonBody, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.d("SERVER_RESPONSE", response.toString());
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("REQUEST ERROR", error.toString());
            }
        });

        queue.add(request);
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {
        // Reference: https://www.androidhive.info/2017/12/android-working-with-bottom-navigation/
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            FragmentTransaction transaction;
            switch (item.getItemId()) {
                case R.id.navigation_profile:
                    MainActivity.this.setTitle("Profile");
                    mapView.setVisibility(View.GONE);
                    tripButton.setVisibility(View.GONE);
                    fragmentArea.setVisibility(View.VISIBLE);

                    transaction = getSupportFragmentManager().beginTransaction();
                    transaction.replace(R.id.home_screen_fragment_area, profileFragment);
                    transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                    transaction.commit();
                    return true;

                case R.id.navigation_home:
                    MainActivity.this.setTitle("FishNChips");
                    mapView.setVisibility(View.VISIBLE);
                    tripButton.setVisibility(View.VISIBLE);
                    fragmentArea.setVisibility(View.GONE);
                    return true;
            }
            return false;
        }
    };

    private class FetchLatestPointFromDB extends AsyncTask<Long, Long, List<AISMessage>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected List<AISMessage> doInBackground(Long... messageIds) {
            AISMessage message = aisMessageDao.getById(messageIds[0]);
            message.setBlackBoxData(MainActivity.this);
            aisMessageDao.update(message);

            attemptToUploadToServer(message);

            Log.d("MainActivity AIS", message.toString());

            long tripId = tripDao.getLatest();
            return aisMessageDao.getByTripId(tripId);
        }

        @Override
        protected void onPostExecute(List<AISMessage> messages) {
            super.onPostExecute(messages);
            // TODO: Show it on the map here
            plotPointsOnMap(messages);
        }
    }

    private void plotPointsOnMap(List<AISMessage> messages) {
        GraphicsOverlay graphicsOverlay = new GraphicsOverlay();

        PointCollection pointCollection = new PointCollection(SpatialReferences.getWgs84());

        for (int i = 0; i < messages.size(); i++) {
            AISMessage message = messages.get(i);
            pointCollection.add(message.phone_lon + (i * 0.001), message.phone_lat + (sqrt(i) * 0.001));
        }

        Polyline polyline = new Polyline(pointCollection);
        SimpleLineSymbol polylineSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, -0xff9c01, 3f);

        Graphic graphic = new Graphic(polyline, polylineSymbol);
        graphicsOverlay.getGraphics().add(graphic);

        mapView.getGraphicsOverlays().clear();
        mapView.getGraphicsOverlays().add(graphicsOverlay);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SELECT_BLUETOOTH_DEVICE) {
            Toast.makeText(this, "Connecting to " + data.getStringExtra("name"), Toast.LENGTH_SHORT).show();

            // attempt to connect to the Bluetooth Device
            bluetoothConnector.connectToDeviceMac(data.getStringExtra("mac_address"));
        }

    }

    private void disconnectBluetoothDevice() {
        bluetoothConnector.disconnect();
    }

    private void tripButtonClicked() {
        if (tripStatus == OFF) {
            // connect to bluetooth
            showDeviceSelector();
        } else {
            // stop the trip
            disconnectBluetoothDevice();
            stopTrackingService();
        }
    }

//    @SuppressLint("ApplySharedPref")
    private void stopTrackingService() {
        tripStatus = OFF;
        tripButton.setText("Start");

        LocationDisplay locationDisplay = mapView.getLocationDisplay();
        locationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.RECENTER);

        showTripEndedAlert();

        editor = pref.edit();
        editor.putBoolean("stop", true);
        editor.apply();
    }

    private void startTrackingService() {
        tripStatus = ON;
        tripButton.setText("Stop");

        LocationDisplay locationDisplay = mapView.getLocationDisplay();
        locationDisplay.setAutoPanMode(LocationDisplay.AutoPanMode.COMPASS_NAVIGATION);

        editor = pref.edit();
        editor.putBoolean("stop", false);
        editor.commit();

        listenForNewAISMessage();

        Intent serviceIntent = new Intent(this, LocationTrackerService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    private void showTripEndedAlert() {
        new AlertDialog.Builder(this)
                .setTitle("Trip saved successfully")
                .setMessage("Do you want to add more info to catch reports?")

                // Specifying a listener allows you to take an action before dismissing the dialog.
                // The dialog is automatically dismissed when a dialog button is clicked.
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Continue with delete operation
                        Intent intent = new Intent(MainActivity.this, TripMapActivity.class);
                        startActivity(intent);
                    }
                })

                // A null listener allows the button to dismiss the dialog and take no further action.
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    private void updateBlackBoxData(JSONObject message) throws JSONException {
        SharedPreferences.Editor editor = blackBoxPref.edit();
        editor.putFloat("altitude", (float) message.getDouble("alt"));
        editor.putFloat("pressure", (float) message.getDouble("p"));
        editor.putFloat("temperature", (float) message.getDouble("tmp"));

        editor.putFloat("latitude", (float) message.getDouble("lat"));
        editor.putFloat("longitude", (float) message.getDouble("lon"));

        editor.putString("timestamp", message.getString("tim"));
        editor.putString("nos", message.getString("nos"));
        editor.putString("sos", message.getString("sos"));
        editor.putString("sos_f", message.getString("sos_f"));

        editor.putBoolean("black_box_integrity", message.getBoolean("bbi"));

        editor.apply();
    }

    @Override
    protected void onPause() {
        mapView.pause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.resume();
    }

    @Override
    protected void onDestroy() {
        mapView.resume();
        super.onDestroy();
    }

    @Override
    public void onMessageReceived(int status, String message) {

        switch (status) {
            case DEVICE_CONNECTED:
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                startTrackingService();
                break;

            case UNABLE_TO_CONNECT:
                // TODO: Remove the following function call
//                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                startTrackingService();
//                tripStatus = OFF;
//                tripButton.setText("Start");
                break;

            case MESSAGE_FROM_DEVICE:
                try {
                    updateBlackBoxData(new JSONObject(message));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
        }
    }
}