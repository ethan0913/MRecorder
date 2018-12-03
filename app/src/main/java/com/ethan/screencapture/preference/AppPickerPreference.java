package com.ethan.screencapture.preference;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.preference.DialogPreference;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;


import com.ethan.screencapture.Const;
import com.ethan.screencapture.R;
import com.ethan.screencapture.adapter.Apps;
import com.ethan.screencapture.adapter.AppsListFragmentAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by wxl on 02-12-2018.
 */

public class AppPickerPreference extends DialogPreference implements AppsListFragmentAdapter.OnItemClicked {
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private ArrayList<Apps> apps;

    public AppPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setPersistent(true);

        //set custom dialog layout
        setDialogLayoutResource(R.layout.layout_apps_list_preference);

    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        // Hide the positive "save" button
        builder.setPositiveButton(null, null);
    }

    @Override
    protected View onCreateDialogView() {
        return super.onCreateDialogView();
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        progressBar = view.findViewById(R.id.appsProgressBar);
        recyclerView = view.findViewById(R.id.appsRecyclerView);

        init();
    }

    private void init() {
        RecyclerView.LayoutManager recyclerViewLayoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(recyclerViewLayoutManager);

        // Generate list of installed apps and display in dialog
        new GetApps().execute();
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, Object defaultValue) {
        super.onSetInitialValue(restorePersistedValue, defaultValue);
    }

    // On item click listener for recycler view
    @Override
    public void onItemClick(int position) {
        Log.d(Const.TAG, "Closing dialog. received result. Pos:" + position);
        // save the selected app's package name to sharedpreference
        persistString(apps.get(position).getPackageName());
        //dismiss dialog after saving the value
        getDialog().dismiss();
    }

    class GetApps extends AsyncTask<Void, Void, ArrayList<Apps>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(ArrayList<Apps> apps) {
            super.onPostExecute(apps);

            // Hide progress bar after the apps list has been loaded
            progressBar.setVisibility(View.GONE);
            AppsListFragmentAdapter recyclerViewAdapter = new AppsListFragmentAdapter(apps);

            // set custom adapter to recycler view
            recyclerView.setAdapter(recyclerViewAdapter);

            // Set recycler view item click listener
            recyclerViewAdapter.setOnClick(AppPickerPreference.this);
        }

        @Override
        protected ArrayList<Apps> doInBackground(Void... voids) {
            PackageManager pm = getContext().getPackageManager();
            apps = new ArrayList<>();

            // Get list of all installs apps including system apps and apps without any launcher activity
            List<PackageInfo> packages = pm.getInstalledPackages(0);

            for (PackageInfo packageInfo : packages) {

                // Check if the app has launcher intent set and exclude our own app
                if (!(getContext().getPackageName().equals(packageInfo.packageName))
                        && !(pm.getLaunchIntentForPackage(packageInfo.packageName) == null)) {

                    Apps app = new Apps(
                            packageInfo.applicationInfo.loadLabel(getContext().getPackageManager()).toString(),
                            packageInfo.packageName,
                            packageInfo.applicationInfo.loadIcon(getContext().getPackageManager())

                    );

                    // Identify the previously selected app
                    app.setSelectedApp(
                            AppPickerPreference.this.getPersistedString("none")
                                    .equals(packageInfo.packageName)
                    );
                    if (pm.getLaunchIntentForPackage(packageInfo.packageName) == null)
                        Log.d(Const.TAG, packageInfo.packageName);
                    apps.add(app);
                }
                Collections.sort(apps);
            }
            return apps;
        }
    }
}
