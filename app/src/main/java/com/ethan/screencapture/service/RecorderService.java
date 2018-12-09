package com.ethan.screencapture.service;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.FileProvider;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

import com.huxq17.floatball.libarary.floatball.FloatBall;
import com.ethan.screencapture.Const;
import com.ethan.screencapture.R;
import com.ethan.screencapture.activity.EditVideoActivity;
import com.ethan.screencapture.activity.MainActivity;
import com.ethan.screencapture.application.ScreenCaptureApplication;
import com.ethan.screencapture.gesture.ShakeEventManager;
import com.ethan.screencapture.view.ScreenShotFinishedDialog;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import VideoHandle.EpEditor;
import VideoHandle.EpVideo;
import VideoHandle.OnEditorListener;

/**
 * Created by wxl on 02-12-2018.
 */
//TODO: Update icons for notifcation
public class RecorderService extends Service implements ShakeEventManager.ShakeListener {
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static int WIDTH, HEIGHT, FPS, DENSITY_DPI;
    private static int BITRATE;
    private static boolean mustRecAudio;
    private static String SAVEPATH;
    private static String saveLocation;
    private static boolean isWaterMark;
    private static boolean isRegionScreen;
    private ProgressDialog mProgressDialog;

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    public static boolean isRecording;
    private boolean useFloatingControls;
    private boolean showTouches;
    private FloatBallService floatBallService;
    private boolean isBound = true;
    private NotificationManager mNotificationManager;
    Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message message) {
            Toast.makeText(RecorderService.this, R.string.screen_recording_stopped_toast, Toast.LENGTH_SHORT).show();
            String path = message.getData().getString("Path");
            Log.d("wxl","---handleMessage-------path--------"+path);
            if (path != null && !"".equals(path)){
                showShareNotification(path);
                Uri uri = Uri.fromFile(new File(path));
                indexFile(path);
                showResultDialog(uri,path);
            }else {
                showShareNotification(SAVEPATH);
                Uri uri = Uri.fromFile(new File(SAVEPATH));
                showResultDialog(uri,SAVEPATH);
            }

        }
    };

    private void showResultDialog(Uri uri, String path) {
        ScreenShotFinishedDialog dialog = new ScreenShotFinishedDialog(
                RecorderService.this, R.style.resultdialog, uri,path);
        dialog.setSavedFileName(getString(
                R.string.notification_recording_finished_text,
                uri.getPath()));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        }else {
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_PHONE);
        }
        dialog.show();
    }

    private ShakeEventManager mShakeDetector;
    private Intent data;
    private int result;
    //Service connection to manage the connection state between this service and the bounded service
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            //Get the service instance
            FloatBallService.ServiceBinder binder = (FloatBallService.ServiceBinder) service;
            floatBallService = binder.getService();
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            floatBallService = null;
            isBound = false;
        }
    };

    private long startTime, elapsedTime = 0;
    private SharedPreferences prefs;
    private WindowManager window;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaProjectionCallback mMediaProjectionCallback;
    private MediaRecorder mMediaRecorder;
    public static final int STATE_RECORDING = 1;
    public static final int STATE_RESUME = 2;
    public static final int STATE_PAUSE = 3;
    public static final int STATE_IDLE = 0;

    //add by lizh16 2018/4/4 RecorderStateChangeListener
    public interface RecorderStateChangeListener {
        void onRecorderStateChange(Const.RecordingState state);
    }

    public static RecorderStateChangeListener mRecorderStateChangeListener = null;

    public static void setOnRecorderStateChangeListener(RecorderStateChangeListener listener) {
        mRecorderStateChangeListener = listener;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createNotificationChannels();


        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        //return super.onStartCommand(intent, flags, startId);
        //Find the action to perform from intent
        switch (intent.getAction()) {
            case Const.SCREEN_RECORDING_START:

                /* Wish MediaRecorder had a method isRecording() or similar. But, we are forced to
                 * manage the state ourself. Let's hope the request is honored.
                  * Request: https://code.google.com/p/android/issues/detail?id=800 */
                if (!isRecording) {
                    //Get values from Default SharedPreferences
                    getValues();
                    data = intent.getParcelableExtra(Const.RECORDER_INTENT_DATA);
                    result = intent.getIntExtra(Const.RECORDER_INTENT_RESULT, Activity.RESULT_OK);


                    // Check if an app has to be started before recording and start the app
                    if (prefs.getBoolean(getString(R.string.preference_enable_target_app_key), false))
                        startAppBeforeRecording(prefs.getString(getString(R.string.preference_app_chooser_key), "none"));

                    boolean isShakeGestureActive = prefs.getBoolean(getString(R.string.preference_shake_gesture_key), false);

                    if (isShakeGestureActive) {
                        //SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
                        mShakeDetector = new ShakeEventManager(this);
                        mShakeDetector.init(this);

                        Bitmap icon = BitmapFactory.decodeResource(getResources(),
                                R.mipmap.ic_launcher);

                        Intent destroyMediaRecorderIntent = new Intent(this, RecorderService.class);
                        destroyMediaRecorderIntent.setAction(Const.SCREEN_RECORDING_DESTORY_SHAKE_GESTURE);
                        PendingIntent pdestroyMediaRecorderIntent = PendingIntent.getService(this, 0, destroyMediaRecorderIntent, 0);

                        NotificationCompat.Builder shakeGestureWaitNotification =
                                new NotificationCompat.Builder(this, Const.RECORDING_NOTIFICATION_CHANNEL_ID)
                                        .setContentTitle("Waiting for device shake")
                                        .setContentText("Shake your device to start recording or press this notification to cancel")
                                        .setOngoing(true)
                                        .setSmallIcon(R.drawable.ic_notification)
                                        .setLargeIcon(
                                                Bitmap.createScaledBitmap(icon, 128, 128, false))
                                        .setContentIntent(pdestroyMediaRecorderIntent);

                        startNotificationForeGround(shakeGestureWaitNotification.build(), Const.SCREEN_RECORDER_SHARE_NOTIFICATION_ID);

                        Toast.makeText(this, R.string.screenrecording_waiting_for_gesture_toast,
                                Toast.LENGTH_LONG).show();
                    } else {
                        startRecording();
                    }

                } else {
                    Toast.makeText(this, R.string.screenrecording_already_active_toast, Toast.LENGTH_SHORT).show();
                }
                break;
            case Const.SCREEN_RECORDING_PAUSE:
                pauseScreenRecording();
                break;
            case Const.SCREEN_RECORDING_RESUME:
                resumeScreenRecording();
                break;
            case Const.SCREEN_RECORDING_STOP:
                //Unbind the floating control service if its bound (naturally unbound if floating controls is disabled)
                /*if (isBound) {
                    unbindService(serviceConnection);
                    Log.d(Const.TAG, "Unbinding connection service");
                }*/

                stopScreenSharing();

                //Send a broadcast receiver to the plugin app to disable show touches since the recording is stopped
                if (showTouches) {
                    Intent TouchIntent = new Intent();
                    TouchIntent.setAction("com.orpheusdroid.screenrecorder.DISABLETOUCH");
                    TouchIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                    sendBroadcast(TouchIntent);
                }

                //The service is started as foreground service and hence has to be stopped
                stopForeground(true);
                break;
            case Const.SCREEN_RECORDING_DESTORY_SHAKE_GESTURE:
                mShakeDetector.stop();
                stopSelf();
                break;
        }
        return START_STICKY;
    }

    // Start the selected app before recording if its enabled and an app is selected
    private void startAppBeforeRecording(String packagename) {
        if (packagename.equals("none"))
            return;

        Intent startAppIntent = getPackageManager().getLaunchIntentForPackage(packagename);
        startActivity(startAppIntent);
    }

    @TargetApi(24)
    private void pauseScreenRecording() {
        mMediaRecorder.pause();
        //calculate total elapsed time until pause
        elapsedTime += (System.currentTimeMillis() - startTime);

        //Set Resume action to Notification and update the current notification
        Intent recordResumeIntent = new Intent(this, RecorderService.class);
        recordResumeIntent.setAction(Const.SCREEN_RECORDING_RESUME);
        PendingIntent precordResumeIntent = PendingIntent.getService(this, 0, recordResumeIntent, 0);
        NotificationCompat.Action action = new NotificationCompat.Action(android.R.drawable.ic_media_play,
                getString(R.string.screen_recording_notification_action_resume), precordResumeIntent);
        updateNotification(createRecordingNotification(action).setUsesChronometer(false).build(), Const.SCREEN_RECORDER_NOTIFICATION_ID);
        Toast.makeText(this, R.string.screen_recording_paused_toast, Toast.LENGTH_SHORT).show();
        if (mRecorderStateChangeListener != null) {
            mRecorderStateChangeListener.onRecorderStateChange(Const.RecordingState.PAUSED);
        }
        if (isBound)
            FloatBallService.setRecordingState(Const.RecordingState.PAUSED);

        //Send a broadcast receiver to the plugin app to disable show touches since the recording is paused
        if (showTouches) {
            Intent TouchIntent = new Intent();
            TouchIntent.setAction("com.orpheusdroid.screenrecorder.DISABLETOUCH");
            TouchIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            sendBroadcast(TouchIntent);
        }
        FloatBall.pauseTimer();
        if (isBound)
            FloatBallService.pauseTimerCount();
    }

    @TargetApi(24)
    private void resumeScreenRecording() {
        mMediaRecorder.resume();
        //Reset startTime to current time again
        startTime = System.currentTimeMillis();

        //set Pause action to Notification and update current Notification
        Intent recordPauseIntent = new Intent(this, RecorderService.class);
        recordPauseIntent.setAction(Const.SCREEN_RECORDING_PAUSE);
        PendingIntent precordPauseIntent = PendingIntent.getService(this, 0, recordPauseIntent, 0);
        NotificationCompat.Action action = new NotificationCompat.Action(android.R.drawable.ic_media_pause,
                getString(R.string.screen_recording_notification_action_pause), precordPauseIntent);
        updateNotification(createRecordingNotification(action).setUsesChronometer(true)
                .setWhen((System.currentTimeMillis() - elapsedTime)).build(), Const.SCREEN_RECORDER_NOTIFICATION_ID);
        Toast.makeText(this, R.string.screen_recording_resumed_toast, Toast.LENGTH_SHORT).show();
        if (mRecorderStateChangeListener != null) {
            mRecorderStateChangeListener.onRecorderStateChange(Const.RecordingState.RESUME);
        }
        if (isBound)
            FloatBallService.setRecordingState(Const.RecordingState.RECORDING);


        //Send a broadcast receiver to the plugin app to enable show touches since the recording is resumed
        if (showTouches) {
            if (showTouches) {
                Intent TouchIntent = new Intent();
                TouchIntent.setAction("com.orpheusdroid.screenrecorder.SHOWTOUCH");
                TouchIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                sendBroadcast(TouchIntent);
            }
        }
        FloatBall.startTimer();
        if (isBound)
            FloatBallService.startTimerCount();
    }

    private void startRecording() {
        //Initialize MediaRecorder class and initialize it with preferred configuration
        mMediaRecorder = new MediaRecorder();
        initRecorder();

        //Set Callback for MediaProjection
        mMediaProjectionCallback = new MediaProjectionCallback();
        MediaProjectionManager mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        //Initialize MediaProjection using data received from Intent
        mMediaProjection = mProjectionManager.getMediaProjection(result, data);

        mMediaProjection.registerCallback(mMediaProjectionCallback, null);

        /* Create a new virtual display with the actual default display
                 * and pass it on to MediaRecorder to start recording */
        mVirtualDisplay = createVirtualDisplay();
        try {
            mMediaRecorder.start();

            //If floating controls is enabled, start the floating control service and bind it here
           /* if (useFloatingControls) {
                Intent floatinControlsIntent = new Intent(this, FloatingControlService.class);
                startService(floatinControlsIntent);
               *//* bindService(floatinControlsIntent,
                        serviceConnection, BIND_AUTO_CREATE);*//*
            }*/

            //Set the state of the recording
            if (isBound)
                FloatBallService.setRecordingState(Const.RecordingState.RECORDING);
            isRecording = true;
            if (mRecorderStateChangeListener != null) {
                mRecorderStateChangeListener.onRecorderStateChange(Const.RecordingState.RECORDING);
            }

            //Send a broadcast receiver to the plugin app to enable show touches since the recording is started
            if (showTouches) {
                Intent TouchIntent = new Intent();
                TouchIntent.setAction("com.orpheusdroid.screenrecorder.SHOWTOUCH");
                TouchIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
                sendBroadcast(TouchIntent);
            }
            Toast.makeText(this, R.string.screen_recording_started_toast, Toast.LENGTH_SHORT).show();
        } catch (IllegalStateException e) {
            Log.d(Const.TAG, "Mediarecorder reached Illegal state exception. Did you start the recording twice?");
            Toast.makeText(this, R.string.recording_failed_toast, Toast.LENGTH_SHORT).show();
            isRecording = false;
        }

                /* Add Pause action to Notification to pause screen recording if the user's android version
                 * is >= Nougat(API 24) since pause() isnt available previous to API24 else build
                 * Notification with only default stop() action */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //startTime is to calculate elapsed recording time to update notification during pause/resume
            startTime = System.currentTimeMillis();
            Intent recordPauseIntent = new Intent(this, RecorderService.class);
            recordPauseIntent.setAction(Const.SCREEN_RECORDING_PAUSE);
            PendingIntent precordPauseIntent = PendingIntent.getService(this, 0, recordPauseIntent, 0);
            NotificationCompat.Action action = new NotificationCompat.Action(android.R.drawable.ic_media_pause,
                    getString(R.string.screen_recording_notification_action_pause), precordPauseIntent);

            //Start Notification as foreground
            startNotificationForeGround(createRecordingNotification(action).build(), Const.SCREEN_RECORDER_NOTIFICATION_ID);
        } else
            startNotificationForeGround(createRecordingNotification(null).build(), Const.SCREEN_RECORDER_NOTIFICATION_ID);
        FloatBall.startTimer();
        if (isBound)
            FloatBallService.startTimerCount();
    }

    //Virtual display created by mirroring the actual physical display
    private VirtualDisplay createVirtualDisplay() {
        return mMediaProjection.createVirtualDisplay("MainActivity",
                WIDTH, HEIGHT, DENSITY_DPI,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mMediaRecorder.getSurface(), null /*Callbacks*/, null
                /*Handler*/);
    }

    /* Initialize MediaRecorder with desired default values and values set by user. Everything is
     * pretty much self explanatory */
    private void initRecorder() {
        try {
            if (mustRecAudio)
                mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mMediaRecorder.setOutputFile(SAVEPATH);
            mMediaRecorder.setVideoSize(WIDTH, HEIGHT);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            if (mustRecAudio)
                mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mMediaRecorder.setVideoEncodingBitRate(BITRATE);
            mMediaRecorder.setVideoFrameRate(FPS);
            int rotation = window.getDefaultDisplay().getRotation();
            int orientation = ORIENTATIONS.get(rotation + 90);
            mMediaRecorder.setOrientationHint(orientation);
            mMediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Add notification channel for supporting Notification in Api 26 (Oreo)
    @TargetApi(26)
    private void createNotificationChannels() {
        List<NotificationChannel> notificationChannels = new ArrayList<>();
        NotificationChannel recordingNotificationChannel = new NotificationChannel(
                Const.RECORDING_NOTIFICATION_CHANNEL_ID,
                Const.RECORDING_NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
        );
        recordingNotificationChannel.enableLights(true);
        recordingNotificationChannel.setLightColor(Color.RED);
        recordingNotificationChannel.setShowBadge(true);
        recordingNotificationChannel.enableVibration(true);
        recordingNotificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        notificationChannels.add(recordingNotificationChannel);

        NotificationChannel shareNotificationChannel = new NotificationChannel(
                Const.SHARE_NOTIFICATION_CHANNEL_ID,
                Const.SHARE_NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
        );
        shareNotificationChannel.enableLights(true);
        shareNotificationChannel.setLightColor(Color.RED);
        shareNotificationChannel.setShowBadge(true);
        shareNotificationChannel.enableVibration(true);
        shareNotificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        notificationChannels.add(shareNotificationChannel);

        getManager().createNotificationChannels(notificationChannels);
    }

    /* Create Notification.Builder with action passed in case user's android version is greater than
     * API24 */
    private NotificationCompat.Builder createRecordingNotification(NotificationCompat.Action action) {
        Bitmap icon = BitmapFactory.decodeResource(getResources(),
                R.mipmap.ic_launcher);

        Intent recordStopIntent = new Intent(this, RecorderService.class);
        recordStopIntent.setAction(Const.SCREEN_RECORDING_STOP);
        PendingIntent precordStopIntent = PendingIntent.getService(this, 0, recordStopIntent, 0);

        Intent UIIntent = new Intent(this, MainActivity.class);
        PendingIntent notificationContentIntent = PendingIntent.getActivity(this, 0, UIIntent, 0);

        NotificationCompat.Builder notification = new NotificationCompat.Builder(this, Const.RECORDING_NOTIFICATION_CHANNEL_ID)
                .setContentTitle(getResources().getString(R.string.screen_recording_notification_title))
                .setTicker(getResources().getString(R.string.screen_recording_notification_title))
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(
                        Bitmap.createScaledBitmap(icon, 128, 128, false))
                .setUsesChronometer(true)
                .setOngoing(true)
                .setContentIntent(notificationContentIntent)
                .setPriority(NotificationManager.IMPORTANCE_DEFAULT)
                .addAction(R.drawable.ic_notification_stop, getResources().getString(R.string.screen_recording_notification_action_stop),
                        precordStopIntent);
        if (action != null)
            notification.addAction(action);
        return notification;
    }

    private void showShareNotification(String path) {
        Bitmap icon = BitmapFactory.decodeResource(getResources(),
                R.mipmap.ic_launcher);

        Uri videoUri = FileProvider.getUriForFile(
                this, this.getApplicationContext().getPackageName() + ".provider",
                new File(path));

        Intent Shareintent = new Intent()
                .setAction(Intent.ACTION_SEND)
                .putExtra(Intent.EXTRA_STREAM, videoUri)
                .setType("video/mp4");

        Intent editIntent = new Intent(this, EditVideoActivity.class);
        editIntent.putExtra(Const.VIDEO_EDIT_URI_KEY, path);
        PendingIntent editPendingIntent = PendingIntent.getActivity(this, 0, editIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent sharePendingIntent = PendingIntent.getActivity(this, 0, Intent.createChooser(
                Shareintent, getString(R.string.share_intent_title)), PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder shareNotification = new NotificationCompat.Builder(this, Const.SHARE_NOTIFICATION_CHANNEL_ID)
                .setContentTitle(getString(R.string.share_intent_notification_title))
                .setContentText(getString(R.string.share_intent_notification_content))
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                .setAutoCancel(true)
                .setContentIntent(sharePendingIntent)
                .addAction(android.R.drawable.ic_menu_share, getString(R.string.share_intent_notification_action_text)
                        , sharePendingIntent)
                .addAction(android.R.drawable.ic_menu_edit, getString(R.string.edit_intent_notification_action_text)
                        , editPendingIntent);
        updateNotification(shareNotification.build(), Const.SCREEN_RECORDER_SHARE_NOTIFICATION_ID);
    }

    //Start service as a foreground service. We dont want the service to be killed in case of low memory
    private void startNotificationForeGround(Notification notification, int ID) {
        startForeground(ID, notification);
    }

    //Update existing notification with its ID and new Notification data
    private void updateNotification(Notification notification, int ID) {
        getManager().notify(ID, notification);
    }

    private NotificationManager getManager() {
        if (mNotificationManager == null) {
            mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return mNotificationManager;
    }

    @Override
    public void onDestroy() {
        Log.d(Const.TAG, "Recorder service destroyed");
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    //Get user's choices for user choosable settings
    public void getValues() {
        String res = prefs.getString(getString(R.string.res_key), getResolution());
        setWidthHeight(res);
        //added by wangxl
        isWaterMark = prefs.getBoolean(getString(R.string.preference_watermark_key),false);
        isRegionScreen = prefs.getBoolean(getString(R.string.preference_partscreen_key),false);
        Log.d("Wxl","isWaterMark----------"+isWaterMark);
        FPS = Integer.parseInt(prefs.getString(getString(R.string.fps_key), "30"));
        BITRATE = Integer.parseInt(prefs.getString(getString(R.string.bitrate_key), "7130317"));
        mustRecAudio = prefs.getBoolean(getString(R.string.audiorec_key), false);
        saveLocation = prefs.getString(getString(R.string.savelocation_key),
                Environment.getExternalStorageDirectory() + File.separator + Const.APPDIR);
        File saveDir = new File(saveLocation);
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) && !saveDir.isDirectory()) {
            saveDir.mkdirs();
        }
        useFloatingControls = prefs.getBoolean(getString(R.string.preference_floating_control_key), false);
        showTouches = prefs.getBoolean(getString(R.string.preference_show_touch_key), false);
        String saveFileName = getFileSaveName();
        SAVEPATH = saveLocation + File.separator + saveFileName + ".mp4";
    }

    /* The PreferenceScreen save values as string and we save the user selected video resolution as
    * WIDTH x HEIGHT. Lets split the string on 'x' and retrieve width and height */
    private void setWidthHeight(String res) {
//        String[] widthHeight = res.split("x");
//        WIDTH = Integer.parseInt(widthHeight[0]);
//        HEIGHT = Integer.parseInt(widthHeight[1]);
        DisplayMetrics metrics = new DisplayMetrics();
        window = (WindowManager)this.getSystemService(Context.WINDOW_SERVICE);
        window.getDefaultDisplay().getRealMetrics(metrics);
        WIDTH = metrics.widthPixels;
        HEIGHT = metrics.heightPixels;
    }

    //Get the device resolution in pixels
    private String getResolution() {
        DisplayMetrics metrics = new DisplayMetrics();
        window = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        window.getDefaultDisplay().getMetrics(metrics);
        DENSITY_DPI = metrics.densityDpi;
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        return width + "x" + height;
    }

    //Return filename of the video to be saved formatted as chosen by the user
    private String getFileSaveName() {
        String filename = prefs.getString(getString(R.string.filename_key), "yyyyMMdd_hhmmss");
        String prefix = prefs.getString(getString(R.string.fileprefix_key), "recording");
        Date today = Calendar.getInstance().getTime();
        SimpleDateFormat formatter = new SimpleDateFormat(filename);
        return prefix + "_" + formatter.format(today);
    }

    //Stop and destroy all the objects used for screen recording
    private void destroyMediaProjection() {
        try {
            mMediaRecorder.stop();

            Log.i(Const.TAG, "MediaProjection Stopped");
        } catch (RuntimeException e) {
            Log.e(Const.TAG, "Fatal exception! Destroying media projection failed." + "\n" + e.getMessage());
            if (new File(SAVEPATH).delete())
                Log.d(Const.TAG, "Corrupted file delete successful");
            Toast.makeText(this, getString(R.string.fatal_exception_message), Toast.LENGTH_SHORT).show();
        } finally {
            mMediaRecorder.reset();
            mVirtualDisplay.release();
            mMediaRecorder.release();
            if (mMediaProjection != null) {
                mMediaProjection.unregisterCallback(mMediaProjectionCallback);
                mMediaProjection.stop();
                mMediaProjection = null;
            }
            if (isWaterMark || isRegionScreen){
                editVideo(SAVEPATH);
            }else {
                indexFile(SAVEPATH);
            }

        }
        isRecording = false;
        if (mRecorderStateChangeListener != null) {
            mRecorderStateChangeListener.onRecorderStateChange(Const.RecordingState.STOPPED);
        }
    }

    /* Its weird that android does not index the files immediately once its created and that causes
     * trouble for user in finding the video in gallery. Let's explicitly announce the file creation
     * to android and index it */
    private void indexFile(String path) {
        //Create a new ArrayList and add the newly created video file path to it
        ArrayList<String> toBeScanned = new ArrayList<>();
        toBeScanned.add(path);
        String[] toBeScannedStr = new String[toBeScanned.size()];
        toBeScannedStr = toBeScanned.toArray(toBeScannedStr);

        //Request MediaScannerConnection to scan the new file and index it
        MediaScannerConnection.scanFile(this, toBeScannedStr, null, new MediaScannerConnection.OnScanCompletedListener() {

            @Override
            public void onScanCompleted(String path, Uri uri) {
                Log.i(Const.TAG, "SCAN COMPLETED: " + path);
                //Show toast on main thread
                if (!isWaterMark && !isRegionScreen){
                    Message message = mHandler.obtainMessage();
                    message.sendToTarget();
                    stopSelf();
                }
            }
        });
    }

    private void stopScreenSharing() {
        if (mVirtualDisplay == null) {
            Log.d(Const.TAG, "Virtual display is null. Screen sharing already stopped");
            return;
        }
        destroyMediaProjection();
        FloatBall.stopTimer();
        if(isBound)
            FloatBallService.stopTimerCount();
        FloatBallService.setRecordingState(Const.RecordingState.STOPPED);
    }

    @Override
    public void onShake() {
        if (!isRecording) {
            Vibrator vibrate = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

            getManager().cancel(Const.SCREEN_RECORDER_WAITING_FOR_SHAKE_NOTIFICATION_ID);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
                vibrate.vibrate(500);
            else
                VibrationEffect.createOneShot(500, 255);

            startRecording();
            Toast.makeText(this, "Rec start", Toast.LENGTH_SHORT).show();
        } else {
            Intent recordStopIntent = new Intent(this, RecorderService.class);
            recordStopIntent.setAction(Const.SCREEN_RECORDING_STOP);
            startService(recordStopIntent);
            Toast.makeText(this, "Rec stop", Toast.LENGTH_SHORT).show();
            mShakeDetector.stop();
        }
    }

    private class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            Log.v(Const.TAG, "Recording Stopped");
            stopScreenSharing();
        }
    }

    public void editVideo(String path){
        createProgressDialogView();
        mProgressDialog.setTitle("正在生成视频");
        if(path != null && !"".equals(path)){
            EpVideo epVideo = new EpVideo(path);
            if (isRegionScreen){
                Log.d("wxl","11111111111111");
                String partMsg = prefs.getString(Const.REGION_LOCATION,"0,0,200,200");
                String [] xywh = partMsg.split(",");
                epVideo.crop(Integer.parseInt(xywh[2]),Integer.parseInt(xywh[3]),Integer.parseInt(xywh[0]),Integer.parseInt(xywh[1]));
            }
            if (isWaterMark){
                Log.d("wxl","22222222222222");
                String  waterMarkText = "Lenovo";
                epVideo.addText(50,50,30,"red", ScreenCaptureApplication.getSavePath() + "msyh.ttf",waterMarkText.trim());
            }

            Log.d("wxl","xxxxxxxx----addWaterWark");
            String fileName = getFileSaveName();
            mProgressDialog.setProgress(0);
            mProgressDialog.show();
            final String outPath = saveLocation + File.separator + fileName + ".mp4";
            EpEditor.exec(epVideo, new EpEditor.OutputOption(outPath), new OnEditorListener() {
                @Override
                public void onSuccess() {
                    mProgressDialog.dismiss();
                    Message message = mHandler.obtainMessage();
                    Bundle bundle = new Bundle();
                    bundle.putString("Path",outPath);  //往Bundle中存放数据
                    message.setData(bundle);
                    message.sendToTarget();
                    deleteFile(SAVEPATH);
                    stopSelf();
                }

                @Override
                public void onFailure() {
                    mProgressDialog.dismiss();
                }

                @Override
                public void onProgress(float v) {
                    mProgressDialog.setProgress((int) (v * 100));
                }
            });
        }
    }
    // 如果文件路径所对应的文件存在，并且是一个文件，则直接删除
    public  boolean deleteFile(String fileName) {
        File file = new File(fileName);
        if (file.exists() && file.isFile()) {
            if (file.delete()) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
    private void createProgressDialogView(){
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setMax(100);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setCanceledOnTouchOutside(false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mProgressDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        }else {
            mProgressDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_PHONE);
        }
    }
}
