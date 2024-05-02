package com.erasmicoin.euspa.gsa.egnss4all.model.convexHullMap;

import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.maps.android.collections.MarkerManager;

public interface CHMapActivity {
    public MarkerManager getMarkerManager();

    public AppCompatActivity getAppCompatActivity();

    public TextView getSampleCountValue();

    public void onNewCentroidCH(CHService.Centroid centroid);
}

