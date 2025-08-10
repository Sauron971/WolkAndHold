package com.kyas.wolkandhold.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.kyas.wolkandhold.database.entities.Route;

import java.util.List;

@Dao
public interface RouteDao {
    @Insert
    long insert(Route route);

    @Delete
    void delete(Route route);
    @Query("SELECT * FROM Route")
    LiveData<List<Route>> getAllRoutes();
}
