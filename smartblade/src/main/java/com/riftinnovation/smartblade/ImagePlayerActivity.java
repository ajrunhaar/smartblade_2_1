package com.riftinnovation.smartblade;

import android.app.Activity;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;

import java.io.InputStream;

import static android.content.ContentValues.TAG;

/**
 * Created by ajrunhaar on 8/28/2017.
 */

public class ImagePlayerActivity extends Activity {
    public static final String TAG = "ImagePlayerActivity";
    private ImageView mPreview;
    Bundle extras;

    View decorView;
    int uiOptions =View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_player);
        mPreview = (ImageView) findViewById(R.id.imageplayer_surface);

        extras = getIntent().getExtras();

        Uri imageUri = Uri.parse(extras.getString("mUriString"));
        if(imageUri!=null) {
            mPreview.setImageURI(imageUri);
        }else{
            Log.e(TAG, "imageUri is null");
        }
        decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(uiOptions);
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

}
