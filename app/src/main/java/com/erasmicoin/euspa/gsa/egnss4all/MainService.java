package com.erasmicoin.euspa.gsa.egnss4all;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.GnssNavigationMessage;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.erasmicoin.euspa.gsa.egnss4all.model.AppDatabase;
import com.erasmicoin.euspa.gsa.egnss4all.model.GNSSLocation.GNSSManager;
import com.erasmicoin.euspa.gsa.egnss4all.model.GNSSLocation.GNSSSettingsStore;
import com.erasmicoin.euspa.gsa.egnss4all.model.LoggedUser;
import com.erasmicoin.euspa.gsa.egnss4all.model.OSNMA.ServerPostPacketTask;
import com.erasmicoin.euspa.gsa.egnss4all.model.OSNMA.ServerPostResponse;
import com.erasmicoin.euspa.gsa.egnss4all.model.PersistData;
import com.erasmicoin.euspa.gsa.egnss4all.model.Photo;
import com.erasmicoin.euspa.gsa.egnss4all.model.PhotoList;
import com.erasmicoin.euspa.gsa.egnss4all.model.Requestor;
import com.erasmicoin.euspa.gsa.egnss4all.model.SyncQueue;
import com.erasmicoin.euspa.gsa.egnss4all.model.Task;
import com.erasmicoin.euspa.gsa.egnss4all.model.TaskList;
import com.erasmicoin.euspa.gsa.egnss4all.model.Util;
import com.erasmicoin.euspa.gsa.egnss4all.model.extbluetooth.BluetoothManager;
import com.erasmicoin.euspa.gsa.egnss4all.model.locationManager.InavMessage;
import com.erasmicoin.euspa.gsa.egnss4all.model.pathTrack.PTPath;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;

public class MainService extends Service implements GNSSManager.GNSSLocationCallback, BluetoothManager.BluetoothLocationCallback {


    public static final String OFFLINE_LM_PROVIDER = "offlineLM";
    public static final String ONLINE_LM_PROVIDER = "onlineLM";
    public static final String FUSED_PROVIDER = "fused";
    public static final String GNSS_PROVIDER = "GNSSProvider";
    public static final String EXTERNAL_PROVIDER = "BTGPSProvider";

    public interface LocationReceiver {
        public void receive(Location location);
    }



    public enum BROADCAST_MSG {
        BROADCAST_ID("mainServiceBroadcast"),
        TYPE("brodcastId"),

        /* excplicit */
        EXPLICIT("EXPLICIT"),
        /**/

        /* implicit */
        STARTED("started"),
        REFRESH_TASKS_STARTED("refreshTasksStarted"),
        REFRESH_TASKS_FINISHED("refreshTasks"),
        UPLOAD_TASK_STATUS("uploadTaskStatus"),
        REFRESH_PHOTOS("refreshPhoto"),
        SYNC_STARTED("SYNC_STARTED"),
        SYNC_FINISHED("SYNC_FINISHED"),
        SYNC_PROGRESS("SYNC_PROGRESS"),
        KEEP_SCREEN_ON("KEEP_SCREEN_ON");
        /**/

        public static BROADCAST_MSG createFromID(String id) {
            for (BROADCAST_MSG broadcast_msg : BROADCAST_MSG.values()) {
                if (broadcast_msg.ID.equals(id)) {
                    return broadcast_msg;
                }
            }
            return null;
        }

        public final String ID;

        BROADCAST_MSG(String id) {
            ID = id;
        }
    }

    public enum BROADCAST_EXPLICIT_PARAMS {
        CLASS_NAME("CLASS_NAME"),
        ACTION("ACTION");

        public final String ID;

        BROADCAST_EXPLICIT_PARAMS(String id) {
            ID = id;
        }
    }

    public enum BROADCAST_REFRESH_TASKS_PARAMS {
        SUCCESS("success"),
        ERROR_MSG("errMsg");

        public final String ID;

        BROADCAST_REFRESH_TASKS_PARAMS(String id) {
            ID = id;
        }
    }

    public enum BROADCAST_UPLOAD_TASK_STATUS_PARAMS {
        SUCCESS("success"),
        ERROR_MSG("errMsg");

        public final String ID;

        BROADCAST_UPLOAD_TASK_STATUS_PARAMS(String id) {
            ID = id;
        }
    }

    public enum BROADCAST_REFRESH_PHOTOS_PARAMS {
        SUCCESS("success"),
        ERROR_MSG("errMsg");

        public final String ID;

        BROADCAST_REFRESH_PHOTOS_PARAMS(String id) {
            ID = id;
        }
    }

