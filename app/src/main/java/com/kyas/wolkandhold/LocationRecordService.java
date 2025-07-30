package com.kyas.wolkandhold;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.yandex.mapkit.Animation;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.geometry.Polyline;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.map.PolylineMapObject;
import com.yandex.runtime.image.ImageProvider;

import java.security.Provider;
import java.util.List;

public class LocationRecordService extends Service {


    private boolean isRecording = false;
    private LocationListener locationListener;
    private LocationManager locationManager;
    private List<Point> recordedPoints;
    private PolylineMapObject recordingPolyline;

    @Override
    public void onCreate() {
        super.onCreate();
//        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//
//        locationListener = location -> {
//            double latitude = location.getLatitude();
//            double longitude = location.getLongitude();
//            Point p = new Point(latitude, longitude);
//            recordedPoints.add(p);
//            if (recordingPolyline != null)
//                recordingPolyline.setGeometry(new Polyline(recordedPoints));
//            mapView.getMapWindow().getMap().move(
//                    new CameraPosition(p, 16f, 0, 10),
//                    new Animation(Animation.Type.SMOOTH, 1),
//                    null
//            );
//        };
//
//        if (isRecording) {
//            btnStartRecording.setImageResource(R.drawable.ic_play);
//            locationManager.removeUpdates(locationListener);
//            PlacemarkMapObject mark = mapView.getMapWindow().getMap().getMapObjects().addPlacemark();
//            mark.setGeometry(recordedPoints.getLast());
//            mark.setIcon(ImageProvider.fromResource(this, R.drawable.ic_pin));
//            isRecording = false;
//        } else {
//            btnStartRecording.setImageResource(R.drawable.ic_stop);
//            getLastLocation();
//            PlacemarkMapObject mark = mapView.getMapWindow().getMap().getMapObjects().addPlacemark();
//            mark.setGeometry(new Point(lat, lon));
//            mark.setIcon(ImageProvider.fromResource(this, R.drawable.ic_pin));
//            if (checkPermissions()) {
//                recordedPoints.clear();
//                recordingPolyline = mapView.getMapWindow().getMap().getMapObjects().addPolyline();
//                locationManager.requestLocationUpdates(
//                        LocationManager.GPS_PROVIDER,
//                        1000,
//                        1,
//                        locationListener
//                );
//                isRecording = true;
//            }
//        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
