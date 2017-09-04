package com.riftinnovation.smartblade;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Date;
import java.util.Calendar;

/**
 * Created by ajrunhaar on 8/7/2017.
 */

public class SessionStatisticsSingleton{

    private static SessionStatisticsSingleton instance = null;

    private static final String TAG = "SessionStatistics";

    private int imagesTaken = 0;
    private int videosTaken = 0;
    private long previewDuration = 0;
    private long recordDuration = 0;
    private int rotationPreference = 0;
    private long sessionDuration = 0;
    private int layoutPreference = 0;

    private Date lastSessionUpdate;
    private Date lastPreviewUpdate;
    private Date lastRecordUpdate;


    public static final String PREFS_NAME = "SmartBladeSharePreferences";
    SharedPreferences sharedPreferences;

    private long sessionThreshold = 5000;

    private boolean firstStart = true;

    Context context;

    private SessionStatisticsSingleton(Context context){
        this.context = context;
        sharedPreferences = context.getApplicationContext().getSharedPreferences(PREFS_NAME,0);
    }

    public static SessionStatisticsSingleton getInstance(){
        if(instance == null){
            synchronized (SessionStatisticsSingleton.class) {
                instance = new SessionStatisticsSingleton(null);
            }
            throw new IllegalArgumentException ("SessionStatisticsSingleton instance has no context. ");
        }

        return instance;
    }

    public static SessionStatisticsSingleton getInstance(Context context){
        if(instance == null){
            instance = new SessionStatisticsSingleton(context);
        }

        return instance;
    }

    public void incrementImageCounter(){
        synchronized (SessionStatisticsSingleton.class) {imagesTaken+=1;}
    }

    public void incrementVideoCounter(){
        synchronized (SessionStatisticsSingleton.class) {videosTaken+=1;}
    }

    public void startSessionTimer(){
        synchronized (SessionStatisticsSingleton.class) {
            Date now = Calendar.getInstance().getTime();
            if (firstStart) {
                lastSessionUpdate = now;
                firstStart = false;
            }

            if ((now.getTime() - lastSessionUpdate.getTime()) < sessionThreshold) {
                lastSessionUpdate = now;
            } else {
                pushToStackAndClear();

            }
            Log.d(TAG, "Start Session Timer");
        }
    }
    public void stopSessionTimer(){
        synchronized (SessionStatisticsSingleton.class) {
            Date now = Calendar.getInstance().getTime();

            sessionDuration += now.getTime() - lastSessionUpdate.getTime();


            lastSessionUpdate = now;

            Log.d(TAG, "Stop Session Timer");
        }

    }



    public void startPreviewTimer(){
        synchronized (SessionStatisticsSingleton.class) {

            lastPreviewUpdate = Calendar.getInstance().getTime();
        }
    }
    public void stopPreviewTimer(){
        synchronized (SessionStatisticsSingleton.class) {
            Date now = Calendar.getInstance().getTime();
            if (lastPreviewUpdate != null) {
                previewDuration += now.getTime() - lastPreviewUpdate.getTime();
            } else {
                Log.e(TAG, "lastPreviewUpdate is Null");
            }

            lastPreviewUpdate = now;

        }
    }

    public void startRecordTimer(){
        synchronized (SessionStatisticsSingleton.class) {
            lastRecordUpdate = Calendar.getInstance().getTime();
        }
    }
    public void stopRecordTimer(){
        synchronized (SessionStatisticsSingleton.class) {
            Date now = Calendar.getInstance().getTime();

            recordDuration += now.getTime() - lastRecordUpdate.getTime();

            lastRecordUpdate = now;
        }

    }

    public JSONObject getMessage() {
        synchronized (SessionStatisticsSingleton.class) {
            rotationPreference = (int) sharedPreferences.getFloat("preview_rotation", 0);
            layoutPreference = sharedPreferences.getInt("layout_option", 0);

            JSONObject jsonObject = new JSONObject();
            try {

                jsonObject.put("Code", "SessionStatistics");
                jsonObject.put("imagesTaken", imagesTaken);
                jsonObject.put("videosTaken", videosTaken);
                jsonObject.put("previewDuration", previewDuration);
                jsonObject.put("recordDuration", recordDuration);
                jsonObject.put("rotationPreference", rotationPreference);
                jsonObject.put("sessionDuration", sessionDuration);
                jsonObject.put("layoutPreference", layoutPreference);
                Log.d(TAG, jsonObject.toString());
                return jsonObject;
            } catch (JSONException e) {
                Log.e(TAG, "JSON Exception: " + e.getMessage());
                return new JSONObject();
            }
        }
    }
    public void flush(){

        Date now = Calendar.getInstance().getTime();
        lastSessionUpdate = now;
        lastPreviewUpdate = now;
        lastRecordUpdate = now;
        sessionDuration=0;
        recordDuration=0;
        previewDuration=0;
        imagesTaken=0;
        videosTaken=0;


    }

    public void pushToStackAndClear(){

        EntryStackSingleton.getInstance().getEntryStack().add(new Entry(sharedPreferences.getString("UserID","Err"),this.getMessage()));
        if(context!=null) {
            context.startService(new Intent(context, PushEntryStackService.class));
        }else{
            Log.e(TAG, "Context is null in PushToStackAndClear");
        }
        this.flush();
    }


}
