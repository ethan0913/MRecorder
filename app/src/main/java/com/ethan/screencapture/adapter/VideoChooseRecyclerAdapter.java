package com.ethan.screencapture.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.ethan.screencapture.R;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by ws on 2018/6/19.
 */

public class VideoChooseRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    ArrayList<File> dateList;
    private Context mContext;
    //    private RequestQueue mQueue;
    private ItemClickListener itemClick;

    public VideoChooseRecyclerAdapter(ArrayList list, Context context, ItemClickListener click) {
        this.dateList = list;
        this.mContext = context;
//        if (mQueue == null) {
//            mQueue = Volley.newRequestQueue(context);
//        }
        this.itemClick = click;
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).
                inflate(R.layout.videochoose_recycler_item, parent, false);
        return new RecyclerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final RecyclerViewHolder itemViewHolder = (RecyclerViewHolder) holder;
        Uri videofile = Uri.fromFile(dateList.get(position));
        ImageLoader.getInstance().displayImage(videofile + "",
                itemViewHolder.mItem, getImageOptions());
        final int p=position;
        itemViewHolder.mItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                itemClick.onClick(p);
            }
        });
//        volley不能加载本地图片
//        ImageRequest imgRequest = new ImageRequest(videofile+"",
//                new Response.Listener<Bitmap>() {
//                    @Override
//                    public void onResponse(Bitmap arg0) {
//                        itemViewHolder.mItem.setImageBitmap(arg0);
//                    }
//                }, 0, 0, Bitmap.Config.ARGB_8888, null);
//        mQueue.add(imgRequest);
//        switch (position % 3) {
//            case 1:
//                itemViewHolder.mItem.setBackgroundColor(mContext.getResources().getColor(R.color.red));
//                break;
//            case 2:
//                itemViewHolder.mItem.setBackgroundColor(mContext.getResources().getColor(R.color.blue));
//                break;
//            case 3:
//                itemViewHolder.mItem.setBackgroundColor(mContext.getResources().getColor(R.color.actionMode_background_black));
//                break;
//            case 4:
//                itemViewHolder.mItem.setBackgroundColor(mContext.getResources().getColor(R.color.green));
//                break;
//        }
    }

    @Override
    public int getItemCount() {
        return dateList.size();
    }

    public static DisplayImageOptions getImageOptions() {
        return new DisplayImageOptions.Builder().showImageForEmptyUri(R.drawable.btn_to_expand)
                .showImageOnLoading(R.drawable.btn_to_expand)//默认图片
                .showImageOnFail(R.drawable.btn_to_expand)//默认图片
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .bitmapConfig(Bitmap.Config.RGB_565).build();
    }

    public class RecyclerViewHolder extends RecyclerView.ViewHolder {
        private ImageView mItem;

        public RecyclerViewHolder(View itemView) {
            super(itemView);
            mItem = itemView.findViewById(R.id.video_choose_item_image);
        }

    }

    public interface ItemClickListener {
        void onClick(int position);
    }

}
