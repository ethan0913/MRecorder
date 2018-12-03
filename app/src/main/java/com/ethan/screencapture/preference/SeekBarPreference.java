package com.ethan.screencapture.preference;

import android.content.Context;
import android.graphics.Color;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.ethan.screencapture.Const;
import com.ethan.screencapture.R;


/**
 * Created by wxl on 02-12-2018.
 */

public class SeekBarPreference extends DialogPreference implements SeekBar.OnSeekBarChangeListener {
    private static final String androidns="http://schemas.android.com/apk/res/android";

    private SeekBar mSeekBar;
    private TextView mValueText;
    private ImageView mFloatingTogglePreview;

    private String mSuffix;
    private int mDefault, mMax, mValue = 0;

    private int defaultColor;

    private ViewGroup.LayoutParams mIV_pararams;

    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context,attrs);

        //Set the preferenceto be persistent
        setPersistent(true);

        //Set custom preferece layout
        setDialogLayoutResource(R.layout.layout_floating_control_preview);

        //Get default values from xml
        mSuffix = attrs.getAttributeValue(androidns,"text");
        mDefault = attrs.getAttributeIntValue(androidns,"defaultValue", 100);
        mMax = attrs.getAttributeIntValue(androidns,"max", 200);
    }

    private void init(){
        mSeekBar.setOnSeekBarChangeListener(this);

        //Get the default layout params of the image view to change layout parameters later
        mIV_pararams = mFloatingTogglePreview.getLayoutParams();

        //Change height and width of imageview with value saved previously
        mFloatingTogglePreview.setLayoutParams(generateLayoutParams(mValue));

        //Display the size in a textview
        mValueText.setText(mSuffix == null ? String.valueOf(mValue) : String.valueOf(mValue).concat(mSuffix));

        Log.d(Const.TAG, "Max: " + mMax + "     ,Progress: " + mValue);
    }

    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);
        mValue = getPersistedInt(mDefault);
        Log.d(Const.TAG, "size is: " + mValue);
        mSeekBar = v.findViewById(R.id.seekBar);
        mValueText = v.findViewById(R.id.tv_size);
        defaultColor = mValueText.getTextColors().getDefaultColor();
        mFloatingTogglePreview = v.findViewById(R.id.iv_floatingControl);
        mSeekBar.setMax(mMax);
        mSeekBar.setProgress(mValue);

        init();
    }

    @Override
    protected void onSetInitialValue(boolean restore, Object defaultValue)
    {
        super.onSetInitialValue(restore, defaultValue);
        if (restore)
            mValue = shouldPersist() ? getPersistedInt(mDefault) : 100;
        else
            mValue = (Integer)defaultValue;
    }

    public void onProgressChanged(SeekBar seek, int value, boolean fromTouch)
    {
        //Set the min value of seekbar to 70.
        if (value < 70)
            mValueText.setTextColor(Color.RED);
        else
            mValueText.setTextColor(defaultColor);

        if (value < 25){
            mSeekBar.setProgress(25);
            return;
        }

        //Display selected value in textview
        String t = String.valueOf(value);
        mValueText.setText(mSuffix == null ? t : t.concat(mSuffix));

        //Update imageview size
        mFloatingTogglePreview.setLayoutParams(generateLayoutParams(value));

        //Save the value to preference
        if (shouldPersist())
            persistInt(value);
        callChangeListener(value);
    }
    public void onStartTrackingTouch(SeekBar seek) {}
    public void onStopTrackingTouch(SeekBar seek) {}

    //Method to generate updated LayoutParams from default LayoutParams
    private ViewGroup.LayoutParams generateLayoutParams(int value){
        int px = dpToPx(value);
        mIV_pararams.height = px;
        mIV_pararams.width = px;
        return mIV_pararams;
    }


    //Method to convert dp to px
    private int dpToPx(int dp) {
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        return Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
    }
}
