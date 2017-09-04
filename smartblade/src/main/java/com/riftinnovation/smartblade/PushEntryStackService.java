package com.riftinnovation.smartblade;


import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by ajrunhaar on 8/7/2017.
 */

public class PushEntryStackService extends IntentService {
    BufferedReader br;
    private static final String TAG = "PushEntryStackService";

    public static final String PREFS_NAME = "SmartBladeSharePreferences";

    SharedPreferences sharedPreferences;

    public PushEntryStackService(){
        super("PushEntryStackService");


    }
    @Override
    protected void onHandleIntent(Intent workIntent) {
        boolean validUser = true;
        Log.d(TAG,"onHandleIntent - Push Entry Stack to server");
        if(!EntryStackSingleton.getInstance().getEntryStack().isEmpty()){
            try {

                for (Entry entry : EntryStackSingleton.getInstance().getEntryStack()){
                    URL url = new URL("http://192.168.0.105:8000/entry");
                    //URL url = new URL("http://ec2-52-56-145-221.eu-west-2.compute.amazonaws.com/entry");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                    conn.setRequestProperty("Accept","application/json");
                    conn.setDoOutput(true);
                    conn.setDoInput(true);

                    DataOutputStream os;
                    os = new DataOutputStream(conn.getOutputStream());
                    JSONObject jsonParam = new JSONObject();
                    jsonParam.put("UserID", entry.getUserID());
                    jsonParam.put("Entry", entry.getEntry());
                    jsonParam.put("Timestamp", entry.getTimeStamp());

                    Log.i("JSON", jsonParam.toString());

                    //os.writeBytes(URLEncoder.encode(jsonParam.toString(), "UTF-8"));
                    os.writeBytes(jsonParam.toString());

                    os.flush();
                    os.close();
                    jsonParam = null;

                    if(conn.getHeaderField("Success").toString() == "True"){

                    }
                    Log.i(TAG,"Username: " + conn.getHeaderField("Username").toString() +" | Email: " +  conn.getHeaderField("Email").toString());
                    if(conn.getHeaderField("Username").toString().equals("Unknown") || conn.getHeaderField("Email").toString().equals("Unknown")){
                        validUser = false;
                        Log.i(TAG,"Unknown user");
                    }else{
                        Log.i(TAG,"User known");
                    }
                    conn.disconnect();
                }
                EntryStackSingleton.getInstance().getEntryStack().clear();

                if(!validUser){

                    sharedPreferences = getSharedPreferences(PREFS_NAME,0);
                    JSONObject userProfileUpdate = new JSONObject();
                    try {
                        userProfileUpdate.put("Code", "UserUpdate");
                        userProfileUpdate.put("UserName", sharedPreferences.getString("user_name","Err"));
                        userProfileUpdate.put("UserEmail", sharedPreferences.getString("user_email","Err"));
                        EntryStackSingleton.getInstance().getEntryStack().add(new Entry(sharedPreferences.getString("UserID","Err"),userProfileUpdate));
                        Log.i(TAG,EntryStackSingleton.getInstance().getEntryStack().toString());
                    }catch(JSONException e){
                        Log.e(TAG, "JSONException: " + e.getMessage());
                    }

                }





                //Log.i(TAG, "STATUS:" + String.valueOf(conn.getResponseCode()));
                //Log.i(TAG , "MSG:" + conn.getResponseMessage());
                //Log.i(TAG , "Response: " + conn.getHeaderField("Success").toString());



            } catch (Exception e) {
                e.printStackTrace();
            }

        }

    }
}