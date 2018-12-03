package com.ethan.screencapture.adapter;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

/**
 * Created by wxl on 02-12-2018.
 */

public class Apps implements Comparable<Apps> {
    private String appName;
    private String packageName;
    private Drawable appIcon;
    private boolean isSelectedApp;

    public Apps(String appName, String packageName, Drawable appIcon) {
        this.appName = appName;
        this.packageName = packageName;
        this.appIcon = appIcon;
    }

    String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    Drawable getAppIcon() {
        return appIcon;
    }

    public void setAppIcon(Drawable appIcon) {
        this.appIcon = appIcon;
    }

    boolean isSelectedApp() {
        return isSelectedApp;
    }

    public void setSelectedApp(boolean selectedApp) {
        isSelectedApp = selectedApp;
    }

    @Override
    public int compareTo(@NonNull Apps apps) {
        return appName.compareTo(apps.appName);
    }
}
