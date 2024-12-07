package com.erasmicoin.euspa.gsa.egnss4all.model.OSNMA;

import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.erasmicoin.euspa.gsa.egnss4all.model.Photo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;

public class OfflineValidation {

    private final static String SERVER_URL = "http://157.230.208.98:8000/galmon";
    private static final String TAG = "OFFLINEVALIDATION";

    public interface ValidationProcess{
         void onValidationResult(int validatedSats);
    }

    public static HashMap<String, Date> satValidationMap = new HashMap<>();


    public static String updateInavMessages(String inavMessages, HashMap<String, Date> satValidationMap){
        org.json.simple.parser.JSONParser parser = new JSONParser();

        try {
            org.json.simple.JSONArray inavs = (org.json.simple.JSONArray) parser.parse(inavMessages);
            for (int i = 0; i < inavs.size(); i++) {
                org.json.simple.JSONObject inav = (org.json.simple.JSONObject) inavs.get(i);
                String svid = String.valueOf(inav.get("svid"));
                String ts = String.valueOf(inav.get("timestamp"));
                inav.put("validated", false);
                for (String satId : satValidationMap.keySet()) {
                    String tsVal = String.valueOf(satValidationMap.get(satId).getTime()).substring(0,10);
                    if(satId.equals(svid) && ts.equals(tsVal)){
                        inav.put("validated", true);
                    }
                }
                inavs.set(i,inav);
            }
            return inavs.toJSONString();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }
    public static void delayedValidation(Photo photo, ValidationProcess result){

        JSONArray messages = null;
        Log.d(TAG,"STARTING OSNMA THREAD");

        try {
            messages = new JSONArray(photo.getInavMessages());
            AtomicInteger validationAnswers = new AtomicInteger();
            int totalMessagesLength = messages.length();

            String sessionID = UUID.randomUUID().toString();

            for (int i=0; i< messages.length(); i++) {

                JSONObject message = messages.getJSONObject(i);

                int svid = message.getInt("svid");

                String ts = String.valueOf(message.getLong("timestamp")).substring(0,10);

                String inavb64 = message.getString("message");

                String deviceName = android.os.Build.MODEL;
                String deviceMan = android.os.Build.MANUFACTURER;
                String deviceVer = Build.VERSION.RELEASE;

                if(!isSatValidated(String.valueOf(svid))){
                    System.out.println("Lancio validazione per svid "+svid);
                    new Thread(() -> {
                        boolean validated = false;
                        JSONObject packet = new JSONObject();
                        try {
                            packet.put("timestamp", ts);
                            packet.put("wn",0);
                            packet.put("tow",0);
                            packet.put("sat_id", String.valueOf(svid));
                            packet.put("gnssid", "2");
                            packet.put("inav_string", inavb64);
                            packet.put("ntp_offset", 0/1000L);
                            packet.put("ntp_time", 0);
                            packet.put("manufacturer", deviceMan);
                            packet.put("model", deviceName);
                            packet.put("uuid", sessionID);
                            packet.put("version",deviceVer);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        MediaType JSON = MediaType.parse("application/json; charset=utf-8");

                        OkHttpClient client = new OkHttpClient.Builder()
                                .connectTimeout(10, TimeUnit.SECONDS)
                                .writeTimeout(10, TimeUnit.SECONDS)
                                .readTimeout(30, TimeUnit.SECONDS)
                                .build();

                        RequestBody body = RequestBody.create(packet.toString(), JSON);
                        okhttp3.Request request = new okhttp3.Request.Builder()
                                .url(SERVER_URL)
                                .post(body)
                                .build();
                        client.newCall(request).enqueue(new Callback() {
                            @Override
                            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                                call.cancel();
                            }
                            @Override
                            public void onResponse(@NonNull Call call, @NonNull okhttp3.Response response) throws IOException {
                                try {
                                    if (response.body() != null) {
                                        JSONObject networkResp = new JSONObject(response.body().string());
                                        ServerPostResponse serverResult = new ServerPostResponse(networkResp.getString("validity_check"), 0, 0);

                                        validationAnswers.getAndIncrement();
                                        String svidStr = String.valueOf(svid);
                                        if (serverResult.getStatus().equalsIgnoreCase(ServerPostResponse.OK)) {
                                            if (!isSatValidated(svidStr)) {
                                                addSatToValidated(svidStr);
                                            }
                                        }

                                        if(validationAnswers.get() >= totalMessagesLength){
                                            result.onValidationResult(satValidationMap.size());
                                        }
                                    }
                                } catch (JSONException e) {
                                    validationAnswers.getAndIncrement();
                                    throw new RuntimeException(e);
                                }
                            }
                        });
                    }).start();
                }else{
                    System.out.println("Salto validazione per svid "+svid+" già validato");
                    validationAnswers.getAndIncrement();
                }

            }

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }



    }

    public static void addSatToValidated(String svid){
        if(!isSatValidated(svid)){
            satValidationMap.put(svid, new Date());
        }
    }
    public static boolean isSatValidated(String svid){
        boolean found = false;
        Iterator<Map.Entry<String, Date>> itr = satValidationMap.entrySet().iterator();

        while(itr.hasNext())
        {
            Map.Entry<String, Date> entry = itr.next();
            String satId = entry.getKey();
            if(satId.equalsIgnoreCase(svid)){
                found = true;
            }
        }
        return found;
    }

}
