package com.cloudbookkeep.www.cloudbookkeepapp.activity;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.cloudbookkeep.www.cloudbookkeepapp.R;
import com.cloudbookkeep.www.cloudbookkeepapp.util.Toaster;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.OpenFileActivityBuilder;


public class SettingsActivity extends Activity {

    private Button selectFolderBtn_;
    private Button enableQueryBtn_;
    private static final int REQUEST_CODE_OPENER = 5;
    private static final int REQUEST_CODE_QUERY = 6;

    private static final String TAG = "ReceiveActivity";

    private static final String REPORTS_FOLDER = "reports_folder_id";
    private static final String BANK_QUERY_FILE = "bank_query_id_file";
    private static GoogleApiClient mGoogleApiClient;

    /**
     * Represents the file picked by the user.
     */
    public static DriveId selectedFolderId_;
    public static DriveId bankQueryFileId_;

    /**
     * Keeps the status whether change events are being listened to or not.
     */
    private boolean isSubscribed = false;
    final private Object mSubscriptionStatusLock = new Object();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        mGoogleApiClient = MainActivity.getGoogleApiClient();
        Toaster.makeLongToast(SettingsActivity.this, getResources().getString(R.string.folder_listener_tip), 15000);
        selectFolderBtn_ = (Button)findViewById(R.id.enable_report_listening_btn);
        enableQueryBtn_ = (Button)findViewById(R.id.enable_query_listening_btn);

        selectFolderBtn_.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectFolderForChangeListening();
            }
        });

        enableQueryBtn_.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectFileForQueryListening();
            }
        });
    }

    @Override
    protected void onResume(){
        super.onResume();
        if(!(mGoogleApiClient == null)){
            mGoogleApiClient.connect();
        }
    }

    /**
     * This method opens Drive activity with list of folders and files
     * and helps user to choose folder for listening changes
     */
    private void selectFolderForChangeListening(){
        Drive.DriveApi.newDriveContents(mGoogleApiClient)
                .setResultCallback(new ResultCallback<DriveApi.DriveContentsResult>() {

                    @Override
                    public void onResult(DriveApi.DriveContentsResult result) {
                        // If the operation was not successful, we cannot do anything
                        // and must
                        // fail.
                        if (!result.getStatus().isSuccess()) {
                            Log.i(TAG, "Failed to create new contents.");
                            return;
                        }

                        // Create an intent for the file chooser, and start it.
                        IntentSender intentSender = Drive.DriveApi
                                .newOpenFileActivityBuilder()
                                .setMimeType(new String[]{"application/vnd.google-apps.folder"})
                                .build(mGoogleApiClient);
                        try {
                            startIntentSenderForResult(
                                    intentSender, REQUEST_CODE_OPENER, null, 0, 0, 0);
                        } catch (IntentSender.SendIntentException e) {
                            Log.i(TAG, "Failed to launch file chooser.");
                        }
                    }
                });
    }

    private void selectFileForQueryListening(){
        Drive.DriveApi.newDriveContents(mGoogleApiClient)
                .setResultCallback(new ResultCallback<DriveApi.DriveContentsResult>() {

                    @Override
                    public void onResult(DriveApi.DriveContentsResult result) {
                        // If the operation was not successful, we cannot do anything
                        // and must
                        // fail.
                        if (!result.getStatus().isSuccess()) {
                            Log.i(TAG, "Failed to create new contents.");
                            return;
                        }

                        // Create an intent for the file chooser, and start it.
                        IntentSender intentSender = Drive.DriveApi
                                .newOpenFileActivityBuilder()
                                .setMimeType(new String[]{"application/vnd.google-apps.spreadsheet"})
                                .build(mGoogleApiClient);
                        try {
                            startIntentSenderForResult(
                                    intentSender, REQUEST_CODE_QUERY, null, 0, 0, 0);
                        } catch (IntentSender.SendIntentException e) {
                            Log.i(TAG, "Failed to launch file chooser.");
                        }
                    }
                });
    }

    private void writeId(String key, String id){
        SharedPreferences.Editor prefs = this.getSharedPreferences(
                "com.example.app", Context.MODE_PRIVATE).edit();
        prefs.putString(key, id);
        prefs.commit();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_OPENER:
                if (resultCode == RESULT_OK) {
                    selectedFolderId_ = (DriveId) data.getParcelableExtra(OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID);
                    // Write id of folder in SharedPreferences
                    writeId(REPORTS_FOLDER, selectedFolderId_.toString());
                    Log.i("Id", selectedFolderId_.toString());
                    toggle();
                }
                break;

            case REQUEST_CODE_QUERY:
                if (resultCode == RESULT_OK) {
                    bankQueryFileId_ = (DriveId) data.getParcelableExtra(OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID);
                    // Write Id in SharedPreferences
                    writeId(BANK_QUERY_FILE, bankQueryFileId_.toString());
                    Log.i("Id", bankQueryFileId_.toString());
                    toggle2();
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * Toggles the subscription status. If there is no selected file, returns
     * immediately.
     */
    private void toggle() {
        if (selectedFolderId_ == null) {
            return;
        }
        synchronized (mSubscriptionStatusLock) {
            DriveFolder folder = Drive.DriveApi.getFolder(mGoogleApiClient, selectedFolderId_);
            Log.d(TAG, "Starting to listen to the file changes.");
            // file.addChangeListener(mGoogleApiClient, changeListener);
            folder.addChangeSubscription(mGoogleApiClient);
            isSubscribed = true;
            selectFolderBtn_.setText(getResources().getString(R.string.enable_report_listening)+ " (enabled)");
//            if (!isSubscribed) {
//                Log.d(TAG, "Starting to listen to the file changes.");
//                // file.addChangeListener(mGoogleApiClient, changeListener);
//                folder.addChangeSubscription(mGoogleApiClient);
//                isSubscribed = true;
//                selectFolderBtn_.setText(getResources().getString(R.string.enable_report_listening)+ " (enabled)");
//            } else {
//                Log.d(TAG, "Stopping to listen to the file changes.");
//                // file.removeChangeListener(mGoogleApiClient, changeListener);
//                folder.removeChangeSubscription(mGoogleApiClient);
//                isSubscribed = false;
//                selectFolderBtn_.setText(getResources().getString(R.string.enable_report_listening)+ " (disabled)");
//            }
        }
        Toast.makeText(SettingsActivity.this,getResources().getString(R.string.reports_folder_listener_status),Toast.LENGTH_LONG).show();
        //refresh();
    }

    /**
     * Toggles the subscription status. If there is no selected file, returns
     * immediately.
     */
    private void toggle2() {
        if (bankQueryFileId_ == null) {
            return;
        }
        synchronized (mSubscriptionStatusLock) {
            DriveFile file = Drive.DriveApi.getFile(mGoogleApiClient, bankQueryFileId_);
            Log.d(TAG, "Starting to listen to the file changes.");
            // file.addChangeListener(mGoogleApiClient, changeListener);
            file.addChangeSubscription(mGoogleApiClient);
            isSubscribed = true;
            enableQueryBtn_.setText(getResources().getString(R.string.enable_query_listening)+ " (enabled)");
        }
        Toast.makeText(SettingsActivity.this,getResources().getString(R.string.queries_folder_listener_status),Toast.LENGTH_LONG).show();
        //refresh();
    }


}
