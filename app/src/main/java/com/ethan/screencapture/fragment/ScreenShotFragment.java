package com.ethan.screencapture.fragment;


import android.Manifest;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ethan.screencapture.Const;
import com.ethan.screencapture.PermissionResultListener;
import com.ethan.screencapture.R;
import com.ethan.screencapture.activity.MainActivity;
import com.ethan.screencapture.adapter.Photo;
import com.ethan.screencapture.adapter.PhotoRecyclerAdapter;

import java.io.File;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Created by wxl on 2018/3/29.
 */

public class ScreenShotFragment extends Fragment  implements PermissionResultListener,SwipeRefreshLayout.OnRefreshListener{

    private RecyclerView photoRV;
    private TextView message;
    private SharedPreferences prefs;
    private SwipeRefreshLayout swipeRefreshLayout;
    private MainActivity activity;
    private ArrayList <Photo> photoList= new ArrayList<>();
    public ScreenShotFragment() {

    }

    //Method to check if the file's meme type is photo
    private static boolean isPhotoFile(String path) {
        String mimeType = URLConnection.guessContentTypeFromName(path);
        return mimeType != null && mimeType.startsWith("image");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_screenshot, container, false);
        message = view.findViewById(R.id.message_tv);
        photoRV = view.findViewById(R.id.photo_rv);

