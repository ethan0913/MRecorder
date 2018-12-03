package com.ethan.screencapture.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ethan.screencapture.Const;
import com.ethan.screencapture.R;
import com.ethan.screencapture.fragment.ScreenShotFragment;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import me.kareluo.imaging.IMGEditActivity;

/**
 * Created by wxl on 03-12-2018.
 */

//Custom Recycler view adapter for photo list fragment
public class PhotoRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_SECTION = 0;
    private static final int VIEW_ITEM = 1;
    private ScreenShotFragment photoListFragment;
    private ArrayList<Photo> photos;
    private Context context;
    private boolean isMultiSelect = false;
    private int count = 0;
    private ActionMode mActionMode;
    // Show a contextual menu in multiselect mode
    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.video_list_action_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false; // Return false if nothing is done
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            // Build an arraylist of selected item positions when an menu item is clicked
            ArrayList<Integer> positions = new ArrayList<>();
            for (Photo photo : photos) {
                if (photo.isSelected()) {
                    positions.add(photos.indexOf(photo));
                }
            }
            switch (item.getItemId()) {
                case R.id.delete:
                    if (!positions.isEmpty())
                        confirmDelete(positions);
                    mActionMode.finish();
                    break;
                case R.id.share:
                    if (!positions.isEmpty())
                        sharephotos(positions);
                    mActionMode.finish();
                    break;
            }
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            // remove all selected photos and reload recyclerview
            for (Photo photo : photos) {
                photo.setSelected(false);
            }
            isMultiSelect = false;
            notifyDataSetChanged();
        }
    };

    public PhotoRecyclerAdapter(Context context, ArrayList<Photo> android, ScreenShotFragment photoListFragment) {
        this.photos = android;
        this.context = context;
        this.photoListFragment = photoListFragment;
    }

    //Find if the view is a section type or photo type
    @Override
    public int getItemViewType(int position) {
        return isSection(position) ? VIEW_SECTION : VIEW_ITEM;
    }

    //Method to determine the type
    public boolean isSection(int position) {
        return photos.get(position).isSection();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        View view;
        switch (viewType) {
            case VIEW_SECTION:
                view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.content_photo_section, viewGroup, false);
                return new SectionViewHolder(view);
            case VIEW_ITEM:
                view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.content_photo, viewGroup, false);
                return new ItemViewHolder(view);
            default:
                view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.content_photo, viewGroup, false);
                return new ItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        final Photo photo = photos.get(position);
        switch (holder.getItemViewType()) {
            case VIEW_ITEM:
                final ItemViewHolder itemViewHolder = (ItemViewHolder) holder;
                //Set photo file name
                itemViewHolder.tv_fileName.setText(photo.getFileName());
                //If thumbnail has failed for some reason, set empty image resource to imageview
                if (photos.get(position).getThumbnail() != null) {
                    itemViewHolder.iv_thumbnail.setImageBitmap(photo.getThumbnail());
                } else {
                    itemViewHolder.iv_thumbnail.setImageResource(0);
                    Log.d(Const.TAG, "thumbnail error");
                }

                // Hide the play image over thumbnail and overflow menu if multiselect enabled
                if (isMultiSelect) {

                } else {

                }

                // Set foreground color to identify selected items
                if (photo.isSelected()) {
                    itemViewHolder.selectableFrame.setForeground(new ColorDrawable(ContextCompat.getColor(context, R.color.multiSelectColor)));
                } else {
                    itemViewHolder.selectableFrame.setForeground(new ColorDrawable(ContextCompat.getColor(context, android.R.color.transparent)));
                }

                itemViewHolder.overflow.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        PopupMenu popupMenu = new PopupMenu(context, view);
                        popupMenu.inflate(R.menu.photo_popupmenu);
                        popupMenu.show();
                        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                switch (item.getItemId()) {
                                    case R.id.share:
                                        sharephoto(itemViewHolder.getAdapterPosition());
                                        break;
                                    case R.id.delete:
                                        confirmDelete(holder.getAdapterPosition());
                                        break;
                                    case R.id.edit:
//                                        Intent editIntent = new Intent(context, EditVideoActivity.class);
//                                        editIntent.putExtra(Const.VIDEO_EDIT_URI_KEY,
//                                                Uri.fromFile(photo.getFile()).toString());
                                        File dir = photo.getFile().getParentFile();
                                        File mImageFile = new File(dir, UUID.randomUUID().toString() + ".png");
//                                        photoListFragment.startActivityForResult(editIntent, Const.VIDEO_EDIT_REQUEST_CODE);
                                        photoListFragment.startActivityForResult(
                                                new Intent(photoListFragment.getActivity(), IMGEditActivity.class)
                                              .putExtra(IMGEditActivity.EXTRA_IMAGE_URI, Uri.fromFile(photo.getFile()))
                                            .putExtra(IMGEditActivity.EXTRA_IMAGE_SAVE_PATH, mImageFile.getAbsolutePath()),
                                        Const.PHOTO_EDIT_REQUEST_CODE);
                                        break;
                                }
                                return true;
                            }
                        });
                    }
                });

                //Show user a chooser to play the photo with
                itemViewHolder.photoCard.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //photo photo = photos.get(itemViewHolder.getAdapterPosition());

                        // If multiselect is enabled, select the items pressed by user
                        if (isMultiSelect) {

                            // main count of the selected items
                            // If the photo is already selected, reduce count else increment count
                            if (photo.isSelected())
                                count--;
                            else
                                count++;

                            // Enable disable selection based on previous choice
                            photo.setSelected(!photo.isSelected());
                            notifyDataSetChanged();
                            mActionMode.setTitle("" + count);

                            // If the count is 0, disable muliselect
                            if (count == 0)
                                setMultiSelect(false);
                            return;
                        }

                        File photoFile = photo.getFile();
                        Log.d("photos List", "photo position clicked: " + itemViewHolder.getAdapterPosition());

                        Uri fileUri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", photoFile);
                        Log.d(Const.TAG, fileUri.toString());
                        Intent openPhotoIntent = new Intent();
                        openPhotoIntent.setAction(Intent.ACTION_VIEW)
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                .setDataAndType(
                                        fileUri,
                                        context.getContentResolver().getType(fileUri));
                        context.startActivity(openPhotoIntent);
                    }
                });

                // LongClickListener to enable multiselect
                itemViewHolder.photoCard.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        if (!isMultiSelect) {
                            setMultiSelect(true);
                            photo.setSelected(true);
                            count++;
                            mActionMode.setTitle("" + count);
                            notifyDataSetChanged();
                        }
                        return true;
                    }
                });

                break;
            case VIEW_SECTION:
                SectionViewHolder sectionViewHolder = (SectionViewHolder) holder;
                sectionViewHolder.section.setText(generateSectionTitle(photo.getLastModified()));
                break;
        }
    }

    private void setMultiSelect(boolean isMultiSelect) {
        if (isMultiSelect) {
            this.isMultiSelect = true;
            count = 0;
            mActionMode = ((AppCompatActivity) photoListFragment.getActivity()).startSupportActionMode(mActionModeCallback);
        } else {
            this.isMultiSelect = false;
            mActionMode.finish();
        }
    }

    /**
     * Share the photos selected
     *
     * @param position Integer value representing the position of the photo to be shared
     * @see #sharephotos(ArrayList positions)
     */
    private void sharephoto(int position) {
        Uri fileUri = FileProvider.getUriForFile(
                context, context.getPackageName() +
                        ".provider",
                photos.get(position).getFile()
        );

        Intent Shareintent = new Intent()
                .setAction(Intent.ACTION_SEND)
                .setType("image/*")
                .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .putExtra(Intent.EXTRA_STREAM, fileUri);
        context.startActivity(Intent.createChooser(Shareintent,
                context.getString(R.string.share_intent_photo_title)));
    }

    /**
     * Share the photos selected
     *
     * @param positions Integer ArrayList containing the positions of the photos to be shared
     *
     * @see #sharephoto(int postion)
     */
    private void sharephotos(ArrayList<Integer> positions) {
        ArrayList<Uri> photoList = new ArrayList<>();
        for (int position : positions) {
            photoList.add(FileProvider.getUriForFile(
                    context, context.getPackageName() +
                            ".provider",
                    photos.get(position).getFile()
            ));
        }
        Intent Shareintent = new Intent()
                .setAction(Intent.ACTION_SEND_MULTIPLE)
                .setType("image/*")
                .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .putParcelableArrayListExtra(Intent.EXTRA_STREAM, photoList);
        context.startActivity(Intent.createChooser(Shareintent,
                context.getString(R.string.share_intent_photo_title)));
    }

    /**
     * Delete the photos selected
     *
     * @param position integer value representing the position of the photo to be deleted
     *
     * @see #deletephoto(int position)
     */
    private void deletephoto(int position) {
        Log.d("photos List", "delete position clicked: " + position);
        File file = new File(photos.get(position).getFile().getPath());
        if (file.delete()) {
            Toast.makeText(context, "File deleted successfully", Toast.LENGTH_SHORT).show();
            photos.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, photos.size());
        }
    }

    /**
     * Delete the photos selected
     *
     * @param positions ArrayList of integers containing the position of the photos to be deleted
     *
     * @see #deletephoto(int position)
     */
    private void deletephotos(ArrayList<Integer> positions) {
        File photo;
        int temp = 0;
        for (int position = 0; position < positions.size(); position++) {
            photo = new File(photos.get(positions.get(position)-temp).getFile().getPath());
            if (photo.delete()) {
                photos.remove(positions.get(position) - temp);
                temp++;
            }
        }
        notifyDataSetChanged();
    }

    /**
     * Show confirmation dialog before the photo is deleted
     *
     * @param position integer representing the postion of the photo in the dataset to delete
     */
    private void confirmDelete(final int position) {
        new AlertDialog.Builder(context)
                .setTitle(context.getResources().getQuantityString(R.plurals.delete_photo_title, 1))
                .setMessage(context.getResources().getQuantityString(R.plurals.delete_photo_message, 1))
                .setCancelable(false)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        deletephoto(position);
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                })
                .show();
    }

    /**
     * Show confirmation dialog before the photo is deleted
     *
     * @param positions Array list of integer containing the positions of the photos in the dataset to delete
     */
    private void confirmDelete(final ArrayList<Integer> positions) {
        int count = positions.size();
        new AlertDialog.Builder(context)
                .setTitle(context.getResources().getQuantityString(R.plurals.delete_photo_title, count))
                .setMessage(context.getResources().getQuantityString(R.plurals.delete_photo_message,
                        count, count))
                .setCancelable(false)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        deletephotos(positions);
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                })
                .show();
    }

    //Generate title for the section depending on the recording date
    private String generateSectionTitle(Date date) {
        Calendar sDate = toCalendar(new Date().getTime());
        Calendar eDate = toCalendar(date.getTime());

        // Get the represented date in milliseconds
        long milis1 = sDate.getTimeInMillis();
        long milis2 = eDate.getTimeInMillis();

        // Calculate difference in milliseconds
        int dayDiff = (int) Math.abs((milis2 - milis1) / (24 * 60 * 60 * 1000));

        int yearDiff = sDate.get(Calendar.YEAR) - eDate.get(Calendar.YEAR);
        Log.d("ScreenRecorder", "yeardiff: " + yearDiff);

        if (yearDiff == 0) {
            switch (dayDiff) {
                case 0:
                    return "Today";
                case 1:
                    return "Yesterday";
                default:
                    SimpleDateFormat format = new SimpleDateFormat("EEEE, dd MMM", Locale.getDefault());
                    return format.format(date);
            }
        } else {
            SimpleDateFormat format = new SimpleDateFormat("EEEE, dd MMM YYYY", Locale.getDefault());
            return format.format(date);
        }
    }

    //Generate Calendar object and return it
    private Calendar toCalendar(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    @Override
    public int getItemCount() {
        return photos.size();
    }

    //ViewHolder class for photo items
    private final class ItemViewHolder extends RecyclerView.ViewHolder {
        private TextView tv_fileName;
        private ImageView iv_thumbnail;
        private RelativeLayout photoCard;
        private FrameLayout selectableFrame;
        private ImageButton overflow;

        ItemViewHolder(View view) {
            super(view);
            tv_fileName = view.findViewById(R.id.fileName);
            iv_thumbnail = view.findViewById(R.id.thumbnail);
            iv_thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
            photoCard = view.findViewById(R.id.videoCard);
            overflow = view.findViewById(R.id.ic_overflow);
            selectableFrame = view.findViewById(R.id.selectableFrame);
        }
    }

    //ViewHolder class for sections
    private final class SectionViewHolder extends RecyclerView.ViewHolder {
        private TextView section;

        SectionViewHolder(View view) {
            super(view);
            section = view.findViewById(R.id.sectionID);
        }
    }
}
