package com.riftinnovation.smartblade;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.android.internal.telephony.ITelephony;
import com.serenegiant.common.BaseActivity;

import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.DeviceFilter;
import com.serenegiant.usbcameracommon.UVCCameraHandler;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.USBMonitor.OnDeviceConnectListener;
import com.serenegiant.usb.USBMonitor.UsbControlBlock;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.widget.CameraViewInterface;
import com.serenegiant.widget.TestInterface;
import com.serenegiant.widget.UVCCameraTextureView;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;


public final class MainActivity extends BaseActivity implements CameraDialog.CameraDialogParent,TestInterface {
    private static final boolean DEBUG = true;	// TODO set false on release
    private static final String TAG = "MainActivity";

    /**
     * set true if you want to record movie using MediaSurfaceEncoder
     * (writing frame data into Surface camera from MediaCodec
     *  by almost same way as USBCameratest2)
     * set false if you want to record movie using MediaVideoEncoder
     */
    private static final boolean USE_SURFACE_ENCODER = false;

    /**
     * preview resolution(width)
     * if your camera does not support specific resolution and mode,
     * {@link UVCCamera#setPreviewSize(int, int, int)} throw exception
     */
    private static final int PREVIEW_WIDTH = 640;
    /**
     * preview resolution(height)
     * if your camera does not support specific resolution and mode,
     * {@link UVCCamera#setPreviewSize(int, int, int)} throw exception
     */
    private static final int PREVIEW_HEIGHT = 480;
    /**
     * preview mode
     * if your camera does not support specific resolution and mode,
     * {@link UVCCamera#setPreviewSize(int, int, int)} throw exception
     * 0:YUYV, other:MJPEG
     */
    private static final int PREVIEW_MODE = 1;

    protected static final int SETTINGS_HIDE_DELAY_MS = 2500;

    /**
     * for accessing USB
     */
    private USBMonitor mUSBMonitor;
    /**
     * Handler to execute camera related methods sequentially on private thread
     */
    private UVCCameraHandler mCameraHandler;
    /**
     * for camera preview display
     */
    private CameraViewInterface mUVCCameraView;
    private ImageButton mCameraButton,mVideoButton,mPhoneButton,mFeedbackButton;
    private ImageButton mCloseButton,mMoreButton,mUserButton,mSettingsButton,mGalleryButton;
    private ImageButton mRotateLeftButton,mRotateRightButton;
    private ImageButton mLayoutLeftButton,mLayoutCenterButton,mLayoutRightButton;

    private FrameLayout r11,r12,r13,r14,r21,r22,r23,r24;
    private FrameLayout l11,l12,l13,l14,l21,l22,l23,l24;
    private FrameLayout c11,c12,c13,c14,c21,c22,c23,c24,c31,c32,c33,c34;

    ArrayList<View> menuButtons;
    ArrayList<View> settingsButtons;

    int layoutOption = 0;
    private float previewRotation = 0;

    private boolean menu_open = false;
    private boolean settings_open = false;

    private View activeView;

    private ImageView previewBorder;

    Boolean capturePreviewAvailable = true;

    View decorView;
    int uiOptions =View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;

    public static final String PREFS_NAME = "SmartBladeSharePreferences";
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    public static final int RESULT_GALLERY = 0;

    public static final int MY_PERMISSIONS_REQUEST_CALL_PHONE = 1;

    RelativeLayout brandingOverlay;

    RectF rect;

    Matrix rotationMatrix;

    public final String ACTION_USB_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";

