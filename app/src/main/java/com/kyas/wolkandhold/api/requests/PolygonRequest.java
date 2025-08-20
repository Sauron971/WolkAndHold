package com.kyas.wolkandhold.api.requests;

import com.yandex.mapkit.geometry.Point;

import java.util.List;

public class PolygonRequest {
    private long userId;
    private String points;
    private double area_m2;
    private long lastUpdated;

    public PolygonRequest(long userId, String points, double area_m2, long lastUpdated) {
        this.userId = userId;
        this.points = points;
        this.area_m2 = area_m2;
        this.lastUpdated = lastUpdated;
    }
}
