package com.fishnchips.app;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.room.Room;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.helpers.LocatorImpl;

import java.util.Date;
import java.util.List;

public class LocationTrackerService extends Service {

    private final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(GlobalVariables.getContext());

    public static final String CHANNEL_ID = "ForegroundServiceChannel";
    private static final int INTERVAL = 2000;
    private Handler handler;
    private Thread thread;

    private LocationManager locationManager;
//    private LocationListener locationListener;

    SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener;

    private boolean newTripInserted = false;

    AppDatabase db = Room.databaseBuilder(GlobalVariables.getContext(),
            AppDatabase.class, "database-name")
            .fallbackToDestructiveMigration()
            .build();

    AISMessageDao aisMessageDao = db.aisMessageDao();
    TripDao tripDao = db.tripDao();

    public LocationTrackerService() {

    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "Location tracking started.", Toast.LENGTH_LONG).show();

        createNotificationChannel();

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("GPS Tracking is on")
//                .setSmallIcon(R.drawable.ic_launcher_music)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        listenForStopService();

        new SaveNewTripToDBAsyncTask().execute();

        startLocationTrackingThread();

        return START_NOT_STICKY;
//        return START_STICKY;
    }

    private Location getLastKnownLocation() {
        // Reference: https://stackoverflow.com/a/20465781/8819252
        locationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
        List<String> providers = locationManager.getProviders(true);
        Location bestLocation = null;

        for (String provider : providers) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                Toast.makeText(this, "Location access needed.", Toast.LENGTH_SHORT).show();
                return null;
            }

            Location l = locationManager.getLastKnownLocation(provider);
            if (l == null) {
                continue;
            }
            if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                // Found best last known location: %s", l);
                bestLocation = l;
            }
        }
        return bestLocation;
    }

    private void getGPSCoordinates() {
        // Reference: https://stackoverflow.com/questions/57863500/how-can-i-get-my-current-location-in-android-using-gps
        Location location = getLastKnownLocation();
        if (location != null) {
//            Log.d("GPS Service", "(" + location.getLatitude() + ", " + location.getLongitude() + ")");
            ProcessLocationThread processLocationThread = new ProcessLocationThread(location);
            processLocationThread.start();
        } else {
            Log.d("GPS Service", "(null, null)");
        }
    }

    private class SaveNewTripToDBAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Trip trip = new Trip(new Date().toString());
            tripDao.insert(trip);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
//            setResult(ADD_NOTICE_RESULT);
//            finish();
            super.onPostExecute(aVoid);
            newTripInserted = true;
        }
    }

    private long saveToDB(Location location) {
        int latestTripId = tripDao.getLatest();

        String timestamp = new Date().toString();
        AISMessage message = new AISMessage(location.getLatitude(), location.getLongitude(), timestamp, latestTripId);
//        Log.d("AIS Message", message.toString());

        return aisMessageDao.insert(message);
    }

    private void triggerSharedPref(long id) {
        SharedPreferences.Editor editor = pref.edit();
        editor.putLong("ais_message_id", id);
        editor.apply();

//        Log.d("SharedPref", id + ", " + pref.getLong("ais_message_id", -1));
    }

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            try {
                if (newTripInserted) {
                    getGPSCoordinates(); //this function can change value of mInterval.
                }
            } finally {
                // Read more here: https://stackoverflow.com/a/6242292/8819252
                // 100% guarantee that this always happens, even if
                // your update method throws an exception
                handler.postDelayed(runnable, INTERVAL);
            }
        }
    };

    private void startLocationTrackingThread() {
        handler = new Handler(Looper.getMainLooper());
        runnable.run();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private void listenForStopService() {
        sharedPreferenceChangeListener = (sharedPreferences, key) -> {
            if (sharedPreferences.getBoolean("stop", true)) {
                Log.d("GPS Service", "Stopped service and thread");
                handler.removeCallbacks(runnable);
                stopSelf();
            }
        };

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        pref.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
//        throw new UnsupportedOperationException("Not yet implemented");
    }

    public class ProcessLocationThread extends Thread {

        private final Location location;

        public ProcessLocationThread(Location location) {
            this.location = location;
        }

        @Override
        public void run() {
            super.run();
            long messageId = saveToDB(location);
            triggerSharedPref(messageId);
        }

    }

}