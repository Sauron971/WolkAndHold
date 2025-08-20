package com.kyas.wolkandhold.api.response;

import com.kyas.wolkandhold.database.entities.Polygon;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class PolygonResponse {
    private long id;
    private UserResponse owner;
    private double square;
    private long lastUpdated;
    private String wkt;

    public Polygon toEntity() {
        String coordsPart = wkt.substring(10, wkt.length()-3);
         List<String> stringList = Arrays.stream(coordsPart.split(","))
                .map((it) -> it.trim().split(" "))
                .map((loc) ->
                        String.format(Locale.getDefault(), "{ \"latitude\": %s, \"longitude\": %s } ", loc[1], loc[0])).collect(Collectors.toList());

        String json = "[\n    "
                + String.join(",\n    ", stringList)
                + "\n]";
        Polygon result = new Polygon();
        result.userId = this.owner.getUserId();
        result.ownerName = this.owner.getUsername();
        result.area = this.square;
        result.lastUpdated = this.lastUpdated;
        result.pointsJson = json;
        return result;
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
