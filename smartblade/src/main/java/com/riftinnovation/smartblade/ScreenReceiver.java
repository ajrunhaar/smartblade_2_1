package com.riftinnovation.smartblade;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.serenegiant.common.BaseActivity;

public class ScreenReceiver extends BroadcastReceiver {
    private String TAG = "ScreenReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            String action = intent.getAction();
            switch (action) {
                case Intent.ACTION_SCREEN_OFF:
                    BaseActivity.unlockScreen();
                    break;

                case Intent.ACTION_SCREEN_ON:
                    // and do whatever you need to do here
                    BaseActivity.clearScreen();
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception ScreenReceiver" + e);

        }
    }

}
