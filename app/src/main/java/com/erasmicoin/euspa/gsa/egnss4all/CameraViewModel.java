package com.erasmicoin.euspa.gsa.egnss4all;

import android.app.Activity;
import android.app.Application;
import android.location.Location;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.erasmicoin.euspa.gsa.egnss4all.model.PersistData;
import com.erasmicoin.euspa.gsa.egnss4all.model.PhotoDataController;
import com.erasmicoin.euspa.gsa.egnss4all.model.ekf.EKFStartExeception;
import com.erasmicoin.euspa.gsa.egnss4all.model.ekf.EkfCreateException;
import com.erasmicoin.euspa.gsa.egnss4all.model.locationManager.InavMessage;
import com.erasmicoin.euspa.gsa.egnss4all.model.locationManager.LMManager;

import java.util.ArrayList;

public class CameraViewModel extends AndroidViewModel {
    public class PositionInfo {

    }

    public static final String TAG = CameraViewModel.class.getSimpleName();

    MainService MS;
    PhotoDataController photoDataController;
    MutableLiveData<Location> currentLocation;

    public CameraViewModel(@NonNull Application application) {
        super(application);
    }

    public void init(MainService MS, Activity activity) throws EkfCreateException, EKFStartExeception {
        this.MS = MS;

        photoDataController = new PhotoDataController(getApplication());
        currentLocation = new MutableLiveData<>();

        photoDataController.setOsnmaEnabled(PersistData.getOSNMAValidated(activity));

        MS.startLocationMonitoring(location -> {
            photoDataController.addLocation(location);
            photoDataController.setCurrentProvider(MS.getProvider());
            currentLocation.postValue(location); //setValue(location);
            if(LMManager.INAV_MESSAGES_GATHERING){
                photoDataController.setInavMessages(MS.getInavMessages());
                ArrayList<InavMessage> inavMessagesTmp = MS.getInavMessages();
                //Keep only the last N messages, N is a constant in LMMANAGER
                if(inavMessagesTmp != null && inavMessagesTmp.size() > 0 ){
                    if(inavMessagesTmp.size() <= LMManager.INAV_MESSAGES_TO_KEEP){
                        photoDataController.setInavMessages(inavMessagesTmp);
                    }else {
                        ArrayList<InavMessage> slicedArray = new ArrayList<>();
                        for(int i = (inavMessagesTmp.size() - 20); i < inavMessagesTmp.size() ; i++){
                            slicedArray.add(inavMessagesTmp.get(i));
                        }
                        photoDataController.setInavMessages(slicedArray);
                    }
                }
            }

        }, activity);
        photoDataController.startImmediately();
    }

    public void snapShot() {
        photoDataController.startSnapShot();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        photoDataController.stop();
    }

    // region get, set
    public PhotoDataController getPhotoDataController() {
        return photoDataController;
    }

    public MutableLiveData<Location> getCurrentLocation() {
        return currentLocation;
    }

    public ArrayList<InavMessage> getInavMessages(){ return MS.getInavMessages(); }

    // endregion
}