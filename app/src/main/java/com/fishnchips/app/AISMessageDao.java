package com.fishnchips.app;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface AISMessageDao {
    @Query("SELECT * FROM aismessage WHERE id = (:id)")
    AISMessage getById(long id);

    @Query("SELECT * FROM aismessage")
    List<AISMessage> getAll();

    @Query("SELECT * FROM aismessage WHERE tripId = (:tripId) ORDER BY id")
    List<AISMessage> getByTripId(long tripId);

    @Insert
    void insertAll(AISMessage... users);

    @Delete
    void delete(AISMessage user);

    @Insert
    long insert(AISMessage message);

    @Update
    void update(AISMessage message);
}
