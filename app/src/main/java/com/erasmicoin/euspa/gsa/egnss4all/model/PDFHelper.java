package com.erasmicoin.euspa.gsa.egnss4all.model;

import android.content.Context;
import android.graphics.pdf.PdfDocument;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.erasmicoin.euspa.gsa.egnss4all.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;

public class PDFHelper{

    private ImageView mapImage;
    private FileOutputStream fileOutput;

    private View content;
    private PdfDocument document;

    private PdfDocument.Page documentPage;

    public interface PDFGeneratedCallback{
        void onPDFGenerated();
    }

    PDFGeneratedCallback rootCallback;

    public void generatePDF(AppCompatActivity ctx, Photo photo, ParcelFileDescriptor pfd, PDFGeneratedCallback callback){

        rootCallback = callback;

        fileOutput = new FileOutputStream(pfd.getFileDescriptor());

        document = new PdfDocument();

        //A4 size
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(1440, 2560, 1).create();

        // start a page
        documentPage = document.startPage(pageInfo);

        // draw something on the page
        LayoutInflater inflater = (LayoutInflater)
                ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        content = inflater.inflate(R.layout.photo_detail_linear_pdf, null);


        /*content.measure(595, 842);
        content.layout(0,0, 595, 842);*/

        DecimalFormat decimalFormat = Util.createPrettyCoordinateFormat();

        String lat = "";
        try{
            lat = decimalFormat.format(photo.getLat());
        }catch(IllegalArgumentException e){
            lat = "Invalid";
        }
        String lng = "";
        try{
            lng = decimalFormat.format(photo.getLng());
        }catch(IllegalArgumentException e){
            lng = "Invalid";
        }

        TextView latitudeTextView = content.findViewById(R.id.pd_textView_latitude);
        latitudeTextView.setText(lat);
        TextView longitudeTextView = content.findViewById(R.id.pd_textView_longitude);
        longitudeTextView.setText(lng);
        TextView createdTextView = content.findViewById(R.id.pd_textView_created);
        createdTextView.setText(photo.getCreated().toString(Util.createPrettyDateTimeFormat()));
        TextView sentTextView = content.findViewById(R.id.pd_textView_sent);
        sentTextView.setText(photo.isSent() ? ctx.getString(R.string.pd_sendedYes) : ctx.getString(R.string.pd_sendedNo));
        TextView noteTextView = content.findViewById(R.id.pd_textView_note);
        noteTextView.setText(photo.getNote());
        ImageView photoImageView = content.findViewById(R.id.pd_imageView_photo);

        mapImage = content.findViewById(R.id.mapImage);

        try {
            photoImageView.setImageBitmap(photo.getRotatedBitmap(1));
        } catch (IOException e) {
            //alert(getString(R.string.pd_errorImageLoadTitle), getString(R.string.pd_errorImageLoadText, e.getMessage()));
            Log.e("PDFHELPER","errore",e);
        }
        TextView isOsnmaValidated = content.findViewById(R.id.pd_OSNMAValidated);
        isOsnmaValidated.setText(photo.isOsnmaValidated() ? "True" : "False" );

        LinearLayout valisatsLayout = content.findViewById(R.id.valisatsLayout);
        if(photo.isOsnmaValidated()){
            valisatsLayout.setVisibility(View.VISIBLE);
            TextView valiSats = content.findViewById(R.id.pd_validatedSats);
            valiSats.setText(String.valueOf(photo.getValidatedSats()));
        }

        int measureWidth = View.MeasureSpec.makeMeasureSpec(documentPage.getCanvas().getWidth(), View.MeasureSpec.EXACTLY);
        int measuredHeight = View.MeasureSpec.makeMeasureSpec(documentPage.getCanvas().getHeight(), View.MeasureSpec.EXACTLY);

        content.measure(measureWidth, measuredHeight);
        content.layout(0, 0, documentPage.getCanvas().getWidth(), documentPage.getCanvas().getHeight());


        /*getMapAsync(googleMap -> {
            LatLng photoMarkerPos = new LatLng(photo.getLat(), photo.getLng());
            googleMap.addMarker(new MarkerOptions()
                    .position(photoMarkerPos)
                    .title("Photo"));
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(photoMarkerPos));
            closeAndWriteDocument();
        });*/
        SupportMapFragment mapFragment = (SupportMapFragment) ctx.getSupportFragmentManager()
                .findFragmentById(R.id.map);

        mapFragment.getMapAsync(googleMap -> {
            //googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            LatLng photoMarkerPos = new LatLng(photo.getLat(), photo.getLng());
            LatLng southWest = new LatLng(photo.getLat()-0.02, photo.getLng()-0.02);
            LatLng northEasth = new LatLng(photo.getLat()-0.02, photo.getLng()+0.02);
            LatLngBounds bounds = new LatLngBounds(southWest, northEasth);
            googleMap.addMarker(new MarkerOptions()
                    .position(photoMarkerPos)
                    .title("Photo"));
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(photoMarkerPos));
            //googleMap.setLatLngBoundsForCameraTarget(bounds);
            googleMap.setOnMapLoadedCallback(this::closeAndWriteDocument);
        });

        //closeAndWriteDocument();
    }

    private void closeAndWriteDocument(){
        try {
            //Thread.sleep(2000);
            content.draw(documentPage.getCanvas());
            // finish the page
            document.finishPage(documentPage);
            // add more pages
            // write the document content
            document.writeTo(fileOutput);

            rootCallback.onPDFGenerated();
        } catch (IOException e) {
            e.printStackTrace();
        } /*catch (InterruptedException e) {
            throw new RuntimeException(e);
        }*/
    }
}
