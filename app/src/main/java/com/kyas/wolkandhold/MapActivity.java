package com.kyas.wolkandhold;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.yandex.mapkit.Animation;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.geometry.Polyline;
import com.yandex.mapkit.layers.ObjectEvent;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.IconStyle;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.map.PolylineMapObject;
import com.yandex.mapkit.map.RotationType;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.mapkit.user_location.UserLocationLayer;
import com.yandex.mapkit.user_location.UserLocationObjectListener;
import com.yandex.mapkit.user_location.UserLocationView;
import com.yandex.runtime.image.ImageProvider;

import java.util.LinkedList;
import java.util.List;

public class MapActivity extends AppCompatActivity implements UserLocationObjectListener {


    private static final int PERMISSION_ID = 44;
    private MapView mapView;
    private String apiKey;
    private double lat, lon;
    FusedLocationProviderClient mFusedLocationClient;
    private FloatingActionButton btnStartRecording;
    private boolean isRecording = false;
    private LocationListener locationListener;
    private LocationManager locationManager;
    private PolylineMapObject recordingPolyline;
    private List<Point> recordedPoints;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        try {
            Context context = getApplicationContext();
            ApplicationInfo appInfo = context
                    .getPackageManager()
                    .getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);

            Bundle metaData = appInfo.metaData;
            if (metaData != null) {
                apiKey = metaData.getString("com.yandex.maps.apikey");
                Log.d("MetaData", "API KEY: " + apiKey);
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("Error", "onCreate: ", e);
        }
        MapKitFactory.setApiKey(apiKey);
        MapKitFactory.initialize(this);
        setContentView(R.layout.activity_map);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        getLastLocation();
        mapView = findViewById(R.id.mapview);
        btnStartRecording = findViewById(R.id.btn_start_recording);
        recordedPoints = new LinkedList<>();

        UserLocationLayer ull = MapKitFactory.getInstance().createUserLocationLayer(mapView.getMapWindow());
        ull.setObjectListener(this);
        ull.setVisible(true);
        ull.setHeadingEnabled(true);



        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        locationListener = location -> {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            Point p = new Point(latitude, longitude);
            recordedPoints.add(p);
            if (recordingPolyline != null)
                recordingPolyline.setGeometry(new Polyline(recordedPoints));
            mapView.getMapWindow().getMap().move(
                    new CameraPosition(p, 16f, 0, 10),
                    new Animation(Animation.Type.SMOOTH, 1),
                    null
            );
        };

        btnStartRecording.setOnClickListener(v -> {
            if (isRecording) {
                // Остановить запись
                btnStartRecording.setImageResource(R.drawable.ic_play);
                locationManager.removeUpdates(locationListener);
                PlacemarkMapObject mark = mapView.getMapWindow().getMap().getMapObjects().addPlacemark();
                mark.setGeometry(recordedPoints.getLast());
                mark.setIcon(ImageProvider.fromResource(this, R.drawable.ic_pin));
                isRecording = false;
            } else {
                // Начать запись
                btnStartRecording.setImageResource(R.drawable.ic_stop);
                getLastLocation();
                PlacemarkMapObject mark = mapView.getMapWindow().getMap().getMapObjects().addPlacemark();
                mark.setGeometry(new Point(lat, lon));
                mark.setIcon(ImageProvider.fromResource(this, R.drawable.ic_pin));
                if (checkPermissions()) {
                    recordedPoints.clear();
                    recordingPolyline = mapView.getMapWindow().getMap().getMapObjects().addPolyline();
                    locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            1000,
                            1,
                            locationListener
                    );
                    isRecording = true;
                }
            }
        });
    }

    @Override
    protected void onStop() {
        mapView.onStop();
        MapKitFactory.getInstance().onStop();
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
        MapKitFactory.getInstance().onStart();
    }


    @SuppressLint("MissingPermission")
    private void getLastLocation() {
        if (checkPermissions()) {

            if (isLocationEnabled()) {

                mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        Location location = task.getResult();
                        if (location == null) {
                            requestNewLocationData();
                        } else {
                            lat = location.getLatitude();
                            lon = location.getLongitude();
                            Log.d("GPS", "latitude = " + lat);
                            Log.d("GPS", "longitude = " + lon);
                            mapView.getMapWindow().getMap().move(new CameraPosition(
                                    new Point(lat, lon),
                                    17.0f,
                                    150.0f,
                                    30.0f));
                        }
                    }
                });
            } else {
                Toast.makeText(this, "Please turn on" + " your location...", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        } else {
            requestPermissions();
        }
    }

    @SuppressLint("MissingPermission")
    private void requestNewLocationData() {
        LocationRequest mLocationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(0)
                .setMaxUpdates(1)
                .build();

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

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
            lat = mLastLocation.getLatitude();
            lon = mLastLocation.getLongitude();
        }
    };

    // method to check for permissions
    private boolean checkPermissions() {

        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        // If we want background location
        // on Android 10.0 and higher,
        // use:
        // ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    // method to request for permissions
    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_ID);
    }

    // method to check
    // if location is enabled
    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }



    @Override
    public void onObjectAdded(@NonNull UserLocationView userLocationView) {
        userLocationView.getPin().setIcon(
                ImageProvider.fromResource(MapActivity.this, R.drawable.ic_man)
        );
        userLocationView.getArrow().setIcon(
                ImageProvider.fromResource(MapActivity.this, R.drawable.ic_arrow),
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
}