package com.kyas.wolkandhold.ui.mapfragment;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.kyas.wolkandhold.data.DataRepository;
import com.kyas.wolkandhold.R;
import com.yandex.mapkit.geometry.Point;

import android.location.Location;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LifecycleService;

public class LocationRecordService extends LifecycleService {

    public static final String ACTION_START_IDLE = "ACTION_START_IDLE";
    public static final String ACTION_START_CAPTURE = "ACTION_START_CAPTURE";
    public static final String ACTION_STOP = "ACTION_STOP";

    private boolean isCaptureMode = false;
    private FusedLocationProviderClient fusedClient;
    private LocationCallback locationCallback;
    private Location lastLocation;
    private DataRepository repo;


    @Override
    public void onCreate() {
        super.onCreate();
        repo = DataRepository.getInstance(getApplicationContext());
        fusedClient = LocationServices.getFusedLocationProviderClient(this);
        Log.d("LocService", "Service start");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (intent != null && intent.getAction() != null ) {
            String action = intent.getAction();
            switch (action) {
                case ACTION_START_IDLE:
                    startLocationUpdates();
                    isCaptureMode = false;
                    repo.clearPoints();
                    break;

                case ACTION_START_CAPTURE:
                    upgradeToForeground();
                    break;
                case ACTION_STOP:
                    stopSelf();
                    break;
            }
        }

        return START_STICKY;
    }

    private void startLocationUpdates() {
        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
                .setMinUpdateIntervalMillis(2000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                Location loc = result.getLastLocation();
                if (loc != null) {
                    boolean movedEnough = lastLocation == null || loc.distanceTo(lastLocation) > 2.0f;
                    boolean accurateEnough = loc.hasAccuracy() && loc.getAccuracy() <= 50.0f;
                    if (movedEnough && accurateEnough) {
                        lastLocation = loc;
                        repo.emitLocation(loc.getLatitude(), loc.getLongitude(), isCaptureMode);
                        if (isCaptureMode) {
                            repo.addPoint(new Point(loc.getLatitude(), loc.getLongitude()));
                        }
                        Log.d("LocService", "Point send");
                    }
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            stopSelf();
            return;
        }

        fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
    }

    private void upgradeToForeground() {
        isCaptureMode = true;
        Notification notification = createNotification();
        startForeground(1, notification);
        Log.d("LocService", "Service upgraded to foreground");
    }

    private Notification createNotification() {
        NotificationChannel channel = new NotificationChannel(
                "location_channel",
                "GPS Tracking",
                NotificationManager.IMPORTANCE_LOW
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);

        return new NotificationCompat.Builder(this, "location_channel")
                .setContentTitle("Запись маршрута")
                .setContentText("Идёт отслеживание GPS")
                .setSmallIcon(R.drawable.ic_pin)
                .setOngoing(true)
                .build();
    }



    @Override
    public void onDestroy() {
        fusedClient.removeLocationUpdates(locationCallback);
        super.onDestroy();
    }
}

