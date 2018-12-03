package com.ethan.screencapture.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.TextView;

import com.ethan.screencapture.Const;
import com.ethan.screencapture.R;
import com.ethan.screencapture.adapter.VideoChooseRecyclerAdapter;

import java.io.File;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
/**
 * Created by ws on 2018/6/4.
 */
public class VideoChoose extends AppCompatActivity {
    RecyclerView mRecyclerView;
    TextView noVideo;
    private SharedPreferences sharedPreferences;
    ArrayList<File> filesList;
    VideoChooseRecyclerAdapter.ItemClickListener itemClickListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_choose);
        mRecyclerView = findViewById(R.id.choose_video_recycler);
        noVideo = findViewById(R.id.chose_video_null);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
//        获取视频数据
        File directory = new File(sharedPreferences.getString(getString(R.string.savelocation_key),
                Environment.getExternalStorageDirectory()
                        + File.separator + "screenrecorder"));
        if (!directory.exists()) {
            File appDir = new File(Environment.getExternalStorageDirectory() + File.separator + Const.APPDIR);
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED) && !appDir.isDirectory()) {
                appDir.mkdirs();
            }
        }
        filesList = new ArrayList<File>();
        if (directory.isDirectory() && directory.exists()) {
            filesList.addAll(Arrays.asList(getVideos(directory.listFiles())));
        }
        itemClickListener = new VideoChooseRecyclerAdapter.ItemClickListener() {

            @Override
            public void onClick(int position) {
                String file = filesList.get(position).toString();
                Intent toEdit = new Intent(VideoChoose.this, VideoEditActivity.class);
                toEdit.putExtra(Const.VIDEO_EDIT_URI_KEY,
                        Uri.fromFile(filesList.get(position)).toString());
                Log.d(Const.TAG_ETHAN + "VideoChoose", "" + file);
                startActivity(toEdit);
                VideoChoose.this.finish();
            }
        };
        mRecyclerView.setHasFixedSize(true);
        final GridLayoutManager layoutManager = new GridLayoutManager(this, 3);
        mRecyclerView.setLayoutManager(layoutManager);
        final VideoChooseRecyclerAdapter adapter = new VideoChooseRecyclerAdapter(
                filesList, this, itemClickListener);
        mRecyclerView.setAdapter(adapter);
    }

    private File[] getVideos(File[] files) {
        List<File> newFiles = new ArrayList<>();
        for (File file : files) {
            if (!file.isDirectory() && isVideoFile(file.getPath()))
                newFiles.add(file);
        }
        return newFiles.toArray(new File[newFiles.size()]);
    }

    private static boolean isVideoFile(String path) {
        String mimeType = URLConnection.guessContentTypeFromName(path);
        return mimeType != null && mimeType.startsWith("video");
    }
}
