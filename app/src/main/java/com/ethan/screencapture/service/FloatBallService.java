package com.ethan.screencapture.service;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.SwitchCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.Chronometer;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.huxq17.floatball.libarary.FloatBallManager;
import com.huxq17.floatball.libarary.floatball.FloatBallCfg;
import com.huxq17.floatball.libarary.menu.FloatMenuCfg;
import com.huxq17.floatball.libarary.menu.MenuItem;
import com.huxq17.floatball.libarary.utils.DensityUtil;
import com.ethan.screencapture.Const;
import com.ethan.screencapture.R;
import com.ethan.screencapture.activity.GetPermissionActivity;
import com.ethan.screencapture.activity.MainActivity;
import com.ethan.screencapture.util.ActivityStackManager;
import com.ethan.screencapture.view.ChoosePenDialog;
import com.ethan.screencapture.view.DrawingView;
import com.ethan.screencapture.view.CameraTextureView;
import com.ethan.screencapture.view.RegionView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by lizh on 2018/3/30.
 */

public class FloatBallService extends Service implements RecorderService.RecorderStateChangeListener, View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    private FloatBallManager mFloatballManager;
    private List<MenuItem> menuItems = new ArrayList<>();
    private MenuItem recorderStartItem;
    private MenuItem recorderStopItem;
    private MenuItem recorderPauseItem;
    private MenuItem recorderResumeItem;
    private MenuItem listItem;
    private MenuItem paintOpenItem;
    private MenuItem paintCloseItem;
    /*
    * paint about
    * */
    private WindowManager mWindowManager;
    private int mScreenWidth;
    private int mScreenHeight;
    private int mScreenDensity = 0;

    public ImageView mBackGroundView;
    public DrawingView mDrawingView;
    private WindowManager.LayoutParams mToolbarLayoutParams;
    private static final int NAVIGATION_BAR_HEIGHT = 48;
    private static final int TOOL_BAR_HEIGHT = 52;
    private LinearLayout mToolbarLayout;
    private LinearLayout mToolbar;

    private LinearLayout mSwitcherLayout;
    private ImageButton mToExpandButton;
    private ImageButton mToCollapseButton;
    //绘画悬浮窗位置
    private float x1;
    private float y1;
    private float x2;
    private float y2;

    private static final int TOUCH_MOVE_TOLERATION = 10;
    private static final int SWITCH_BUTTON_HEIGHT = 48;

    private TranslateAnimation mShowToolbarAnimation;
    private TranslateAnimation mHiddenToolbarAnimation;
    private TranslateAnimation mHiddenToolbarAnimationClose;
    private RotateAnimation mShowExpandButtonAnimation;
    private RotateAnimation mShowCollapseButtonAnimation;
    private RotateAnimation mShowCollapseButtonAnimationClose;
    private static final long ANIMATION_DURATION = 500;


    /* FloatingControlService about star*/
    private LinearLayout floatingControls;
    private View controls;
    private static ImageButton pauseResumeIB;
    private static ImageButton shotIB;
    private static ImageButton stopIB;
    private static ImageButton paintIB;
    private static ImageButton toolIB;
    private static ImageButton homeIB;
    private static ImageButton cameraIB;
    private static TextView tvRecording;
    private static ImageView imageExit;
    private static Chronometer ch;
    private FrameLayout mPaintFrameLayout;
    private GridView mGridview;
    //added by wangxl for toolsettings
    public static SwitchCompat mCameraSwitch;
    public static SwitchCompat mRecordeVoice;
    private SwitchCompat mClickAction;
    private SwitchCompat mWaterMark;
    private SwitchCompat mPartScreen;
    private RelativeLayout mChooseLayout;
    public static ImageView mRegionBgView;
    public static RegionView mRegionDrawingView;
    private Dialog mToolDlg;
    private ImageView mDlgExit;
    private static int second;
    private SharedPreferences prefs;
    private IBinder binder = new ServiceBinder();
    //Binder class for binding to recording service

    private static Const.RecordingState mState = Const.RecordingState.STOPPED;

    private List<Integer> drawableList = new ArrayList<>();
    private Integer[] mPenSizes = {DrawingView.STROKE_WIDTH_THIN, DrawingView.STROKE_WIDTH_MEDIUM, DrawingView.STROKE_WIDTH_THICK};
    private Integer[] mPenColors = {0xbfff0000, 0xbf00ff00, 0xbf0000ff};
    private Integer[] mPenShapes = {DrawingView.SHAPE_CURVE, DrawingView.SHAPE_ROUND_RECT, DrawingView.SHAPE_ARROW};
    private Integer[] mBackGrounds = {R.color.transparent, R.drawable.paper_white, R.drawable.paper_black};

     /* FloatingControlService about over*/

    @Override
    public void onCreate() {
        super.onCreate();
        initScreenParameters();
        createBackGroundView();
        initSinglePageFloatball();
        createDrawingView();
        createToolbarView();
        createSwitcherView();
        //added by wangxl for regionScreen
        createRegionBgView();
        createRegionDrawingView();
        mRegionBgView.setVisibility(View.GONE);
        mRegionDrawingView.setVisibility(View.GONE);
        //ended
        setAnimations();
        setViewsInitVisibility();
        mFloatballManager.show();
        RecorderService.setOnRecorderStateChangeListener(this);
        createFloatingControl();
        startForeground(1, new Notification());

        //add by qujq 2018/4/9 注册camera setting的广播
        registerCameraBroadcastReceiver();

    }

    public void createFloatingControl() {
        LayoutInflater li = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
//        floatingControls = (LinearLayout) li.inflate(R.layout.layout_floating_controls, null);
        floatingControls = (LinearLayout) li.inflate(R.layout.layout_float_tools, null);
        controls = floatingControls.findViewById(R.id.controls);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Initialize imageButtons
        stopIB = controls.findViewById(R.id.stop);
//        pauseIB = controls.findViewById(R.id.pause);
//        resumeIB = controls.findViewById(R.id.resume);
//        resumeIB.setEnabled(false);
        tvRecording = floatingControls.findViewById(R.id.tv_recording);
        ch = floatingControls.findViewById(R.id.chronometer);
        ch.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {
                chronometer.setText(FormatMiss(second));
                second++;
            }
        });
        imageExit = floatingControls.findViewById(R.id.exit);
        mPaintFrameLayout = floatingControls.findViewById(R.id.fl_paint);
        mGridview = floatingControls.findViewById(R.id.my_gridView);
        pauseResumeIB = controls.findViewById(R.id.pause_resume);
        pauseResumeIB.setEnabled(true);
        toolIB = controls.findViewById(R.id.setting);
        homeIB = controls.findViewById(R.id.home);
        shotIB = controls.findViewById(R.id.shot);
        paintIB = controls.findViewById(R.id.paint);
        cameraIB = controls.findViewById(R.id.camera);
        stopIB.setOnClickListener(this);
        shotIB.setOnClickListener(this);
        paintIB.setOnClickListener(this);
        cameraIB.setOnClickListener(this);
        pauseResumeIB.setOnClickListener(this);
        imageExit.setOnClickListener(this);
        toolIB.setOnClickListener(this);
        homeIB.setOnClickListener(this);
        mToolDlg = new Dialog(this, R.style.dialog);
        mToolDlg.setContentView(R.layout.dialog_tools);
        mToolDlg.setCancelable(false);
        mToolDlg.getWindow().setType((WindowManager.LayoutParams.TYPE_SYSTEM_ALERT));

        //Pause/Resume doesnt work below SDK version 24. Remove them
            pauseResumeIB.setVisibility(View.GONE);
            pauseResumeIB.setOnClickListener(this);

        //Set layout params to display the controls over any screen.
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.setTitle("jiayue_switcher_view");
//        dpToPx(pref.getInt(getString(R.string.preference_floating_control_size_key), 200))
        // From API26, TYPE_PHONE depricated. Use TYPE_APPLICATION_OVERLAY for O
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
//            params.type = WindowManager.LayoutParams.TYPE_PHONE;
        params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        //Initial position of the floating controls
        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        params.x = 0;
        params.y = 0;

        //Add the controls view to windowmanager
        mWindowManager.addView(floatingControls, params);

        //Add touch listerner to floating controls view to move/close/expand the controls
        try {
            floatingControls.setOnTouchListener(new View.OnTouchListener() {
                boolean isMoving = false;
                private WindowManager.LayoutParams paramsF = params;
                private int initialX;
                private int initialY;
                private float initialTouchX;
                private float initialTouchY;

                @Override
                public boolean onTouch(View v, MotionEvent event) {

                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            isMoving = false;
                            initialX = paramsF.x;
                            initialY = paramsF.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            break;
                        case MotionEvent.ACTION_UP:
                           /* if (!isMoving) {
                                if (controls.getVisibility() == View.INVISIBLE) {
                                    expandFloatingControls();
                                } else {
                                    collapseFloatingControls();
                                }
                            }*/
                            break;
                        case MotionEvent.ACTION_MOVE:
                            int xDiff = (int) (event.getRawX() - initialTouchX);
                            int yDiff = (int) (event.getRawY() - initialTouchY);
                            paramsF.x = initialX + xDiff;
                            paramsF.y = initialY + yDiff;
                            /* Set an offset of 10 pixels to determine controls moving. Else, normal touches
                             * could react as moving the control window around */
                            if (Math.abs(xDiff) > 10 || Math.abs(yDiff) > 10)
                                isMoving = true;
                            mWindowManager.updateViewLayout(floatingControls, paramsF);
                            break;
                    }
                    return false;
                }
            });
        } catch (Exception e) {
            // TODO: handle exception
        }
        drawableList.add(R.drawable.stroke_width_thin);
        drawableList.add(R.drawable.stroke_width_medium);
        drawableList.add(R.drawable.stroke_width_thick);
        drawableList.add(R.color.red);
        drawableList.add(R.color.green);
        drawableList.add(R.color.blue);
        drawableList.add(R.drawable.shape_curve);
        drawableList.add(R.drawable.shape_roundrectangle);
        drawableList.add(R.drawable.shape_arrow);
        drawableList.add(R.color.transparent);
        drawableList.add(R.drawable.paper_white);
        drawableList.add(R.drawable.paper_black);
        List<Map<String, Object>> items = new ArrayList<>();
        for (Object item : drawableList) {
            Map<String, Object> map = new HashMap<>();
            map.put("image", item);
            items.add(map);
        }
        SimpleAdapter adapter = new SimpleAdapter(this, items,
                R.layout.choosepen_grid_item, new String[]{"image"},
                new int[]{R.id.chose_item_image});

        mGridview.setAdapter(adapter);
        mGridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                if (position < 3) {
                    mDrawingView.setPenSize(mPenSizes[position]);
                } else if (position < 6) {
                    mDrawingView.setPenColor(mPenColors[position - 3]);
                } else if (position < 9) {
                    mDrawingView.setShape(mPenShapes[position - 6]);
                } else if (position < 12) {
                    mBackGroundView.setImageResource(mBackGrounds[position - 9]);
                }
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//        if (intent != null && intent.getAction() != null) {
//            switch (intent.getAction()) {
//                case Const.PAINT_SHOW_CLOSE:
//
//            }
//        }
        return START_NOT_STICKY;
