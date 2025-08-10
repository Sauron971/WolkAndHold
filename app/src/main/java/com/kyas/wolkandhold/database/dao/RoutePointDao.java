package com.kyas.wolkandhold.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.kyas.wolkandhold.database.entities.RoutePoint;

import java.util.List;

@Dao
public interface RoutePointDao {
    @Insert
    void insert(RoutePoint point);
    @Insert
    void insertAll(List<RoutePoint> pointList);
    @Query("SELECT * FROM RoutePoint WHERE routeId = :routeId")
    List<RoutePoint> getPointsForRoute(long routeId);

    @Query("SELECT * FROM RoutePoint ")
    List<RoutePoint> getAllPointsRoute();

}
