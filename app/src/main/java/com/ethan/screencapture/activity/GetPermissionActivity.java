package com.ethan.screencapture.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.ethan.screencapture.Const;
import com.ethan.screencapture.R;
import com.ethan.screencapture.application.ScreenCaptureApplication;
import com.ethan.screencapture.service.FloatBallService;
import com.ethan.screencapture.service.RecorderService;
import com.ethan.screencapture.util.ActivityStackManager;
import com.ethan.screencapture.util.FileUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class GetPermissionActivity extends Activity {
    public static final int REQUEST_MEDIA_RECORDER_CODE = 1;
    public static final int REQUEST_MEDIA_SCREENSHOT_CODE = 2;
    public static final int REQUEST_MEDIA_CAMERA_CODE = 3;
    public static final int REQUEST_MEDIA_AUDIO_CODE = 4;
    private MediaProjectionManager mMediaProjectionManager;
    //added by wangxl for shot
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private ImageReader mImageReader;
    private WindowManager mWindowManager;
    private int mScreenWidth;
    private int mScreenHeight;
    private int mScreenDensity;
    private static Intent mResultData = null;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityStackManager.getActivityManager().addActivity(this);
        setStatusBarTranparent();
        setContentView(R.layout.activity_empty);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mMediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (getIntent().getIntExtra("flag", 0) == REQUEST_MEDIA_RECORDER_CODE) {
            startActivityForResult(mMediaProjectionManager.createScreenCaptureIntent(), REQUEST_MEDIA_RECORDER_CODE);
        } else if (getIntent().getIntExtra("flag", 0) == REQUEST_MEDIA_SCREENSHOT_CODE) {
            startActivityForResult(mMediaProjectionManager.createScreenCaptureIntent(), REQUEST_MEDIA_SCREENSHOT_CODE);
        } else if (getIntent().getIntExtra("flag",0) == REQUEST_MEDIA_CAMERA_CODE){
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(GetPermissionActivity.this,
                        new String[]{Manifest.permission.CAMERA},
                        Const.CAMERA_REQUEST_CODE);
            }
        }else if (getIntent().getIntExtra("flag", 0) == REQUEST_MEDIA_AUDIO_CODE) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(GetPermissionActivity.this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        Const.AUDIO_REQUEST_CODE);
            }
        }
    }

    private void setStatusBarTranparent() {
        Window window = getWindow();
        //清除系统提供的默认保护色
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        //设置系统UI的显示方式
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        //添加属性可以自定义设置系统工具栏颜色
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(Color.TRANSPARENT);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == REQUEST_MEDIA_RECORDER_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                Intent recorderService = new Intent(this, RecorderService.class);
                recorderService.setAction(Const.SCREEN_RECORDING_START);
                recorderService.putExtra(Const.RECORDER_INTENT_DATA, resultData);
                recorderService.putExtra(Const.RECORDER_INTENT_RESULT, resultCode);
                startService(recorderService);

            }
        }
        //截屏的权限弹框
        if (requestCode == REQUEST_MEDIA_SCREENSHOT_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                mResultData =resultData;
                //截屏功能代码
                mWindowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
                DisplayMetrics metrics = new DisplayMetrics();
                mWindowManager.getDefaultDisplay().getRealMetrics(metrics);
                mScreenDensity = metrics.densityDpi;
                mScreenWidth = metrics.widthPixels;
                mScreenHeight = metrics.heightPixels;
                createImageReader();
                startScreenShot();
            }


        }
        if (resultCode == Activity.RESULT_CANCELED) {
            Toast.makeText(this,
                    getString(R.string.screen_recording_permission_denied), Toast.LENGTH_SHORT).show();
        }
        finish();
    }
    private void startScreenShot() {

        Handler handler1 = new Handler();
        handler1.postDelayed(new Runnable() {
            public void run() {
                //start virtual
                startVirtual();
            }
        }, 50);

        handler1.postDelayed(new Runnable() {
            public void run() {
                //capture the screen
                startCapture();

            }
        }, 30);
    }

    public void startVirtual() {
        if (mMediaProjection != null) {
            virtualDisplay();
        } else {
            setUpMediaProjection();
            virtualDisplay();
        }
    }
    private void startCapture() {

        Image image = mImageReader.acquireLatestImage();

        if (image == null) {
            startScreenShot();
        } else {
            SaveTask mSaveTask = new SaveTask();
            mSaveTask.execute(image);
//            AsyncTaskCompat.executeParallel(mSaveTask, image);
        }
    }
    private void virtualDisplay() {
        if (mMediaProjection == null){
            mMediaProjection = getMediaProjectionManager().getMediaProjection(RESULT_OK, mResultData);
        }
        mVirtualDisplay = mMediaProjection.createVirtualDisplay("screen-mirror",
                mScreenWidth, mScreenHeight, mScreenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mImageReader.getSurface(), null, null);
    }


    public void setUpMediaProjection() {
        if (mResultData == null) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            startActivity(intent);
        } else {
            mMediaProjection = getMediaProjectionManager().getMediaProjection(RESULT_OK, mResultData);
        }
    }

    private MediaProjectionManager getMediaProjectionManager() {

        return (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }

    public class SaveTask extends AsyncTask<Image, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(Image... params) {

            if (params == null || params.length < 1 || params[0] == null) {

                return null;
            }

            Image image = params[0];

            int width = image.getWidth();
            int height = image.getHeight();
            final Image.Plane[] planes = image.getPlanes();
            final ByteBuffer buffer = planes[0].getBuffer();
            //每个像素的间距
            int pixelStride = planes[0].getPixelStride();
            //总的间距
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * width;
            Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(buffer);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);
            image.close();
            File fileImage = null;
            if (bitmap != null) {
                try {
                    fileImage = new File(FileUtil.getScreenShotsName(getApplicationContext()));
                    if (!fileImage.exists()) {
                        fileImage.createNewFile();
                    }
                    FileOutputStream out = new FileOutputStream(fileImage);
                    if (out != null) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                        out.flush();
                        out.close();
                        Intent media = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        Uri contentUri = Uri.fromFile(fileImage);
                        media.setData(contentUri);
                        sendBroadcast(media);
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    fileImage = null;
                } catch (IOException e) {
                    e.printStackTrace();
                    fileImage = null;
                }
            }

            if (fileImage != null) {
                return bitmap;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            //预览图片
            if (bitmap != null) {

                ((ScreenCaptureApplication) getApplication()).setmScreenCaptureBitmap(bitmap);
                Log.e("ryze", "获取图片成功");
                startActivity(PreviewPictureActivity.newIntent(getApplicationContext()));
            }

        }
    }

    private void createImageReader() {

        mImageReader = ImageReader.newInstance(mScreenWidth, mScreenHeight, PixelFormat.RGBA_8888, 1);

    }

    private void tearDownMediaProjection() {
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
    }

    private void stopVirtual() {
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.release();
        mVirtualDisplay = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopVirtual();
        tearDownMediaProjection();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (prefs == null){
            prefs = PreferenceManager.getDefaultSharedPreferences(GetPermissionActivity.this);
        }
        switch (requestCode) {
            case Const.CAMERA_REQUEST_CODE:
                if ((grantResults.length > 0) &&
                        (grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
                    Log.d(Const.TAG, "write storage Permission Denied");
                    /* Disable floating action Button in case write storage permission is denied.
                     * There is no use in recording screen when the video is unable to be saved */
                    FloatBallService.mCameraSwitch.setChecked(false);
                } else {
                    /* Since we have write storage permission now, lets create the app directory
                    * in external storage*/
                    Log.d(Const.TAG, "write storage Permission granted");
                    Intent intent = new Intent("open_camera");
                    GetPermissionActivity.this.sendBroadcast(intent);
                    prefs.edit().putBoolean(getString(R.string.preference_camera_key),true).commit();
                }
                break;
            case Const.AUDIO_REQUEST_CODE:
                if ((grantResults.length > 0) &&
                        (grantResults[0] != PackageManager.PERMISSION_GRANTED)) {
                    Log.d(Const.TAG, "write storage Permission Denied");
                    /* Disable floating action Button in case write storage permission is denied.
                     * There is no use in recording screen when the video is unable to be saved */
                    FloatBallService.mRecordeVoice.setChecked(false);
                } else {
                    /* Since we have write storage permission now, lets create the app directory
                    * in external storage*/
                    Log.d(Const.TAG, "write storage Permission granted");
                    prefs.edit().putBoolean(getString(R.string.audiorec_key),true).commit();
                }
                break;
        }
    }
}
