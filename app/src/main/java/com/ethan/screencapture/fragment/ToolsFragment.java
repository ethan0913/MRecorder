package com.ethan.screencapture.fragment;


import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.ethan.screencapture.R;
import com.ethan.screencapture.activity.VideoChoose;

/**
 * Created by limz on 2018/5/29.
 */

public class ToolsFragment extends Fragment {
    LinearLayout editingLayout;
    LinearLayout splicingLayout;
    LinearLayout toGifLayout;

    public ToolsFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tools, container, false);
        editingLayout = view.findViewById(R.id.video_editing);
        splicingLayout = view.findViewById(R.id.video_splicing);
        toGifLayout = view.findViewById(R.id.video_to_gif);
        editingLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent chooseToEditing = new Intent(getActivity(),
                        VideoChoose.class);
                startActivity(chooseToEditing);
            }
        });
        splicingLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent chooseToEditing = new Intent(getActivity(),
                        VideoChoose.class);
                startActivity(chooseToEditing);
            }
        });
        toGifLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent chooseToEditing = new Intent(getActivity(),
                        VideoChoose.class);
                startActivity(chooseToEditing);
            }
        });
        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