    public enum BROADCAST_SYNC_PROGRESS_PARAMS {
        PROGRESS("PROGRESS");

        public final String ID;

        BROADCAST_SYNC_PROGRESS_PARAMS(String id) {
            ID = id;
        }
    }

    public enum BROADCAST_KEEP_SCREEN_ON_PARAMS {
        IS_ACTIVE("IS_ACTIVE");

        public final String ID;

        BROADCAST_KEEP_SCREEN_ON_PARAMS(String id) {
            ID = id;
        }
    }

    private class SyncNotifier {
        private int maxUnit;
        private int processedUnit = 0;
        private int progress = 0;

        SyncNotifier(int maxUnit) {
            this.maxUnit = maxUnit;
        }

        void addUnits(int unitCount) {
            processedUnit += unitCount;

            if (maxUnit != 0) {
                progress = 100 * processedUnit / maxUnit;
            } else {
                progress = 100;
            }

            notifyProgress();
        }

        void notifyProgress() {
            syncNotifyProgress();
        }

    }

    public static final String TAG = MainService.class.getSimpleName();

    private String provider;

    public static final boolean IS_FOREGROUND = false;
    private static final int LOCATION_REQUEST_MILS = 100;
    private static final int LOCATION_REQUEST_MILS_FASTEST = 100;
    public static final String CHANNEL_ID = "EGNSS4ALLChannel";
    public static final int FOREGROUND_SERVICE_ID = 1;
    private static final int MODE_START = START_STICKY;
    private static final int TASK_UPDATER_INTERVALS_MILS = 3600000; // 1 hod

    private Class mainClass = MainActivity.class;
    private Notification notification;
    private boolean isRunning = false;
    private AppDatabase appDatabase;
    private Requestor requestor;
    private boolean syncSuccess = true;
    private List<String> lastSyncErrors = new ArrayList<>();
    private Boolean isSync = false;
    private Phaser syncPhaser;
    private SyncNotifier taskSyncNotifier;
    private SyncNotifier pathSyncNotifier;

    private Handler tasksUpdaterHandler;
    private Runnable tasksUpdaterRunnable;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private Location currentLocation;

    private boolean isKeepScreenOnActive = false;

    // determines the global path / path recording status
    private boolean isPathsUploading = false;

    public class LocalBinder extends Binder {
        public MainService getService() {
            return MainService.this;
        }
    }