        swipeRefreshLayout = view.findViewById(R.id.swipeRefresh);
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary,
                android.R.color.holo_green_dark,
                android.R.color.holo_orange_dark,
                android.R.color.holo_blue_dark);

        prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            getPhotoData();
        }
        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = (MainActivity) getActivity();
        setHasOptionsMenu(true);
    }

    //Load photo from the directory only when the fragment is visible to the screen
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            Log.d(Const.TAG, "photos fragment is visible load the photos");
            checkPermission();

        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        MenuItem refresh = menu.add("Refresh");
        refresh.setIcon(R.drawable.ic_refresh_white_24dp);
        refresh.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS);
        refresh.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                // Prevent repeated refresh requests
                if (swipeRefreshLayout.isRefreshing())
                    return false;
                photoList.clear();
                checkPermission();
                Log.d(Const.TAG, "Refreshing");
                return false;
            }
        });
    }

    //Check if we have permission to read the external storage. The fragment is useless without this
    private void checkPermission() {
        if (activity != null) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                if (activity instanceof MainActivity) {
                    ((MainActivity) activity).setPermissionResultListener(this);
                    ((MainActivity) activity).requestPermissionStorage();
                }
            } else {
                //We have required permission now and lets populate the photo from the selected
                // directory if the arraylist holding photo is empty
               getPhotoData();
            }
        }

    }

    private File[] getPhotos(File[] files) {
        List<File> newFiles = new ArrayList<>();
        for (File file : files) {
            if (!file.isDirectory() && isPhotoFile(file.getPath()))
                newFiles.add(file);
        }
        return newFiles.toArray(new File[newFiles.size()]);
    }


    //Init recyclerview once the photo list is ready
    private void setRecyclerView(ArrayList<Photo> photos) {
        photoRV.setHasFixedSize(true);
        final GridLayoutManager layoutManager = new GridLayoutManager(activity, 2);
        photoRV.setLayoutManager(layoutManager);
        final PhotoRecyclerAdapter adapter = new PhotoRecyclerAdapter(activity, photos, this);
        photoRV.setAdapter(adapter);
        //Set the span to 1 (width to match the screen) if the view type is section
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return adapter.isSection(position) ? layoutManager.getSpanCount() : 1;
            }
        });
    }

    //Permission result callback method
    @Override
    public void onPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case Const.EXTDIR_REQUEST_CODE:
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Log.d(Const.TAG, "Storage permission granted.");
                    //Performing storage task immediately after granting permission sometimes causes
                    //permission not taking effect.
                    checkPermission();
                } else {
                    Log.d(Const.TAG, "Storage permission denied.");
                    photoRV.setVisibility(View.GONE);
                    message.setText(R.string.storage_permission_denied_message);
                }
                break;
        }
    }

    //Clear the photos ArrayList once the save directory is changed which forces reloading of photos from new directory
    public void removephotosList() {
        photoList.clear();
        Log.d(Const.TAG, "Reached photo fragment");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(Const.TAG, "Refresh data after edit!");
        removephotosList();
        checkPermission();
    }

    @Override
    public void onRefresh() {
        photoList.clear();
        checkPermission();
        Log.d(Const.TAG, "Refreshing");
    }

    //Class to retrieve photo details in async
    class GetPhotosAsync extends AsyncTask<File[], Integer, ArrayList<Photo>> {
        //ProgressDialog progress;
        File[] files;
        ContentResolver resolver;

        GetPhotosAsync() {
            resolver = activity.getApplicationContext().getContentResolver();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //Set refreshing to true
            swipeRefreshLayout.setRefreshing(true);
        }

        @Override
        protected void onPostExecute(ArrayList<Photo> photos) {
            //If the directory has no photos, remove recyclerview from rootview and show empty message.
            // Else set recyclerview and remove message textview
            if (photos.isEmpty()) {
                photoRV.setVisibility(View.GONE);
                message.setVisibility(View.VISIBLE);
            } else {
                //Sort the photos in a descending order
                Collections.sort(photos, Collections.<Photo>reverseOrder());
                setRecyclerView(addSections(photos));
                photoRV.setVisibility(View.VISIBLE);
                message.setVisibility(View.GONE);
            }
            //Finish refreshing
            swipeRefreshLayout.setRefreshing(false);
        }

        //Add sections depending on the date the photo is recorded to array list
        private ArrayList<Photo> addSections(ArrayList<Photo> photos) {
            ArrayList<Photo> photosWithSections = new ArrayList<>();
            Date currentSection = new Date();
            Log.d(Const.TAG, "Original Length: " + photos.size());
            for (int i = 0; i < photos.size(); i++) {
                Photo photo = photos.get(i);
                //Add the first section arbitrarily
                if (i == 0) {
                    photosWithSections.add(new Photo(true, photo.getLastModified()));
                    photosWithSections.add(photo);
                    currentSection = photo.getLastModified();
                    continue;
                }
                if (addNewSection(currentSection, photo.getLastModified())) {
                    photosWithSections.add(new Photo(true, photo.getLastModified()));
                    currentSection = photo.getLastModified();
                }
                photosWithSections.add(photo);
            }
            Log.d(Const.TAG, "Length with sections: " + photosWithSections.size());
            return photosWithSections;
        }

        //Check if a new Section is to be added by comparing the difference of the section date
        // and the photo's last modified date
        private boolean addNewSection(Date current, Date next) {
            Calendar currentSectionDate = toCalendar(current.getTime());
            Calendar nextPhotoDate = toCalendar(next.getTime());

            // Get the represented date in milliseconds
            long milis1 = currentSectionDate.getTimeInMillis();
            long milis2 = nextPhotoDate.getTimeInMillis();

            // Calculate difference in milliseconds
            int dayDiff = (int) Math.abs((milis2 - milis1) / (24 * 60 * 60 * 1000));
            Log.d(Const.TAG, "Date diff is: " + (dayDiff));
            return dayDiff > 0;
        }

        //Generate and return new Calendar object
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
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);

            Log.d(Const.TAG, "Progress is :" + values[0]);
        }

        @Override
        protected ArrayList<Photo> doInBackground(File[]... arg) {
            //Get photo file name, Uri and photo thumbnail from mediastore
            files = arg[0];
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                if (!file.isDirectory() && isPhotoFile(file.getAbsolutePath())) {
                    photoList.add(new Photo(file.getName(),file,getBitmap(file.getAbsolutePath()),new Date(file.lastModified())));
                    //Update progress dialog
                    publishProgress(i);
                }
            }
            return photoList;
        }

        public Bitmap getBitmap(String path) {
          /*  Bitmap bitmap = BitmapFactory.decodeFile(path);
            return bitmap;*/
            InputStream is = null;
            try {
                is = new FileInputStream(path);
            } catch (Exception e) {
                e.printStackTrace();
            }
            //2.为位图设置100K的缓存
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inTempStorage = new byte[100 * 1024];
            //3.设置位图颜色显示优化方式
            //Android默认的颜色模式为ARGB_8888，这个颜色模式色彩最细腻，显示质量最高。但同样的，占用的内存//也最大。也就意味着一个像素点占用4个字节的内存。我们来做一个简单的计算题：3200*2400*4 bytes //=30M。如此惊人的数字！哪怕生命周期超不过10s，Android也不会答应的。
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
            //4.设置图片可以被回收，创建Bitmap用于存储Pixel的内存空间在系统内存不足时可以被回收
            opts.inPurgeable = true;
            //5.设置位图缩放比例
            //width，hight设为原来的四分一（该参数请使用2的整数倍）,这也减小了位图占用的内存大小；例如，一张//分辨率为2048*1536px的图像使用inSampleSize值为4的设置来解码，产生的Bitmap大小约为//512*384px。相较于完整图片占用12M的内存，这种方式只需0.75M内存(假设Bitmap配置为//ARGB_8888)。
            opts.inSampleSize = 4;
            //6.设置解码位图的尺寸信息
            opts.inInputShareable = true;
            //7.解码位图
            Bitmap btp = BitmapFactory.decodeStream(is, null, opts);
            return btp;
        }
    }

    /**
     * 获取当前需要查询的文件夹
     **/
    public String getPicRootDir(Context context) {

        return Environment.getExternalStorageDirectory()+File.separator+"ScreenCapture" + File.separator + "Screenshots";
    }

    public void getPhotoData(){
        if (photoList.isEmpty()) {
//                    File directory = new File(prefs.getString(getString(R.string.savelocation_key),
//                            Environment.getExternalStorageDirectory()
//                                    + File.separator + "screenrecorder"));
            File dir = new File(getPicRootDir(activity));
            //Remove directory pointers and other files from the list
            if (!dir.exists()) {
                dir.mkdirs();
                Log.d(Const.TAG, "Directory missing! Creating dir");
            }
            ArrayList<File> filesList = new ArrayList<File>();
            if (dir.isDirectory() && dir.exists()) {
                filesList.addAll(Arrays.asList(getPhotos(dir.listFiles())));
            }
            //Read the photo and extract details from it in async.
            // This is essential if the directory contains huge number of photo

            new ScreenShotFragment.GetPhotosAsync().execute(filesList.toArray(new File[filesList.size()]));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
