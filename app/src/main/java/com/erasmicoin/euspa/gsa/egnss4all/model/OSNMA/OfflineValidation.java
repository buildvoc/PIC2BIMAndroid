package com.erasmicoin.euspa.gsa.egnss4all.model.OSNMA;

import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import com.erasmicoin.euspa.gsa.egnss4all.model.Photo;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

public class OfflineValidation {

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
                        AsyncTask<String, Void, ServerPostResponse> server_response = new ServerPostPacketTask().execute(
                                ts,
                                String.valueOf(svid),
                                "2",
                                inavb64,
                                deviceMan,
                                deviceName,
                                sessionID,
                                deviceVer);

                        try {
                            ServerPostResponse serverResult = server_response.get();
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

                        } catch (ExecutionException | InterruptedException e) {
                            validationAnswers.getAndIncrement();
                            throw new RuntimeException(e);
                        }
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
