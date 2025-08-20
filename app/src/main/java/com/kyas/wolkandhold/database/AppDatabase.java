package com.kyas.wolkandhold.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.kyas.wolkandhold.database.dao.PolygonDao;
import com.kyas.wolkandhold.database.dao.RouteDao;
import com.kyas.wolkandhold.database.dao.RoutePointDao;
import com.kyas.wolkandhold.database.entities.Polygon;
import com.kyas.wolkandhold.database.entities.Route;
import com.kyas.wolkandhold.database.entities.RoutePoint;

@Database(entities = {Route.class, RoutePoint.class, Polygon.class}, version = 7)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase INSTANCE;
    public abstract RouteDao getRouteDao();
    public abstract RoutePointDao getRoutePointDao();
    public abstract PolygonDao getPolygonDao();
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "routes_db")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
