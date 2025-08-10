package com.kyas.wolkandhold.database.entities;

import static androidx.room.ForeignKey.CASCADE;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;


@Entity(foreignKeys = @ForeignKey(
        entity = Route.class,
        parentColumns = "id",
        childColumns = "routeId",
        onDelete = CASCADE
))
public class RoutePoint {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long routeId;

    public double latitude;
    public double longitude;

    @Override
    public String toString() {
        return "RoutePoint{" +
                "id=" + id +
                ", routeId=" + routeId +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }
}
