package com.kyas.wolkandhold;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.kyas.wolkandhold.database.AppDatabase;
import com.kyas.wolkandhold.database.entities.Route;
import com.kyas.wolkandhold.mapfragment.BufferedRoute;
import com.yandex.mapkit.geometry.Point;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RouteViewModel extends AndroidViewModel {
    private final MutableLiveData<List<Point>> _points = new MutableLiveData<>(new ArrayList<>());

    private final LiveData<List<Route>> routes;

    private MutableLiveData<Point> location;

    public RouteViewModel(@NonNull Application application) {
        super(application);
        routes = AppDatabase.getInstance(getApplication()).getRouteDao().getAllRoutes();
    }

    public MutableLiveData<Point> getLocation() {
        if (location == null) {
            location = new MutableLiveData<>(new Point(0.0, 0.0));
        }
        Log.d("TAG", "getLocation: " + location.getValue().getLongitude() + " | " + location.getValue().getLongitude());
        return location;
    }


    public LiveData<List<Route>> getRoutes() {
        return routes;
    }

    public void updatePoints() {
        List<Point> current = BufferedRoute.getAll();
        _points.setValue(current);
    }

    public LiveData<List<Point>> getPoints() {
        return _points;
    }


    public void clear() {
        _points.setValue(new ArrayList<>());
    }
}
