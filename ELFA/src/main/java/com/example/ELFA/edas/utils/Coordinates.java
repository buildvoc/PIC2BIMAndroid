package com.example.ELFA.edas.utils;

import android.annotation.SuppressLint;
import android.os.Build;

import androidx.annotation.RequiresApi;

import java.util.Objects;

public class Coordinates {
    private int longitude;
    private int latitude;

    public Coordinates(int longitude, int latitude) {
        this.longitude = longitude;
        this.latitude = latitude;
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Coordinates that = (Coordinates) o;
        return longitude == that.longitude &&
                latitude == that.latitude;
    }

    @SuppressLint("NewApi")
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public int hashCode() {
        return Objects.hash(longitude, latitude);
    }

    public int getLongitude(){
        return longitude;
    }

    public int getLatitude(){
        return latitude;
    }


}
