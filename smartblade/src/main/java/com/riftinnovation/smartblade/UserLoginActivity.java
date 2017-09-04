package com.riftinnovation.smartblade;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

/**
 * Created by ajrunhaar on 8/29/2017.
 */

public class UserLoginActivity extends Activity {

    public static final String PREFS_NAME = "SmartBladeSharePreferences";
    public static final String TAG = "LoginActivity";
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    EditText user_name;
    EditText user_email;

    Button mLoginButton;

    Boolean validEntry;

    View decorView;
    int uiOptions =View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = getSharedPreferences(PREFS_NAME,0);
        editor = sharedPreferences.edit();

        setContentView(R.layout.activity_user_login);
        decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(uiOptions);

        mLoginButton = (Button) findViewById(R.id.btn_login);
        mLoginButton.setOnClickListener(mOnClickListener);

        user_name = (EditText) findViewById(R.id.input_name);
        user_email = (EditText) findViewById(R.id.input_email);



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
                case R.id.btn_login:
                    validEntry=true;
                    if(user_name.getText().toString()==""){
                        Toast.makeText(UserLoginActivity.this,"Please enter you name",Toast.LENGTH_SHORT).show();
                        validEntry=false;
                    }

                    if(!isEmailValid(user_email.getText().toString())){
                        Toast.makeText(UserLoginActivity.this,"Please enter a valid email address", Toast.LENGTH_SHORT).show();
                        validEntry=false;
                    }

                    if(validEntry){
                        editor.putString("UserID", UUID.randomUUID().toString());
                        editor.putString("user_name", user_name.getText().toString());
                        editor.putString("user_email", user_email.getText().toString());
                        editor.putBoolean("account_valid",true);
                        editor.commit();
                        JSONObject userProfileUpdate = new JSONObject();
                        try {
                            userProfileUpdate.put("Code", "UpdateID");
                            userProfileUpdate.put("UserID", sharedPreferences.getString("UserID","Err"));
                            userProfileUpdate.put("UserName", sharedPreferences.getString("user_name","Err"));
                            userProfileUpdate.put("UserEmail", sharedPreferences.getString("user_email","Err"));
                            EntryStackSingleton.getInstance().getEntryStack().add(new Entry(sharedPreferences.getString("UserID","Err"),userProfileUpdate));
                            UserLoginActivity.this.startService(new Intent(UserLoginActivity.this,PushEntryStackService.class));
                        }catch(JSONException e){
                            Log.e(TAG, "JSONException: " + e.getMessage());
                        }

                        finish();

                    }
            }
        }
    };

    public boolean isEmailValid(String email)
    {

        CharSequence inputStr = email;
        Log.d(TAG,inputStr.toString());
        if (inputStr == null){
            return false;
        }else{
            return Patterns.EMAIL_ADDRESS.matcher(inputStr).matches();
        }

    }
}