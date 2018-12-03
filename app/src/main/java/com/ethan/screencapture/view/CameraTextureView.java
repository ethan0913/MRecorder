package com.ethan.screencapture.view;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.ethan.screencapture.R;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;

import static android.content.Context.WINDOW_SERVICE;

/**
 * camera 小窗口
 */
public class CameraTextureView extends TextureView implements TextureView.SurfaceTextureListener {
    private static final String TAG = CameraTextureView.class.getSimpleName();
    //CameraTextureView 的默认尺寸
    private static final int VIEW_SIZE_DEFAULT = 240;
    //CameraTextureView 的最大尺寸
    private static final int VIEW_SIZE_MAX = 400;
    //CameraTextureView 的最小尺寸
    private static final int VIEW_SIZE_MIN = 120;
    //CameraTextureView 的圆角尺寸
    private static final int VIEW_CORNER_RADIUS = 10;
    //CameraTextureView 的缩放事件触发区域比例
    private static final float EVENT_SCALE_RANGE = 0.2f;
    //CameraTextureView 的关闭事件触发区域比例
    private static final float EVENT_CLOSE_RANGE = 0.2f;
    //CameraTextureView 的切换事件触发区域比例
    private static final float EVENT_SWITCH_RANGE = 0.2f;
    //CameraTextureView 点击事件的时间阈值
    private static final int EVENT_CLICK_TIME = 400;
    //CameraTextureView 镜头切换动画旋转部分持续时间
    private static final int ANIMATION_SWITCH_ROTATION_DURATION = 1000;
    //CameraTextureView 镜头切换动画缩放部分的持续时间
    private static final int ANIMATION_SWITCH_SCALE_DURATION = 300;
    //CameraTextureView 镜头切换动画缩放部分的比例
    private static final float ANIMATION_SWITCH_SCALE_SIZE = 0.8f;
    @SuppressLint("StaticFieldLeak")
    private static Context mContext;
    @SuppressLint("StaticFieldLeak")
    private static CameraTextureView mCameraTextureView;
    @SuppressLint("StaticFieldLeak")
    private static RelativeLayout mOverlayRoot;
    private RelativeLayout mOverlay;
    protected Camera mCamera;
    private Camera.Parameters mCameraParameters;
    private volatile int mCurrentCameraId = -1;
    private int mFrontCameraId = -1;
    private int mBackCameraId = -1;
    private int mCurrentOrientation = -1;
    private SurfaceTexture mSurfaceTexture;
    private WindowManager mWindowManager;
    private int mMinViewSize;
    private int mMaxViewSize;
    private int mScreenWidth;
    private int mScreenHeight;
    private int mStatusBarHeight;
    private OrientationEventListener mOrientationListener;

    public CameraTextureView(Context context) {
        this(context, null);
    }

