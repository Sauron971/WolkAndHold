package com.kyas.wolkandhold.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import androidx.room.Upsert;

import com.kyas.wolkandhold.database.entities.Polygon;

import java.util.List;

@Dao
public interface PolygonDao {

    @Insert
    long addPolygon(Polygon polygon);

    @Update
    int updatePolygon(Polygon polygon);

    @Upsert
    long upsertPolygon(Polygon polygon);

    @Query("SELECT * FROM polygons WHERE id = :id LIMIT 1")
    Polygon getById(long id);

    @Query("Select * From polygons Where userId = :userId")
    List<Polygon> getPolygonsByUser(long userId);

    @Query("Select * From polygons")
    LiveData<List<Polygon>> getAllPolygons();

    @Transaction
    default void upsert(Polygon newPolygon) {
        Polygon existing = getById(newPolygon.id);
        if (existing == null) {
            addPolygon(newPolygon);
        } else if (newPolygon.lastUpdated > existing.lastUpdated) {
            updatePolygon(newPolygon);
        }
    }
}
