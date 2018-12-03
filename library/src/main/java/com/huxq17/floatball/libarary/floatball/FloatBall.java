package com.huxq17.floatball.libarary.floatball;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.huxq17.floatball.libarary.FloatBallManager;
import com.huxq17.floatball.libarary.FloatBallUtil;
import com.huxq17.floatball.libarary.R;
import com.huxq17.floatball.libarary.runner.ICarrier;
import com.huxq17.floatball.libarary.runner.OnceRunnable;
import com.huxq17.floatball.libarary.runner.ScrollRunner;
import com.huxq17.floatball.libarary.utils.DensityUtil;
import com.huxq17.floatball.libarary.utils.MotionVelocityUtil;
import com.huxq17.floatball.libarary.utils.Util;


public class FloatBall extends FrameLayout implements ICarrier {

    private FloatBallManager floatBallManager;
    private ImageView imageView;
    private WindowManager.LayoutParams mLayoutParams;
    private WindowManager windowManager;
    private boolean isFirst = true;
    private boolean isAdded = false;
    private int mTouchSlop;
    /**
     * flag a touch is click event
     */
    private boolean isClick;
    private int mDownX, mDownY, mLastX, mLastY;
    private int mSize;
    private ScrollRunner mRunner;
    private int mVelocityX, mVelocityY;
    private MotionVelocityUtil mVelocity;
    private boolean sleep = false;
    private FloatBallCfg mConfig;
    private boolean mHideHalfLater = true;
    private boolean mLayoutChanged = false;
    private int mSleepX = -1;

    private View view;
    private static int second;

    private OnceRunnable mSleepRunnable = new OnceRunnable() {
        @Override
        public void onRun() {
            if (mHideHalfLater && !sleep && isAdded) {
                sleep = true;
                moveToEdge(false, sleep);
                mSleepX = mLayoutParams.x;
            }
        }
    };

    public FloatBall(Context context, FloatBallManager floatBallManager, FloatBallCfg config) {
        super(context);
        this.floatBallManager = floatBallManager;
        mConfig = config;
        init(context);
    }

    //格式转换为HH:MM:SS
    public static String FormatMiss(int second) {
        if (second == 0) {
            return "00:00";
        }
        String hh = second / 3600 > 9 ? second / 3600 + "" : "0" + second / 3600;
        String mm = (second % 3600) / 60 > 9 ? (second % 3600) / 60 + "" : "0" + (second % 3600) / 60;
        String ss = (second % 3600) % 60 > 9 ? (second % 3600) % 60 + "" : "0" + (second % 3600) % 60;
        if(second>3600){
            return hh+":"+mm+":"+ss;
        }
        return mm + ":" + ss;
    }

    public static void startTimer() {
        if (imageCamera != null) {
            imageCamera.setVisibility(View.GONE);
        }
        if (ch != null) {
            ch.setVisibility(View.VISIBLE);
            ch.start();
        }
    }

    public static void stopTimer() {

        if (ch != null) {
            ch.stop();
            second = 0;
            ch.setText(FormatMiss(second));
            ch.setVisibility(View.GONE);
        }
        if (imageCamera != null) {
            imageCamera.setVisibility(View.VISIBLE);
        }
    }

    public static void pauseTimer() {
        if (ch != null) {
            ch.stop();
            ch.setText(FormatMiss(second));
        }
    }

    private static Chronometer ch;
    private static ImageView imageCamera;

    private void init(Context context) {
        imageView = new ImageView(context);
        view = LayoutInflater.from(context).inflate(context.getResources().getLayout(R.layout.layout_view), null);
        ch = view.findViewById(R.id.chronometer);
        imageCamera = view.findViewById(R.id.image_camera);
        ch.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {
                chronometer.setText(FormatMiss(second));
                second++;
            }
        });
        final Drawable icon = mConfig.mIcon;
        mSize = mConfig.mSize;
        Util.setBackground(imageView, icon);
