package com.kyas.wolkandhold.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class Polygon {
    @PrimaryKey
    public long userId;

    public String pointsJson;
    public double area;
    public long lastUpdated;
}
