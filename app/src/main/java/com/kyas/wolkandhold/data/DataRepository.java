package com.kyas.wolkandhold.data;


import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.kyas.wolkandhold.BuildConfig;
import com.kyas.wolkandhold.data.api.ApiService;
import com.kyas.wolkandhold.data.api.AuthInterceptor;
import com.kyas.wolkandhold.data.api.GameSocketManager;
import com.kyas.wolkandhold.data.api.requests.PolygonRequest;
import com.kyas.wolkandhold.data.api.requests.UserRequest;
import com.kyas.wolkandhold.data.api.response.PolygonResponse;
import com.kyas.wolkandhold.data.api.response.TailResponse;
import com.kyas.wolkandhold.data.database.AppDatabase;
import com.kyas.wolkandhold.data.database.RouteRepository;
import com.kyas.wolkandhold.data.database.dao.PolygonDao;
import com.kyas.wolkandhold.data.database.dao.RouteDao;
import com.kyas.wolkandhold.data.database.dao.RoutePointDao;
import com.kyas.wolkandhold.data.models.PlayerModel;
import com.kyas.wolkandhold.data.database.entities.Polygon;
import com.kyas.wolkandhold.data.database.entities.Route;
import com.kyas.wolkandhold.ui.data.UserRepository;
import com.kyas.wolkandhold.ui.data.model.LoggedInUser;
import com.yandex.mapkit.geometry.Point;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;

public class DataRepository {


    private static volatile DataRepository INSTANCE;
    private final MutableLiveData<List<Point>> points = new MutableLiveData<>(new ArrayList<>());
    private final LiveData<List<Polygon>> polygons;
    private final MutableLiveData<Map<Long, PlayerModel>> players;
    private final MutableLiveData<PolygonResponse> polygonSaved = new MutableLiveData<>();
    private final MutableLiveData<List<Route>> routes = new MutableLiveData<>();
    private final MutableLiveData<Point> location = new MutableLiveData<>();
    private final MutableLiveData<TailResponse> croppedTail = new MutableLiveData<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final FusedLocationProviderClient mFusedLocationClient;
    private final SharedPreferences set;
    private final AppDatabase db;
    private final UserRepository userRepo = UserRepository.getInstance();

    private Retrofit retrofit;
    private final ApiService apiService;
    private final GameSocketManager socketManager;


    public DataRepository(Context context) {
        db = AppDatabase.getInstance(context);
        executor.execute(() -> {
            routes.postValue(db.getRouteDao().getAllRoutes());
        });
        set = context.getApplicationContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        polygons = db.getPolygonDao().getAllPolygonsLive();
        players = new MutableLiveData<>();
        players.setValue(new HashMap<>());
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new AuthInterceptor(set))
                .build();
        this.socketManager = new GameSocketManager();

        retrofit = new Retrofit.Builder()
                .baseUrl(BuildConfig.API_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        setPolygonListener();
        setPlayerListener();
        setTailListener();
    }

