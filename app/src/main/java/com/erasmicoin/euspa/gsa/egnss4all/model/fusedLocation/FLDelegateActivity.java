package com.erasmicoin.euspa.gsa.egnss4all.model.fusedLocation;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.location.LocationResult;
import com.google.android.gms.maps.GoogleMap;

public interface FLDelegateActivity {
    AppCompatActivity getAppCompatActivity();

    public GoogleMap getGoogleMap();

    void onNewFusedLocations(LocationResult locationResult);

    void onFLStarted();

    void onFLEnded();

    void onFLError();
}


