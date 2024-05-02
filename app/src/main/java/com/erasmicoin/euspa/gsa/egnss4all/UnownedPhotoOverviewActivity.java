package com.erasmicoin.euspa.gsa.egnss4all;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.erasmicoin.euspa.gsa.egnss4all.model.AppDatabase;
import com.erasmicoin.euspa.gsa.egnss4all.model.LoggedUser;
import com.erasmicoin.euspa.gsa.egnss4all.model.MyAlertDialog;
import com.erasmicoin.euspa.gsa.egnss4all.model.Photo;
import com.erasmicoin.euspa.gsa.egnss4all.model.PhotoList;
import com.erasmicoin.euspa.gsa.egnss4all.model.Task;
import com.erasmicoin.euspa.gsa.egnss4all.model.Util;
import com.erasmicoin.euspa.gsa.egnss4all.model.functionInterface.Consumer;

import org.json.JSONException;

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class UnownedPhotoOverviewActivity extends BaseActivity {

    public enum BROADCAST_ACTION {
        REFRESH_PHOTOS
    }

    public enum BROADCAST_REFRESH_PHOTOS {
        SCROLL_TOP
    }

    public static final String INTENT_ACTION_REFRESH_PHOTOS = "intentRefresh";
    public static final String INTENT_ACTION_REFRESH_PHOTOS_SCROLL_TOP = "scrollTop";

    private int firstVisibleItem = 0;
    private RecyclerView photosRecyclerView;
    private PhotoList photoList;
    private AtomicBoolean isUploading = new AtomicBoolean(false);

    public class UnownedPhotoAdapter extends RecyclerView.Adapter<UnownedPhotoAdapter.UnownedPhotoHolder> {

        public class UnownedPhotoHolder extends RecyclerView.ViewHolder {

            ImageView photoImageView;
            TextView latitudeTextView;
            TextView longitudeTextView;
            TextView createdTextView;
            TextView sentTextView;

            TextView netStatTextView;
            TextView validateText;
            TextView noteTextView;
            ConstraintLayout mainLayout;

            LinearLayout netStatLayout;

            public UnownedPhotoHolder(@NonNull View itemView) {
                super(itemView);
                photoImageView = itemView.findViewById(R.id.uplt_imageView_photo);
                latitudeTextView = itemView.findViewById(R.id.uplt_textView_latitude);
                longitudeTextView = itemView.findViewById(R.id.uplt_textView_longitude);
                createdTextView = itemView.findViewById(R.id.uplt_textView_created);
                sentTextView = itemView.findViewById(R.id.uplt_textView_sent);
                netStatTextView = itemView.findViewById(R.id.uplt_textView_netstat);
                netStatLayout = itemView.findViewById(R.id.uplt_netstat_layout);
                validateText = itemView.findViewById(R.id.uplt_textView_validated);
                noteTextView = itemView.findViewById(R.id.uplt_textView_note);
                mainLayout = itemView.findViewById(R.id.uplt_constraintLayout_main);
            }
        }

        Context context;
        PhotoList photoList;

        public UnownedPhotoAdapter(Context context, PhotoList photoList) {
            this.context = context;
            this.photoList = photoList;
        }

        @NonNull
        @Override
        public UnownedPhotoHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(R.layout.unowned_photo_list_item, parent, false);
            return new UnownedPhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull UnownedPhotoHolder holder, int position) {
            Photo photo = photoList.getPhotos().get(position);
            try {
                Bitmap rotatedBitmap = photo.getRotatedBitmap(8);
                holder.photoImageView.setImageBitmap(rotatedBitmap);
            } catch (IOException e) {
                BaseActivity baseActivity = (BaseActivity) context;
                baseActivity.alert(context.getString(R.string.pd_errorImageLoadTitle), context.getString(R.string.pd_errorImageLoadText, e.getMessage()));
            }
            DecimalFormat decimalFormat = new DecimalFormat("#.#######");
            decimalFormat.setDecimalFormatSymbols(new DecimalFormatSymbols(Locale.ENGLISH));
            String lat = "";
            try{
                lat = decimalFormat.format(photo.getLat());
            }catch(IllegalArgumentException e){
                lat = "Invalid";
            }
            holder.latitudeTextView.setText(lat);
            String lng = "";
            try{
                lng = decimalFormat.format(photo.getLng());
            }catch(IllegalArgumentException e){
                lng = "Invalid";
            }
            holder.longitudeTextView.setText(lng);
            /*String validated = (photo.isOsnmaValidated() ? "True" : "False");
            holder.validateText.setText(validated);*/

            if(photo.isOsnmaEnabled()){
                if(photo.getProvider().equals(MainService.OFFLINE_LM_PROVIDER)){
                    if(photo.isSent()){
                        holder.validateText.setText(photo.isOsnmaValidated() ? "True" : "False");
                    }else{
                        holder.validateText.setText(getString(R.string.pd_validationPending));
                    }
                }else{
                    holder.validateText.setText(photo.isOsnmaValidated() ? "True" : "False" );

                }
            }else{
                holder.validateText.setText("False");
            }

            String dateTime = photo.getCreated().toString(Util.createPrettyDateTimeFormat());
            holder.createdTextView.setText(dateTime);
            holder.sentTextView.setText(photo.isSent() ? context.getString(R.string.pd_sendedYes) : context.getString(R.string.pd_sendedNo));

            if(photo.getProvider() != null){
                holder.netStatLayout.setVisibility(View.VISIBLE);
                holder.netStatTextView.setText(photo.getProvider().equals(MainService.ONLINE_LM_PROVIDER) ? getString(R.string.pd_online) : getString(R.string.pd_offline));
            }else{
                holder.netStatLayout.setVisibility(View.GONE);
            }

            holder.noteTextView.setText(photo.getNote());
            holder.mainLayout.setOnClickListener(v -> {
                saveLastPhotoPosition();
                toPhotoDetail(photo);
            });
        }

        @Override
        public int getItemCount() {
            return photoList.getPhotos().size();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unowned_photo_overview);
        setToolbar(R.id.toolbar);
    }

    @Override
    public void serviceInit() {
        super.serviceInit();
        photosRecyclerView = findViewById(R.id.up_recyclerView_photoList);
        photosRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        refreshPhotoList(false);
    }

    public void toCamera(View view) {
        Intent intent = new Intent(this, CameraActivity.class);
        intent.setAction(CameraActivity.INTENT_ACTION_START);
        startActivity(intent);
    }

    public void showMap(View view) {
        Intent intent = new Intent(this, MapActivity.class);
        intent.setAction(MapActivity.INTENT_ACTION_START);
        intent.putExtra(MapActivity.INTENT_ACTION_START_MODE, MapActivity.START_MODE.UNOWNED_PHOTOS.name());
        startActivity(intent);
    }

    public void uploadAllPhotosDialog(View view) {
        synchronized (isUploading) {
            if (isUploading.get()) {
                return;
            }
            isUploading.set(true);
            if (!serviceController.isServiceInitialized()) {
                isUploading.set(false);
                return;
            }
            int countToUpload = PhotoList.countUnownedPhotosNotSent(MS.getAppDatabase());
            if (countToUpload == 0) {
                alert(getString(R.string.un_alertNothingToUploadTitle), getString(R.string.un_alertNothingToUploadText));
                isUploading.set(false);
                return;
            }
            AtomicBoolean isUploadStarted = new AtomicBoolean(false);
            MyAlertDialog.Builder builder = new MyAlertDialog.Builder(this);
            MyAlertDialog myAlertDialog = builder.build();
            myAlertDialog.setTitle(getString(R.string.un_uploadDialogTitle));
            myAlertDialog.setMessage(getString(R.string.un_uploadDialogText, String.valueOf(countToUpload)));
            myAlertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(R.string.dl_Cancel), (dialog, which) -> {
                isUploading.set(false);
            });
            myAlertDialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.dl_OK), (dialog, which) -> {
                isUploadStarted.set(true);
                uploadAllPhotos();
            });
            myAlertDialog.getAlertDialog().setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    if (!isUploadStarted.get()) {
                        isUploading.set(false);
                    }
                }
            });
            myAlertDialog.show();
        }
    }

    private void uploadAllPhotos() {
        synchronized (isUploading) {
            PhotoList photoListNotSent = PhotoList.createFromAppDatabaseUnownedPhotos(MS.getAppDatabase(), true, getApplicationContext());
            Consumer failedProcess = (errorString) -> {
                photoListNotSent.setLastSendFailed(true);
                photoListNotSent.recreateToDB();
                if (isCreated) {
                    alert(getString(R.string.pd_errorImagesLoadTitle), getString(R.string.pd_errorImagesLoadText, errorString.toString()));
                }
            };
            try {
                Task virtualTask = Task.createFromAppDatabaseSpecialUnownedPhotos(MS.getAppDatabase(), LoggedUser.createFromAppDatabase(MS.getAppDatabase()).getId());
                virtualTask.setNotSentPhotos(true);
                if (photoListNotSent.getPhotos().size() == 0) {
                    return;
                }
                Toast.makeText(this, getString(R.string.pd_startUploading), Toast.LENGTH_LONG).show();
                virtualTask.updateCompleteInMultipleRequest(MS.getAppDatabase(), this, new Task.UpdateTaskReceiver() {
                    @Override
                    protected void success(AppDatabase appDatabase, Task task) {
                        Toast.makeText(UnownedPhotoOverviewActivity.this.getApplicationContext(), getString(R.string.pd_completedUploading), Toast.LENGTH_LONG).show();
                        photoListNotSent.setSent(true);
                        photoListNotSent.setLastSendFailed(false);
                        photoListNotSent.recreateToDB();
                        if (isCreated) {
                            restartActivity();
                        }
                    }

                    @Override
                    protected void success(AppDatabase appDatabase) {

                    }

                    @Override
                    protected void failed(String error) {
                        failedProcess.accept(error);
                        Toast.makeText(UnownedPhotoOverviewActivity.this, getString(R.string.pd_failedUploading), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    protected void finish(boolean success) {
                        if (isCreated) {
                            isUploading.set(false);
                        }
                    }
                }, MS.getRequestor(), photoListNotSent);
            } catch (JSONException e) {
                failedProcess.accept(e.getMessage());
                isUploading.set(false);
            }
        }
    }

    private void refreshPhotoList(boolean scrollToTop) {
        photoList = PhotoList.createFromAppDatabaseByTaskGroup(MS.getAppDatabase(), null, getApplicationContext());
        UnownedPhotoAdapter unownedPhotoAdapter = new UnownedPhotoAdapter(this, photoList);
        photosRecyclerView.setAdapter(unownedPhotoAdapter);
        if (scrollToTop) {
            photosRecyclerView.scrollToPosition(0);
        } else {
            photosRecyclerView.scrollToPosition(firstVisibleItem);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getAction() != null && intent.getAction().equals(INTENT_ACTION_REFRESH_PHOTOS)) {
            refreshPhotoList(intent.getBooleanExtra(INTENT_ACTION_REFRESH_PHOTOS_SCROLL_TOP, false));
        }
    }

    private void saveLastPhotoPosition() {
        firstVisibleItem = ((LinearLayoutManager) photosRecyclerView.getLayoutManager()).findFirstCompletelyVisibleItemPosition();
    }

    private void toPhotoDetail(Photo photo) {
        Intent intent = new Intent(this, UnownedPhotoDetailActivity.class);
        intent.setAction(UnownedPhotoDetailActivity.INTENT_ACTION_START);
        intent.putExtra(UnownedPhotoDetailActivity.INTENT_ACTION_START_PHOTO_ID, photo.getId());
        startActivity(intent);
    }

    @Override
    protected MainService.BROADCAST_MSG broadcastExplicitReceiver(Context context, Intent intent) {
        if (BROADCAST_ACTION.REFRESH_PHOTOS.name().equals(intent.getStringExtra(MainService.BROADCAST_EXPLICIT_PARAMS.ACTION.ID))) {
            refreshPhotoList(intent.getBooleanExtra(BROADCAST_REFRESH_PHOTOS.SCROLL_TOP.name(), false));
        }
        return null;
    }
}