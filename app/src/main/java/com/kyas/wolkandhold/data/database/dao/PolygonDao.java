package com.kyas.wolkandhold.data.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;
import androidx.room.Upsert;

import com.kyas.wolkandhold.data.database.entities.Polygon;

import java.util.List;

@Dao
public interface PolygonDao {

    @Insert
    long addPolygon(Polygon polygon);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Polygon> polygons);

    @Insert
    void insert(Polygon polygon);

    @Update
    int updatePolygon(Polygon polygon);

    @Upsert
    long upsertPolygon(Polygon polygon);

    @Query("SELECT * FROM polygons WHERE id = :id LIMIT 1")
    Polygon getById(long id);

    @Query("Select * From polygons Where userId = :userId")
    List<Polygon> getPolygonsByUser(long userId);

    @Query("Select * From polygons")
    List<Polygon> getAllPolygons();

    @Query("Select * From polygons")
    LiveData<List<Polygon>> getAllPolygonsLive();

    @Query("Delete From polygons Where id = :id")
    void deletePolygonById(long id);

    @Query("Delete From polygons ")
    void deleteAll();

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
