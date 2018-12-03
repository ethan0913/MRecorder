package com.ethan.screencapture.fragment.dialogfragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.ethan.screencapture.R;

/**
 * Created by ws on 2018/7/4.
 */

public class DialogFragmentRotate extends DialogFragment implements View.OnClickListener {
    public RadioGroup mRadioGroup;
    public Button rotateButton;
    public Button rotateCloseButton;
    int rotate = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_fragment_rotate, container);
        mRadioGroup = view.findViewById(R.id.rotate_chose_group);
        rotateButton = view.findViewById(R.id.rotate_button);
        rotateCloseButton = view.findViewById(R.id.rotate_close_button);
        rotateButton.setOnClickListener(this);
        rotateCloseButton.setOnClickListener(this);
        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.rotate_chose_90:
                        rotate = 90;
                        break;
                    case R.id.rotate_chose_180:
                        rotate = 180;
                        break;
                    case R.id.rotate_chose_270:
                        rotate = 270;
                        break;
                }
            }
        });
        return view;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.rotate_close_button:
                dismiss();
                break;
            case R.id.rotate_button:
                if (rotate != 0) {
                    dismiss();
                } else {
                    Toast.makeText(getActivity(), "请选择旋转角度", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
}
