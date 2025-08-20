package com.kyas.wolkandhold.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "polygons")
public class Polygon {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public long userId;
    public String ownerName;

    public String pointsJson;
    public double area;
    public long lastUpdated;
}
