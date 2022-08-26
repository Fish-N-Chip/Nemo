package com.fishnchips.app;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Trip {
    @PrimaryKey(autoGenerate = true)
    public int tripId;

    @ColumnInfo(name = "timestamp")
    public String date;

    @ColumnInfo(name = "inProgress")
    public Boolean inProgress;

    public Trip(String date) {
        this.date = date;
        this.inProgress = true;
    }

    @Override
    public String toString() {
        return "Trip{" +
                "tripId=" + tripId +
                ", date='" + date + '\'' +
                ", inProgress=" + inProgress +
                '}';
    }
}