    private final BroadcastReceiver screenReceiver = new ScreenReceiver();


    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG) Log.v(TAG, "onCreate:");
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        final View view = findViewById(R.id.camera_view);
        view.setOnLongClickListener(mOnLongClickListener);
        mUVCCameraView = (CameraViewInterface)view;
        mUVCCameraView.setAspectRatio(PREVIEW_WIDTH / (float)PREVIEW_HEIGHT);
        mUVCCameraView.setTestInterfaceCallback(this);

        mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);
        mCameraHandler = UVCCameraHandler.createHandler(this, mUVCCameraView,
                USE_SURFACE_ENCODER ? 0 : 1, PREVIEW_WIDTH, PREVIEW_HEIGHT, PREVIEW_MODE);

        /*=======Initialise shared preferences and parameters =======*/
        sharedPreferences = getSharedPreferences(PREFS_NAME, 0);
        editor = sharedPreferences.edit();

        previewRotation = sharedPreferences.getFloat("preview_rotation", 0);
        layoutOption = sharedPreferences.getInt("layout_option", 0);

        /*============Programatically reqeust permission. Result to : onRequestPermissionsResult========*/
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.CALL_PHONE},
                MY_PERMISSIONS_REQUEST_CALL_PHONE);

        StateListener phoneStateListener = new StateListener();
        TelephonyManager telephonymanager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        telephonymanager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

        /*============Power button protection========*/
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenReceiver, filter);

        /*======= Hide Top Panel =======*/

        decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(uiOptions);

        /*=======Initialise UI =======*/

        brandingOverlay = (RelativeLayout) findViewById(R.id.mainScreenOverlay);
        //brandingOverlay.setAlpha(0.0f);

        setBrandingOverlay(true);

        previewBorder = (ImageView) findViewById(R.id.preview_border);

        CreateButtons();

        SetLayoutIDs();

        CreateLayouts();

        /*=======Initialise SessionStatistics with context only on Create =======*///ToDo: Add the context to the PushEntry Stack Service instead.

        SessionStatisticsSingleton.getInstance(this);



    }

    @Override
    public void testCallbackFunction() {/*
        Log.d(TAG,"TestCallback - Surface created!");

        if(mUSBMonitor!=null){
            final List<DeviceFilter> filter = DeviceFilter.getDeviceFilters(MainActivity.this, com.serenegiant.uvccamera.R.xml.device_filter);
            Log.v(TAG,"Device List: " + mUSBMonitor.getDeviceList().toString());
            List<UsbDevice> mDeviceList = mUSBMonitor.getDeviceList(filter.get(0));
            if(!mDeviceList.isEmpty()) {
                Toast imageCaptured = Toast.makeText(MainActivity.this, "Connect from surface created callback", Toast.LENGTH_SHORT);
                if (mDeviceList.get(0) instanceof UsbDevice) {

                    mUSBMonitor.checkUSB();
                }
            }else{
                Log.d(TAG,"No device connected");
            }
        }else{
            Log.e(TAG, "USBMonitor is null");
        }*/

    }

    private void setBrandingOverlay(boolean visibility){
        final boolean _visibility = visibility;
        runOnUiThread(new Runnable() { //TODO: Move this to a standalone function. It is repeated a few times.
            @Override
            public void run() {

                if(_visibility){
                    float alpha = 1.0f;
                    brandingOverlay.setAlpha(alpha);
                }else{
                    float alpha = 0.0f;
                    brandingOverlay.setAlpha(alpha);
                }
            }
        });
    }

    boolean tmpLatch = true;

    @Override
    protected void onResume() {
        super.onResume();

        SessionStatisticsSingleton.getInstance().startSessionTimer();

        if (DEBUG) Log.v(TAG, "onResume:");
        decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(uiOptions);


        if(mUVCCameraView.hasSurface()){

            Log.e(TAG, "Reconnected while activity visible");
            Toast imageCaptured = Toast.makeText(MainActivity.this, "Reconnected while activity visible", Toast.LENGTH_SHORT);
            //Log.v(TAG, "Device List: " + mUSBMonitor.getDeviceList().toString());
            final List<DeviceFilter> filter = DeviceFilter.getDeviceFilters(MainActivity.this, com.serenegiant.uvccamera.R.xml.device_filter);
            List<UsbDevice> mDeviceList = mUSBMonitor.getDeviceList(filter.get(0));
            if (!mDeviceList.isEmpty()) {
                if(tmpLatch) {
                    Log.v(TAG, "Device List Not Empty: " + mUSBMonitor.getDeviceList(filter.get(0)));
                    Log.e(TAG, mDeviceList.toString());
                    if (mDeviceList.get(0) instanceof UsbDevice) {
                        //mUSBMonitor.requestPermission(mDeviceList.get(0));
                        mUSBMonitor.checkUSB();
                    }
                    tmpLatch = false;
                }
            } else {
                Log.v(TAG, "Device List Empty");
            }


        }else{
            Log.e(TAG, "hasSurface() = " + mUVCCameraView.hasSurface() + " | isActivityVisible() = " +isActivityVisible());
        }

        this.activityResumed();

    }

    @Override
    protected void onStart() {
        if (DEBUG) Log.v(TAG, "onStart:");

        /*======= Hide Top Panel =======*/

        if(!sharedPreferences.getBoolean("account_valid",false)){
            MainActivity.this.startActivity(new Intent(MainActivity.this,UserLoginActivity.class));

        }else{
            Log.d(TAG, "User valid");
        }

        mUSBMonitor.register();

        if (mUVCCameraView != null) {
            mUVCCameraView.onResume();
        }

        super.onStart();
    }

    @Override
    protected void onStop() {
        if (DEBUG) Log.v(TAG, "onStop:");
        mCameraHandler.close();
        if (mUVCCameraView != null)
            mUVCCameraView.onPause();
        super.onStop();
        this.activityStopped();
    }

    @Override
    protected void onPause() {
        if (DEBUG) Log.v(TAG, "onPause:");

        SessionStatisticsSingleton.getInstance().stopSessionTimer();

        super.onPause();

    }

    @Override
    public void onDestroy() {
        if (DEBUG) Log.v(TAG, "onDestroy:");
        if (mCameraHandler != null) {
            mCameraHandler.release();
            mCameraHandler = null;
        }
        if (mUSBMonitor != null) {

            mUSBMonitor.destroy();
            mUSBMonitor = null;
        }
        mUVCCameraView = null;
        super.onDestroy();
    }

    /**
     * event handler when click camera / capture button
     */
    private final OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick(final View view) {
            switch (view.getId()) {
                case R.id.imagebutton_camera:
                    if (mCameraHandler.isOpened() && capturePreviewAvailable && !mCameraHandler.isRecording()) {
                        if (checkPermissionWriteExternalStorage()) {
                            mCameraButton.setImageResource(R.mipmap.ic_sri_camera_pressed);
                            previewBorder.setImageResource(R.drawable.border_imagecapture_1);
                            capturePreviewAvailable = false;
                            mCameraHandler.captureStill();


                            final Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    mCameraButton.setImageResource(R.mipmap.ic_sri_camera);
                                    previewBorder.setImageResource(R.drawable.border_transparent);
                                    final Toast imageCaptured = Toast.makeText(MainActivity.this, "Image Captured", Toast.LENGTH_SHORT);
                                    imageCaptured.show();
                                    Handler handler = new Handler();
                                    handler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            imageCaptured.cancel();
                                        }
                                    }, 1000);
                                    capturePreviewAvailable = true;
                                }
                            }, 1000);
                            SessionStatisticsSingleton.getInstance().incrementImageCounter();
                        }
                    }
                    break;
                case R.id.imagebutton_video:
                    if (mCameraHandler.isOpened() && capturePreviewAvailable) {
                        if (checkPermissionWriteExternalStorage()) {
                            if (!mCameraHandler.isRecording()) {
                                mVideoButton.setImageResource(R.mipmap.ic_sri_video_pressed);
                                previewBorder.setImageResource(R.drawable.border_video_capture);
                                mCameraHandler.startRecording();
                                final Toast startRecording = Toast.makeText(MainActivity.this, "Recording Started", Toast.LENGTH_SHORT);
                                startRecording.show();
                                Handler handler = new Handler();
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        startRecording.cancel();
                                    }
                                }, 1000);
                                SessionStatisticsSingleton.getInstance().startRecordTimer();
                            } else {
                                mVideoButton.setImageResource(R.mipmap.ic_sri_video);	// return to default color
                                previewBorder.setImageResource(R.drawable.border_transparent);
                                mCameraHandler.stopRecording();
                                final Toast stopRecording = Toast.makeText(MainActivity.this, "Recording Stopped", Toast.LENGTH_SHORT);
                                stopRecording.show();
                                Handler handler = new Handler();
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        stopRecording.cancel();
                                    }
                                }, 1000);
                                SessionStatisticsSingleton.getInstance().stopRecordTimer();
                                SessionStatisticsSingleton.getInstance().incrementVideoCounter();
                            }
                        }
                    }else{
                        Toast.makeText(MainActivity.this, "SmartBlade not connected", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case R.id.imagebutton_phone:
                    Toast.makeText(MainActivity.this,"Video Call : Future Feature", Toast.LENGTH_SHORT).show();
                    break;
                case R.id.imagebutton_message:
                    Intent message_intent = new Intent(MainActivity.this,UserFeedbackActivity.class);
                    startActivity(message_intent);
                    break;
                case R.id.imagebutton_user:
                    Intent user_intent = new Intent(MainActivity.this,UserProfileActivity.class);
                    startActivity(user_intent);
                    break;
                case R.id.imagebutton_gallery:
                    Intent galleryIntent = new Intent(
                            Intent.ACTION_PICK,
                            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    galleryIntent.setType("image/* video/*");
                    startActivityForResult(galleryIntent , RESULT_GALLERY );
                    break;
                case R.id.imagebutton_settings:

                    settings_open = toggleButtonVisibility(settingsButtons,settings_open);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(settings_open){
                                mSettingsButton.setImageResource(R.mipmap.ic_sri_settings_pressed);
                            }else{
                                mSettingsButton.setImageResource(R.mipmap.ic_sri_settings);
                            }
                        }
                    });

                    break;
                case R.id.imagebutton_rotateleft:
                    previewRotation -=1;
                    if(previewRotation==-1){
                        previewRotation=359;
                    }
                    mUVCCameraView.setCustomRotation(previewRotation);


                    break;
                case R.id.imagebutton_rotateright:
                    previewRotation +=1;
                    if(previewRotation==360){
                        previewRotation=0;
                    }
                    mUVCCameraView.setCustomRotation(previewRotation);

                    break;
                case R.id.imagebutton_arrowleft:
                    if(layoutOption!=0){
                        layoutOption=0;
                        CreateLayouts();
                        menu_open = toggleButtonVisibility(menuButtons,true);
                        settings_open = toggleButtonVisibility(settingsButtons,true);
                        runOnUiThread(new Runnable() { //TODO: Move this to a standalone function. It is repeated a few times.
                            @Override
                            public void run() {

                                if(menu_open){
                                    mMoreButton.setImageResource(R.mipmap.ic_sri_more_pressed);
                                }else{
                                    mMoreButton.setImageResource(R.mipmap.ic_sri_more);
                                    mSettingsButton.setImageResource(R.mipmap.ic_sri_settings);
                                }
                            }
                        });
                        editor.putInt("layout_option",layoutOption);
                        editor.commit();
                    }else{
                        Toast.makeText(getApplicationContext(),"Layout is currently set to left",Toast.LENGTH_SHORT).show();
                    }
                    break;
                case R.id.imagebutton_arrowdown:
                    if(layoutOption!=2){
                        layoutOption=2;
                        CreateLayouts();
                        menu_open = toggleButtonVisibility(menuButtons,true);
                        settings_open = toggleButtonVisibility(settingsButtons,true);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                if(menu_open){
                                    mMoreButton.setImageResource(R.mipmap.ic_sri_more_pressed);
                                }else{
                                    mMoreButton.setImageResource(R.mipmap.ic_sri_more);
                                    mSettingsButton.setImageResource(R.mipmap.ic_sri_settings);
                                }
                            }
                        });

                        editor.putInt("layout_option",layoutOption);
                        editor.commit();
                    }else{
                        Toast.makeText(getApplicationContext(),"Layout is currently set to center",Toast.LENGTH_SHORT).show();
                    }
                    break;
                case R.id.imagebutton_arrowright:
                    if(layoutOption!=1){
                        layoutOption=1;
                        CreateLayouts();
                        menu_open = toggleButtonVisibility(menuButtons,true);
                        settings_open = toggleButtonVisibility(settingsButtons,true);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                                if(menu_open){
                                    mMoreButton.setImageResource(R.mipmap.ic_sri_more_pressed);
                                }else{
                                    mMoreButton.setImageResource(R.mipmap.ic_sri_more);
                                    mSettingsButton.setImageResource(R.mipmap.ic_sri_settings);
                                }
                            }
                        });

                        editor.putInt("layout_option",layoutOption);
                        editor.commit();
                    }else{
                        Toast.makeText(getApplicationContext(),"Layout is currently set to right",Toast.LENGTH_SHORT).show();
                    }
                    break;
                case R.id.imagebutton_more:

                    menu_open = toggleButtonVisibility(menuButtons,menu_open);
                    settings_open = toggleButtonVisibility(settingsButtons,true);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            if(menu_open){
                                mMoreButton.setImageResource(R.mipmap.ic_sri_more_pressed);
                            }else{
                                mMoreButton.setImageResource(R.mipmap.ic_sri_more);
                                mSettingsButton.setImageResource(R.mipmap.ic_sri_settings);
                            }
                        }
                    });


                    break;

            }
        }
    };


    /**
     * capture still image when you long click on preview image(not on buttons)
     */
    private final OnLongClickListener mOnLongClickListener = new OnLongClickListener() {
        @Override
        public boolean onLongClick(final View view) {
            switch (view.getId()) {

                case R.id.imagebutton_close:
                    /*Close the application by starting the home screen, as the home button would.
                    * Do not merely finish, or kill the process, let the garbage collector do that.
                        * This also keeps the application in memory for quick startup*/
                    menu_open = toggleButtonVisibility(menuButtons,true);
                    settings_open = toggleButtonVisibility(settingsButtons,true);
                    if(menu_open){
                        mMoreButton.setImageResource(R.mipmap.ic_sri_more_pressed);
                    }else{
                        mMoreButton.setImageResource(R.mipmap.ic_sri_more);
                        mSettingsButton.setImageResource(R.mipmap.ic_sri_settings);
                    }
                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.addCategory(Intent.CATEGORY_HOME);
                    startActivity(intent);
                    break;

            }
            return false;
        }
    };

    private boolean toggleButtonVisibility(final ArrayList<View> buttonArray,final boolean visbilityStatus) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(!visbilityStatus) {

                    for (View imageButton : buttonArray) {
                        imageButton.setVisibility(View.VISIBLE);
                        imageButton.animate()
                                .alpha(1.0f)
                                .setDuration(300)
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);

                                    }
                                });
                    }

                }else{

                    for (View imageButton : buttonArray) {
                        activeView = imageButton;
                        imageButton.animate()
                                .alpha(0.0f)
                                .setDuration(300)
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        super.onAnimationEnd(animation);
                                        activeView.setVisibility(View.GONE);
                                    }
                                });
                    }

                }
            }
        }, 0);
        updateItems();
        return !visbilityStatus;
    }

    private void startPreview() {
        final SurfaceTexture st = mUVCCameraView.getSurfaceTexture();
        mCameraHandler.startPreview(new Surface(st));
    }


    private static boolean activityVisible;
    public static boolean isActivityVisible() {
        return activityVisible;
    }
    public static void activityResumed() {
        activityVisible = true;
    }
    public static void activityStopped() {
        activityVisible = false;
    }




    private boolean isAppOnForeground(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        if (appProcesses == null) {
            return false;
        }
        final String packageName = context.getPackageName();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName.equals(packageName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }

    private final OnDeviceConnectListener mOnDeviceConnectListener = new OnDeviceConnectListener() {
        @Override
        public void onAttach(final UsbDevice device) {
            Toast.makeText(MainActivity.this, "USB_DEVICE_ATTACHED", Toast.LENGTH_SHORT).show();
            /*
            if(!isActivityVisible()) {
                startActivity(new Intent(MainActivity.this, MainActivity.class));
            }*/

/*
            //if(isActivityVisible()){
                final List<DeviceFilter> filter = DeviceFilter.getDeviceFilters(MainActivity.this, com.serenegiant.uvccamera.R.xml.device_filter);
                List<UsbDevice> mDeviceList = mUSBMonitor.getDeviceList(filter.get(0));
                if(!mDeviceList.isEmpty()) {
                    Log.e(TAG, mDeviceList.toString());
                    if (mDeviceList.get(0) instanceof UsbDevice) {
                        mUSBMonitor.requestPermission(mDeviceList.get(0));

                    }
                }
            //}else{
                //Toast.makeText(MainActivity.this, "Please Navigate to SmartBlade Main Page", Toast.LENGTH_SHORT).show();
            //}
*/

        }

        @Override
        public void onConnect(final UsbDevice device, final UsbControlBlock ctrlBlock, final boolean createNew) {
            if (DEBUG) Log.v(TAG, "onConnect:");
            mCameraHandler.open(ctrlBlock);
            startPreview();
            updateItems();
            setBrandingOverlay(false);
            SessionStatisticsSingleton.getInstance().startPreviewTimer();
        }

        @Override
        public void onDisconnect(final UsbDevice device, final UsbControlBlock ctrlBlock) {
            if (DEBUG) Log.v(TAG, "onDisconnect:");
            if (mCameraHandler != null) {
                queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        mCameraHandler.close();
                    }
                }, 0);
                updateItems();
            }
            setBrandingOverlay(true);
            tmpLatch = true;
            SessionStatisticsSingleton.getInstance().stopPreviewTimer();
        }
        @Override
        public void onDettach(final UsbDevice device) {
            Toast.makeText(MainActivity.this, "SmartBlade Disconnected", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCancel(final UsbDevice device) {

        }
    };

    private final CompoundButton.OnCheckedChangeListener mOnCheckedChangeListener
            = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(final CompoundButton compoundButton, final boolean isChecked) {
            switch (compoundButton.getId()) {

            }
        }
    };

    /**
     * to access from CameraDialog
     * @return
     */
    @Override
    public USBMonitor getUSBMonitor() {
        return mUSBMonitor;
    }

    @Override
    public void onDialogResult(boolean canceled) {
        if (DEBUG) Log.v(TAG, "onDialogResult:canceled=" + canceled);
        if (canceled) {

        }
    }
    //================================================================================
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode,data);
        if(resultCode == this.RESULT_OK) {
            if(requestCode==RESULT_GALLERY) {
                Uri fileUri = data.getData();
                Log.d(TAG,"Filepath:" + fileUri);

                if(fileUri.toString().contains("/video/")){
                    Intent videoPlayerIntent = new Intent(MainActivity.this, VideoPlayerActivity.class);
                    videoPlayerIntent.putExtra("mUriString",fileUri.toString());
                    MainActivity.this.startActivity(videoPlayerIntent);
                }else if(fileUri.toString().contains("/images/")){
                    Intent imagePlayerIntent = new Intent(MainActivity.this, ImagePlayerActivity.class);
                    imagePlayerIntent.putExtra("mUriString",fileUri.toString());
                    MainActivity.this.startActivity(imagePlayerIntent);
                }

            }

        }
    }

    //================================================================================


    private void CreateButtons(){

        mMoreButton = (ImageButton) getLayoutInflater().inflate(R.layout.menu_imagebutton,null);
        mMoreButton.setImageResource(R.mipmap.ic_sri_more);
        mMoreButton.setId(R.id.imagebutton_more);
        mMoreButton.setOnClickListener(mOnClickListener);

        mCameraButton = (ImageButton) getLayoutInflater().inflate(R.layout.menu_imagebutton,null);
        mCameraButton.setImageResource(R.mipmap.ic_sri_camera);
        mCameraButton.setId(R.id.imagebutton_camera);
        mCameraButton.setOnClickListener(mOnClickListener);

        mVideoButton = (ImageButton) getLayoutInflater().inflate(R.layout.menu_imagebutton,null);
        mVideoButton.setImageResource(R.mipmap.ic_sri_video);
        mVideoButton.setId(R.id.imagebutton_video);
        mVideoButton.setOnClickListener(mOnClickListener);

        mPhoneButton = (ImageButton) getLayoutInflater().inflate(R.layout.menu_imagebutton,null);
        mPhoneButton.setImageResource(R.mipmap.ic_sri_phone);
        mPhoneButton.setId(R.id.imagebutton_phone);
        mPhoneButton.setOnClickListener(mOnClickListener);


        mFeedbackButton = (ImageButton) getLayoutInflater().inflate(R.layout.menu_imagebutton,null);
        mFeedbackButton.setImageResource(R.mipmap.ic_sri_message);
        mFeedbackButton.setId(R.id.imagebutton_message);
        mFeedbackButton.setOnClickListener(mOnClickListener);
        mFeedbackButton.setAlpha(0.0f);
        mFeedbackButton.setVisibility(View.GONE);

        mUserButton = (ImageButton) getLayoutInflater().inflate(R.layout.menu_imagebutton,null);
        mUserButton.setImageResource(R.mipmap.ic_sri_user);
        mUserButton.setId(R.id.imagebutton_user);
        mUserButton.setOnClickListener(mOnClickListener);
        mUserButton.setAlpha(0.0f);
        mUserButton.setVisibility(View.GONE);

        mSettingsButton = (ImageButton) getLayoutInflater().inflate(R.layout.menu_imagebutton,null);
        mSettingsButton.setImageResource(R.mipmap.ic_sri_settings);
        mSettingsButton.setId(R.id.imagebutton_settings);
        mSettingsButton.setOnClickListener(mOnClickListener);
        mSettingsButton.setAlpha(0.0f);
        mSettingsButton.setVisibility(View.GONE);

        mGalleryButton = (ImageButton) getLayoutInflater().inflate(R.layout.menu_imagebutton,null);
        mGalleryButton.setImageResource(R.mipmap.ic_sri_files);
        mGalleryButton.setId(R.id.imagebutton_gallery);
        mGalleryButton.setOnClickListener(mOnClickListener);
        mGalleryButton.setAlpha(0.0f);
        mGalleryButton.setVisibility(View.GONE);

        mCloseButton = (ImageButton) getLayoutInflater().inflate(R.layout.menu_imagebutton,null);
        mCloseButton.setImageResource(R.mipmap.ic_sri_close);
        mCloseButton.setId(R.id.imagebutton_close);
        mCloseButton.setOnLongClickListener(mOnLongClickListener);
        mCloseButton.setAlpha(0.0f);
        mCloseButton.setVisibility(View.GONE);

        mRotateLeftButton = (ImageButton) getLayoutInflater().inflate(R.layout.menu_imagebutton,null);
        mRotateLeftButton.setImageResource(R.mipmap.ic_sri_arrowccw);
        mRotateLeftButton.setId(R.id.imagebutton_rotateleft);
        mRotateLeftButton.setOnClickListener(mOnClickListener);
        mRotateLeftButton.setAlpha(0.0f);
        mRotateLeftButton.setVisibility(View.GONE);

        mRotateRightButton = (ImageButton) getLayoutInflater().inflate(R.layout.menu_imagebutton,null);
        mRotateRightButton.setImageResource(R.mipmap.ic_sri_arrowcw);
        mRotateRightButton.setId(R.id.imagebutton_rotateright);
        mRotateRightButton.setOnClickListener(mOnClickListener);
        mRotateRightButton.setAlpha(0.0f);
        mRotateRightButton.setVisibility(View.GONE);

        mLayoutLeftButton = (ImageButton) getLayoutInflater().inflate(R.layout.menu_imagebutton,null);
        mLayoutLeftButton.setImageResource(R.mipmap.ic_sri_arrowleft);
        mLayoutLeftButton.setId(R.id.imagebutton_arrowleft);
        mLayoutLeftButton.setOnClickListener(mOnClickListener);
        mLayoutLeftButton.setAlpha(0.0f);
        mLayoutLeftButton.setVisibility(View.GONE);

        mLayoutRightButton = (ImageButton) getLayoutInflater().inflate(R.layout.menu_imagebutton,null);
        mLayoutRightButton.setImageResource(R.mipmap.ic_sri_arrorright);
        mLayoutRightButton.setId(R.id.imagebutton_arrowright);
        mLayoutRightButton.setOnClickListener(mOnClickListener);
        mLayoutRightButton.setAlpha(0.0f);
        mLayoutRightButton.setVisibility(View.GONE);

        mLayoutCenterButton = (ImageButton) getLayoutInflater().inflate(R.layout.menu_imagebutton,null);
        mLayoutCenterButton.setImageResource(R.mipmap.ic_sri_arrowdown);
        mLayoutCenterButton.setId(R.id.imagebutton_arrowdown);
        mLayoutCenterButton.setOnClickListener(mOnClickListener);
        mLayoutCenterButton.setAlpha(0.0f);
        mLayoutCenterButton.setVisibility(View.GONE);

        menuButtons = new ArrayList<View>();
        menuButtons.add(mFeedbackButton);
        menuButtons.add(mUserButton);
        menuButtons.add(mGalleryButton);
        menuButtons.add(mCloseButton);
        menuButtons.add(mSettingsButton);

        settingsButtons = new ArrayList<View>();
        settingsButtons.add(mRotateLeftButton);
        settingsButtons.add(mRotateRightButton);
        settingsButtons.add(mLayoutLeftButton);
        settingsButtons.add(mLayoutCenterButton);
        settingsButtons.add(mLayoutRightButton);

    }
    private void SetLayoutIDs(){
        l11 = (FrameLayout) findViewById(R.id.l11);
        l12 = (FrameLayout) findViewById(R.id.l12);
        l13 = (FrameLayout) findViewById(R.id.l13);
        l14 = (FrameLayout) findViewById(R.id.l14);

        l21 = (FrameLayout) findViewById(R.id.l21);
        l22 = (FrameLayout) findViewById(R.id.l22);
        l23 = (FrameLayout) findViewById(R.id.l23);
        l24 = (FrameLayout) findViewById(R.id.l24);

        c11 = (FrameLayout) findViewById(R.id.c11);
        c12 = (FrameLayout) findViewById(R.id.c12);
        c13 = (FrameLayout) findViewById(R.id.c13);
        c14 = (FrameLayout) findViewById(R.id.c14);

        c21 = (FrameLayout) findViewById(R.id.c21);
        c22 = (FrameLayout) findViewById(R.id.c22);
        c23 = (FrameLayout) findViewById(R.id.c23);
        c24 = (FrameLayout) findViewById(R.id.c24);

        c31 = (FrameLayout) findViewById(R.id.c31);
        c32 = (FrameLayout) findViewById(R.id.c32);
        c33 = (FrameLayout) findViewById(R.id.c33);
        c34 = (FrameLayout) findViewById(R.id.c34);

        r11 = (FrameLayout) findViewById(R.id.r11);
        r12 = (FrameLayout) findViewById(R.id.r12);
        r13 = (FrameLayout) findViewById(R.id.r13);
        r14 = (FrameLayout) findViewById(R.id.r14);

        r21 = (FrameLayout) findViewById(R.id.r21);
        r22 = (FrameLayout) findViewById(R.id.r22);
        r23 = (FrameLayout) findViewById(R.id.r23);
        r24 = (FrameLayout) findViewById(R.id.r24);
    }
    private void CreateLayouts(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {


                    l11.removeAllViews();
                    l12.removeAllViews();
                    l13.removeAllViews();
                    l14.removeAllViews();

                    l21.removeAllViews();
                    l22.removeAllViews();
                    l23.removeAllViews();
                    l24.removeAllViews();

                    r11.removeAllViews();
                    r12.removeAllViews();
                    r13.removeAllViews();
                    r14.removeAllViews();

                    r21.removeAllViews();
                    r22.removeAllViews();
                    r23.removeAllViews();
                    r24.removeAllViews();

                    c11.removeAllViews();
                    c12.removeAllViews();
                    c13.removeAllViews();
                    c14.removeAllViews();

                    c21.removeAllViews();
                    c22.removeAllViews();
                    c23.removeAllViews();
                    c24.removeAllViews();

                    c31.removeAllViews();
                    c32.removeAllViews();
                    c33.removeAllViews();
                    c34.removeAllViews();

                switch(layoutOption) {
                    case(0):

                        l11.addView(mMoreButton);

                        l12.addView(mPhoneButton);


                        l13.addView(mVideoButton);


                        l14.addView(mCameraButton);

            /*
            * Left column 2, secondary menu
            */


                        l21.addView(mUserButton);
                        mUserButton.setVisibility(View.GONE);


                        l22.addView(mFeedbackButton);
                        mFeedbackButton.setVisibility(View.GONE);


                        l23.addView(mGalleryButton);
                        mGalleryButton.setVisibility(View.GONE);


                        l24.addView(mSettingsButton);
                        mSettingsButton.setVisibility(View.GONE);

            /*
            * Right Menu, exit button
            */

                        r11.addView(mCloseButton);
                        mCloseButton.setVisibility(View.GONE);

                /*
                Center Menu, Setting buttons
                 */
                        c12.addView(mRotateLeftButton);
                        mRotateLeftButton.setVisibility(View.GONE);

                        c32.addView(mRotateRightButton);
                        mRotateRightButton.setVisibility(View.GONE);

                        c13.addView(mLayoutLeftButton);
                        mLayoutLeftButton.setVisibility(View.GONE);

                        c33.addView(mLayoutRightButton);
                        mLayoutRightButton.setVisibility(View.GONE);

                        c23.addView(mLayoutCenterButton);
                        mLayoutCenterButton.setVisibility(View.GONE);

                        break;
                    case(1):

                        r11.addView(mMoreButton);

                        r12.addView(mPhoneButton);

                        r13.addView(mVideoButton);


                        r14.addView(mCameraButton);

            /*
            * Right column 2, secondary menu
            */


                        r21.addView(mUserButton);
                        mUserButton.setVisibility(View.GONE);


                        r22.addView(mFeedbackButton);
                        mFeedbackButton.setVisibility(View.GONE);


                        r23.addView(mGalleryButton);
                        mGalleryButton.setVisibility(View.GONE);


                        r24.addView(mSettingsButton);
                        mSettingsButton.setVisibility(View.GONE);

            /*
            * Left Menu, exit button
            */

                        l11.addView(mCloseButton);
                        mCloseButton.setVisibility(View.GONE);

                                /*
                Center Menu, Setting buttons
                 */
                        c12.addView(mRotateLeftButton);
                        mRotateLeftButton.setVisibility(View.GONE);

                        c32.addView(mRotateRightButton);
                        mRotateRightButton.setVisibility(View.GONE);

                        c13.addView(mLayoutLeftButton);
                        mLayoutLeftButton.setVisibility(View.GONE);

                        c33.addView(mLayoutRightButton);
                        mLayoutRightButton.setVisibility(View.GONE);

                        c23.addView(mLayoutCenterButton);
                        mLayoutCenterButton.setVisibility(View.GONE);



                        break;
                    case(2):

                        l11.addView(mMoreButton);

                        l12.addView(mPhoneButton);

                        c34.addView(mVideoButton);


                        c14.addView(mCameraButton);

            /*
            * Left column 2, secondary menu
            */


                        l21.addView(mUserButton);
                        mUserButton.setVisibility(View.GONE);


                        l22.addView(mFeedbackButton);
                        mFeedbackButton.setVisibility(View.GONE);


                        l23.addView(mGalleryButton);
                        mGalleryButton.setVisibility(View.GONE);


                        l24.addView(mSettingsButton);
                        mSettingsButton.setVisibility(View.GONE);

            /*
            * Right Menu, exit button
            */

                        r11.addView(mCloseButton);
                        mCloseButton.setVisibility(View.GONE);

                                /*
                Center Menu, Setting buttons
                 */
                        c12.addView(mRotateLeftButton);
                        mRotateLeftButton.setVisibility(View.GONE);

                        c32.addView(mRotateRightButton);
                        mRotateRightButton.setVisibility(View.GONE);

                        c13.addView(mLayoutLeftButton);
                        mLayoutLeftButton.setVisibility(View.GONE);

                        c33.addView(mLayoutRightButton);
                        mLayoutRightButton.setVisibility(View.GONE);

                        c23.addView(mLayoutCenterButton);
                        mLayoutCenterButton.setVisibility(View.GONE);



                        break;

                }
            }
        });
        updateItems();


    }

    //================================================================================//
    private boolean isActive() {
        return mCameraHandler != null && mCameraHandler.isOpened();
    }

    private boolean checkSupportFlag(final int flag) {
        return mCameraHandler != null && mCameraHandler.checkSupportFlag(flag);
    }

    private int getValue(final int flag) {
        return mCameraHandler != null ? mCameraHandler.getValue(flag) : 0;
    }

    private int setValue(final int flag, final int value) {
        return mCameraHandler != null ? mCameraHandler.setValue(flag, value) : 0;
    }

    private int resetValue(final int flag) {
        return mCameraHandler != null ? mCameraHandler.resetValue(flag) : 0;
    }

    private void updateItems() {
        runOnUiThread(mUpdateItemsOnUITask, 100);
    }

    private final Runnable mUpdateItemsOnUITask = new Runnable() {
        @Override
        public void run() {
            if (isFinishing()) return;
            final int visible_active = isActive() ? View.VISIBLE : View.INVISIBLE;

        }
    };


    //================================================================================//

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        boolean result;
        switch( event.getKeyCode() ) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                result = true;
                break;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                result = true;
                break;

            default:
                result= super.dispatchKeyEvent(event);
                break;
        }

        return result;
    }

    @Override
    public void onBackPressed() {
        //No action to disable back button
    }


    class StateListener extends PhoneStateListener{
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            switch(state){
                case TelephonyManager.CALL_STATE_RINGING:
                    //Disconnect the call here...
                    Log.d(TAG, "Call State Ringing");
                    Toast.makeText(MainActivity.this,"Incoming Call Blocked", Toast.LENGTH_SHORT).show();
                    try{
                        TelephonyManager manager = (TelephonyManager)MainActivity.this.getSystemService(Context.TELEPHONY_SERVICE);
                        Class c = Class.forName(manager.getClass().getName());
                        Method m = c.getDeclaredMethod("getITelephony");
                        m.setAccessible(true);
                        ITelephony telephony = (ITelephony)m.invoke(manager);
                        telephony.endCall();
                    } catch(Exception e){
                        Log.d(TAG,e.getMessage());
                    }
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    break;
            }
        }
    };




}