package com.ethan.screencapture.folderpicker;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.Preference;

/**
 * Created by wxl on 02-12-2018.
 */

public class SavedStateHandler extends Preference.BaseSavedState {
    public static final Creator<SavedStateHandler> CREATOR = new Creator<SavedStateHandler>() {
        public SavedStateHandler createFromParcel(Parcel in) {
            return new SavedStateHandler(in);
        }

        public SavedStateHandler[] newArray(int size) {
            return new SavedStateHandler[size];
        }
    };
    public final String selectedDir;
    public final Bundle dialogState;

    public SavedStateHandler(Parcelable superState, String selectedDir, Bundle dialogState) {
        super(superState);
        this.selectedDir = selectedDir;
        this.dialogState = dialogState;
    }

    public SavedStateHandler(Parcel source) {
        super(source);
        selectedDir = source.readString();
        dialogState = source.readBundle();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(selectedDir);
        dest.writeBundle(dialogState);
    }
}
