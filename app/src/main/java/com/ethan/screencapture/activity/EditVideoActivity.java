package com.ethan.screencapture.activity;

import android.app.ProgressDialog;
import android.media.MediaMetadataRetriever;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.ethan.screencapture.Const;
import com.ethan.screencapture.R;
import com.ethan.screencapture.util.ActivityStackManager;

import java.io.File;
import java.util.ArrayList;

import life.knowledge4.videotrimmer.K4LVideoTrimmer;
import life.knowledge4.videotrimmer.interfaces.OnTrimVideoListener;

public class EditVideoActivity extends AppCompatActivity implements OnTrimVideoListener{
    private ProgressDialog saveprogress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityStackManager.getActivityManager().addActivity(this);
        setContentView(R.layout.activity_edit_video);

        if(!getIntent().hasExtra(Const.VIDEO_EDIT_URI_KEY)) {
            Toast.makeText(this, "Video not found. Please try again", Toast.LENGTH_SHORT).show();
            finish();
        }

        Uri videoUri = Uri.parse(getIntent().getStringExtra(Const.VIDEO_EDIT_URI_KEY));

        K4LVideoTrimmer videoTrimmer = findViewById(R.id.videoTimeLine);

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        //use one of overloaded setDataSource() functions to set your data source
        retriever.setDataSource(this, videoUri);
        String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        int timeInMins = (((int)Long.parseLong(time)) / 1000)+1000;
        Log.d(Const.TAG, timeInMins+"");

        File video = new File(videoUri.getPath());

        videoTrimmer.setOnTrimVideoListener(this);
        videoTrimmer.setVideoURI(videoUri);
        videoTrimmer.setMaxDuration(timeInMins);
        Log.d(Const.TAG, "Edited file save name: " + video.getAbsolutePath());
        videoTrimmer.setDestinationPath(video.getParent()+"/");
    }

    @Override
    public void getResult(Uri uri) {
        Log.d(Const.TAG, uri.getPath());
        indexFile(uri.getPath());

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                saveprogress = new ProgressDialog(EditVideoActivity.this);
                saveprogress.setMessage("Please wait while the video is being saved");
                saveprogress.setTitle("Please wait");
                saveprogress.setIndeterminate(true);
                saveprogress.show();
            }
        });
    }

    @Override
    public void cancelAction() {
        finish();
    }

    private void indexFile(String SAVEPATH) {
        //Create a new ArrayList and add the newly created video file path to it
        ArrayList<String> toBeScanned = new ArrayList<>();
        toBeScanned.add(SAVEPATH);
        String[] toBeScannedStr = new String[toBeScanned.size()];
        toBeScannedStr = toBeScanned.toArray(toBeScannedStr);

        //Request MediaScannerConnection to scan the new file and index it
        MediaScannerConnection.scanFile(this, toBeScannedStr, null, new MediaScannerConnection.OnScanCompletedListener() {

            @Override
            public void onScanCompleted(String path, Uri uri) {
                Log.i(Const.TAG, "SCAN COMPLETED: " + path);
                saveprogress.cancel();
                setResult(Const.VIDEO_EDIT_RESULT_CODE);
                finish();
            }
        });
    }

}
