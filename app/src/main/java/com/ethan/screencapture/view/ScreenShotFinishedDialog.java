package com.ethan.screencapture.view;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.FileProvider;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.ethan.screencapture.R;
import com.ethan.screencapture.service.RecorderService;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class ScreenShotFinishedDialog extends Dialog {
    Button shareBtn;
    Button deleteBtn;
    Button cancelBtn;
    ImageView imageView;
    TextView textView;
    TextView tileView;
    RecorderService floatButtonService;
    Uri uri;
    String SAVEPATH;

    public ScreenShotFinishedDialog(Context context, int theme, Uri uri, String SAVEPATH) {
        super(context, theme);
        floatButtonService = (RecorderService) context;
        this.uri = uri;
        this.SAVEPATH=SAVEPATH;
        setContentView(R.layout.notification_template_big_picture);
        setCanceledOnTouchOutside(true);
        initDialog();
    }

    private void initDialog() {
        shareBtn = (Button) findViewById(R.id.share);
        deleteBtn = (Button) findViewById(R.id.delete);
        cancelBtn = (Button) findViewById(R.id.cancel);
        imageView = (ImageView) findViewById(R.id.big_picture);
        textView = (TextView) findViewById(R.id.text);
        tileView = (TextView) findViewById(R.id.title);
        tileView.setText(R.string.drawing_saved);
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(uri.getPath());
        Bitmap thumbnail = mmr.getFrameAtTime();
        if (thumbnail != null) {
            imageView.setImageBitmap(thumbnail);
        }
        textView.setText(R.string.notification_recording_finished_text);
        ImageView iconview = (ImageView) findViewById(R.id.icon);
        iconview.setImageResource(R.drawable.ic_save);

        shareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                shareImage();
                dismiss();

            }
        });

        deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                File file = new File(uri.getPath());
                if (file.exists())
                    file.delete();
                Intent media = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                media.setData(uri);
                floatButtonService.sendBroadcast(media);
                dismiss();
            }
        });

        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                cancel();
            }
        });

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
               Uri  videoUri = FileProvider.getUriForFile(
                        getContext(), getContext().getPackageName() + ".provider",
                        new File(SAVEPATH));
                Intent intent = new Intent(Intent.ACTION_VIEW);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.setDataAndType(videoUri, "video/*");
                } else {
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setDataAndType(Uri.fromFile(new File(SAVEPATH)), "video/*");
                }
                floatButtonService.startActivity(intent);

                dismiss();
            }
        });
    }

    private void shareImage() {
        long time = System.currentTimeMillis();

        String subjectDate = new SimpleDateFormat("hh:mma, MMM dd, yyyy", Locale.getDefault())
                .format(new Date(time));
        String subject = String.format("ScreenShot (%s)", subjectDate);
        Intent sharingIntent = new Intent(Intent.ACTION_SEND);
        sharingIntent.setType("video/mp4");
        sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);
        sharingIntent.putExtra(Intent.EXTRA_SUBJECT, subject);

        Intent chooserIntent = Intent
                .createChooser(sharingIntent, null);
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_NEW_TASK);
        floatButtonService.startActivity(chooserIntent);
    }

    public void setBigPicture(Bitmap bm) {
        imageView.setImageBitmap(bm);
    }

    public void setSavedFileName(String s) {
        textView.setText(s);
    }

}
