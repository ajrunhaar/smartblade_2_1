package com.riftinnovation.smartblade;


import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;


/**
 * Created by ajrunhaar on 6/14/2017.
 */

public class UserFeedbackActivity extends Activity {

    EditText user_message;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    public static final String PREFS_NAME = "SmartBladeSharePreferences";

    public static final String TAG = "UserFeedbackActivity";

    View decorView;
    int uiOptions =View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = getSharedPreferences(PREFS_NAME,0);

        setContentView(R.layout.activity_user_feedback);

        Button submitButton = (Button) findViewById(R.id.user_feedback_submit);
        submitButton.setOnClickListener(mOnClickListener);

        Button resetButton = (Button) findViewById(R.id.user_feedback_reset);
        resetButton.setOnClickListener(mOnClickListener);

        Button exitButton = (Button) findViewById(R.id.user_feedback_exit);
        exitButton.setOnClickListener(mOnClickListener);

        user_message = (EditText) findViewById(R.id.user_message);

        decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(uiOptions);

    }

    @Override
    protected void onStart() {
        super.onStart();
        decorView.setSystemUiVisibility(uiOptions);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Log.d("SessionStatistics","User Resume");
        SessionStatisticsSingleton.getInstance().startSessionTimer();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //Log.d("SessionStatistics","User Pause");
        SessionStatisticsSingleton.getInstance().stopSessionTimer();
    }

    View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.user_feedback_submit:
                    if(!user_message.getText().toString().replace(" ","").matches("")){

                        JSONObject feedbackMessage = new JSONObject();
                        try {
                            feedbackMessage.put("Code", "UserFeedback");
                            feedbackMessage.put("Message", user_message.getText().toString());
                            EntryStackSingleton.getInstance().getEntryStack().add(new Entry(sharedPreferences.getString("UserID","Err"),feedbackMessage));
                            UserFeedbackActivity.this.startService(new Intent(UserFeedbackActivity.this,PushEntryStackService.class));
                            Toast.makeText(UserFeedbackActivity.this,"Message Submitted",Toast.LENGTH_SHORT).show();
                        }catch(JSONException e){
                            Log.e(TAG, "JSONException: " + e.getMessage());
                        }

                        finish();
                    }
                    else{
                        Toast.makeText(UserFeedbackActivity.this,"Empty Message",Toast.LENGTH_SHORT).show();
                    }
                    break;
                case R.id.user_feedback_reset:
                    user_message.setText("");
                    break;
                case R.id.user_feedback_exit:
                    finish();
                    break;

            }

        }
    };

}