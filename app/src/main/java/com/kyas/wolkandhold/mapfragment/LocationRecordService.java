package com.kyas.wolkandhold.mapfragment;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.kyas.wolkandhold.R;

import android.location.Location;
import android.net.Uri;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LifecycleService;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class LocationRecordService extends LifecycleService {

    private FusedLocationProviderClient fusedClient;
    private LocationCallback locationCallback;
    private Location lastLocation;


    @Override
    public void onCreate() {
        super.onCreate();
        fusedClient = LocationServices.getFusedLocationProviderClient(this);
        requestBatteryOptimizationIgnore();
        startForeground(1, createNotification());
        startLocationUpdates();
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
                    if (lastLocation == null || loc.distanceTo(lastLocation) > 2 && loc.getAccuracy() < 25) {

                        lastLocation = loc;
                        BufferedRoute.add(new com.yandex.mapkit.geometry.Point(
                                loc.getLatitude(),
                                loc.getLongitude()
                        ));
                        Intent intent = new Intent("LOCATION_UPDATE");
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
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

    private void requestBatteryOptimizationIgnore() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    @Override
    public void onDestroy() {
        fusedClient.removeLocationUpdates(locationCallback);
        super.onDestroy();
    }
}

