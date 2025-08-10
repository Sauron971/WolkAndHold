package com.kyas.wolkandhold.mapfragment;


import android.location.Location;

import com.yandex.mapkit.geometry.Point;

import java.util.LinkedList;
import java.util.List;

public class BufferedRoute {
    private static final List<Point> points = new LinkedList<>();
    public static void add(Point point) {
        points.add(point);
    }
    public static List<Point> getAll() {
        return new LinkedList<>(points);
    }
    public static double getDistance() {
        float[] distance = new float[1];
        double result = 0.0;
        for (int i = 0; i < points.size()-2; i++) {
            Location.distanceBetween(
                    points.get(i).getLatitude(), points.get(i).getLongitude(),
                    points.get(i+1).getLatitude(), points.get(i+1).getLongitude(), distance);
            result += distance[0];
        }
        return result;
    }
    public static void clear() {
        points.clear();
    }
}
