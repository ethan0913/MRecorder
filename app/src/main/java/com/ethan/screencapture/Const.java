package com.ethan.screencapture;

public class Const {
    public static final long SECOND_IN_MILLIS = 1000;
    public static final long MINUTE_IN_MILLIS = SECOND_IN_MILLIS * 60;
    public static final int VIDEO_EDIT_REQUEST_CODE = 1004;
    public static final int VIDEO_EDIT_RESULT_CODE = 1005;
    public static final String TAG = "SCREENRECORDER_LOG";
    public static final String TAG_ETHAN = "ETHAN";
    public static final String APPDIR = "screenrecorder";
    public static final String ALERT_EXTR_STORAGE_CB_KEY = "ext_dir_warn_donot_show_again";
    public static final String VIDEO_EDIT_URI_KEY = "edit_video";
    public static final int EXTDIR_REQUEST_CODE = 1000;
    public static final int AUDIO_REQUEST_CODE = 1001;
    public static final int SYSTEM_WINDOWS_CODE = 1002;
    public static final int SCREEN_RECORD_REQUEST_CODE = 1003;
    public static final int CAMERA_REQUEST_CODE = 1004;
    public static final String SCREEN_RECORDING_START = "com.lenovo.screenrecorder.services.action.startrecording";
    public static final String SCREEN_RECORDING_PAUSE = "com.lenovo.screenrecorder.services.action.pauserecording";
    public static final String SCREEN_RECORDING_RESUME = "com.lenovo.screenrecorder.services.action.resumerecording";
    public static final String SCREEN_RECORDING_STOP = "com.lenovo.screenrecorder.services.action.stoprecording";
    public static final String SCREEN_RECORDING_DESTORY_SHAKE_GESTURE = "com.lenovo.screenrecorder.services.action.destoryshakegesture";
    public static final int SCREEN_RECORDER_NOTIFICATION_ID = 5001;
    public static final int SCREEN_RECORDER_SHARE_NOTIFICATION_ID = 5002;
    public static final int SCREEN_RECORDER_WAITING_FOR_SHAKE_NOTIFICATION_ID = 5003;
    public static final String RECORDER_INTENT_DATA = "recorder_intent_data";
    public static final String RECORDER_INTENT_RESULT = "recorder_intent_result";
    public static final String RECORDING_NOTIFICATION_CHANNEL_ID = "recording_notification_channel_id1";
    public static final String SHARE_NOTIFICATION_CHANNEL_ID = "share_notification_channel_id1";
    public static final String RECORDING_NOTIFICATION_CHANNEL_NAME = "Persistent notification shown when recording screen or when waiting for shake gesture";
    public static final String SHARE_NOTIFICATION_CHANNEL_NAME = "Notification shown to share or edit the recorded video";
    public static final String ANALYTICS_URL = "https://analytics.orpheusdroid.com";
    public static final String ANALYTICS_API_KEY = "07273a5c91f8a932685be1e3ad0d160d3de6d4ba";

    public static final String PREFS_REQUEST_ANALYTICS_PERMISSION = "request_analytics_permission";
    public static final String PREFS_LIGHT_THEME = "light_theme";
    public static final String PREFS_DARK_THEME = "dark_theme";
    public static final String PREFS_BLACK_THEME = "black_theme";
    public static final int PHOTO_EDIT_REQUEST_CODE = 1008;
    public static final String INTENT_PAGE_CODE ="intent_page_code";
    public static final String PAINT_SHOW_CLOSE = "com.lenovo.FloatBallService.action.PAINT_SHOW_CLOSE";
    public static final String REGION_LOCATION ="region_location";
    public enum RecordingState {
        RECORDING, PAUSED, RESUME,STOPPED
    }

    public enum analytics {
        CRASHREPORTING, USAGESTATS
    }
}
