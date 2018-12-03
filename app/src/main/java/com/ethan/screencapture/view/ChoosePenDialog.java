package com.ethan.screencapture.view;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.SimpleAdapter;


import com.ethan.screencapture.R;
import com.ethan.screencapture.service.FloatBallService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChoosePenDialog extends Dialog {

    private Integer[] mPenSizes = {DrawingView.STROKE_WIDTH_THIN, DrawingView.STROKE_WIDTH_MEDIUM, DrawingView.STROKE_WIDTH_THICK};
    private Integer[] mPenColors = {0xbfff0000, 0xbf00ff00, 0xbf0000ff};
    private Integer[] mPenShapes = {DrawingView.SHAPE_CURVE, DrawingView.SHAPE_ROUND_RECT, DrawingView.SHAPE_ARROW};
    private Integer[] mBackGrounds = {R.color.transparent, R.drawable.paper_white, R.drawable.paper_black};

    private List<Integer> drawableList = new ArrayList<>();

    {
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

    }


    private FloatBallService floatBallService;

    private void initGrid() {
        List<Map<String, Object>> items = new ArrayList<>();

        for (Object item : drawableList) {
            Map<String, Object> map = new HashMap<>();
            map.put("image", item);
            items.add(map);
        }

        SimpleAdapter adapter = new SimpleAdapter(getContext(), items,
                R.layout.choosepen_dialog_grid_item, new String[]{"image"},
                new int[]{R.id.item_image});

        GridView gridview = (GridView) findViewById(R.id.myGridView);

        gridview.setAdapter(adapter);

        gridview.setOnItemClickListener(new OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                if (position < 3) {
                    floatBallService.mDrawingView.setPenSize(mPenSizes[position]);
                } else if (position < 6) {
                    floatBallService.mDrawingView.setPenColor(mPenColors[position - 3]);
                } else if (position < 9) {
                    floatBallService.mDrawingView.setShape(mPenShapes[position - 6]);
                } else if (position < 12) {
                    floatBallService.mBackGroundView.setImageResource(mBackGrounds[position - 9]);
                }
                dismiss();
            }
        });

    }

    public ChoosePenDialog(Context context, int theme) {
        super(context, theme);
        floatBallService = (FloatBallService) context;
        setContentView(R.layout.dialog_choose_pen);
        setCanceledOnTouchOutside(true);
        initGrid();
    }

}
