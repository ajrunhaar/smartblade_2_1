package com.riftinnovation.smartblade;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.serenegiant.common.BaseActivity;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by ajrunhaar on 8/30/2017.
 */

public class UserProfileActivity extends BaseActivity{
    public static final String PREFS_NAME = "SmartBladeSharePreferences";
    public static final String TAG = "UserProfileActivity";


    EditText user_name;
    EditText user_occupation;
    EditText user_email;

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

        setContentView(R.layout.activity_user_profile);

        Button submitButton = (Button) findViewById(R.id.user_profile_submit);
        submitButton.setOnClickListener(mOnClickListener);

        Button resetButton = (Button) findViewById(R.id.user_profile_reset);
        resetButton.setOnClickListener(mOnClickListener);

        Button exitButton = (Button) findViewById(R.id.user_profile_exit);
        exitButton.setOnClickListener(mOnClickListener);

        // Restore preferences
        SharedPreferences user_profile = getSharedPreferences(PREFS_NAME, 0);

        user_name = (EditText) findViewById(R.id.user_name);
        user_name.setText(user_profile.getString("user_name", ""));

        user_occupation = (EditText) findViewById(R.id.user_occupation);
        user_occupation.setText(user_profile.getString("user_occupation", ""));

        user_email = (EditText) findViewById(R.id.user_email);
        user_email.setText(user_profile.getString("user_email", ""));

                /*======= Hide Top Panel =======*/

        decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(uiOptions);


    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onResume() {
        super.onResume();
        SessionStatisticsSingleton.getInstance().startSessionTimer();
    }

    @Override
    protected void onPause() {
        super.onPause();
        SessionStatisticsSingleton.getInstance().stopSessionTimer();
    }

    View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.user_profile_submit:
                    SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, 0);
                    if(sharedPreferences!=null) {
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("user_name", user_name.getText().toString());
                        editor.putString("user_occupation", user_occupation.getText().toString());
                        editor.putString("user_email", user_email.getText().toString());
                        Toast.makeText(UserProfileActivity.this,"User Profile Updated",Toast.LENGTH_SHORT).show();
                        // Commit the edits!
                        editor.commit();

                        JSONObject userUpdateMessage = new JSONObject();
                        try {
                            userUpdateMessage.put("Code", "UserUpdate");
                            userUpdateMessage.put("UserName", user_name.getText().toString());
                            userUpdateMessage.put("UserOccupation", user_occupation.getText().toString());
                            userUpdateMessage.put("UserEmail", user_email.getText().toString());
                            EntryStackSingleton.getInstance().getEntryStack().add(new Entry(sharedPreferences.getString("UserID","Err"),userUpdateMessage));
                            UserProfileActivity.this.startService(new Intent(UserProfileActivity.this,PushEntryStackService.class));

                            Toast.makeText(UserProfileActivity.this,"Profile updated",Toast.LENGTH_SHORT).show();
                        }catch(JSONException e){
                            Log.e(TAG, "JSONException: " + e.getMessage());
                        }

                        finish();
                    }else{
                        Log.e(TAG,"User Profile is null");
                    }
                    break;
                case R.id.user_profile_reset:
                    user_name.setText("");
                    user_occupation.setText("");
                    user_email.setText("");
                    break;
                case R.id.user_profile_exit:
                    finish();
                    break;

            }

        }
    };

}
