package com.ethan.screencapture.activity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.ImageView;

import com.ethan.screencapture.Const;
import com.ethan.screencapture.GlobalScreenShot;
import com.ethan.screencapture.R;
import com.ethan.screencapture.application.ScreenCaptureApplication;
import com.ethan.screencapture.util.ActivityStackManager;

public class PreviewPictureActivity extends FragmentActivity implements GlobalScreenShot.onScreenShotListener {

    public static final Intent newIntent(Context context) {
        Intent intent = new Intent(context, PreviewPictureActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

  private ImageView mPreviewImageView;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityStackManager.getActivityManager().addActivity(this);
        setContentView(R.layout.activity_preview_layout);
        mPreviewImageView = (ImageView) findViewById(R.id.preview_image);

    GlobalScreenShot screenshot = new GlobalScreenShot(getApplicationContext());

    Bitmap bitmap = ((ScreenCaptureApplication) getApplication()).getmScreenCaptureBitmap();


    mPreviewImageView.setImageBitmap(bitmap);
    mPreviewImageView.setVisibility(View.GONE);

    if (bitmap != null) {
      screenshot.takeScreenshot(bitmap, this, true, true);
    }

  }

  @Override
  public void onStartShot() {

  }

  @Override
  public void onFinishShot(boolean success) {
    mPreviewImageView.setVisibility(View.VISIBLE);
  }

  @Override
  public void onBackPressed() {
    super.onBackPressed();
    Intent intent = new Intent(PreviewPictureActivity.this,MainActivity.class);
    intent.putExtra(Const.INTENT_PAGE_CODE,2);
    startActivity(intent);
  }
}
