package com.erasmicoin.euspa.gsa.egnss4all.model.locationManager;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.location.LocationResult;
import com.google.android.gms.maps.GoogleMap;

public interface LMDelegateActivity {
    AppCompatActivity getAppCompatActivity();

    public GoogleMap getGoogleMap();

    void onNewLocation(LocationResult locationResult);

    void onLMStarted();

    void onLMEnded();

    void onLMError();
}


