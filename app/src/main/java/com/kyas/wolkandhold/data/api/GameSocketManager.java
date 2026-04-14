package com.kyas.wolkandhold.data.api;

import android.annotation.SuppressLint;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kyas.wolkandhold.BuildConfig;
import com.kyas.wolkandhold.data.Constants;
import com.kyas.wolkandhold.data.api.requests.LocationRequest;
import com.kyas.wolkandhold.data.api.requests.PathRequest;
import com.kyas.wolkandhold.data.api.response.PolygonResponse;
import com.kyas.wolkandhold.data.api.response.TailResponse;
import com.kyas.wolkandhold.data.api.response.UserResponse;
import com.kyas.wolkandhold.data.models.PlayerModel;
import com.kyas.wolkandhold.data.database.entities.Polygon;
import com.yandex.mapkit.geometry.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;
import ua.naiksoftware.stomp.dto.StompHeader;

public class GameSocketManager {
    private StompClient stompClient;
    private final Gson gson = new Gson();
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private Double pendingLat = null;
    private Double pendingLon = null;
    private double lastSubLat;
    private double lastSubLon;
    private long lastSubTimeMs = 0;
    private static final double MIN_DISTANCE_M = 300;
    private static final long MIN_TIME_MS = 60_000;
    private static final double RADIUS_M = Constants.DEFAULT_SEARCH_RADIUS_METERS;

    public interface OnPolygonReceivedListener {
        void onNewPolygons(List<Polygon> polygons);
    }

    private OnPolygonReceivedListener listenerPolygon;

    public void setPolygonListener(OnPolygonReceivedListener listener) {
        this.listenerPolygon = listener;
    }

    public interface OnPlayerReceivedListener {
        void onNewPlayer(PlayerModel player);
    }

    private OnPlayerReceivedListener listenerPlayer;

    public void setPlayerListener(OnPlayerReceivedListener listener) {
        this.listenerPlayer = listener;
    }
    public interface OnPlayerTailReceivedListener {
        void onCutTail(TailResponse tail);
    }

    private OnPlayerTailReceivedListener listenerTail;

    public void setPlayerTailListener(OnPlayerTailReceivedListener listener) {
        this.listenerTail = listener;
    }

    public void connect(String jwt) {
        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, BuildConfig.WS_URL);

        List<StompHeader> headers = new ArrayList<>();
        headers.add(new StompHeader("Authorization", "Bearer " + jwt));

        // Мониторинг состояния подключения
        Disposable lifecycle = stompClient.lifecycle().subscribe(lifecycleEvent -> {
            switch (lifecycleEvent.getType()) {
                case OPENED:
                    Log.d("WS", "Connected");
                    subscribeToPolygons();
                    subscribeToLocationsOfPlayer();
                    subscribeToTailsOfPlayers();

                    break;
                case ERROR:
                    Log.e("WS", "Error", lifecycleEvent.getException());
                    new Handler(Looper.getMainLooper()).postDelayed(() -> connect(jwt), 5000);
                    break;
                case CLOSED:
                    Log.d("WS", "Closed");
                    break;
            }
        });
        Log.d("WS_DEBUG", "Connecting to ws with jwt: " + jwt);
        compositeDisposable.add(lifecycle);
        stompClient.connect(headers);
        if (pendingLon != null && pendingLat != null) {
            updatePolygonSubscription(pendingLat, pendingLon);
        }
    }

    // Подписка на личную очередь (получение захваченных зон)
    @SuppressLint("CheckResult")
    private void subscribeToPolygons() {
        Disposable topic = stompClient.topic("/user/queue/polygons")
                .subscribe(msg -> {
                    List<PolygonResponse> response = gson.fromJson(msg.getPayload(),
                            new TypeToken<List<PolygonResponse>>(){}.getType());
                    if (listenerPolygon != null) {
                        List<Polygon> polygons = new ArrayList<>();
                        for (PolygonResponse resp : response) {
                            polygons.addAll(resp.toEntities());
                        }
                        listenerPolygon.onNewPolygons(polygons);
                    }
                }, err -> Log.e("WS", "Topic error", err));

        compositeDisposable.add(topic);
    }

    private void subscribeToLocationsOfPlayer() {
        Disposable topic = stompClient.topic("/topic/all_players")
                .subscribe(msg -> {
                    UserResponse response = gson.fromJson(msg.getPayload(), UserResponse.class);
                    Log.d("WS_PLAYER", "from server: userId=" + response.getUserId()
                            + " lat=" + response.getLat()
                            + " lon=" + response.getLon()
                            + " isCapture=" + response.isCapture());
                    if (listenerPlayer != null) {
                        listenerPlayer.onNewPlayer(new PlayerModel(
                                response.getUserId(),
                                response.getUsername(),
                                response.getLat(),
                                response.getLon(),
                                response.isCapture(),
                                System.currentTimeMillis()));
                    }
                }, err -> Log.e("WS", "Topic error", err));
        compositeDisposable.add(topic);
    }

    private void subscribeToTailsOfPlayers() {
        Disposable topic = stompClient.topic("/topic/tails")
                .subscribe(msg -> {
                    TailResponse response = gson.fromJson(msg.getPayload(), TailResponse.class);
                    if (listenerTail != null) {
                        listenerTail.onCutTail(response);
                    }
                }, err -> Log.e("WS", "Topic error", err));

        compositeDisposable.add(topic);
    }

    @SuppressLint("CheckResult")
    public void sendLocation(double lat, double lon, boolean isCapture) {
        if (stompClient != null && stompClient.isConnected()) {
            // Создаем простой объект для отправки
            LocationRequest request = new LocationRequest(lat, lon, isCapture);
            String json = gson.toJson(request);

            stompClient.send("/app/move", json)
                    .subscribe(() -> {},
                            err -> Log.e("WS", "Send error", err));
        }
    }
    @SuppressLint("CheckResult")
    public void sendRequestCutTail(List<Point> path) {
        if (stompClient != null && stompClient.isConnected()) {
            // Создаем простой объект для отправки
            PathRequest request = new PathRequest(null, null, path);
            String json = gson.toJson(request);

            stompClient.send("/app/cutTail", json)
                    .subscribe(() -> {},
                            err -> Log.e("WS", "Send error", err));
        }
    }


    @SuppressLint("CheckResult")
    public void updatePolygonSubscription(double lat, double lon) {
        long now = System.currentTimeMillis();

        if (stompClient == null || !stompClient.isConnected()) {
            pendingLat = lat;
            pendingLon = lon;
            return;
        }

        boolean timeOK = (now - lastSubTimeMs) > MIN_TIME_MS;
        float[] distance = new float[1];
        Location.distanceBetween(lastSubLat, lastSubLon, lat, lon, distance);
        boolean distOk =  distance[0] > MIN_DISTANCE_M;
        if (!timeOK && !distOk)
            return;



        String body = String.format(
                Locale.getDefault(),
                "{\"lat\":%f,\"lon\":%f,\"radius\":%.0f}",
                lat,
                lon,
                RADIUS_M);

        stompClient.send("/app/subscribe/polygons", body)
                .subscribe(
                        () -> Log.d("WS", "Send OK"),
                        err -> Log.e("WS", "Send error", err));

        lastSubLat = lat;
        lastSubLon = lon;
        lastSubTimeMs = now;
        pendingLat = null;
        pendingLon = null;
    }

    public void disconnect() {
        if (compositeDisposable != null) compositeDisposable.dispose();
        if (stompClient != null) stompClient.disconnect();
    }
}