    public static DataRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (DataRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new DataRepository(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    public LiveData<List<Point>> getPoints() {
        return points;
    }

    //Расчет дистации для замыкания круга
    public double distanceFirstToLast(List<Point> points) {
        if (points.isEmpty())
            return Double.MAX_VALUE;
        double x1lat = points.get(0).getLatitude();
        double y1lon = points.get(0).getLongitude();
        double x2lat = points.get(points.size()-1).getLatitude();
        double y2lon = points.get(points.size()-1).getLongitude();
        float[] distance = new float[1];
        Location.distanceBetween(x1lat, y1lon, x2lat, y2lon, distance);
        return distance[0];
    }

    public void addPoint(Point newPoint) {
        // 1. Берём текущие точки и готовим новый список
        List<Point> previous = new ArrayList<>(points.getValue());
        List<Point> current  = new ArrayList<>(previous);
        current.add(newPoint);
        // 2. Считаем расстояние "до" и "после" по ОДНОМУ и тому же типу списка
        double prevDist = distanceFirstToLast(previous);
        double newDist  = distanceFirstToLast(current);
        // 3. Считаем площадь по current
        double area = polygonAreaOnEarth(current);
        // 4. Проверяем переход через порог + минимальное количество вершин
        boolean enoughPoints = current.size() >= 4;
        boolean wasOpen   = prevDist > 10;
        boolean nowClosed = newDist <= 10;

        Log.d("PointsRepo", "Area=" + polygonAreaOnEarth(current));
        if (enoughPoints
                && wasOpen
                && nowClosed
                && area >= 30){
            saveRoute(LocalDateTime.now().toString());
        }
        points.postValue(current);
    }
    public void clearPoints() {
        if (points.getValue() != null || !points.getValue().isEmpty())
            socketManager.sendRequestCutTail(points.getValue());
        points.postValue(new ArrayList<>());
    }

    public MutableLiveData<List<Route>> getRoutes() {
        return routes;
    }

    private void reloadRoutes() {
        executor.execute(() -> routes.postValue(db.getRouteDao().getAllRoutes()));
    }

    public void saveRoute(String routeName) {
        executor.execute(() -> {
            List<Point> points = this.points.getValue();
            saveRouteInternal(routeName, points);
        });
        clearPoints();
    }

    public void saveRoute(String routeName, List<Point> points) {
        executor.execute(() -> {
            saveRouteInternal(routeName, points);
        });
    }

    private void saveRouteInternal(String routeName, List<Point> points) {
        RouteDao rd = db.getRouteDao();
        RoutePointDao rpd = db.getRoutePointDao();
        PolygonDao pd = db.getPolygonDao();
        // Сохраняем маршрут и его точки в базу данных
        RouteRepository routeRep = new RouteRepository(rd, rpd);
        // Используем LOCAL_USER_ID для обозначения собственных маршрутов пользователя
        routeRep.addNewRoute(routeName, getDistance(points), Constants.LOCAL_USER_ID);
        routeRep.addPointsToRoute(points);
        
        // Cоздаем новую территорию для локального пользователя
        // LOCAL_USER_ID означает, что это собственный полигон пользователя
        Polygon dbPoly = new Polygon();
        dbPoly.userId = Constants.LOCAL_USER_ID;
        dbPoly.lastUpdated = System.currentTimeMillis();
        Gson gson = new Gson();
        dbPoly.pointsJson = gson.toJson(points);
        dbPoly.area = polygonAreaOnEarth(points);
        // Используем upsert для автоматического обновления или создания
        //pd.insert(dbPoly); Все равно приходит с сервера наш же полигон когда он сохранен
        sendPolygonRequest(dbPoly);

        Log.d("DialogSaveRoute", "saved new route with id:" + routeRep.currentRouteId);
        reloadRoutes();
    }

    public void renameRoute(Route route, String newName) {
        executor.execute(() -> {
            route.name = newName;
            RouteRepository rr = new RouteRepository(db.getRouteDao(), db.getRoutePointDao());
            rr.updateRoute(route);
            reloadRoutes();
        });
    }

    public void deleteRoute(long routeId) {
        executor.execute(() -> {
            RouteRepository rr = new RouteRepository(db.getRouteDao(), db.getRoutePointDao());
            rr.deleteRoute(routeId);
            reloadRoutes();
        });
    }
    public double getDistance(List<Point> points) {
        if (points == null || points.size() < 2) return 0.0;

        double result = 0.0;
        float[] distance = new float[1];

        for (int i = 0; i < points.size() - 1; i++) {
            Location.distanceBetween(
                    points.get(i).getLatitude(), points.get(i).getLongitude(),
                    points.get(i + 1).getLatitude(), points.get(i + 1).getLongitude(),
                    distance
            );
            result += distance[0];
        }

        return result;
    }
    public void sendPolygonRequest(Polygon poly) {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<Point>>(){}.getType();
        List<Point> points = gson.fromJson(poly.pointsJson, listType);
        PolygonRequest request = new PolygonRequest(
                new UserRequest(poly.userId, poly.ownerName),
                points,
                poly.area,
                poly.lastUpdated);
        Log.d("Polygon", "sendPolygonRequest: " + gson.toJson(request));
        apiService.insertPolygon(request).enqueue(new Callback<PolygonResponse>() {
            @Override
            public void onResponse(@NonNull Call<PolygonResponse> call, @NonNull Response<PolygonResponse> response) {
                polygonSaved.postValue(response.body());
            }

            @Override
            public void onFailure(@NonNull Call<PolygonResponse> call, @NonNull Throwable t) {
            }
        });
    }

    public LiveData<List<Polygon>> getPolygons() {
        return polygons;
    }

    public LiveData<PolygonResponse> getPolygonSaved() {
        return polygonSaved;
    }

    public MutableLiveData<Map<Long, PlayerModel>> getPlayers() {
        return players;
    }

    public MutableLiveData<TailResponse> getCroppedTail() {
        return croppedTail;
    }

    @SuppressLint("CheckResult")
    public void loadPolygons() {
        apiService.getPolygonsInRadius(
                location.getValue().getLatitude(),
                location.getValue().getLongitude(), Constants.DEFAULT_SEARCH_RADIUS_METERS).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<List<PolygonResponse>> call, @NonNull Response<List<PolygonResponse>> response) {
                if (response.isSuccessful()) {
                    List<PolygonResponse> responses = response.body();
                    if (responses != null) {
                        executor.execute(() -> {
                            List<Polygon> list = new ArrayList<>();
                            responses.forEach((r) -> {
                                list.addAll(r.toEntities());
                            });
                            PolygonDao dao = db.getPolygonDao();
                            list.forEach(dao::upsert);
                        });
                        Log.d("API", "Get polygons: " + responses);
                        if (responses.isEmpty()) {
                            executor.execute(() -> {
                                db.getPolygonDao().deleteAll();
                            });
                        }
                    }
                } else {
                    Log.d("API", "Response get polygons return null polygons");
                }
            }

            @Override
            public void onFailure(Call<List<PolygonResponse>> call, Throwable t) {

                Log.d("API", "Failure response get polygons " + t.getLocalizedMessage());
            }
        });

    }

    //Метод для высчитывания площади в м2 произвольного многоугольника на поверхности земли
    private double polygonAreaOnEarth(List<Point> coords) {
        if (coords.size() < 3) return 0;

        double total = 0.0;
        int n = coords.size();

        for (int i = 0; i < n; i++) {
            double[] p1 = {coords.get(i).getLatitude(), coords.get(i).getLongitude()};
            double[] p2 = {coords.get((i + 1) % n).getLatitude(), coords.get((i + 1) % n).getLongitude()};

            double lat1 = Math.toRadians(p1[0]);
            double lon1 = Math.toRadians(p1[1]);
            double lat2 = Math.toRadians(p2[0]);
            double lon2 = Math.toRadians(p2[1]);

            total += (lon2 - lon1) * (2 + Math.sin(lat1) + Math.sin(lat2));
        }

        double earthRadius = 6378137;
        return Math.abs(total * earthRadius * earthRadius / 2.0);
    }
    public void startGameSession(String token) {
        socketManager.connect(token);
    }


    // Метод для отправки координат из GPS-сервиса
    public void emitLocation(double lat, double lon, boolean isCapture) {
        socketManager.sendLocation(lat, lon, isCapture);
        socketManager.updatePolygonSubscription(lat, lon);
    }

    public void setPolygonListener() {
        socketManager.setPolygonListener(polys -> {
            executor.execute(() -> {
                if (polys.stream().noneMatch(p -> p.pointsJson.isEmpty())) {
                    for (Polygon poly : polys.stream().filter(p-> p.pointsJson.isEmpty()).collect(Collectors.toList())) {
                        db.getPolygonDao().deletePolygonById(poly.id);
                        polys.remove(poly);
                    }
                }
                db.getPolygonDao().insertAll(polys);
                boolean pointContains = false;
                int index = -1;
                List<Point> currentPoints = points.getValue();
                if (!polys.isEmpty() && currentPoints != null) {
                    for (int i = 0; i < polys.size(); i++) {
                        if (polygonSaved.getValue() == null || polys.get(i).id != polygonSaved.getValue().getId()) {
                            for (int i1 = currentPoints.size()-1; i1 > 0; i1--) {
                                if (pointIsInside(currentPoints.get(i1), polys.get(i).getPoints())) {
                                    pointContains = true;
                                    index = i1;
                                    break;
                                }
                            }
                            if (pointContains)
                                break;
                        }
                    }
                }
                if (pointContains && index != -1) {
                    socketManager.sendRequestCutTail(currentPoints.subList(index, currentPoints.size()));
                    points.postValue(currentPoints.subList(index, currentPoints.size()));
                } else {
                    clearPoints();
                }
            });
        });
    }

    public void setPlayerListener() {
        socketManager.setPlayerListener(player -> {
            Map<Long, PlayerModel> currentMap = players.getValue();
            if (currentMap == null) currentMap = new HashMap<>();
            Map<Long, PlayerModel> updatedMap = new HashMap<>(currentMap);
            updatedMap.put(player.playerId, player);
            players.postValue(updatedMap);
        });
    }
    public void setTailListener() {
        socketManager.setPlayerTailListener(croppedTail::postValue);
    }

    @SuppressLint("MissingPermission")
    public void getLastLocation() {
        mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                Location loc = task.getResult();
                if (loc == null) {
                    requestNewLocationData();
                } else {
                    location.postValue(new Point(loc.getLatitude(), loc.getLongitude()));
                    Log.d("GPS", "getLocation: " + loc.getLatitude() + " | " + loc.getLongitude());

                }
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void requestNewLocationData() {
        LocationRequest mLocationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(0)
                .setMaxUpdates(1)
                .build();

        mFusedLocationClient.requestLocationUpdates(
                mLocationRequest,
                mLocationCallback,
                Looper.getMainLooper()
        );
    }
    private final LocationCallback mLocationCallback = new LocationCallback() {

        @Override
        public void onLocationResult(LocationResult locationResult) {
            Location mLastLocation = locationResult.getLastLocation();
            assert mLastLocation != null;
            location.setValue(new Point(
                    mLastLocation.getLatitude(),
                    mLastLocation.getLongitude()
            ));
        }
    };
    public static boolean pointIsInside (Point target, List<Point> polygon) {
        boolean result = false;
        int n = polygon.size();

        for (int i = 0, j = n - 1; i < n; j = i++) {
            Point p1 = polygon.get(i);
            Point p2 = polygon.get(j);

            if ((p1.getLatitude() > target.getLatitude()) != (p2.getLatitude() > target.getLatitude())) {
                double intersectLon = (p2.getLongitude() - p1.getLongitude()) * (target.getLatitude() - p1.getLatitude()) / (p2.getLatitude() - p1.getLatitude()) + p1.getLongitude();

                if (target.getLongitude() < intersectLon) {
                    result = !result;
                }
            }
        }
        return result;
    }

    public MutableLiveData<Point> getLocation() {
        return location;
    }

    public void saveSession(Long id, String username, String jwt) {
        userRepo.saveSession(new LoggedInUser(id, username, jwt));
    }
    public void saveSession(LoggedInUser user) {
        userRepo.saveSession(user);
    }

    public LoggedInUser getSession() {
        return userRepo.getSession();
    }
    public void clearSession() {
        userRepo.clearSession();
        SharedPreferences.Editor editor = set.edit();
        editor.putString("token", "");
        editor.apply();
    }
}
