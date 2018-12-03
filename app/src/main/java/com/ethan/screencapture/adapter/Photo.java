package com.ethan.screencapture.adapter;

import android.graphics.Bitmap;

import java.io.File;
import java.util.Date;

/**
 * Created by wxl on 03-12-2018.
 */

public class Photo implements Comparable<Photo> {
    private String FileName;
    private File file;
    private Bitmap thumbnail;
    private Date lastModified;
    private boolean isSection = false;
    private boolean isSelected = false;

    public Photo(boolean isSection, Date lastModified) {
        this.isSection = isSection;
        this.lastModified = lastModified;
    }

    public Photo(String fileName, File file, Bitmap thumbnail, Date lastModified) {
        FileName = fileName;
        this.file = file;
        this.thumbnail = thumbnail;
        this.lastModified = lastModified;
    }

    public String getFileName() {
        return FileName;
    }

    public File getFile() {
        return file;
    }

    public Bitmap getThumbnail() {
        return thumbnail;
    }

    public Date getLastModified() {
        return lastModified;
    }

    public boolean isSection() {
        return isSection;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    @Override
    public int compareTo(Photo video) {
        return getLastModified().compareTo(video.getLastModified());
    }
}