    public final IBinder binder = new LocalBinder();

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();
        initAppDatabase();
        initRequestor();
        // initTasksUpdator();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(CHANNEL_ID, getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private void initAppDatabase() {
        appDatabase = AppDatabase.build(getApplicationContext());
    }

    private void initRequestor() {
        requestor = new Requestor(this);
    }

    private void initTasksUpdator() {
        tasksUpdaterHandler = new Handler();
        tasksUpdaterRunnable = new Runnable() {
            @Override
            public void run() {
                syncAll();
                tasksUpdaterHandler.postDelayed(this, TASK_UPDATER_INTERVALS_MILS);
            }
        };
        tasksUpdaterHandler.postDelayed(tasksUpdaterRunnable, TASK_UPDATER_INTERVALS_MILS);
    }

    private void finishTaskSync() {
        Log.d("SYNC", "tasks finished");
        syncPhaser.arriveAndDeregister();
    }

    private void finishPathSync() {
        Log.d("SYNC", "paths finished");
        syncPhaser.arriveAndDeregister();
    }

    public void syncAll() {
        if (!LoggedUser.isLogged(appDatabase)) {
            return;
        }
        synchronized (isSync) {
            if (isSync) {
                return;
            }
            isSync = true;
            sendBroadcastMessage(createBroadcastImplicitIntent(BROADCAST_MSG.SYNC_STARTED));
        }
        syncErrorClear();

        pathSyncNotifier = null;
        taskSyncNotifier = null;
        syncNotifyProgress();

        syncPhaser = new Phaser(2);
        syncPaths();
        syncTasks();

        Handler joiner = new Handler(Looper.getMainLooper());
        Executors.newSingleThreadExecutor().submit(() -> {
            syncPhaser.awaitAdvance(syncPhaser.arriveAndDeregister());
            joiner.post(() -> {
                synchronized (isSync) {
                    isSync = false;
                    sendBroadcastMessage(createBroadcastImplicitIntent(BROADCAST_MSG.SYNC_FINISHED));
                    Log.d("SYNC", "finished all");
                }
            });
        });
    }

    public void syncTasks() {
        sendBroadcastMessage(createBroadcastImplicitIntent(BROADCAST_MSG.REFRESH_TASKS_STARTED));
        TaskList currentTaskList = TaskList.createFromAppDatabase(appDatabase);
        final SyncQueue syncQ = new SyncQueue("syncTasks");
        Task.UpdateTaskReceiver updateTaskReceiver = new Task.UpdateTaskReceiver() {

            @Override
            public void success(AppDatabase appDatabase, Task task) {
                sendBroadcastMessage(createBroadcastImplicitIntent(BROADCAST_MSG.UPLOAD_TASK_STATUS)
                        .putExtra(BROADCAST_UPLOAD_TASK_STATUS_PARAMS.SUCCESS.ID, true));
            }

            @Override
            protected void success(AppDatabase appDatabase) {
                // not used
            }

            @Override
            public void failed(String error) {
                syncErrorAdd("SyncTasks failed\n" + error);
                sendBroadcastMessage(createBroadcastImplicitIntent(BROADCAST_MSG.UPLOAD_TASK_STATUS)
                        .putExtra(BROADCAST_UPLOAD_TASK_STATUS_PARAMS.SUCCESS.ID, false)
                        .putExtra(BROADCAST_UPLOAD_TASK_STATUS_PARAMS.ERROR_MSG.ID, error));
            }

            @Override
            public void finish(boolean success) {
            }
        };
        updateTaskReceiver.setSyncQueue(syncQ);
        currentTaskList.uploadStatus(appDatabase, this, updateTaskReceiver, requestor);

        syncQ.addAsyncExecutor(new SyncQueue.AsyncExecutor("loadTaskExecutor") {
            @Override
            protected void run() {
                if (syncSuccess) {
                    loadTasks(syncQ);
                } else {
                    finishTaskSync();
                    syncQ.executionFinish();
                }
            }
        });
        syncQ.executeQueue();
    }

    private void loadTasks(final SyncQueue syncQueue) {
        String currentServer = GNSSSettingsStore.readCurrentServer(this);
        requestor.requestAuth(currentServer+"/egnss4allservices/comm_tasks.php", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    String status = jsonObject.getString("status").trim();
                    if (!status.equals("ok")) {
                        String errMsg = jsonObject.getString("error_msg");
                        syncErrorAdd(errMsg);
                        sendBroadcastMessage(createBroadcastImplicitIntent(BROADCAST_MSG.REFRESH_TASKS_FINISHED).putExtra(BROADCAST_REFRESH_TASKS_PARAMS.SUCCESS.ID, false)
                                .putExtra(BROADCAST_REFRESH_TASKS_PARAMS.ERROR_MSG.ID, errMsg));
                    }
                    final TaskList taskList = TaskList.createFromJSONArray(appDatabase, jsonObject.getJSONArray("tasks"), LoggedUser.createFromAppDatabase(appDatabase).getId());
                    taskList.refreshToDB();
                    String userId = LoggedUser.createFromAppDatabase(appDatabase).getId();

                    syncQueue.addAsyncExecutor(new SyncQueue.AsyncExecutor() {
                        @Override
                        protected void run() {
                            try {
                                Task virtualTask = Task.createFromAppDatabaseSpecialUnownedPhotos(appDatabase, userId);
                                virtualTask.downloadUnassignedPhotoRealIds(appDatabase, MainService.this, requestor, new Task.UpdateTaskReceiver(syncQueue) {
                                    @Override
                                    protected void success(AppDatabase appDatabase, Task task) {

                                    }

                                    @Override
                                    protected void success(AppDatabase appDatabase) {

                                    }

                                    @Override
                                    protected void failed(String error) {
                                        syncErrorAdd(error);
                                        addEndChainTaskSync(syncQueue, error);
                                    }

                                    @Override
                                    protected void finish(boolean success) {

                                    }
                                });
                                taskList.addTask(virtualTask);
                            } catch (JSONException e) {
                                syncErrorAdd(e.getMessage());
                            }
                        }
                    });

                    syncQueue.addAsyncExecutor(new SyncQueue.AsyncExecutor() {
                        @Override
                        protected void run() {
                            taskList.prepareRealIdsForProcessing(appDatabase);
                            taskSyncNotifier = new SyncNotifier(taskList.getCountToUpdatePhotoRealId());
                            taskSyncNotifier.notifyProgress();

                            taskList.updatePhotosByRealIds(MainService.this, requestor, new Photo.UpdatePhotoReceiver() {
                                @Override
                                protected void success(AppDatabase appDatabase, Photo photo) {
                                    taskSyncNotifier.addUnits(1);
                                }

                                @Override
                                protected void success(AppDatabase appDatabase) {

                                }

                                @Override
                                protected void failed(String error) {
                                    syncErrorAdd(error);
                                }

                                @Override
                                protected void finish(boolean success) {

                                }
                            }, new Task.UpdateTaskReceiver() {
                                @Override
                                protected void success(AppDatabase appDatabase, Task task) {
                                    PhotoList photoList = PhotoList.createFromAppDatabaseByTaskGroup(appDatabase, task.getId(), MainService.this);
                                    photoList.deleteDuplicatePhotosInDatabaseByDigest();
                                    photoList.unlockAllImmediately();
                                }

                                @Override
                                protected void success(AppDatabase appDatabase) {

                                }

                                @Override
                                protected void failed(String error) {
                                    syncErrorAdd(error);
                                }

                                @Override
                                protected void finish(boolean success) {

                                }
                            }, new Task.UpdateTaskReceiver(syncQueue) {
                                @Override
                                protected void success(AppDatabase appDatabase, Task task) {
                                    sendBroadcastMessage(createBroadcastImplicitIntent(BROADCAST_MSG.REFRESH_PHOTOS)
                                            .putExtra(BROADCAST_REFRESH_PHOTOS_PARAMS.SUCCESS.ID, true));
                                }

                                @Override
                                protected void success(AppDatabase appDatabase) {

                                }

                                @Override
                                protected void failed(String error) {
                                    syncErrorAdd(error);
                                    sendBroadcastMessage(createBroadcastImplicitIntent(BROADCAST_MSG.REFRESH_PHOTOS)
                                            .putExtra(BROADCAST_REFRESH_PHOTOS_PARAMS.SUCCESS.ID, false)
                                            .putExtra(BROADCAST_REFRESH_PHOTOS_PARAMS.ERROR_MSG.ID, error));
                                }

                                @Override
                                protected void finish(boolean success) {
                                    Log.d("DEBUG", "FINISH");
                                }
                            });
                        }
                    });

                    addEndChainTaskSync(syncQueue, null);

                } catch (JSONException e) {
                    syncErrorAdd(e.getMessage());
                    addEndChainTaskSync(syncQueue, e.getMessage());
                } finally {
                    syncQueue.executionFinish();
                }
            }
        }, error -> {
            syncErrorAdd(error.getMessage());
            sendBroadcastMessage(createBroadcastImplicitIntent(BROADCAST_MSG.REFRESH_TASKS_FINISHED).putExtra(BROADCAST_REFRESH_TASKS_PARAMS.SUCCESS.ID, false)
                    .putExtra(BROADCAST_REFRESH_TASKS_PARAMS.ERROR_MSG.ID, error.getMessage()));
            finishTaskSync();
            syncQueue.executionFinish();
        }, new Requestor.Req() {
            @Override
            public Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", LoggedUser.createFromAppDatabase(appDatabase).getId());
                return params;
            }
        });
    }

    private void addEndChainTaskSync(SyncQueue syncQueue, String endErrMsg) {
        syncQueue.addAsyncExecutor(new SyncQueue.AsyncExecutor() {
            @Override
            protected void run() {
                if (syncSuccess) {
                    sendBroadcastMessage(createBroadcastImplicitIntent(BROADCAST_MSG.REFRESH_TASKS_FINISHED).putExtra(BROADCAST_REFRESH_TASKS_PARAMS.SUCCESS.ID, true));
                } else {
                    String errMsg = endErrMsg;
                    if (errMsg == null && lastSyncErrors.size() > 0) {
                        errMsg = lastSyncErrors.get(lastSyncErrors.size() - 1);
                    }
                    sendBroadcastMessage(createBroadcastImplicitIntent(BROADCAST_MSG.REFRESH_TASKS_FINISHED).putExtra(BROADCAST_REFRESH_TASKS_PARAMS.SUCCESS.ID, false)
                            .putExtra(BROADCAST_REFRESH_TASKS_PARAMS.ERROR_MSG.ID, errMsg));
                }
                finishTaskSync();
                syncQueue.executionFinishTotal();
            }
        });
    }

    private void syncPaths() {
        if (PTPath.getUploadingOpenedCount(appDatabase) > 0) {
            syncErrorAdd(getString(R.string.pt_downloadRestrictedError));
            finishPathSync();
        } else {
            downloadPaths();
        }
    }

    private void downloadPaths() {
        pathSyncNotifier = new SyncNotifier(1);
        pathSyncNotifier.notifyProgress();
        String currentServer = GNSSSettingsStore.readCurrentServer(this);
        requestor.requestAuth(currentServer+"/egnss4allservices/comm_get_paths.php", new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    String status = jsonObject.getString("status").trim();
                    if (!status.equals("ok")) {
                        String errMsg = jsonObject.getString("error_msg");
                        syncErrorAdd("downloadPaths failed, bad status\n" + errMsg);
                        return;
                    }
                    JSONArray jsonPaths = jsonObject.getJSONArray("paths");
                    List<PTPath> paths = new ArrayList<>();
                    for (int i = 0; i < jsonPaths.length(); ++i) {
                        PTPath.createFromJSON(appDatabase, jsonPaths.getJSONObject(i)).saveToDB(appDatabase);
                    }

                    /* auto upload - deprecated */
                    // uploadPaths();
                    pathSyncNotifier.addUnits(1);
                    finishPathSync();
                    /**/
                } catch (JSONException jsonException) {
                    syncErrorAdd("downloadPaths failed, json error\n" + jsonException.getMessage());
                    finishPathSync();
                } finally {
                    Log.d("SYNC PATH DOWNLOAD", response);
                    Log.d("SYNC", "paths finished");
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                syncErrorAdd("downloadPaths failed, network error\n" + Util.volleyErrorMsg(error));
                finishPathSync();
            }
        }, new Requestor.Req() {
            @Override
            public Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("user_id", LoggedUser.createFromAppDatabase(appDatabase).getId());
                return params;
            }
        });
    }

    @Deprecated
    private void uploadPaths() {
        List<PTPath> unsentPaths = PTPath.createListFromAppDatabaseUnsent(appDatabase);
        Phaser phaser = new Phaser(unsentPaths.size() + 1);
        PTPath.UploadReceiver uploadReceiver = new PTPath.UploadReceiver() {
            @Override
            protected void success() {
                // nothing
            }

            @Override
            protected void failed(String errMsg) {
                syncErrorAdd(errMsg);
            }

            @Override
            protected void complete() {
                phaser.arriveAndDeregister();
            }
        };
        for (PTPath ptPath : unsentPaths) {
            ptPath.upload(appDatabase, requestor, uploadReceiver, this);
        }

        Handler joiner = new Handler(Looper.getMainLooper());
        Executors.newSingleThreadExecutor().submit(() -> {
            phaser.awaitAdvance(phaser.arriveAndDeregister());
            joiner.post(() -> finishPathSync());
        });
    }

    private void syncErrorAdd(String msg) {
        syncSuccess = false;
        lastSyncErrors.add(msg);
    }

    private void syncErrorClear() {
        syncSuccess = true;
        lastSyncErrors.clear();
    }

    private void syncNotifyProgress() {
        int progress = 0;
        if (taskSyncNotifier != null) {
            progress += 90 * taskSyncNotifier.progress / 100;
        }
        if (pathSyncNotifier != null) {
            progress += 10 * pathSyncNotifier.progress / 100;
        }
        sendBroadcastMessage(createBroadcastImplicitIntent(BROADCAST_MSG.SYNC_PROGRESS)
                .putExtra(BROADCAST_SYNC_PROGRESS_PARAMS.PROGRESS.ID, progress));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (isRunning) {
            return MODE_START;
        }

        Intent notificationIntent = new Intent(this, StartActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.ms_notificationContent))
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentIntent(pendingIntent)
                .build();

        if (IS_FOREGROUND) {
            startForeground(FOREGROUND_SERVICE_ID, notification);
        }

        isRunning = true;
        sendBroadcastMessage(createBroadcastImplicitIntent(BROADCAST_MSG.STARTED));
        return MODE_START;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;

        if (tasksUpdaterHandler != null && tasksUpdaterRunnable != null) {
            tasksUpdaterHandler.removeCallbacks(tasksUpdaterRunnable);
            tasksUpdaterHandler = null;
            tasksUpdaterRunnable = null;
        }
    }


    public Intent createBroadcastImplicitIntent(BROADCAST_MSG broadcast_msg) {
        Intent intent = new Intent(BROADCAST_MSG.BROADCAST_ID.ID);
        intent.putExtra(BROADCAST_MSG.TYPE.ID, broadcast_msg.ID);
        return intent;
    }

    public Intent createBroadcastExplicitIntent(Class targetClass, String action) {
        Intent intent = new Intent(BROADCAST_MSG.BROADCAST_ID.ID);
        intent.putExtra(BROADCAST_MSG.TYPE.ID, BROADCAST_MSG.EXPLICIT.ID);
        intent.putExtra(BROADCAST_EXPLICIT_PARAMS.CLASS_NAME.ID, targetClass.getName());
        intent.putExtra(BROADCAST_EXPLICIT_PARAMS.ACTION.ID, action);
        return intent;
    }

    public void sendBroadcastMessage(Intent intent) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private LocationManager mLocationManager;
    private ArrayList<InavMessage> inavMessages;

    public ArrayList<InavMessage> getInavMessages(){
        return inavMessages;
    }

    public static HashMap<String, Date> satValidationMap = new HashMap<>();

    private static int satValidationTimeframe = 240; //in seconds
    private final int MIN_OSNMA_VALIDATED_FOR_FIX = 1;

    private LocationListener locationListener;

    private GnssNavigationMessage.Callback gnssNavigationMessageListener;

    public void startLMLocationMonitoring(final LocationReceiver receiver, boolean reachableInternet){
        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        inavMessages = new ArrayList<>();

        satValidationMap = new HashMap<>();

        sessionID = UUID.randomUUID().toString();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // nothing
        } else {
            gnssNavigationMessageListener = new GnssNavigationMessage.Callback() {
                @Override
                public void onGnssNavigationMessageReceived(GnssNavigationMessage event) {
                    if(event.getType() == GnssNavigationMessage.TYPE_GAL_I){

                        byte[] rawData = event.getData();

                        String b64Inav = Base64.getEncoder().encodeToString(rawData);

                        long rt = new Date().getTime();
                        String rawDate = String.valueOf(rt).substring(0,10);

                        InavMessage msgTmp = new InavMessage( event.getSvid(), Long.parseLong(rawDate), b64Inav);
                        inavMessages.add(msgTmp);

                        if(reachableInternet && PersistData.getOSNMAValidated(getApplicationContext())){
                            verify_osnma_packet(b64Inav, event.getSvid());
                        }
                    }
                }

                @Override
                public void onStatusChanged(int status) {
                }
            };
            mLocationManager.registerGnssNavigationMessageCallback(gnssNavigationMessageListener);
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {

                    if(!reachableInternet) {
                        currentLocation = location;
                        Bundle bnd = new Bundle();
                        bnd.putString("provider", OFFLINE_LM_PROVIDER);
                        if(PersistData.getOSNMAValidated(getApplicationContext())){
                            bnd.putBoolean("isOsnma", true);
                            bnd.putInt("valisats", 0);
                        }
                        currentLocation.setExtras(bnd);
                        if (receiver != null) {
                            receiver.receive(currentLocation);
                        }
                    }else{
                        if(PersistData.getOSNMAValidated(getApplicationContext())){
                            if (satValidationMap.size() >= MIN_OSNMA_VALIDATED_FOR_FIX )
                            {
                                currentLocation = location;
                                Bundle bnd = new Bundle();
                                bnd.putString("provider", ONLINE_LM_PROVIDER);
                                bnd.putBoolean("isOsnma", true);
                                bnd.putInt("valisats", satValidationMap.size());
                                currentLocation.setExtras(bnd);
                                if (receiver != null) {
                                    receiver.receive(currentLocation);
                                }
                            }
                        }else{
                            currentLocation = location;
                            Bundle bnd = new Bundle();
                            bnd.putString("provider", ONLINE_LM_PROVIDER);
                            currentLocation.setExtras(bnd);
                            if (receiver != null) {
                                receiver.receive(currentLocation);
                            }
                        }
                    }
                }
            };
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener , this.getMainLooper());
        }
    }

    private String sessionID = "";

    private Thread osnmaThread;
    private void verify_osnma_packet(String b64Inav, int svid) {
        Log.d(TAG,"STARTING OSNMA THREAD");
        osnmaThread = new Thread(() -> {
            try {
                long rt = new Date().getTime();
                String rawDate = String.valueOf(rt).substring(0,10);

                Thread.sleep(10000);

                String deviceName = android.os.Build.MODEL;
                String deviceMan = android.os.Build.MANUFACTURER;
                String deviceVer = Build.VERSION.RELEASE;

                AsyncTask<String, Void, ServerPostResponse> server_response = new ServerPostPacketTask().execute(
                        rawDate,
                        String.valueOf(svid),
                        "2",
                        b64Inav,
                        deviceMan,
                        deviceName,
                        sessionID,
                        deviceVer);

                try {
                    ServerPostResponse result = server_response.get();
                    String svidStr = String.valueOf(svid);
                    boolean validated = false;
                    if(result.getStatus().equalsIgnoreCase(ServerPostResponse.OK)) {
                        if (!isSatValidated(svidStr)) {
                            addSatToValidated(svidStr);
                        } else {
                            if(isValidationExpired(svidStr)){
                                removeSatFromValidated(svidStr);
                                addSatToValidated(svidStr);
                            }
                        }
                        validated = true;
                    }else if(result.getStatus().equalsIgnoreCase(ServerPostResponse.KO)) {
                        if (isSatValidated(svidStr) && isValidationExpired(svidStr)) {
                            removeSatFromValidated(String.valueOf(svid));
                        }
                        validated = false;
                    }

                    /*for (InavMessage inavMessage : inavMessages) {
                        if(inavMessage.getSvid() == svid && String.valueOf(inavMessage.getTimestamp()).equals(rawDate)){
                            inavMessage.setValidated(validated);
                        }
                    }*/

                    //Thread.currentThread().interrupt();
                    Log.d(TAG,"Server response: <"+ svid +"> - <"+result.getStatus()+">");
                } catch (ExecutionException | InterruptedException e) {
                    Log.e(TAG,"Server error",e);
                }
            } catch (InterruptedException e) {
                Log.e(TAG,"OSNMA Validation THREAD interrupted",e);
            }
        });
        osnmaThread.start();
        Log.d(TAG,"OSNMA THREAD STATE: <"+osnmaThread.getState().toString()+">");
    }

    public static void addSatToValidated(String svid){
        if(!isSatValidated(svid)){
            satValidationMap.put(svid, new Date());
        }
    }

    public static void removeSatFromValidated(String svid){
        satValidationMap.remove(svid);
    }

    public static boolean isSatValidated(String svid){
        boolean found = false;
        Iterator<Map.Entry<String, Date>> itr = satValidationMap.entrySet().iterator();

        while(itr.hasNext())
        {
            Map.Entry<String, Date> entry = itr.next();
            String satId = entry.getKey();
            Date dataValidazione = entry.getValue();
            if(satId.equalsIgnoreCase(svid)){
                found = true;
            }
        }
        return found;
    }

    public static boolean isValidationExpired(String svid){
        Date now = new Date();
        Date validationDate = satValidationMap.get(svid);
        long seconds = (now.getTime() - validationDate.getTime())/1000;
        if(seconds > satValidationTimeframe){
            return true;
        }else{
            return false;
        }
    }
    public int getValidatedSatsNum(){
        return satValidationMap.size();
    }

    public HashMap<String, Date> getSatValidationMap(){
        return satValidationMap;
    }

    public void startFusedLocationMonitoring(final LocationReceiver receiver){
        provider = FUSED_PROVIDER;
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(LOCATION_REQUEST_MILS);
        locationRequest.setFastestInterval(LOCATION_REQUEST_MILS_FASTEST);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        currentLocation = location;
                        Bundle bnd = new Bundle();
                        bnd.putString("provider", FUSED_PROVIDER);
                        currentLocation.setExtras(bnd);
                        if (receiver != null) {
                            receiver.receive(location);
                        }
                    }
                }
            }
        };
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // nothing
        } else {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());
        }
    }

    private boolean isOSNMAValidatedActive = false;
    public boolean isOSNMAValidatedActive(){
        return isOSNMAValidatedActive;
    }
    @SuppressLint("MissingPermission")
    public void startLocationMonitoring(final LocationReceiver receiver, Activity activity) {

        boolean reachableInternet = Util.isInternetAvailable();


        rootReceiver = receiver;
        if(GNSSSettingsStore.readExternalBT(getApplicationContext())){
            provider = EXTERNAL_PROVIDER;
            bluetoothManager = new BluetoothManager(getApplicationContext());
            bluetoothManager.setBluetoothLocationCallback(this);
            bluetoothManager.requestLocationUpdates();
            ProgressDialog searchDialog = ProgressDialog.show(activity, "",
                    "Searching for device. Please wait...", true);
            bluetoothManager.setDeviceFoundCallback(new BluetoothManager.DeviceFoundCallback() {
                @Override
                public void onDeviceFound(BluetoothDevice device) {
                    searchDialog.dismiss();
                    bluetoothManager.start(activity);
                }

                @Override
                public void onDeviceNotFound() {
                    searchDialog.dismiss();
                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    builder.setTitle(R.string.bts_notFoundDevice)
                            .setPositiveButton("OK",(dialogInterface, i) -> {
                                dialogInterface.dismiss();
                                activity.finish();
                            });
                    builder.create().show();
                }
            });
            bluetoothManager.getDeviceByName(GNSSSettingsStore.readExternalBTName(getApplicationContext()), activity);
        }else {
            isOSNMAValidatedActive = PersistData.getOSNMAValidated(this);
            /*if(reachableInternet) {
                inavMessages = new HashMap<>();

                provider = GNSS_PROVIDER;
                gnssManager = new GNSSManager(getApplicationContext());
                gnssManager.setNewLocationCallback(new GNSSManager.GNSSLocationCallback() {
                    @Override
                    public void onNewLocation(LocationResult locationResult) {
                        if (locationResult == null) {
                            return;
                        }
                        for (Location location : locationResult.getLocations()) {
                            if (location != null) {
                                currentLocation = location;
                                if (receiver != null) {
                                    receiver.receive(location);
                                }
                            }
                        }
                        if(gnssManager.getInavMessages() != null)
                            inavMessages = gnssManager.getInavMessages();
                    }
                });
                gnssManager.setLooper(getMainLooper());

                gnssManager.start();
            }else{
                provider = OFFLINE_LM_PROVIDER;
                Log.d(TAG, "No internet, using Location Manager");
                //startFusedLocationMonitoring(receiver);
                startLMLocationMonitoring(receiver);
            }*/
            if(reachableInternet){
                provider = ONLINE_LM_PROVIDER;
                Log.d(TAG, "No internet, using Location Manager");
                //startFusedLocationMonitoring(receiver);
                startLMLocationMonitoring(receiver, reachableInternet);
            }else{
                provider = OFFLINE_LM_PROVIDER;
                Log.d(TAG, "No internet, using Location Manager");
                //startFusedLocationMonitoring(receiver);
                startLMLocationMonitoring(receiver, reachableInternet);
            }
        }
    }

    GNSSManager gnssManager;
    LocationReceiver rootReceiver;

    BluetoothManager bluetoothManager;

    @Override
    public void onNewLocation(LocationResult locationResult) {
        currentLocation = locationResult.getLastLocation();
        rootReceiver.receive(locationResult.getLastLocation());
    }


    public void stopFusedLocationMonitoring(OnSuccessListener onSuccessListener, OnFailureListener onFailureListener, OnCompleteListener onCompleteListener) {
        if (fusedLocationClient != null) {
            com.google.android.gms.tasks.Task task = fusedLocationClient.removeLocationUpdates(locationCallback);
            if (onSuccessListener != null) {
                task.addOnSuccessListener(onSuccessListener);
            }
            if (onFailureListener != null) {
                task.addOnFailureListener(onFailureListener);
            }
            if (onCompleteListener != null) {
                task.addOnCompleteListener(onCompleteListener);
            }
        }
    }

    public void stopLocationMonitoring(OnSuccessListener onSuccessListener, OnFailureListener onFailureListener, OnCompleteListener onCompleteListener) {
        if(gnssManager != null){
            gnssManager.stop();
        }
        if(bluetoothManager != null){
            bluetoothManager.stop();
        }
        if(provider.equals(OFFLINE_LM_PROVIDER) || provider.equals(ONLINE_LM_PROVIDER)){
            mLocationManager.removeUpdates(locationListener);
            if(PersistData.getOSNMAValidated(getApplicationContext())){
                mLocationManager.unregisterGnssNavigationMessageCallback(gnssNavigationMessageListener);
                if(osnmaThread != null && osnmaThread.isAlive()){
                    osnmaThread.interrupt();
                }
            }
        }
    }

    public void setKeepScreenOnActive(boolean isActive) {
        isKeepScreenOnActive = isActive;
        sendBroadcastMessage(createBroadcastImplicitIntent(BROADCAST_MSG.KEEP_SCREEN_ON).putExtra(BROADCAST_KEEP_SCREEN_ON_PARAMS.IS_ACTIVE.ID, isActive));
    }

    // region set, get

    public boolean isRunning() {
        return isRunning;
    }

    public String getProvider(){
        return provider;
    }

    public AppDatabase getAppDatabase() {
        return appDatabase;
    }

    public Requestor getRequestor() {
        return requestor;
    }

    public Class getMainClass() {
        return mainClass;
    }

    public Location getCurrentLocation() {
        return currentLocation;
    }

    public Boolean isSync() {
        return isSync;
    }

    public boolean isSyncSuccess() {
        return syncSuccess;
    }

    public List<String> getLastSyncErrors() {
        return new ArrayList<>(lastSyncErrors);
    }

    public boolean isPathsUploading() {
        return isPathsUploading;
    }

    public void setPathsUploading(boolean pathsUploading) {
        isPathsUploading = pathsUploading;
    }

    public boolean isKeepScreenOnActive() {
        return isKeepScreenOnActive;
    }

    // endregion
}


