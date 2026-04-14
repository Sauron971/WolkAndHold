package com.kyas.wolkandhold.data;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kyas.wolkandhold.data.api.response.PolygonResponse;
import com.kyas.wolkandhold.data.api.response.TailResponse;
import com.kyas.wolkandhold.data.models.PlayerModel;
import com.kyas.wolkandhold.data.database.entities.Polygon;
import com.kyas.wolkandhold.data.database.entities.Route;
import com.kyas.wolkandhold.ui.data.LoginDataSource;
import com.kyas.wolkandhold.ui.data.LoginRepository;
import com.kyas.wolkandhold.ui.data.UserRepository;
import com.kyas.wolkandhold.ui.data.model.LoggedInUser;
import com.kyas.wolkandhold.ui.mapfragment.PlayerMarkUiModel;
import com.kyas.wolkandhold.ui.mapfragment.PolygonUiModel;
import com.yandex.mapkit.geometry.Point;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class RouteViewModel extends AndroidViewModel {

    private final DataRepository repo = DataRepository.getInstance(getApplication());

    public RouteViewModel(@NonNull Application application) {
        super(application);
    }


    public MutableLiveData<Point> getLocation() {
        return repo.getLocation();
    }
    public void requestLocation() {
        repo.getLastLocation();
    }
    private List<Point> parsePoints(String jsonPoints) {
        Gson gson = new Gson();
        Type pointListType = new TypeToken<List<Point>>(){}.getType();
        return gson.fromJson(jsonPoints, pointListType);
    }

    public LiveData<List<Route>> getRoutes() {
        return repo.getRoutes();
    }

    // CHANGE: Прокси-методы для обновления/удаления
    public void renameRoute(Route route, String newName) {
        repo.renameRoute(route, newName);
    }
    public void deleteRoute(long routeId) {
        repo.deleteRoute(routeId);
    }

    public LiveData<List<PolygonUiModel>> getPolygons() {
        LiveData<List<PolygonUiModel>> polygonsUi = Transformations.map(repo.getPolygons(), entities -> {
            List<PolygonUiModel> result = new ArrayList<>();
            for (Polygon e : entities) {
                PolygonUiModel ui = new PolygonUiModel();
                ui.id = String.valueOf(e.id);
                ui.ownerLabel = e.ownerName != null ? e.ownerName : "Аноним";
                ui.points = parsePoints(e.pointsJson);
                ui.area = e.area;
                result.add(ui);
            }
            return result;
        });
        return polygonsUi;
    }

    public void loadPolygonsFromApi() {
        repo.loadPolygons();
    }
    public void connectWebSocket(String token) {
        repo.startGameSession(token);
    }

    public LiveData<List<PlayerMarkUiModel>> getPlayersMarks() {
        LiveData<List<PlayerMarkUiModel>> playersUi = Transformations.map(repo.getPlayers(), entities -> {
            List<PlayerMarkUiModel> result = new ArrayList<>();
            for (var entry : entities.entrySet()) {
                PlayerModel e = entry.getValue();
                PlayerMarkUiModel ui = new PlayerMarkUiModel();
                ui.id = e.playerId;
                ui.playerName = e.playerName;
                ui.point = new Point(e.lat, e.lon);
                ui.isCapture = e.isCapture;
                result.add(ui);
            }
            return result;
        });
        return playersUi;
    }
    public LiveData<TailResponse> getCroppedTail() {
        return repo.getCroppedTail();
    }
    public LiveData<List<Point>> getPoints() {
        return repo.getPoints();
    }
    public void clearPoints() {
        repo.clearPoints();
    }
    public double distanceFirstToLast(List<Point> points) {
        return repo.distanceFirstToLast(points);
    }

    public void saveRoute(String routeName) {
        repo.saveRoute(routeName);
    }

    public void saveRoute(String routeName, List<Point> points) {
        repo.saveRoute(routeName, points);
    }
    public void addPoint(double lat, double lon) {
        repo.addPoint(new Point(
                lat,
                lon
        ));
    }
    public LiveData<PolygonResponse> getPolygonSaved() {
        return repo.getPolygonSaved();
    }

    public void uploadPolygon(Polygon polygon) {
        repo.sendPolygonRequest(polygon);
    }

    public LoggedInUser getSession() {
        return repo.getSession();
    }
    public void clearSession() {
        repo.clearSession();

    }


}
