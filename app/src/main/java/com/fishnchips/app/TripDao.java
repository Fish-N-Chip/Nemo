package com.fishnchips.app;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface TripDao {
    @Query("SELECT * FROM trip")
    List<Trip> getAll();

    @Query("SELECT tripId FROM trip ORDER BY timestamp DESC LIMIT 1")
    int getLatest();

    @Insert
    void insertAll(Trip... trips);

//    @Update

    @Insert
    void insert(Trip trip);
}
