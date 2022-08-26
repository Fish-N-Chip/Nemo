package com.fishnchips.app;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

import static androidx.room.ForeignKey.CASCADE;

@Entity(foreignKeys = {
        @ForeignKey(entity = Trip.class,
                parentColumns = "tripId",
                childColumns = "tripId",
                onDelete = CASCADE)
})
public class AISMessage {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "phone_lat")
    public double phone_lat;

    @ColumnInfo(name = "phone_lon")
    public double phone_lon;

    @ColumnInfo(name = "timestamp")
    public String timestamp;

    // This is a ForeignKey
    public long tripId;

    @ColumnInfo(name = "uploaded")
    public boolean uploaded;

    public AISMessage(double phone_lat, double phone_lon, String timestamp, long tripId) {
        this.phone_lat = phone_lat;
        this.phone_lon = phone_lon;
        this.timestamp = timestamp;
        this.tripId = tripId;
        this.uploaded = false;
    }

    @Override
    public String toString() {
        return "AISMessage{" +
                "id=" + id +
                ", Phone_GPS=(" + phone_lat +
                ", " + phone_lon +
                "), date='" + timestamp + '\'' +
                ", tripId=" + tripId +
                ", alt=" + altitude +
                ", p=" + pressure +
                ", tmp=" + temperature +
                ", BB_GPS=(" + bb_lat +
                ", " + bb_lon +
                "), BB_timestamp='" + bb_timestamp + '\'' +
                '}';
    }

    @ColumnInfo(name = "altitude")
    public float altitude;

    @ColumnInfo(name = "pressure")
    public float pressure;

    @ColumnInfo(name = "temperature")
    public float temperature;

    @ColumnInfo(name = "bb_lat")
    public float bb_lat;

    @ColumnInfo(name = "bb_lon")
    public float bb_lon;

    @ColumnInfo(name = "bb_timestamp")
    public String bb_timestamp;

    @ColumnInfo(name = "nos")
    public String nos;

    @ColumnInfo(name = "sos")
    public String sos;

    @ColumnInfo(name = "sos_f")
    public String sos_f;

    @ColumnInfo(name = "blackBoxIntegrity")
    public boolean blackBoxIntegrity;

    public void setBlackBoxData(Context context) {
        SharedPreferences pref = context.getSharedPreferences("black_box_signals", Context.MODE_PRIVATE);
        this.altitude = pref.getFloat("altitude", 0);
        this.pressure = pref.getFloat("pressure", 0);
        this.temperature = pref.getFloat("temperature", 0);
        this.bb_lat = pref.getFloat("latitude", 0);
        this.bb_lon = pref.getFloat("longitude", 0);

        this.bb_timestamp = pref.getString("timestamp", "1970-01-01 00:00:00");
        this.nos = pref.getString("nos", "0");
        this.sos = pref.getString("sos", "0");
        this.sos_f = pref.getString("sos_f", "0");

        this.blackBoxIntegrity = pref.getBoolean("black_box_integrity", true);
    }
}