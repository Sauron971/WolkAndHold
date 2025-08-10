package com.kyas.wolkandhold.database.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity
public class Route {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String name;
    public long userId;
    public long createdAt;
    public double distance;
    @Ignore
    public boolean showMenu = false;

    @NonNull
    @Override
    public String toString() {
        return "Route{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