    public CameraTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initCameraInfo();
        setSurfaceTextureListener(this);
        mWindowManager = (WindowManager) context.getSystemService(WINDOW_SERVICE);
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        mScreenWidth = displayMetrics.widthPixels;
        mScreenHeight = displayMetrics.heightPixels;
        mStatusBarHeight = getStatusHeight(context);
        mMinViewSize = Math.round(VIEW_SIZE_MIN * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        mMaxViewSize = Math.round(VIEW_SIZE_MAX * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        setOutlineProvider(new TextureVideoViewOutlineProvider(this, dpToPx(context, VIEW_CORNER_RADIUS) * 1.0f));
        setClipToOutline(true);
        mOrientationListener = new OrientationEventListener(context) {
            @Override
            public void onOrientationChanged(int orientation) {
                updateCameraParameters(getOrientation());
            }
        };
        mOrientationListener.enable();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
        Log.d(TAG, "onSurfaceTextureAvailable");
        mSurfaceTexture = surfaceTexture;
        openCamera(mFrontCameraId);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {
        Log.d(TAG, "onSurfaceTextureSizeChanged:" + i + "x" + i1);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        Log.d(TAG, "onSurfaceTextureDestroyed");
        closeCamera();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
    }

    /**
     * 初始化Camera id
     */
    private void initCameraInfo() {
        int cameraNum = Camera.getNumberOfCameras();
        Camera.CameraInfo[] mInfo = new Camera.CameraInfo[cameraNum];
        for (int i = 0; i < cameraNum; i++) {
            mInfo[i] = new Camera.CameraInfo();
            android.hardware.Camera.getCameraInfo(i, mInfo[i]);
        }
        for (int i = 0; i < cameraNum; i++) {
            if (mBackCameraId == -1 && mInfo[i].facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                mBackCameraId = i;
            } else if (mFrontCameraId == -1 && mInfo[i].facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                mFrontCameraId = i;
            }
        }
    }

    /**
     * 异步打卡指定Camera
     *
     * @param cameraId cameraId
     */
    private void openCamera(final int cameraId) {
        new Thread() {
            @Override
            public void run() {
                try {
                    if (mCamera != null) {
                        closeCamera();
                        mCameraParameters = null;
                        mCurrentOrientation = -1;
                    }
                    mCamera = Camera.open(cameraId);
                    mCurrentCameraId = cameraId;
                    if (mCamera == null) {
                        return;
                    }
                    mCamera.setPreviewTexture(mSurfaceTexture);
                    mCameraParameters = mCamera.getParameters();
                    updateCameraParameters(getOrientation());
                    mCamera.setParameters(mCameraParameters);
                    mCamera.startPreview();
                    mCamera.cancelAutoFocus();
                    post(new Runnable() {
                        @Override
                        public void run() {
                            requestLayout();
                        }
                    });
                } catch (IOException e) {
                    Log.e(TAG, "e:" + e.getLocalizedMessage());
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * 关闭camera
     */
    private void closeCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    /**
     * 更新camera参数
     *
     * @param orientation 方向
     */
    private void updateCameraParameters(int orientation) {
        if (mCurrentOrientation != orientation && mCamera != null && mCameraParameters != null) {
            Log.d(TAG, "[updateCameraParameters]orientation:" + orientation);
            mCurrentOrientation = orientation;
            List<Camera.Size> supportedPreviewSizes = mCameraParameters.getSupportedPreviewSizes();
            Camera.Size previewSize = getPreviewSize(orientation, supportedPreviewSizes);
            mCameraParameters.setPreviewSize(previewSize.width, previewSize.height);
            mCamera.setDisplayOrientation(orientation);

            //当屏幕旋转时,更新CameraTextureView的位置,防止显示在屏幕外
            post(new Runnable() {
                @Override
                public void run() {
                    if (mWindowManager != null) {
                        if (mOverlayRoot != null && mOverlayRoot.isAttachedToWindow()) {
                            WindowManager.LayoutParams overlayLayoutParams = (WindowManager.LayoutParams) mOverlayRoot.getLayoutParams();
                            mWindowManager.updateViewLayout(mOverlayRoot, detectBounds(overlayLayoutParams));
                        }
                        WindowManager.LayoutParams layoutParams = (WindowManager.LayoutParams) getLayoutParams();
                        mWindowManager.updateViewLayout(CameraTextureView.this, detectBounds(layoutParams));
                    }
                }
            });
        }
    }

    /**
     * 根据当前TextureView的宽高返回最适合的preview尺寸
     *
     * @param orientation 方向
     * @param preSizeList 支持的preview尺寸
     * @return 最合适的preview尺寸
     */
    private Camera.Size getPreviewSize(int orientation, List<Camera.Size> preSizeList) {
        int surfaceWidth = getWidth();
        int surfaceHeight = getHeight();
        int reqTmpWidth;
        int reqTmpHeight;
        if (orientation == 90 || orientation == 270) {
            reqTmpWidth = surfaceHeight;
            reqTmpHeight = surfaceWidth;
        } else {
            reqTmpWidth = surfaceWidth;
            reqTmpHeight = surfaceHeight;
        }
        for (Camera.Size size : preSizeList) {
            if ((size.width == reqTmpWidth) && (size.height == reqTmpHeight)) {
                return size;
            }
        }

        float reqRatio = ((float) reqTmpWidth) / reqTmpHeight;
        float curRatio, deltaRatio;
        float deltaRatioMin = Float.MAX_VALUE;
        Camera.Size retSize = null;
        for (Camera.Size size : preSizeList) {
            curRatio = ((float) size.width) / size.height;
            deltaRatio = Math.abs(reqRatio - curRatio);
            if (deltaRatio < deltaRatioMin) {
                deltaRatioMin = deltaRatio;
                retSize = size;
            }
        }
        return retSize;
    }

    /**
     * 打开camera小窗口
     *
     * @param context  上下文
     * @param listener {@link CameraTextureListener} 小窗口状态监听
     */
    @TargetApi(Build.VERSION_CODES.M)
    public static void open(Context context, CameraTextureListener listener) {
        if (context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            mContext = context.getApplicationContext();
            try {
                mCameraTextureView = new CameraTextureView(mContext);
                mCameraTextureView.setCameraTextureListenerListener(listener);
                WindowManager windowManager = (WindowManager) mContext.getSystemService(WINDOW_SERVICE);
                WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                        dpToPx(context, VIEW_SIZE_DEFAULT),
                        dpToPx(context, VIEW_SIZE_DEFAULT),
                        WindowManager.LayoutParams.TYPE_PHONE,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                        PixelFormat.TRANSLUCENT);
                params.x = 0;
                params.y = 0;
                params.gravity = Gravity.START | Gravity.TOP;
                windowManager.addView(mCameraTextureView, params);
            } catch (Exception e) {
                Log.e(TAG, "e:" + e.getLocalizedMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * 关闭camera小窗口
     *
     * @param context 上下文
     */
    public static void close(Context context) {
        try {
            WindowManager windowManager = (WindowManager) context.getSystemService(WINDOW_SERVICE);
            if (mOverlayRoot != null) {
                windowManager.removeView(mOverlayRoot);
                mOverlayRoot = null;
            }
            if (mCameraTextureView != null) {
                mCameraTextureView.onClose();
                windowManager.removeView(mCameraTextureView);
                mCameraTextureView = null;
            }
            mContext = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取矫正后的camera方向
     *
     * @return cameraOrientation
     */
    public int getOrientation() {
        Display display = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int rotation = display.getRotation();
        int orientation;
        boolean expectPortrait;
        switch (rotation) {
            case Surface.ROTATION_0:
            default:
                orientation = 90;
                expectPortrait = true;
                break;
            case Surface.ROTATION_90:
                orientation = 0;
                expectPortrait = false;
                break;
            case Surface.ROTATION_180:
                orientation = 270;
                expectPortrait = true;
                break;
            case Surface.ROTATION_270:
                orientation = 180;
                expectPortrait = false;
                break;
        }
        boolean isPortrait = display.getHeight() > display.getWidth();
        if (isPortrait != expectPortrait) {
            orientation = (orientation + 270) % 360;
        }
        return orientation;
    }

    //CameraTextureView 是否正在拉伸
    boolean isDrag;
    //CameraTextureView Overlay是否显示
    boolean isOverlay;
    float downX;
    float downY;
    float downRawX;
    float downRawY;
    float downLeft;
    float downTop;
    float downWidth;
    float downHeight;
    WindowManager.LayoutParams previewParams;
    WindowManager.LayoutParams overlayParams;
    long downTime;

    @SuppressLint("InflateParams")
    @Override
    public boolean performClick() {
        if (isOverlay) {
            closeOverlay();
        } else {
            openOverlay();
        }
        return super.performClick();
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downTime = System.currentTimeMillis();
                previewParams = (WindowManager.LayoutParams) getLayoutParams();
                if (isOverlay && mOverlayRoot != null) {
                    overlayParams = (WindowManager.LayoutParams) mOverlayRoot.getLayoutParams();
                }
                downLeft = previewParams.x;
                downTop = previewParams.y;
                downWidth = previewParams.width;
                downHeight = previewParams.height;
                downX = event.getX();
                downY = event.getY();
                downRawX = event.getRawX();
                downRawY = event.getRawY();
                isDrag = isDragEvent(event);
                if (isDrag && isOverlay) {
                    previewParams.width = mMaxViewSize;
                    previewParams.height = mMaxViewSize;
                    mWindowManager.updateViewLayout(this, previewParams);

                    overlayParams.width = mMaxViewSize;
                    overlayParams.height = mMaxViewSize;
                    mWindowManager.updateViewLayout(mOverlayRoot, overlayParams);

                    previewParams.width = (int) downWidth;
                    previewParams.height = (int) downHeight;
                    scalePreview(previewParams.width * 1.0f / mMaxViewSize * 1.0f);
                    updateOverlay();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (isDrag && isOverlay) {
                    previewParams.width = (int) (downWidth + event.getX() - downX);
                    previewParams.height = previewParams.width;
                    if (previewParams.width < mMinViewSize) {
                        previewParams.width = mMinViewSize;
                    }
                    if (previewParams.width > mMaxViewSize) {
                        previewParams.width = mMaxViewSize;
                    }
                    if (previewParams.height < mMinViewSize) {
                        previewParams.height = mMinViewSize;
                    }
                    if (previewParams.height > mMaxViewSize) {
                        previewParams.height = mMaxViewSize;
                    }
                    invalidateOutline();
                    scalePreview(previewParams.width * 1.0f / mMaxViewSize * 1.0f);
                    updateOverlay();
                    overlayParams.width = previewParams.width;
                    overlayParams.height = previewParams.height;
                } else {
                    previewParams.x = (int) (downLeft + event.getRawX() - downRawX);
                    previewParams.y = (int) (downTop + event.getRawY() - downRawY);
                    mWindowManager.updateViewLayout(this, detectBounds(previewParams));
                    scalePreview(1.0f);
                    if (isOverlay) {
                        overlayParams.x = previewParams.x;
                        overlayParams.y = previewParams.y;
                        mWindowManager.updateViewLayout(mOverlayRoot, overlayParams);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                float dx = Math.abs(event.getRawX() - downRawX);
                float dy = Math.abs(event.getRawY() - downRawY);
                if (dx < 10 && dy < 10 && System.currentTimeMillis() - downTime < EVENT_CLICK_TIME) {
                    if (isOverlay && isCloseEvent(event)) {
                        close(mContext);
                    } else if (isOverlay && isRotationEvent(event)) {
                        rotation();
                    } else {
                        performClick();
                    }
                    break;
                }
                if (isDrag && isOverlay) {
                    mWindowManager.updateViewLayout(this, previewParams);
                    scalePreview(1.0f);
                    mWindowManager.updateViewLayout(mOverlayRoot, overlayParams);
                    updateOverlay();
                }
                break;
            default:
                if (isDrag && isOverlay) {
                    mWindowManager.updateViewLayout(this, previewParams);
                    scalePreview(1.0f);
                    mWindowManager.updateViewLayout(mOverlayRoot, overlayParams);
                    updateOverlay();
                }
                break;
        }
        return true;
    }

    /**
     * 判断是否是拉伸动作
     *
     * @param event {@link MotionEvent} 动作
     * @return true 是 false 不是
     */
    private boolean isDragEvent(MotionEvent event) {
        return event.getX() > getWidth() * (1 - EVENT_SCALE_RANGE) && event.getY() < getHeight() * EVENT_SCALE_RANGE;
    }

    /**
     * 判断是否是关闭动作
     *
     * @param event {@link MotionEvent} 动作
     * @return true 是 false 不是
     */
    private boolean isCloseEvent(MotionEvent event) {
        return event.getX() > getWidth() * (1 - EVENT_CLOSE_RANGE) && event.getY() > getHeight() * (1 - EVENT_CLOSE_RANGE);
    }

    /**
     * 判断是否是旋转动作
     *
     * @param event {@link MotionEvent} 动作
     * @return true 是 false 不是
     */
    private boolean isRotationEvent(MotionEvent event) {
        return event.getX() < getWidth() * EVENT_SWITCH_RANGE && event.getY() > getHeight() * (1 - EVENT_SWITCH_RANGE);
    }

    /**
     * 缩放preview
     *
     * @param scale 缩放比例 0.0f~1.0f
     */
    private void scalePreview(float scale) {
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale, 0, 0);

        Rect rect = new Rect();
        getGlobalVisibleRect(rect);
        int leftMargin = 0;
        int topMargin = 0;
        RectF selfRect = new RectF(leftMargin, topMargin,
                rect.right - rect.left - leftMargin, rect.bottom - rect.top - topMargin);
        matrix.mapRect(selfRect);
        setTransform(matrix);
    }

    /**
     * 旋转preview
     */
    private void rotation() {
        closeOverlay();
        openCamera((mCurrentCameraId == mFrontCameraId ? mBackCameraId : mFrontCameraId));

        AnimatorSet animatorSet = new AnimatorSet();
        ObjectAnimator objectAnimatorX1 = ObjectAnimator.ofFloat(this, "scaleX", 1.0f, ANIMATION_SWITCH_SCALE_SIZE);
        ObjectAnimator objectAnimatorY1 = ObjectAnimator.ofFloat(this, "scaleY", 1.0f, ANIMATION_SWITCH_SCALE_SIZE);
        objectAnimatorX1.setDuration(ANIMATION_SWITCH_SCALE_DURATION);
        objectAnimatorY1.setDuration(ANIMATION_SWITCH_SCALE_DURATION);
        ObjectAnimator objectAnimatorX2 = ObjectAnimator.ofFloat(this, "scaleX", ANIMATION_SWITCH_SCALE_SIZE, 1.0f);
        ObjectAnimator objectAnimatorY2 = ObjectAnimator.ofFloat(this, "scaleY", ANIMATION_SWITCH_SCALE_SIZE, 1.0f);
        objectAnimatorX2.setDuration(ANIMATION_SWITCH_SCALE_DURATION);
        objectAnimatorY2.setDuration(ANIMATION_SWITCH_SCALE_DURATION);
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 180);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float value = (float) animation.getAnimatedValue();
                if (value > 90) {
                    value += 180;
                }
                setRotationY(value);
            }
        });

        valueAnimator.setDuration(ANIMATION_SWITCH_ROTATION_DURATION);
        animatorSet.play(objectAnimatorX1).with(objectAnimatorY1);
        animatorSet.play(valueAnimator).after(objectAnimatorX1);
        animatorSet.play(objectAnimatorX2).with(objectAnimatorY2).after(valueAnimator);
        animatorSet.start();
    }

    /**
     * 显示overlay
     */
    @SuppressLint("InflateParams")
    private void openOverlay() {
        if (previewParams != null) {
            LayoutInflater layoutInflater = (LayoutInflater)
                    mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mOverlayRoot = (RelativeLayout) layoutInflater.inflate(R.layout.layout_camera_overlay, null, false);
            mOverlay = mOverlayRoot.findViewById(R.id.rl_overlay_camera_layout);
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    previewParams.width,
                    previewParams.height,
                    WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    PixelFormat.TRANSLUCENT);
            params.x = previewParams.x;
            params.y = previewParams.y;
            params.gravity = Gravity.START | Gravity.TOP;
            mWindowManager.addView(mOverlayRoot, params);

            isOverlay = true;
        } else {
            Log.e(TAG, "[performClick]previewParams is null");
        }
    }

    /**
     * 关闭overlay
     */
    public void closeOverlay() {
        try {
            mWindowManager.removeView(mOverlayRoot);
            isOverlay = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 更新overlay
     */
    public void updateOverlay() {
        if (isOverlay && mOverlay != null && previewParams != null) {
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mOverlay.getLayoutParams();
            params.width = previewParams.width;
            params.height = previewParams.height;
            mOverlay.setLayoutParams(params);
        }
    }

    /**
     * 边界检测,根据屏幕宽高对view的位置进行矫正.
     *
     * @param params 待矫正的LayoutParameter
     * @return 矫正后的LayoutParam
     */
    private WindowManager.LayoutParams detectBounds(WindowManager.LayoutParams params) {
        DisplayMetrics displayMetrics = mContext.getResources().getDisplayMetrics();
        mScreenWidth = displayMetrics.widthPixels;
        mScreenHeight = displayMetrics.heightPixels;
        if (params.x < 0) {
            params.x = 0;
        }
        if (params.y < 0) {
            params.y = 0;
        }
        if (params.x > mScreenWidth - getWidth()) {
            params.x = mScreenWidth - getWidth();
        }
        if (params.y > mScreenHeight - mStatusBarHeight - getHeight()) {
            params.y = mScreenHeight - mStatusBarHeight - getHeight();
        }
        return params;
    }

    /**
     * 获取状态栏的高度
     *
     * @param context 上下文
     * @return 状态栏高度
     */
    public static int getStatusHeight(Context context) {
        int statusBarHeight = -1;
        try {
            @SuppressLint("PrivateApi") Class<?> clazz = Class.forName("com.android.internal.R$dimen");
            Object object = clazz.newInstance();
            int height = Integer.parseInt(clazz.getField("status_bar_height")
                    .get(object).toString());
            statusBarHeight = context.getResources().getDimensionPixelSize(height);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return statusBarHeight;
    }

    /**
     * dp转px
     *
     * @param context 上下文
     * @param dp      dp
     * @return px
     */
    private static int dpToPx(Context context, double dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return Math.round((int) dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    /**
     * CameraTextureView的OutLineProvider
     * 圆角功能实现
     */
    static class TextureVideoViewOutlineProvider extends ViewOutlineProvider {
        private float mRadius;
        private WeakReference<CameraTextureView> mViewWeakReference;
        private WindowManager.LayoutParams mPreviewParams;

        private TextureVideoViewOutlineProvider(CameraTextureView textureView, float radius) {
            this.mRadius = radius;
            this.mViewWeakReference = new WeakReference<>(textureView);
        }

        @Override
        public void getOutline(View view, Outline outline) {
            CameraTextureView cameraTextureView = mViewWeakReference.get();
            if (cameraTextureView != null) {
                Rect rect = new Rect();
                view.getGlobalVisibleRect(rect);
                mPreviewParams = (WindowManager.LayoutParams) cameraTextureView.getLayoutParams();
                if (cameraTextureView.isDrag && cameraTextureView.isOverlay) {
                    rect.right = mPreviewParams.width;
                    rect.bottom = mPreviewParams.height;
                }
                int leftMargin = 0;
                int topMargin = 0;
                Rect selfRect = new Rect(leftMargin, topMargin,
                        rect.right - rect.left - leftMargin, rect.bottom - rect.top - topMargin);
                outline.setRoundRect(selfRect, mRadius);
            }
        }
    }

    /**
     * 当Camera小窗口关闭时调用
     */
    private void onClose() {
        if (mOrientationListener != null) {
            mOrientationListener.disable();
            mOrientationListener = null;
        }
        if (mListener != null) {
            mListener.onClose();
            mListener = null;
        }
    }

    private CameraTextureListener mListener;

    /**
     * 设置Camera小窗口监听
     *
     * @param listener 监听
     */
    public void setCameraTextureListenerListener(CameraTextureListener listener) {
        this.mListener = listener;
    }

    /**
     * Camera小窗口状态监听,
     * 可通过{@link CameraTextureView#setCameraTextureListenerListener(CameraTextureListener)}
     * 进行设置
     */
    public interface CameraTextureListener {
        void onClose();
    }
}