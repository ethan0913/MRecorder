package com.ethan.screencapture.activity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.ethan.screencapture.Const;
import com.ethan.screencapture.R;
import com.ethan.screencapture.fragment.dialogfragment.DialogFragmentRotate;

import java.io.File;
import java.util.Locale;

/**
 * Created by ws on 2018/6/4.
 */
public class VideoEditActivity extends AppCompatActivity implements View.OnClickListener {
    public VideoView mVideoView;
    public Uri videoUri;
    public ImageView playImage;
    public SeekBar mSeekBar;
    public TextView play_star;
    public TextView play_end;
    static Handler handler;
    public File directory;
    public String originalPath;
    public String mPath;
    public String editPath;

    public LinearLayout videoEditTrim;
    public LinearLayout videoEditAddMusic;
    public LinearLayout videoEditAddCaption;
    public LinearLayout videoEditAddBg;
    public LinearLayout videoEditEditCrop;
    public LinearLayout videoEditEditSpeed;
    public LinearLayout videoEditEditRotate;
    private SharedPreferences sharedPreferences;
    private ProgressDialog mProgressDialog;

    public static final int SEEK_BAR_PROGRESS = 0;
    public static final int DISMISS_PROGRESS_DIALOG = 1;

    DialogFragmentRotate mRotateDialog;

    Thread CleanFile = new Thread(new Runnable() {
        @Override
        public void run() {
            if (directory.list().length > 0) {
                File[] files = directory.listFiles();
                for (int i = 0; i < files.length; i++) {
                    File f = files[i];
                    f.delete();
                }
            }
            handler.sendEmptyMessage(DISMISS_PROGRESS_DIALOG);
        }
    });

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_edit);
        if (!getIntent().hasExtra(Const.VIDEO_EDIT_URI_KEY)) {
            Toast.makeText(this, "Video not found. Please try again", Toast.LENGTH_SHORT).show();
            finish();
        }
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        originalPath = getIntent().getStringExtra(Const.VIDEO_EDIT_URI_KEY);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        directory = new File(sharedPreferences.getString(getString(R.string.savelocation_key),
                Environment.getExternalStorageDirectory()
                        + File.separator + "screenrecorder/edit"));
        if (!directory.exists()) {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) && !directory.isDirectory()) {
                directory.mkdirs();
            }
        }
        mPath = originalPath;
        videoUri = Uri.parse(originalPath);
        handler = new Handler() {
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case SEEK_BAR_PROGRESS:
                        setScckBar();
                        if (mVideoView.isPlaying()) {
                            removeMessages(SEEK_BAR_PROGRESS);
                            sendEmptyMessageDelayed(SEEK_BAR_PROGRESS, 1000);
                        }
                        break;
                    case DISMISS_PROGRESS_DIALOG:
                        mProgressDialog.dismiss();
                        break;
                }
            }
        };
        initView();
        initVideo();
        mProgressDialog.setTitle("视频处理中");
        mProgressDialog.show();
        CleanFile.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        menu.add(0, 1, 0, "保存").setEnabled(true);
        menu.add(0, 1, 0, "保存")
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish(); // back button
                break;
            case 1:
                Toast.makeText(this, "111111111", Toast.LENGTH_SHORT).show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    public void initView() {
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
        mVideoView = findViewById(R.id.video_edit_video);
        mVideoView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mVideoView.pause();
                playImage.setVisibility(View.VISIBLE);
                return false;
            }
        });
        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                playImage.setVisibility(View.VISIBLE);
            }
        });
        mSeekBar = findViewById(R.id.video_edit_seekbar);
        playImage = findViewById(R.id.video_play);
        playImage.setOnClickListener(this);
        play_star = findViewById(R.id.video_edit_star);
        play_end = findViewById(R.id.video_edit_end);
        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                mVideoView.seekTo(progress);
                handler.sendEmptyMessage(SEEK_BAR_PROGRESS);
            }
        });
        videoEditTrim = findViewById(R.id.video_edit_trim);
        videoEditAddMusic = findViewById(R.id.video_edit_add_music);
        videoEditAddCaption = findViewById(R.id.video_edit_add_caption);
        videoEditAddBg = findViewById(R.id.video_edit_add_bg);
        videoEditEditCrop = findViewById(R.id.video_edit_crop);
        videoEditEditSpeed = findViewById(R.id.video_edit_speed);
        videoEditEditRotate = findViewById(R.id.video_edit_rotate);
        videoEditEditRotate.setOnClickListener(this);
    }

    public void initVideo() {
        mVideoView.setVideoURI(videoUri);
        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                playImage.setVisibility(View.VISIBLE);
            }
        });

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.video_play:
                mVideoView.start();
                handler.sendEmptyMessage(SEEK_BAR_PROGRESS);
                playImage.setVisibility(View.GONE);
                break;
            case R.id.video_edit_trim:

                break;
            case R.id.video_edit_add_music:
                break;
            case R.id.video_edit_add_caption:
                break;
            case R.id.video_edit_add_bg:
                break;
            case R.id.video_edit_crop:
                break;
            case R.id.video_edit_speed:
                break;
            case R.id.video_edit_rotate:
                mRotateDialog = new DialogFragmentRotate();
                mRotateDialog.show(getSupportFragmentManager(), "rotate");
                break;
        }
    }

    public static String formatTime(String pattern, int milli) {
        int m = (int) (milli / Const.MINUTE_IN_MILLIS);
        int s = (int) ((milli / Const.SECOND_IN_MILLIS) % 60);
        String mm = String.format(Locale.getDefault(), "%02d", m);
        String ss = String.format(Locale.getDefault(), "%02d", s);
        return pattern.replace("mm", mm).replace("ss", ss);
    }

    public void setScckBar() {
        mSeekBar.setMax(mVideoView.getDuration());
        mSeekBar.setProgress(mVideoView.getCurrentPosition());
        play_end.setText(formatTime("mm:ss", mVideoView.getDuration()));
        play_star.setText(formatTime("mm:ss", mVideoView.getCurrentPosition()));
//        MediaController mediaController=new MediaController(this);
//        mediaController.setMediaPlayer(mVideoView);
//        mVideoView.setMediaController(mediaController);
    }


}
