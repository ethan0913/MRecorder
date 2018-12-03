package com.ethan.screencapture.folderpicker;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;


import com.ethan.screencapture.Const;
import com.ethan.screencapture.R;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by wxl on 02-12-2018.
 */

class DirectoryRecyclerAdapter extends RecyclerView.Adapter<DirectoryRecyclerAdapter.ItemViewHolder> {
    private static OnDirectoryClickedListerner onDirectoryClickedListerner;
    private Context context;
    private ArrayList<File> directories;

    DirectoryRecyclerAdapter(Context context, OnDirectoryClickedListerner listerner, ArrayList<File> directories){
        this.context = context;
        onDirectoryClickedListerner = listerner;
        this.directories = directories;
    }

    @Override
    public DirectoryRecyclerAdapter.ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.content_directory_chooser, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final DirectoryRecyclerAdapter.ItemViewHolder holder, int position) {
        holder.dir.setText(directories.get(position).getName());
        holder.dir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(Const.TAG, "Item clicked: " + directories.get(holder.getAdapterPosition()));
                onDirectoryClickedListerner.OnDirectoryClicked(directories.get(holder.getAdapterPosition()));
            }
        });
    }

    @Override
    public int getItemCount() {
        return directories.size();
    }

    interface OnDirectoryClickedListerner {
        void OnDirectoryClicked(File directory);
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder{
        TextView dir;
        LinearLayout dir_view;
        public ItemViewHolder(View itemView) {
            super(itemView);
            dir = itemView.findViewById(R.id.directory);
            dir_view = itemView.findViewById(R.id.directory_view);
        }
    }
}
