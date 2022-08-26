package com.fishnchips.app;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {
        AISMessage.class,
        Trip.class}, version = 8)
public abstract class AppDatabase extends RoomDatabase {
    public abstract AISMessageDao aisMessageDao();

    public abstract TripDao tripDao();
}
