package com.ethan.screencapture.application;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Environment;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Created by wxl on 02-12-2018.
 */
public class ScreenCaptureApplication extends Application {

  private static String savePath;
  private Bitmap mScreenCaptureBitmap;

  @Override
  public void onCreate() {
    super.onCreate();
    choseSavePath();
    copyFilesFassets(getApplicationContext(), "Ress", savePath);
    ImageLoader.getInstance().init(ImageLoaderConfiguration.createDefault(this));
  }


  public Bitmap getmScreenCaptureBitmap() {
    return mScreenCaptureBitmap;
  }

  public void setmScreenCaptureBitmap(Bitmap mScreenCaptureBitmap) {
    this.mScreenCaptureBitmap = mScreenCaptureBitmap;
  }

  private void choseSavePath() {
    PackageManager pm = this.getPackageManager();
    String appName = getApplicationInfo().loadLabel(pm).toString();
    savePath = Environment.getExternalStorageDirectory().getPath() + "/"+appName+"/";
    File file = new File(savePath);
    if (!file.exists()) {
      file.mkdirs();
    }
  }

  /**
   * 获取缓存路径
   *
   * @return
   */
  public static String getSavePath() {
    return savePath;
  }

  /**
   * 从assets目录中复制文件到本地
   *
   * @param context Context
   * @param oldPath String  原文件路径
   * @param newPath String  复制后路径
   */
  public static void copyFilesFassets(Context context, String oldPath, String newPath) {
    try {
      String fileNames[] = context.getAssets().list(oldPath);
      if (fileNames.length > 0) {
        File file = new File(newPath);
        file.mkdirs();
        for (String fileName : fileNames) {
          copyFilesFassets(context, oldPath + "/" + fileName, newPath + "/" + fileName);
        }
      } else {
        InputStream is = context.getAssets().open(oldPath);
        File ff = new File(newPath);
        if (!ff.exists()) {
          FileOutputStream fos = new FileOutputStream(ff);
          byte[] buffer = new byte[1024];
          int byteCount = 0;
          while ((byteCount = is.read(buffer)) != -1) {
            fos.write(buffer, 0, byteCount);
          }
          fos.flush();
          is.close();
          fos.close();
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
