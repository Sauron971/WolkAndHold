package com.kyas.wolkandhold;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.kyas.wolkandhold.api.ApiService;
import com.kyas.wolkandhold.api.response.PolygonResponse;
import com.kyas.wolkandhold.database.AppDatabase;
import com.kyas.wolkandhold.database.dao.PolygonDao;
import com.kyas.wolkandhold.database.entities.Polygon;
import com.kyas.wolkandhold.database.entities.Route;
import com.kyas.wolkandhold.mapfragment.BufferedRoute;
import com.yandex.mapkit.geometry.Point;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RouteViewModel extends AndroidViewModel {
    private final MutableLiveData<List<Point>> _points = new MutableLiveData<>(new ArrayList<>());

    private final LiveData<List<Route>> routes;
    private final LiveData<List<Polygon>> polygons;

    private MutableLiveData<Point> location;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public RouteViewModel(@NonNull Application application) {
        super(application);
        routes = AppDatabase.getInstance(getApplication()).getRouteDao().getAllRoutes();
        polygons = AppDatabase.getInstance(getApplication()).getPolygonDao().getAllPolygons();
    }

    public MutableLiveData<Point> getLocation() {
        if (location == null) {
            location = new MutableLiveData<>(new Point(0.0, 0.0));
        }
        Log.d("GPS", "getLocation: " + location.getValue().getLongitude() + " | " + location.getValue().getLongitude());
        return location;
    }


    public LiveData<List<Route>> getRoutes() {
        return routes;
    }

    public LiveData<List<Polygon>> getPolygons() {
        return polygons;
    }

    public void loadPolygonsFromApi(ApiService apiService) {
        if (location.getValue() != null) {
            apiService.getPolygonsInRadius(
                    location.getValue().getLatitude(),
                    location.getValue().getLongitude(), 100).enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<List<PolygonResponse>> call, @NonNull Response<List<PolygonResponse>> response) {
                    if (response.isSuccessful()) {
                        List<PolygonResponse> responses = response.body();
                        if (responses != null) {
                            executor.execute(() -> {
                                PolygonDao dao = AppDatabase.getInstance(getApplication()).getPolygonDao();
                                responses.forEach((r) -> {
                                    dao.upsert(r.toEntity());
                                });
                                Log.d("Response", responses.toString());
                            });
                        }
                    } else {
                        Log.d("API", "Response get polygons not successful");
                    }
                }

                @Override
                public void onFailure(Call<List<PolygonResponse>> call, Throwable t) {

                    Log.d("API", "Failure response get polygons " + t.getLocalizedMessage());
                }
            });
        }
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
