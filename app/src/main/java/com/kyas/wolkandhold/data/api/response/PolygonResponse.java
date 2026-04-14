package com.kyas.wolkandhold.data.api.response;

import android.util.Log;

import com.google.gson.Gson;
import com.kyas.wolkandhold.data.database.entities.Polygon;
import com.yandex.mapkit.geometry.Point;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PolygonResponse {
    private long id;
    private UserResponse owner;
    private double square;
    private long lastUpdated;
    private String wkt;

    public List<Polygon> toEntities() {
        // 1) Нормализация
        String src = wkt == null ? "" : wkt.trim();
        if (src.isEmpty()) return Collections.emptyList();
        // 2) Убираем префикс геометрии, но не режем строку "по индексам"
        // Поддержка: POLYGON(...) и MULTIPOLYGON(...)
        String body;
        if (src.startsWith("MULTIPOLYGON")) {
            body = src.substring("MULTIPOLYGON".length()).trim();
        } else if (src.startsWith("POLYGON")) {
            body = src.substring("POLYGON".length()).trim();
        } else {
            // неизвестный формат
            return Collections.emptyList();
        }
        // 3) Удаляем все скобки, чтобы гарантированно не осталось ")" в числах
        String flat = body.replace("(", " ").replace(")", " ").trim();
        // 4) Делим на пары координат "lon lat" по запятым
        // пример после flatten: "37.61 55.75, 37.62 55.76, ..."
        String[] pairs = flat.split(",");
        List<Point> points = new ArrayList<>();
        for (String pairRaw : pairs) {
            String pair = pairRaw.trim();
            if (pair.isEmpty()) continue;
            // Важно: split по любому количеству пробелов
            String[] parts = pair.split("\\s+");
            if (parts.length < 2) continue;
            // WKT обычно: lon lat
            String lonStr = parts[0].trim();
            String latStr = parts[1].trim();
            try {
                double lon = Double.parseDouble(lonStr);
                double lat = Double.parseDouble(latStr);
                points.add(new Point(lat, lon)); // yandex Point(lat, lon)
            } catch (NumberFormatException ignoreBadPair) {
                // лог + пропуск кривой пары
                Log.e("PolygonResponse", "Response not parse, coordinates is wrong");
            }
        }
        if (points.isEmpty()) return Collections.emptyList();
        // 5) Если полигон "замкнут" (последняя точка = первая), можно убрать дубль
        if (points.size() > 1 && isSame(points.get(0), points.get(points.size() - 1))) {
            points.remove(points.size() - 1);
        }
        // 6) Сериализуем points в JSON безопасно через Gson
        String pointsJson = new Gson().toJson(points);
        Polygon poly = new Polygon();
        poly.id = this.id;
        poly.userId = this.owner.getUserId();
        poly.ownerName = this.owner.getUsername();
        poly.area = this.square;
        poly.lastUpdated = this.lastUpdated;
        poly.pointsJson = pointsJson;
        return List.of(poly); // если нужен один контур
    }
    // с эпсилоном, чтобы не страдать от погрешностей double
    private boolean isSame(Point a, Point b) {
        double eps = 1e-9;
        return Math.abs(a.getLatitude() - b.getLatitude()) < eps
                && Math.abs(a.getLongitude() - b.getLongitude()) < eps;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public UserResponse getOwner() {
        return owner;
    }

    public void setOwner(UserResponse owner) {
        this.owner = owner;
    }

    public double getSquare() {
        return square;
    }

    public void setSquare(double square) {
        this.square = square;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getWkt() {
        return wkt;
    }

    public void setWkt(String wkt) {
        this.wkt = wkt;
    }

    @Override
    public String toString() {
        return "PolygonResponse{" +
                "id=" + id +
                ", userId=" + owner +
                ", square=" + square +
                ", lastUpdated=" + lastUpdated +
                ", wkt='" + wkt + '\'' +
                '}';
    }
}