//        addView(imageView, new ViewGroup.LayoutParams(mSize, mSize));
        int ballSize = DensityUtil.dip2px(context, 45);
        addView(view, new ViewGroup.LayoutParams(ballSize, ballSize));
        initLayoutParams(context);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        mRunner = new ScrollRunner(this);
        mVelocity = new MotionVelocityUtil(context);
    }

    private void initLayoutParams(Context context) {
        mLayoutParams = FloatBallUtil.getLayoutParams(context);
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == VISIBLE) {
            onConfigurationChanged(null);
        }
    }

    public void attachToWindow(WindowManager windowManager) {
        this.windowManager = windowManager;
        if (!isAdded) {
            windowManager.addView(this, mLayoutParams);
            isAdded = true;
        }
    }

    public void detachFromWindow(WindowManager windowManager) {
        this.windowManager = null;
        if (isAdded) {
            removeSleepRunnable();
            if (getContext() instanceof Activity) {
                windowManager.removeViewImmediate(this);
            } else {
                windowManager.removeView(this);
            }
            isAdded = false;
            sleep = false;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int height = getMeasuredHeight();
        int width = getMeasuredWidth();

        int curX = mLayoutParams.x;
        if (sleep && curX != mSleepX && !mRunner.isRunning()) {
            sleep = false;
            postSleepRunnable();
        }
        if (mRunner.isRunning()) {
            mLayoutChanged = false;
        }
        if (height != 0 && isFirst || mLayoutChanged) {
            if (isFirst&&height != 0) {
                location(width, height);
            } else {
                moveToEdge(false, sleep);
            }
            isFirst = false;
            mLayoutChanged = false;
        }
    }

    private void location(int width, int height) {
        FloatBallCfg.Gravity cfgGravity = mConfig.mGravity;
        mHideHalfLater = mConfig.mHideHalfLater;
        int gravity = cfgGravity.getGravity();
        int x;
        int y;
        int topLimit = 0;
        int bottomLimit = floatBallManager.mScreenHeight - height;
        int statusBarHeight = floatBallManager.getStatusBarHeight();
        if ((gravity & Gravity.LEFT) == Gravity.LEFT) {
            x = 0;
        } else {
            x = floatBallManager.mScreenWidth - width;
        }
        if ((gravity & Gravity.TOP) == Gravity.TOP) {
            y = topLimit;
        } else if ((gravity & Gravity.BOTTOM) == Gravity.BOTTOM) {
            y = floatBallManager.mScreenHeight - height - statusBarHeight;
        } else {
            y = floatBallManager.mScreenHeight / 2 - height / 2 - statusBarHeight;
        }
        y = mConfig.mOffsetY != 0 ? y + mConfig.mOffsetY : y;
        if (y < 0) y = topLimit;
        if (y > bottomLimit)
            y = topLimit;
        onLocation(x, y);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mLayoutChanged = true;
        floatBallManager.onConfigurationChanged(newConfig);
        moveToEdge(false, false);
        postSleepRunnable();
    }

    public void onLayoutChange() {
        mLayoutChanged = true;
        requestLayout();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        int x = (int) event.getRawX();
        int y = (int) event.getRawY();
        mVelocity.acquireVelocityTracker(event);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                touchDown(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                touchMove(x, y);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                touchUp();
                break;
        }
        return super.onTouchEvent(event);
    }

    private void touchDown(int x, int y) {
        mDownX = x;
        mDownY = y;
        mLastX = mDownX;
        mLastY = mDownY;
        isClick = true;
        removeSleepRunnable();
    }

    private void touchMove(int x, int y) {
        int totalDeltaX = x - mDownX;
        int totalDeltaY = y - mDownY;
        int deltaX = x - mLastX;
        int deltaY = y - mLastY;
        if (Math.abs(totalDeltaX) > mTouchSlop || Math.abs(totalDeltaY) > mTouchSlop) {
            isClick = false;
        }
        mLastX = x;
        mLastY = y;
        if (!isClick) {
            onMove(deltaX, deltaY);
        }
    }

    private void touchUp() {
        mVelocity.computeCurrentVelocity();
        mVelocityX = (int) mVelocity.getXVelocity();
        mVelocityY = (int) mVelocity.getYVelocity();
        mVelocity.releaseVelocityTracker();
        if (sleep) {
            wakeUp();
        } else {
            if (isClick) {
                onClick();
            } else {
                moveToEdge(true, false);
            }
        }
        mVelocityX = 0;
        mVelocityY = 0;
    }

    private void moveToX(boolean smooth, int destX) {
        int statusBarHeight = floatBallManager.getStatusBarHeight();
        final int screenHeight = floatBallManager.mScreenHeight - statusBarHeight;
        int height = getHeight();
        int destY = 0;
        if (mLayoutParams.y < 0) {
            destY = 0 - mLayoutParams.y;
        } else if (mLayoutParams.y > screenHeight - height) {
            destY = screenHeight - height - mLayoutParams.y;
        }
        if (smooth) {
            int dx = destX - mLayoutParams.x;
            int duration = getScrollDuration(Math.abs(dx));
            mRunner.start(dx, destY, duration);
        } else {
            onMove(destX - mLayoutParams.x, destY);
            postSleepRunnable();
        }
    }

    private void wakeUp() {
        final int screenWidth = floatBallManager.mScreenWidth;
        int width = getWidth();
        int halfWidth = width / 2;
        int centerX = (screenWidth / 2 - halfWidth);
        int destX;
        destX = mLayoutParams.x < centerX ? 0 : screenWidth - width;
        sleep = false;
        moveToX(true, destX);
    }

    private void moveToEdge(boolean smooth, boolean forceSleep) {
        final int screenWidth = floatBallManager.mScreenWidth;
        int width = getWidth();
        int halfWidth = width / 2;
        int centerX = (screenWidth / 2 - halfWidth);
        int destX;
        final int minVelocity = mVelocity.getMinVelocity();
        if (mLayoutParams.x < centerX) {
            sleep = forceSleep ? true : Math.abs(mVelocityX) > minVelocity && mVelocityX < 0 || mLayoutParams.x < 0;
            destX = sleep ? -halfWidth : 0;
        } else {
            sleep = forceSleep ? true : Math.abs(mVelocityX) > minVelocity && mVelocityX > 0 || mLayoutParams.x > screenWidth - width;
            destX = sleep ? screenWidth - halfWidth : screenWidth - width;
        }
        if (sleep) {
            mSleepX = destX;
        }
        moveToX(smooth, destX);
    }

    private int getScrollDuration(int distance) {
        return (int) (250 * (1.0f * distance / 800));
    }

    private void onMove(int deltaX, int deltaY) {
        mLayoutParams.x += deltaX;
        mLayoutParams.y += deltaY;
        if (windowManager != null) {
            windowManager.updateViewLayout(this, mLayoutParams);
        }
    }

    public void onLocation(int x, int y) {
        mLayoutParams.x = x;
        mLayoutParams.y = y;
        if (windowManager != null) {
            windowManager.updateViewLayout(this, mLayoutParams);
        }
    }

    public void onMove(int lastX, int lastY, int curX, int curY) {
        onMove(curX - lastX, curY - lastY);
    }

    @Override
    public void onDone() {
        postSleepRunnable();
    }

    private void moveTo(int x, int y) {
        mLayoutParams.x += x - mLayoutParams.x;
        mLayoutParams.y += y - mLayoutParams.y;
        if (windowManager != null) {
            windowManager.updateViewLayout(this, mLayoutParams);
        }
    }

    public int getSize() {
        return mSize;
    }

    private void onClick() {
        floatBallManager.floatballX = mLayoutParams.x;
        floatBallManager.floatballY = mLayoutParams.y;
        floatBallManager.onFloatBallClick();
    }

    private void removeSleepRunnable() {
        mSleepRunnable.removeSelf(this);
    }

    public void postSleepRunnable() {
        if (mHideHalfLater && !sleep && isAdded) {
            mSleepRunnable.postDelaySelf(this, 3000);
        }
    }
}
