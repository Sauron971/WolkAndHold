package com.kyas.wolkandhold.ui.mapfragment;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PointF;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.kyas.wolkandhold.R;
import com.kyas.wolkandhold.data.Constants;
import com.kyas.wolkandhold.data.RouteViewModel;
import com.kyas.wolkandhold.ui.MainActivity;
import com.kyas.wolkandhold.ui.ui.login.LoginActivity;
import com.kyas.wolkandhold.utils.DialogFactory;
import com.yandex.mapkit.Animation;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.LinearRing;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.geometry.Polygon;
import com.yandex.mapkit.geometry.Polyline;
import com.yandex.mapkit.layers.ObjectEvent;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.IconStyle;
import com.yandex.mapkit.map.MapObject;
import com.yandex.mapkit.map.MapObjectTapListener;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.map.PolygonMapObject;
import com.yandex.mapkit.map.PolylineMapObject;
import com.yandex.mapkit.map.RotationType;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.mapkit.user_location.UserLocationLayer;
import com.yandex.mapkit.user_location.UserLocationObjectListener;
import com.yandex.mapkit.user_location.UserLocationView;
import com.yandex.runtime.image.ImageProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class MapFragment extends Fragment implements UserLocationObjectListener {


    private static final int PERMISSION_ID = 44;
    private Activity activity;
    private ExtendedFloatingActionButton btnStartRecording;
    private FloatingActionButton btnCenterLocation;
    private FloatingActionButton btnAvatar;
    private View dimOverlay;
    private TextView infoOwner, infoArea;
    private MapView mapView;
    private boolean isRecording = false;
    private ExecutorService executor;
    private RouteViewModel routeViewModel;
    private PolylineMapObject recordingPolyline;
    private PlacemarkMapObject markStartRoute;
    private Map<Long, PolygonData> polygonsMapObjects;
    private Map<Long, PlacemarkMapObject> playersMarkMapObjects;
    private Map<Long, PolylineMapObject> playersPolylineMapObjects;



    public MapFragment() {
        // Required empty public constructor

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mapView = view.findViewById(R.id.mapview);
        btnStartRecording = view.findViewById(R.id.fab_start_record);
        btnCenterLocation = view.findViewById(R.id.fab_center_location);
        btnAvatar = view.findViewById(R.id.fap_avatar);

        dimOverlay = view.findViewById(R.id.dim_overlay);
        infoOwner = view.findViewById(R.id.info_owner);
        infoArea = view.findViewById(R.id.info_area);
        polygonsMapObjects = new HashMap<>();
        playersMarkMapObjects = new HashMap<>();
        playersPolylineMapObjects = new HashMap<>();

        UserLocationLayer ull = MapKitFactory.getInstance().createUserLocationLayer(mapView.getMapWindow());
        ull.setObjectListener(this);
        ull.setVisible(true);
        ull.setHeadingModeActive(true);

        view.findViewById(R.id.btn_close_info).setOnClickListener(v -> hidePolygonInfo());
        dimOverlay.setOnClickListener(v -> hidePolygonInfo());

        routeViewModel = new ViewModelProvider(requireActivity()).get(RouteViewModel.class);
        tryGetLocation();
        routeViewModel.getLocation().observe(getViewLifecycleOwner(), new Observer<Point>() {
            @Override
            public void onChanged(Point loc) {
                if (loc != null && (loc.getLatitude() != 0.0 || loc.getLongitude() != 0.0)) {
                    mapView.getMapWindow().getMap().move(
                            new CameraPosition(loc, 17f, 0, 20),
                            new Animation(Animation.Type.SMOOTH, 1),
                            null);
                    routeViewModel.getLocation().removeObserver(this);
                }
            }
        });
        routeViewModel.getPoints().observe(getViewLifecycleOwner(), updatedPoints -> {
            // Обновление линии записи для отображения на карте
            if (recordingPolyline != null) {
                recordingPolyline.setGeometry(new Polyline(updatedPoints));
                if (updatedPoints.isEmpty()) {
                    return;
                }
                mapView.getMapWindow().getMap().move(
                        new CameraPosition(updatedPoints.get(updatedPoints.size()-1), 17f, 0, 20),
                        new Animation(Animation.Type.SMOOTH, 1),
                        null
                );
            } else {
                // Отображается по щелчку на маршрут в рекуклере на routesFragment
                mapView.getMapWindow()
                        .getMap()
                        .getMapObjects()
                        .addPolyline(new Polyline(updatedPoints));
            }
        });
        routeViewModel.getLocation().observe(getViewLifecycleOwner(), new Observer<Point>() {
            // Однократный запрос на апи при получении координат пользователя
            @Override
            public void onChanged(Point loc) {
                if (loc != null && (loc.getLatitude() != 0.0 || loc.getLongitude() != 0.0)) {
                    routeViewModel.loadPolygonsFromApi();
                    routeViewModel.getLocation().removeObserver(this);

                }
            }
        });
        routeViewModel.getPolygons().observe(getViewLifecycleOwner(), (list) -> {
            List<Long> idsToDelete = list
                    .stream()
                    .filter(model -> polygonsMapObjects.containsKey(Long.valueOf(model.id)))
                    .map(model -> Long.valueOf(model.id))
                    .collect(Collectors.toList());
            idsToDelete.forEach(polygonsMapObjects::remove);
            list.forEach(this::renderPolygon);
        });

        routeViewModel.getPlayersMarks().observe(getViewLifecycleOwner(), (list) -> {
            //Обновление отображения точки и маршрута других игроков
            list.forEach((m) -> {
                renderingMarkOfPlayer(m.id, m.playerName, m.point);
                if(m.isCapture && !Objects.equals(routeViewModel.getSession().getUserId(), m.id)) {
                    renderingPolylineOfPlayer(m.id, m.point);
                }
            });
        });
        routeViewModel.getCroppedTail().observe(getViewLifecycleOwner(), tail -> {
            if (tail != null && !tail.getPath().isEmpty() && tail.getUserId() != null && !Objects.equals(routeViewModel.getSession().getUserId(), tail.getUserId())) {
                renderingPolylineOfPlayer(tail.getUserId(), tail.getPath());
                Log.d("CropTail", "Crop tail of player: " + tail.getUsername());
            }
        });


        btnStartRecording.setOnClickListener(v -> {
            if (isRecording) {
                stopRecording();
            } else {
                startRecording();
            }
        });


        btnStartRecording.setOnLongClickListener((v) -> {
            if (btnStartRecording.isExtended()) {
                btnStartRecording.shrink();
            } else {
                btnStartRecording.extend();
            }
            return true;
        });


        btnCenterLocation.setOnClickListener((v) -> {
            tryGetLocation();
            routeViewModel.getLocation().observe(getViewLifecycleOwner(), new Observer<Point>() {
                @Override
                public void onChanged(Point loc) {
                    mapView.getMapWindow().getMap().move(
                            new CameraPosition(loc, 17f, 0, 20),
                            new Animation(Animation.Type.SMOOTH, 1),
                            null);
                    routeViewModel.getLocation().removeObserver(this);
                }
            });

        });
        btnAvatar.setOnClickListener(v -> {
            routeViewModel.clearSession();
            Intent mainAct = new Intent(this.getContext(), LoginActivity.class);
            startActivity(mainAct);
        });
        routeViewModel.connectWebSocket(getActivity().getApplicationContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getString("token", ""));

    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = requireActivity();



        executor = Executors.newSingleThreadExecutor();


    }

    @Override
    public void onStop() {
        mapView.onStop();
        super.onStop();
    }

    @Override
    public void onStart() {
        super.onStart();
        mapView.onStart();
        Intent intent = new Intent(getContext(), LocationRecordService.class);
        intent.setAction(LocationRecordService.ACTION_START_IDLE);
        getContext().startService(intent);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (hidden) {
            mapView.onStop();
        } else {
            mapView.onStart();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_map, container, false);
    }
    @Override public void onDestroyView() {
        executor.shutdown();
        super.onDestroyView();
    }




    private void startRecording() {

        // Начать запись
        btnStartRecording.setText(R.string.stop_record);
        btnStartRecording.setIconResource(R.drawable.ic_stop);
        //markStartRoute = mapView.getMapWindow().getMap().getMapObjects().addPlacemark();
        requestBatteryOptimizationIgnore();
        tryGetLocation();
        //когда координаты пользователя найдены запускаем сервис
        routeViewModel.getLocation().observe(getViewLifecycleOwner(), new Observer<Point>() {
            @Override
            public void onChanged(Point point) {
                //if (markStartRoute.isValid()) {
                    //markStartRoute.setGeometry(point);
                    //markStartRoute.setIcon(ImageProvider.fromResource(activity, R.drawable.ic_pin));
                    if (checkPermissions()) {
                        recordingPolyline = mapView.getMapWindow().getMap().getMapObjects().addPolyline();
                        Intent intent = new Intent(getContext(), LocationRecordService.class);
                        intent.setAction(LocationRecordService.ACTION_START_CAPTURE);
                        getContext().startService(intent);
                        isRecording = true;
                    }
                //}
                routeViewModel.getLocation().removeObserver(this);
            }
        });
    }
    private void stopRecordingWithSave(Intent service, String routeName) {
        // Остановить запись
        btnStartRecording.setIconResource(R.drawable.ic_play);
        btnStartRecording.setText(R.string.start_record);
        activity.stopService(service);
        mapView.getMapWindow().getMap().getMapObjects().remove(markStartRoute);
        // Сохранить маршрут
        routeViewModel.saveRoute(routeName);
        mapView.getMapWindow().getMap().getMapObjects().remove(recordingPolyline);
        recordingPolyline = null;
        routeViewModel.clearPoints();
        isRecording = false;
    }
    private void stopRecording() {
        Intent intent = new Intent(getContext(), LocationRecordService.class);
        intent.setAction(LocationRecordService.ACTION_START_IDLE);
        getContext().startService(intent);
        btnStartRecording.setIconResource(R.drawable.ic_play);
        btnStartRecording.setText(R.string.start_record);
        routeViewModel.clearPoints();
        mapView.getMapWindow().getMap().getMapObjects().remove(recordingPolyline);
        recordingPolyline = null;
        isRecording = false;
    }

    private void renderPolygon(PolygonUiModel uiModel) {
        if (!polygonsMapObjects.containsKey(Long.valueOf(uiModel.id))) {
            PolygonMapObject polygonMapObject = mapView.getMapWindow().getMap().getMapObjects().addPolygon(new Polygon(new LinearRing(uiModel.points), new ArrayList<>()));
            int color = getRandomColor(75);
            polygonMapObject.setFillColor(color);
            polygonMapObject.setStrokeColor(ColorUtils.setAlphaComponent(color, 255));
            MapObjectTapListener mapObjectTapListener = new MapObjectTapListener() {
                @Override
                public boolean onMapObjectTap(@NonNull MapObject mapObject, @NonNull Point point) {
                    showPolygonInfo(uiModel);
                    return true;
                }
            };
            polygonMapObject.addTapListener(mapObjectTapListener);
            polygonsMapObjects.put(Long.valueOf(uiModel.id), new PolygonData(polygonMapObject, mapObjectTapListener));
        } else {
            PolygonData polyData = polygonsMapObjects.get(Long.valueOf(uiModel.id));
            if (polyData != null) {
                polyData.obj.setGeometry(new Polygon(new LinearRing(uiModel.points), new ArrayList<>()));
            }
        }
    }

    private void renderingMarkOfPlayer(Long id, String playerName, Point point) {
        if (!playersMarkMapObjects.containsKey(id)) {
            PlacemarkMapObject mark = mapView.getMapWindow().getMap().getMapObjects().addPlacemark();
            mark.setGeometry(point);
            mark.setText(playerName);
            mark.setIcon(ImageProvider.fromResource(activity, R.drawable.ic_pin));
            playersMarkMapObjects.put(id, mark);
        } else {
            PlacemarkMapObject mark = playersMarkMapObjects.get(id);
            if (mark != null) {
                mark.setGeometry(point);
            }
        }

    }

    private void renderingPolylineOfPlayer(Long id, Point point) {
        if (!playersPolylineMapObjects.containsKey(id)) {
            PolylineMapObject line = mapView.getMapWindow().getMap().getMapObjects().addPolyline();
            Polyline geometry = new Polyline(List.of(point));
            line.setGeometry(geometry);
            line.setStrokeColor(getRandomColor(Constants.ALPHA_COLOR_OF_POLYLINE));
            playersPolylineMapObjects.put(id, line);
        } else {
            PolylineMapObject line = playersPolylineMapObjects.get(id);
            List<Point> points = new ArrayList<>(line.getGeometry().getPoints());
            points.add(point);
            Polyline geometry = new Polyline(points);
            line.setGeometry(geometry);
        }
    }
    private void renderingPolylineOfPlayer(Long id, List<Point> points) {
        if (!playersPolylineMapObjects.containsKey(id)) {
            PolylineMapObject line = mapView.getMapWindow().getMap().getMapObjects().addPolyline();
            Polyline geometry = new Polyline(points);
            line.setGeometry(geometry);
            line.setStrokeColor(getRandomColor(Constants.ALPHA_COLOR_OF_POLYLINE));
            playersPolylineMapObjects.put(id, line);
        } else {
            PolylineMapObject line = playersPolylineMapObjects.get(id);
            Polyline geometry = new Polyline(points);
            line.setGeometry(geometry);
        }
    }

    private int getRandomColor(int alpha) {
        Random rnd = new Random();
        int r = rnd.nextInt(256);
        int g = rnd.nextInt(256);
        int b = rnd.nextInt(256);
        return Color.argb(alpha, r, g, b);
    }
    private void showPolygonInfo(PolygonUiModel data) {
        infoArea.setText(String.format(Locale.getDefault(), "Площадь: %.2f м²", data.area));
        infoOwner.setText("Владелец: " + data.ownerLabel);

        dimOverlay.setAlpha(0f);
        dimOverlay.setVisibility(View.VISIBLE);
        dimOverlay.animate().alpha(1f).setDuration(200).start();
    }
    private void hidePolygonInfo() {
        dimOverlay.animate().alpha(0f).setDuration(200).withEndAction(() -> {
            dimOverlay.setVisibility(View.GONE);
        }).start();
    }


    @Override
    public void onObjectAdded(@NonNull UserLocationView userLocationView) {
        userLocationView.getPin().setIcon(
                ImageProvider.fromResource(activity, R.drawable.ic_man)
        );
        userLocationView.getArrow().setIcon(
                ImageProvider.fromResource(activity, R.drawable.ic_arrow),
                new IconStyle()
                        .setRotationType(RotationType.ROTATE)
                        .setAnchor(new PointF(0.5f, 0.5f))
                        .setScale(1.5f)
        );
        Log.d("MAPKIT", "onObjectAdded triggered");
    }

    @Override
    public void onObjectRemoved(@NonNull UserLocationView userLocationView) {}

    @Override
    public void onObjectUpdated(@NonNull UserLocationView userLocationView, @NonNull ObjectEvent objectEvent) {
    }


    private void tryGetLocation() {
        if (!checkPermissions()) {
            requestLocationPermissions();
            return;
        }
        if (!isLocationEnabled()) {
            Toast.makeText(activity, R.string.location_is_disabled, Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            return;
        }
        routeViewModel.requestLocation();
    }



    // CHANGE: Разделили foreground и background разрешения. Для старта отслеживания
    // достаточно foreground. Background будет запрошен отдельным потоком, если нужно.
    private boolean hasForegroundLocation() {
        return ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    // CHANGE: Вынесена отдельная проверка background разрешения
    private boolean hasBackgroundLocation() {
        return ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    // CHANGE: Раньше требовали и BACKGROUND, что блокировало старт сервиса. Теперь достаточно FG
    private boolean checkPermissions() {
        return hasForegroundLocation();
    }

    private void requestLocationPermissions() {
        // CHANGE: Запрашиваем только foreground на первом этапе
        ActivityCompat.requestPermissions(activity, new String[]{
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_ID);
        // CHANGE: Если впоследствии потребуется запись в фоне, запускайте отдельный флоу
        // для получения ACCESS_BACKGROUND_LOCATION (Android 10+ это отдельный шаг)
    }
    private void requestBatteryOptimizationIgnore() {
        PowerManager pm = (PowerManager) activity.getSystemService(Context.POWER_SERVICE);
        if (!pm.isIgnoringBatteryOptimizations(activity.getPackageName())) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + activity.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }


    static class PolygonData {
        final PolygonMapObject obj;
        final MapObjectTapListener listener;

        public PolygonData(PolygonMapObject obj, MapObjectTapListener listener) {
            this.obj = obj;
            this.listener = listener;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;
            PolygonData that = (PolygonData) o;
            return Objects.equals(obj, that.obj) && Objects.equals(listener, that.listener);
        }

        @Override
        public int hashCode() {
            return Objects.hash(obj, listener);
        }
    }
}