//        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        initScreenParameters();
        if (mBackGroundView != null) {
            mWindowManager.removeView(mBackGroundView);
            createBackGroundView();
        }
        if (mDrawingView != null) {
            mWindowManager.removeView(mDrawingView);
            createDrawingView();
        }
        if (mToolbarLayout != null) {
            mWindowManager.removeView(mToolbarLayout);
            createToolbarView();
        }
        if (mSwitcherLayout != null) {
            mWindowManager.removeView(mSwitcherLayout);
            createSwitcherView();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //add by qujq 2018/4/9 解注册监听关闭 camera texture
        CameraTextureView.close(this);
        if (floatingControls != null) mWindowManager.removeView(floatingControls);
        unregisterReceiver(mCameraBroadcastReceiver);
        stopForeground(true);
    }

    @Override
    public void onRecorderStateChange(Const.RecordingState state) {
        Log.d("wxl", "-----Float----" + state);
        switch (state) {
            case STOPPED:
                if (menuItems.size() > 1) {
                    menuItems.set(0, recorderStartItem);
                    menuItems.set(1, listItem);
                    mFloatballManager.setMenu(menuItems).buildMenu();
                }
                if (mToExpandButton.getVisibility() == View.VISIBLE) {
                    menuItems.set(3, paintOpenItem);
                    mFloatballManager.setMenu(menuItems).buildMenu();
                    if (mToolbar.getVisibility() == View.VISIBLE) {
                        collapseToolbarCloseSwitcherView();
                    } else {
                        mToExpandButton.setVisibility(View.GONE);
                    }
                }
                break;
            case RECORDING:
                if (menuItems.size() > 1) {
                    menuItems.set(0, recorderPauseItem);
                    menuItems.set(1, recorderStopItem);
                    mFloatballManager.setMenu(menuItems).buildMenu();
                }
                break;
            case RESUME:
                if (menuItems.size() > 1) {
                    menuItems.set(0, recorderPauseItem);
                    mFloatballManager.setMenu(menuItems).buildMenu();
                }
                break;
            case PAUSED:
                if (menuItems.size() > 1) {
                    menuItems.set(0, recorderResumeItem);
                    mFloatballManager.setMenu(menuItems).buildMenu();
                }
                break;
            default:
                break;
        }

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void initSinglePageFloatball() {
        //1 初始化悬浮球配置，定义好悬浮球大小和icon的drawable
        int ballSize = DensityUtil.dip2px(getApplicationContext(), 45);
        Drawable ballIcon = getApplicationContext().getResources().getDrawable(R.drawable.ic_float_button_bg);
        FloatBallCfg ballCfg = new FloatBallCfg(ballSize, ballIcon, FloatBallCfg.Gravity.RIGHT_CENTER);
        //设置悬浮球不半隐藏
//        ballCfg.setHideHalfLater(false);
        //2 需要显示悬浮菜单
        //2.1 初始化悬浮菜单配置，有菜单item的大小和菜单item的个数
        int menuSize = DensityUtil.dip2px(getApplicationContext(), 180);
        int menuItemSize = DensityUtil.dip2px(getApplicationContext(), 30);
        FloatMenuCfg menuCfg = new FloatMenuCfg(menuSize, menuItemSize);
        //3 生成floatballManager
        //必须传入Activity
        mFloatballManager = new FloatBallManager(this, ballCfg, menuCfg);
        addFloatMenuItem();
    }

    private void addFloatMenuItem() {
        recorderResumeItem = new MenuItem(getApplicationContext().getResources().getDrawable(R.drawable.ic_continue_record_bg)) {
            @Override
            public void action() {
                resumeScreenRecording();
                mFloatballManager.closeMenu();
            }
        };
        recorderPauseItem = new MenuItem(getApplicationContext().getResources().getDrawable(R.drawable.ic_pause_record_bg)) {
            @Override
            public void action() {
                if (RecorderService.isRecording) {
                    pauseScreenRecording();
                }
                mFloatballManager.closeMenu();
            }
        };
        recorderStopItem = new MenuItem(getApplicationContext().getResources().getDrawable(R.drawable.ic_stop_record_bg)) {
            @Override
            public void action() {
                if (RecorderService.isRecording) {
                    stopScreenRecording();
                }
                mFloatballManager.closeMenu();
            }
        };
        recorderStartItem = new MenuItem(getApplicationContext().getResources().getDrawable(R.drawable.ic_start_record_bg)) {
            @Override
            public void action() {
                if (!RecorderService.isRecording) {
                    getRecorderPermission();
                }
                mFloatballManager.closeMenu();
            }
        };

        listItem = new MenuItem(getApplicationContext().getResources().getDrawable(R.drawable.ic_list_videos_bg)) {
            @Override
            public void action() {
                Intent intent = new Intent(FloatBallService.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(Const.INTENT_PAGE_CODE, 1);
                startActivity(intent);
                mFloatballManager.closeMenu();
            }
        };
        MenuItem screenShotItem = new MenuItem(getApplicationContext().getResources().getDrawable(R.drawable.ic_screen_shot_bg)) {
            @Override
            public void action() {
                mFloatballManager.closeMenu();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                getScreenShotPermission();
            }
        };
        paintOpenItem = new MenuItem(getApplicationContext().getResources().getDrawable(R.drawable.ic_brush_bg)) {
            @Override
            public void action() {
                menuItems.set(3, paintCloseItem);
                mFloatballManager.setMenu(menuItems).buildMenu();
                mToExpandButton.setVisibility(View.VISIBLE);
                mFloatballManager.closeMenu();
            }
        };
        paintCloseItem = new MenuItem(getApplicationContext().getResources().getDrawable(R.drawable.ic_brush_close_bg)) {
            @Override
            public void action() {
                menuItems.set(3, paintOpenItem);
                mFloatballManager.setMenu(menuItems).buildMenu();
                if (mToolbar.getVisibility() == View.VISIBLE) {
                    collapseToolbarCloseSwitcherView();
                } else {
                    mToExpandButton.setVisibility(View.GONE);
                }
                mFloatballManager.closeMenu();
            }
        };
        MenuItem quitItem = new MenuItem(getApplicationContext().getResources().getDrawable(R.drawable.ic_exit_bg)) {
            @Override
            public void action() {
                AlertDialog.Builder builder = new AlertDialog.Builder(
                        new ContextThemeWrapper(
                                FloatBallService.this,
                                android.R.style.Theme_DeviceDefault_Light_Dialog))
                        .setTitle(R.string.exit)
                        .setMessage(R.string.message_exit)
                        .setPositiveButton(android.R.string.yes,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int id) {
                                        if (RecorderService.isRecording) {
                                            stopScreenRecording();
                                        }
                                        FloatBallService.this.stopSelf();
                                        mFloatballManager.hide();
                                        menuItems.clear();
                                        System.exit(0);
                                    }
                                })
                        .setNegativeButton(android.R.string.no,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog,
                                                        int id) {
                                        dialog.cancel();
                                    }
                                });
                AlertDialog alert = builder.create();
                alert.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                alert.show();
                mFloatballManager.closeMenu();
            }
        };
        menuItems.add(recorderStartItem);
        menuItems.add(listItem);
        menuItems.add(screenShotItem);
        menuItems.add(paintOpenItem);
        menuItems.add(quitItem);
        mFloatballManager.setMenu(menuItems).buildMenu();
    }

    private void getRecorderPermission() {
        if (!ActivityStackManager.getActivityManager().isForeground(this, getPackageName() + ".activity.MainActivity")) {
            ActivityStackManager.getActivityManager().finishAllActivity();
        }
        Intent intent = new Intent(this, GetPermissionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("flag", GetPermissionActivity.REQUEST_MEDIA_RECORDER_CODE);
        startActivity(intent);
    }

    private void getScreenShotPermission() {
        if (!ActivityStackManager.getActivityManager().isForeground(this, getPackageName() + ".activity.MainActivity")) {
            ActivityStackManager.getActivityManager().finishAllActivity();
        }
        Intent intent = new Intent(this, GetPermissionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("flag", GetPermissionActivity.REQUEST_MEDIA_SCREENSHOT_CODE);
        startActivity(intent);
    }


    // Set stop intent and start the recording service
    private void stopScreenRecording() {
        Intent stopIntent = new Intent(this, RecorderService.class);
        stopIntent.setAction(Const.SCREEN_RECORDING_STOP);
        startService(stopIntent);
    }

    // set resume
    private void resumeScreenRecording() {
        Intent resumeIntent = new Intent(this, RecorderService.class);
        resumeIntent.setAction(Const.SCREEN_RECORDING_RESUME);
        startService(resumeIntent);
    }

    // Set pause intent and start the recording service
    private void pauseScreenRecording() {
        Intent pauseIntent = new Intent(this, RecorderService.class);
        pauseIntent.setAction(Const.SCREEN_RECORDING_PAUSE);
        startService(pauseIntent);
    }

    /**
     * 获取屏幕信息
     */
    private void initScreenParameters() {
        mWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getRealMetrics(metrics);
        mScreenWidth = metrics.widthPixels;
        mScreenHeight = metrics.heightPixels;
        mScreenDensity = metrics.densityDpi;
    }

    /**
     * 创建背景布局
     */
    private void createBackGroundView() {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        lp.format = PixelFormat.RGBA_8888;
        lp.gravity = Gravity.START | Gravity.TOP;
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = mScreenHeight;
        lp.setTitle("jiayue_background_view");
        if (mBackGroundView == null) {
            mBackGroundView = new ImageView(this);
        }
        mWindowManager.addView(mBackGroundView, lp);
        mBackGroundView.setScaleType(ImageView.ScaleType.CENTER_CROP);
    }

    /**
     * 创建绘画布局
     */
    private void createDrawingView() {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        lp.format = PixelFormat.RGBA_8888;
        lp.gravity = Gravity.START | Gravity.TOP;
        lp.height = mScreenHeight;
        lp.setTitle("jiayue_drawing_view");
        if (mDrawingView == null) {
            mDrawingView = new DrawingView(this);
        }
        mWindowManager.addView(mDrawingView, lp);
    }

    /**
     * 创建画笔控制界面
     */
    private void createToolbarView() {
        mToolbarLayoutParams = new WindowManager.LayoutParams();
        mToolbarLayoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        mToolbarLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        mToolbarLayoutParams.format = PixelFormat.RGBA_8888;
        mToolbarLayoutParams.gravity = Gravity.START | Gravity.TOP;
        mToolbarLayoutParams.x = 0;
        mToolbarLayoutParams.y = mScreenHeight - dp2px(TOOL_BAR_HEIGHT) - dp2px(NAVIGATION_BAR_HEIGHT);
        mToolbarLayoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mToolbarLayoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mToolbarLayoutParams.setTitle("jiayue_toolbar_view");
        if (mToolbarLayout == null) {
            LayoutInflater inflater = LayoutInflater.from(getApplication());
            mToolbarLayout = (LinearLayout) inflater.inflate(R.layout.toolbar_layout, null);
        }
        mWindowManager.addView(mToolbarLayout, mToolbarLayoutParams);

        mToolbar = (LinearLayout) mToolbarLayout.findViewById(R.id.ToolBar);

        ImageButton mPenButton = (ImageButton) mToolbarLayout.findViewById(R.id.ImageButtonPen);
        mPenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dialog dialog = new ChoosePenDialog(FloatBallService.this, R.style.dialog);
                showDialog(dialog, dp2px(42), dp2px(35));
            }
        });

        ImageButton mClearButton = (ImageButton) mToolbarLayout.findViewById(R.id.imageButtonClear);
        mClearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDrawingView.undo();
            }
        });
        mClearButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mDrawingView.clear();
                return true;
            }
        });

        ImageButton mQuitAppButton = (ImageButton) mToolbarLayout.findViewById(R.id.ImageButtonQuitApp);
        mQuitAppButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menuItems.set(3, paintOpenItem);
                mFloatballManager.setMenu(menuItems).buildMenu();
                collapseToolbarCloseSwitcherView();
            }
        });
    }

    /**
     * dp转换px
     *
     * @param dp
     * @return
     */
    public int dp2px(float dp) {
        float scale = getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    /**
     * 画笔设置dialog
     *
     * @param dialog
     * @param x
     * @param y
     */
    private void showDialog(Dialog dialog, int x, int y) {
        Window dialogWindow = dialog.getWindow();
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        if (mToolbarLayoutParams.x < 0) {
            lp.x = x;
        } else {
            lp.x = mToolbarLayoutParams.x + x;
        }

        if (mToolbarLayoutParams.y < 0) {
            lp.y = y;
            dialogWindow.setGravity(Gravity.START | Gravity.TOP);
        } else if (mToolbarLayoutParams.y <= mScreenHeight / 2) {
            lp.y = mToolbarLayoutParams.y + y;
            dialogWindow.setGravity(Gravity.START | Gravity.TOP);
        } else {
            lp.y = (mScreenHeight - mToolbarLayoutParams.y) + y - dp2px(64);
            dialogWindow.setGravity(Gravity.START | Gravity.BOTTOM);
        }

        dialogWindow.setAttributes(lp);
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dialog.show();
    }

    /**
     * 创建绘画悬浮球
     */
    private void createSwitcherView() {
        final WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        lp.format = PixelFormat.RGBA_8888;
        lp.gravity = Gravity.START | Gravity.TOP;
        lp.x = 0;
        lp.y = mScreenHeight - dp2px(TOOL_BAR_HEIGHT) - dp2px(NAVIGATION_BAR_HEIGHT);
        lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.setTitle("jiayue_switcher_view");
        if (mSwitcherLayout == null) {
            LayoutInflater inflater = LayoutInflater.from(getApplication());
            mSwitcherLayout = (LinearLayout) inflater.inflate(R.layout.switcher_button_layout, null);
        }
        mWindowManager.addView(mSwitcherLayout, lp);

        mToExpandButton = (ImageButton) mSwitcherLayout.findViewById(R.id.ImageButtonToExpand);
        mToCollapseButton = (ImageButton) mSwitcherLayout.findViewById(R.id.ImageButtonToCollapse);

        mToExpandButton.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    x1 = event.getX();
                    y1 = event.getY();
                }

                if (Math.abs(event.getX() - x1) > TOUCH_MOVE_TOLERATION
                        || Math.abs(event.getY() - y1) > TOUCH_MOVE_TOLERATION) {
                    lp.x = (int) event.getRawX()
                            - mToExpandButton.getMeasuredWidth() / 2;
                    lp.y = (int) event.getRawY()
                            - mToExpandButton.getMeasuredHeight() / 2;
                    if (lp.x < 0)
                        lp.x = 0;
                    if (lp.x > mScreenWidth - dp2px(SWITCH_BUTTON_HEIGHT))
                        lp.x = mScreenWidth - dp2px(SWITCH_BUTTON_HEIGHT);
                    if (lp.y < 0)
                        lp.y = 0;
                    if (lp.y > mScreenHeight - dp2px(NAVIGATION_BAR_HEIGHT) - dp2px(SWITCH_BUTTON_HEIGHT))
                        lp.y = mScreenHeight - dp2px(NAVIGATION_BAR_HEIGHT) - dp2px(SWITCH_BUTTON_HEIGHT);

                    mWindowManager.updateViewLayout(mSwitcherLayout, lp);
                    mToolbarLayoutParams.x = lp.x;
                    mToolbarLayoutParams.y = lp.y;
                    mWindowManager.updateViewLayout(mToolbarLayout, mToolbarLayoutParams);
                }

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    x2 = event.getX();
                    y2 = event.getY();
                    if (Math.abs(x1 - x2) > TOUCH_MOVE_TOLERATION
                            || Math.abs(y1 - y2) > TOUCH_MOVE_TOLERATION) {
                        return true;
                    }
                }
                return false;
            }

        });

        mToExpandButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mToExpandButton.startAnimation(mShowExpandButtonAnimation);
                mToolbar.startAnimation(mShowToolbarAnimation);
                mToolbar.setVisibility(View.VISIBLE);
                mDrawingView.setVisibility(View.VISIBLE);
                mBackGroundView.setVisibility(View.VISIBLE);
                mPaintFrameLayout.setVisibility(View.VISIBLE);
            }
        });

        mToCollapseButton.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    x1 = event.getX();
                    y1 = event.getY();
                }

                if (Math.abs(event.getX() - x1) > TOUCH_MOVE_TOLERATION
                        || Math.abs(event.getY() - y1) > TOUCH_MOVE_TOLERATION) {
                    lp.x = (int) event.getRawX()
                            - mToExpandButton.getMeasuredWidth() / 2;
                    lp.y = (int) event.getRawY()
                            - mToExpandButton.getMeasuredHeight() / 2;
                    if (lp.x < 0)
                        lp.x = 0;
                    if (lp.x > mScreenWidth - dp2px(SWITCH_BUTTON_HEIGHT))
                        lp.x = mScreenWidth - dp2px(SWITCH_BUTTON_HEIGHT);
                    if (lp.y < 0)
                        lp.y = 0;
                    if (lp.y > mScreenHeight - dp2px(NAVIGATION_BAR_HEIGHT) - dp2px(SWITCH_BUTTON_HEIGHT))
                        lp.y = mScreenHeight - dp2px(NAVIGATION_BAR_HEIGHT) - dp2px(SWITCH_BUTTON_HEIGHT);
                    mWindowManager.updateViewLayout(mSwitcherLayout, lp);
                    mToolbarLayoutParams.x = lp.x;
                    mToolbarLayoutParams.y = lp.y;
                    mWindowManager.updateViewLayout(mToolbarLayout, mToolbarLayoutParams);
                }

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    x2 = event.getX();
                    y2 = event.getY();
                    if (Math.abs(x1 - x2) > TOUCH_MOVE_TOLERATION
                            || Math.abs(y1 - y2) > TOUCH_MOVE_TOLERATION) {
                        return true;
                    }
                }
                return false;
            }

        });

        mToCollapseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                collapseToolbar();
            }
        });
    }

    /**
     * 收起画笔控制
     */
    public void collapseToolbar() {
        mToCollapseButton.setVisibility(View.VISIBLE);
        mToCollapseButton.startAnimation(mShowCollapseButtonAnimation);
        mToolbar.startAnimation(mHiddenToolbarAnimation);
    }

    /**
     * 收起画笔控制并关闭
     */
    public void collapseToolbarCloseSwitcherView() {
        mToCollapseButton.setVisibility(View.VISIBLE);
        mToCollapseButton.startAnimation(mShowCollapseButtonAnimationClose);
        mToolbar.startAnimation(mHiddenToolbarAnimationClose);
    }

    /**
     * 初始化动画效果
     */
    private void setAnimations() {
        mShowExpandButtonAnimation = new RotateAnimation(0.0f, +350.0f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        mShowExpandButtonAnimation.setDuration(ANIMATION_DURATION);
        mShowExpandButtonAnimation
                .setAnimationListener(new Animation.AnimationListener() {

                    @Override
                    public void onAnimationStart(Animation arg0) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation arg0) {
                    }

                    @Override
                    public void onAnimationEnd(Animation arg0) {
                        mToExpandButton.setVisibility(View.GONE);
                        mToCollapseButton.setVisibility(View.VISIBLE);
                    }
                });


        mShowCollapseButtonAnimation = new RotateAnimation(0.0f, -350.0f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        mShowCollapseButtonAnimation.setDuration(ANIMATION_DURATION);
        mShowCollapseButtonAnimation
                .setAnimationListener(new Animation.AnimationListener() {

                    @Override
                    public void onAnimationStart(Animation arg0) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation arg0) {
                    }

                    @Override
                    public void onAnimationEnd(Animation arg0) {
                        mToExpandButton.setVisibility(View.VISIBLE);
                        mToCollapseButton.setVisibility(View.GONE);
                    }
                });

        mShowToolbarAnimation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, -1.0f, Animation.RELATIVE_TO_SELF,
                0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f);
        mShowToolbarAnimation.setDuration(ANIMATION_DURATION);
        mShowToolbarAnimation
                .setAnimationListener(new Animation.AnimationListener() {

                    @Override
                    public void onAnimationStart(Animation arg0) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation arg0) {
                    }

                    @Override
                    public void onAnimationEnd(Animation arg0) {
                        mToolbar.setVisibility(View.VISIBLE);
                        mDrawingView.setVisibility(View.VISIBLE);
                        mBackGroundView.setVisibility(View.VISIBLE);
                    }
                });
        mShowCollapseButtonAnimationClose = new RotateAnimation(0.0f, -350.0f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        mShowCollapseButtonAnimationClose.setDuration(ANIMATION_DURATION);
        mShowCollapseButtonAnimationClose
                .setAnimationListener(new Animation.AnimationListener() {

                    @Override
                    public void onAnimationStart(Animation arg0) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation arg0) {
                    }

                    @Override
                    public void onAnimationEnd(Animation arg0) {
                        mToCollapseButton.setVisibility(View.GONE);
                    }
                });
        mHiddenToolbarAnimationClose = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
                -1.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f);
        mHiddenToolbarAnimationClose.setDuration(ANIMATION_DURATION);
        mHiddenToolbarAnimationClose
                .setAnimationListener(new Animation.AnimationListener() {

                    @Override
                    public void onAnimationStart(Animation arg0) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation arg0) {
                    }

                    @Override
                    public void onAnimationEnd(Animation arg0) {
                        mToolbar.setVisibility(View.GONE);
                        mDrawingView.setVisibility(View.GONE);
                        mDrawingView.clear();
                        mBackGroundView.setVisibility(View.GONE);
                        mToExpandButton.setVisibility(View.GONE);
                        mPaintFrameLayout.setVisibility(View.GONE);
                        mToCollapseButton.setVisibility(View.GONE);
                    }
                });
        mHiddenToolbarAnimation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
                -1.0f, Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f);
        mHiddenToolbarAnimation.setDuration(ANIMATION_DURATION);
        mHiddenToolbarAnimation
                .setAnimationListener(new Animation.AnimationListener() {

                    @Override
                    public void onAnimationStart(Animation arg0) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation arg0) {
                    }

                    @Override
                    public void onAnimationEnd(Animation arg0) {
                        mToolbar.setVisibility(View.GONE);
                        mDrawingView.setVisibility(View.GONE);
                        mDrawingView.clear();
                        mBackGroundView.setVisibility(View.GONE);
                        mPaintFrameLayout.setVisibility(View.GONE);
                        mToExpandButton.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void setViewsInitVisibility() {
        mBackGroundView.setVisibility(View.GONE);
        mDrawingView.setVisibility(View.GONE);
        mToolbar.setVisibility(View.GONE);
        mToExpandButton.setVisibility(View.GONE);
    }


    /**
     * add by qujq
     * camera setting 监听
     */
    CameraBroadcastReceiver mCameraBroadcastReceiver;

    private void registerCameraBroadcastReceiver() {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isCameraOpen = preferences.getBoolean(getString(R.string.preference_camera_key), false);
        if (isCameraOpen) {
            CameraTextureView.open(this, new CameraTextureView.CameraTextureListener() {
                @Override
                public void onClose() {
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putBoolean(getString(R.string.preference_camera_key), false);
                    editor.apply();
                }
            });
        }
        mCameraBroadcastReceiver = new CameraBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("open_camera");
        intentFilter.addAction("close_camera");
        registerReceiver(mCameraBroadcastReceiver, intentFilter);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.exit_dialog:
                if (mToolDlg != null) {
                    mToolDlg.dismiss();
                }
                break;
            case R.id.setting:
                //tool show dialog for tool
                if (mToolDlg != null) {
                    mToolDlg.show();
                    initToolsView();
                }
                break;
            case R.id.home:
                Intent intentMain = new Intent(FloatBallService.this, MainActivity.class);
                intentMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intentMain);
                break;
            case R.id.exit:
                if (RecorderService.isRecording) {
                    stopScreenSharing();
                }
                FloatBallService.this.stopSelf();
                System.exit(0);
                break;
            case R.id.stop:
                if (mState == Const.RecordingState.STOPPED) {
                    getRecorderPermission();
                } else {
                    stopScreenSharing();
                }
                break;
//            case R.id.pause:
//                pauseScreenRecording();
//                break;
//            case R.id.resume:
//                resumeScreenRecording();
//                break;
            case R.id.pause_resume:
                if (mState == Const.RecordingState.RECORDING || mState == Const.RecordingState.RESUME) {
                    pauseScreenRecording();
                }
                if (mState == Const.RecordingState.PAUSED) {
                    resumeScreenRecording();
                }
                break;

            case R.id.shot:
                getScreenShotPermission();
                break;
            case R.id.paint:

                if (mPaintFrameLayout.getVisibility() == View.GONE) {
                    mPaintFrameLayout.setVisibility(View.VISIBLE);
                    menuItems.set(3, paintCloseItem);
                    mFloatballManager.setMenu(menuItems).buildMenu();
                    mToExpandButton.setVisibility(View.VISIBLE);
                    mToExpandButton.startAnimation(mShowExpandButtonAnimation);
                    mToolbar.startAnimation(mShowToolbarAnimation);
                    mToolbar.setVisibility(View.VISIBLE);
                    mDrawingView.setVisibility(View.VISIBLE);
                    mBackGroundView.setVisibility(View.VISIBLE);
                } else {
                    mPaintFrameLayout.setVisibility(View.GONE);
                    menuItems.set(3, paintOpenItem);
                    mFloatballManager.setMenu(menuItems).buildMenu();
                    if (mToolbar.getVisibility() == View.VISIBLE) {
                        collapseToolbar();
                    } else {
                        mToExpandButton.setVisibility(View.GONE);
                    }
                }
                break;
            case R.id.camera:
                if (prefs == null) {
                    prefs = PreferenceManager.getDefaultSharedPreferences(this);
                }
                boolean isCameraOpen = prefs.getBoolean(getString(R.string.preference_camera_key), false);
                if (!isCameraOpen) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                            != PackageManager.PERMISSION_GRANTED) {
                        getCameraPreviewPermission();
                    } else {
                        Intent intent = new Intent("open_camera");
                        FloatBallService.this.sendBroadcast(intent);
                        prefs.edit().putBoolean(getString(R.string.preference_camera_key), true).commit();
                    }
                } else {
                    Intent intent = new Intent("close_camera");
                    FloatBallService.this.sendBroadcast(intent);
                    prefs.edit().putBoolean(getString(R.string.preference_camera_key), false).commit();
                }
                break;
            case R.id.rl_choose_region :
                mToolDlg.dismiss();
                mRegionBgView.setVisibility(View.VISIBLE);
                mRegionDrawingView.setVisibility(View.VISIBLE);
                break;
        }

        //Provide an haptic feedback on button press
        Vibrator vibrate = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);
        vibrate.vibrate(100);
    }

    private void initToolsView() {
        if(prefs == null){
            prefs = PreferenceManager.getDefaultSharedPreferences(this);
        }
        mDlgExit = mToolDlg.findViewById(R.id.exit_dialog);
        mDlgExit.setOnClickListener(this);
        //added by wangxl for quick settings
        //摄像头
        mCameraSwitch = mToolDlg.findViewById(R.id.btn_switch_camera);
        mCameraSwitch.setChecked(prefs.getBoolean(getString(R.string.preference_camera_key), false));
        mCameraSwitch.setOnCheckedChangeListener(this);
        //录制音频
        mRecordeVoice =mToolDlg.findViewById(R.id.btn_switch_recorder);
        mRecordeVoice.setChecked(prefs.getBoolean(getString(R.string.audiorec_key), false));
        mRecordeVoice.setOnCheckedChangeListener(this);

        //局部录屏
        mChooseLayout = mToolDlg.findViewById(R.id.rl_choose_region);
        mChooseLayout.setOnClickListener(this);
        mPartScreen =mToolDlg.findViewById(R.id.btn_switch_partscreen);
        boolean isChecked = prefs.getBoolean(getString(R.string.preference_partscreen_key),false);
        mPartScreen.setChecked(isChecked);
        mPartScreen.setOnCheckedChangeListener(this);
        if (isChecked){
            mChooseLayout.setVisibility(View.VISIBLE);
        }else {
            mChooseLayout.setVisibility(View.GONE);
        }
        //显示点击操作
        mClickAction = mToolDlg.findViewById(R.id.btn_switch_show_touch);
        mClickAction.setChecked(prefs.getBoolean(getString(R.string.preference_show_touch_key), false));
        mClickAction.setOnCheckedChangeListener(this);
        //水印
        mWaterMark =mToolDlg.findViewById(R.id.btn_switch_water_mark);
        mWaterMark.setChecked(prefs.getBoolean(getString(R.string.preference_watermark_key), false));
        mWaterMark.setOnCheckedChangeListener(this);
        Log.d("Wxl","-------mWaterMark---------"+prefs.getBoolean(getString(R.string.preference_watermark_key), false));
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (prefs == null) {
            prefs = PreferenceManager.getDefaultSharedPreferences(this);
        }
        switch (compoundButton.getId()) {
            case R.id.btn_switch_camera:
                boolean isCameraOpen = prefs.getBoolean(getString(R.string.preference_camera_key), false);
                if (b) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                            != PackageManager.PERMISSION_GRANTED) {
                        getCameraPreviewPermission();
                    } else {
                        Intent intent = new Intent("open_camera");
                        FloatBallService.this.sendBroadcast(intent);
                        prefs.edit().putBoolean(getString(R.string.preference_camera_key), true).commit();
                    }
                } else {
                    Intent intent = new Intent("close_camera");
                    FloatBallService.this.sendBroadcast(intent);
                    prefs.edit().putBoolean(getString(R.string.preference_camera_key), false).commit();
                }
                break;
            case R.id.btn_switch_recorder :
                if (b){
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                            != PackageManager.PERMISSION_GRANTED){
                        getRecorderAudioPermission();
                    }else {
                        prefs.edit().putBoolean(getString(R.string.audiorec_key),true).commit();
                    }
                }else {
                    prefs.edit().putBoolean(getString(R.string.audiorec_key),false).commit();
                }
                break;
            case R.id.btn_switch_partscreen:
                if (b){
                    prefs.edit().putBoolean(getString(R.string.preference_partscreen_key),true).commit();
                    mChooseLayout.setVisibility(View.VISIBLE);
                }else {
                    prefs.edit().putBoolean(getString(R.string.preference_partscreen_key),false).commit();
                    mChooseLayout.setVisibility(View.GONE);
                    mRegionBgView.setVisibility(View.GONE);
                    mRegionDrawingView.setVisibility(View.GONE);
                }
                break;
            case R.id.btn_switch_show_touch:

                break;
            case R.id.btn_switch_water_mark:
                if (b){
                    prefs.edit().putBoolean(getString(R.string.preference_watermark_key),true).commit();
                }else {
                    prefs.edit().putBoolean(getString(R.string.preference_watermark_key),false).commit();
                }
                break;
            default:
                break;
        }
    }

    static class CameraBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, Intent intent) {
            switch (intent.getAction()) {
                case "open_camera":
                    CameraTextureView.open(context, new CameraTextureView.CameraTextureListener() {
                        @Override
                        public void onClose() {
                            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putBoolean(context.getString(R.string.preference_camera_key), false);
                            editor.apply();
                        }
                    });
                    break;
                case "close_camera":
                    CameraTextureView.close(context);
                    break;
            }
        }
    }

    public static void startTimerCount() {
        tvRecording.setText(R.string.text_recording);
        startTimer();
    }

    public static void stopTimerCount() {
        tvRecording.setText(R.string.text_recorder);
        stopTimer();
    }

    public static void pauseTimerCount() {
        tvRecording.setText(R.string.text_recording_pause);
        pauseTimer();
    }

    public static void setRecordingState(Const.RecordingState state) {
        mState = state;
        switch (state) {
            case PAUSED:
//                pauseIB.setEnabled(false);
//                resumeIB.setEnabled(true);
                pauseResumeIB.setImageResource(R.drawable.ic_continue_record_bg);
                break;
            case RECORDING:
                stopIB.setImageResource(R.drawable.ic_stop_record_bg);
                pauseResumeIB.setVisibility(View.VISIBLE);
                homeIB.setVisibility(View.GONE);
                pauseResumeIB.setImageResource(R.drawable.ic_pause_record_bg);
//                pauseIB.setEnabled(true);
//                resumeIB.setEnabled(false);
                break;
            case STOPPED:
                stopIB.setImageResource(R.drawable.ic_start_record_bg);
                pauseResumeIB.setVisibility(View.GONE);
                homeIB.setVisibility(View.VISIBLE);
                break;

        }
    }

    public static void startTimer() {

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
    }

    public static void pauseTimer() {
        if (ch != null) {
            ch.stop();
            ch.setText(FormatMiss(second));
        }
    }

    public class ServiceBinder extends Binder {
        FloatBallService getService() {
            return FloatBallService.this;
        }
    }

    //格式转换为HH:MM:SS
    public static String FormatMiss(int second) {
        if (second == 0) {
            return "00:00";
        }
        String hh = second / 3600 > 9 ? second / 3600 + "" : "0" + second / 3600;
        String mm = (second % 3600) / 60 > 9 ? (second % 3600) / 60 + "" : "0" + (second % 3600) / 60;
        String ss = (second % 3600) % 60 > 9 ? (second % 3600) % 60 + "" : "0" + (second % 3600) % 60;
        if (second > 3600) {
            return hh + ":" + mm + ":" + ss;
        }
        return mm + ":" + ss;
    }

    private void getCameraPreviewPermission() {
        if (!ActivityStackManager.getActivityManager().isForeground(this, getPackageName() + ".activity.MainActivity")) {
            ActivityStackManager.getActivityManager().finishAllActivity();
        }
        Intent intent = new Intent(this, GetPermissionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("flag", GetPermissionActivity.REQUEST_MEDIA_CAMERA_CODE);
        startActivity(intent);
    }
    private void getRecorderAudioPermission() {
        if (!ActivityStackManager.getActivityManager().isForeground(this,getPackageName()+".activity.MainActivity")) {
            ActivityStackManager.getActivityManager().finishAllActivity();
        }
        Intent intent = new Intent(this, GetPermissionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("flag", GetPermissionActivity.REQUEST_MEDIA_AUDIO_CODE);
        startActivity(intent);
    }

    private void createRegionBgView(){
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        lp.format = PixelFormat.RGBA_8888;
        lp.gravity = Gravity.START | Gravity.TOP;
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = mScreenHeight;
        lp.setTitle("jiayue_background_view");
        if (mRegionBgView == null) {
            mRegionBgView = new ImageView(this);
        }
        mRegionBgView.setImageResource(R.color.transparent);
        mWindowManager.addView(mRegionBgView, lp);
        mRegionBgView.setScaleType(ImageView.ScaleType.CENTER_CROP);
    }
    private void createRegionDrawingView(){
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        lp.format = PixelFormat.RGBA_8888;
        lp.gravity = Gravity.START | Gravity.TOP;
        lp.height = mScreenHeight;
        lp.setTitle("jiayue_drawing_view");
        if (mRegionDrawingView == null) {
            mRegionDrawingView = new RegionView(this);
        }
        mWindowManager.addView(mRegionDrawingView, lp);
    }
    // Set stop intent and start the recording service
    private void stopScreenSharing() {
        Intent stopIntent = new Intent(this, RecorderService.class);
        stopIntent.setAction(Const.SCREEN_RECORDING_STOP);
        startService(stopIntent);
    }
}
