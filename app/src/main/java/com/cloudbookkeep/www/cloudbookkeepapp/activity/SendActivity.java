package com.cloudbookkeep.www.cloudbookkeepapp.activity;


import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
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
import com.google.android.gms.drive.DriveResource;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.OpenFileActivityBuilder;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;


public class SendActivity extends Activity {

    private static final int REQUEST_CODE_CAPTURE_IMAGE = 1;
    private static final int REQUEST_CODE_CREATOR = 2;
    private static final int REQUEST_CODE_RESOLUTION = 3;
    private static final int REQUEST_CODE_MANAGE = 4;
    private static final int REQUEST_CODE_OPEN_TODO_FORM = 5;

    private static final String TAG = "drive-quickstart";
    private static final int LONG_DELAY = 3500;

    private DriveId folderId_;
    private String link_;

    private Button sendEmailBtn_;
    private Button uploadToDriveBtn_;
    private Button sendTaskRequestBtn_;

    private static GoogleApiClient mGoogleApiClient;
    private Bitmap mBitmapToSave;

    private void saveFileToDrive() {
        // Start by creating a new contents, and setting a callback.
        Log.i(TAG, "Creating new contents.");
        final Bitmap image = mBitmapToSave;
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
                        // Otherwise, we can write our data to the new contents.
                        Log.i(TAG, "New contents created.");
                        // Get an output stream for the contents.
                        OutputStream outputStream = result.getDriveContents().getOutputStream();
                        // Write the bitmap data from it.
                        ByteArrayOutputStream bitmapStream = new ByteArrayOutputStream();
                        image.compress(Bitmap.CompressFormat.PNG, 100, bitmapStream);
                        try {
                            outputStream.write(bitmapStream.toByteArray());
                        } catch (IOException e1) {
                            Log.i(TAG, "Unable to write file contents.");
                        }
                        // Create the initial metadata - MIME type and title.
                        // Note that the user will be able to change the title later.
                        MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
                                .setMimeType("image/jpeg").setTitle("Date.Expense.png").build();
                        // Create an intent for the file chooser, and start it.
                        IntentSender intentSender = Drive.DriveApi
                                .newCreateFileActivityBuilder()
                                .setInitialMetadata(metadataChangeSet)
                                .setInitialDriveContents(result.getDriveContents())
                                .build(mGoogleApiClient);
                        try {
                            startIntentSenderForResult(
                                    intentSender, REQUEST_CODE_CREATOR, null, 0, 0, 0);
                        } catch (IntentSender.SendIntentException e) {
                            Log.i(TAG, "Failed to launch file chooser.");
                        }
                    }
                });

        Toaster.makeLongToast(SendActivity.this, getResources().getString(R.string.choose_file_name_tip), 15000);
//       Toast.makeText(SendActivity.this, getResources().getString(R.string.choose_file_name_tip), Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_CAPTURE_IMAGE:
                // Called after a photo has been taken.
                if (resultCode == Activity.RESULT_OK) {
                    // Store the image data as a bitmap for writing later.
                    mBitmapToSave = (Bitmap) data.getExtras().get("data");
                    saveFileToDrive();
                }
                break;
            case REQUEST_CODE_CREATOR:
                // Called after a file is saved to Drive.
                if (resultCode == RESULT_OK) {
                    Log.i(TAG, "Image successfully saved.");
                    mBitmapToSave = null;
                    Toast.makeText(this, getResources().getString(R.string.succesfull_uploading), Toast.LENGTH_LONG).show();
                }
                break;

            case REQUEST_CODE_OPEN_TODO_FORM:
                if (resultCode == RESULT_OK) {
                    DriveId driveId = (DriveId) data.getParcelableExtra(
                            OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID);
                    DriveFile file = driveId.asDriveFile();
                    file.getMetadata(mGoogleApiClient)
                            .setResultCallback(metadataCallback);
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    final private ResultCallback<DriveResource.MetadataResult> metadataCallback = new
            ResultCallback<DriveResource.MetadataResult>() {
                @Override
                public void onResult(DriveResource.MetadataResult result) {
                    if (!result.getStatus().isSuccess()) {
//                        showMessage("Problem while trying to fetch metadata");
                        return;
                    }
                    Metadata metadata = result.getMetadata();
                    Log.v("Link", metadata.getEmbedLink());
                    openGoogleForm(metadata.getEmbedLink());
                }
            };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send);
        sendEmailBtn_ = (Button) findViewById(R.id.send_email_btn);
        uploadToDriveBtn_ = (Button) findViewById(R.id.upload_file_btn);
        sendTaskRequestBtn_ = (Button) findViewById(R.id.send_task_btn);
        mGoogleApiClient = MainActivity.getGoogleApiClient();

        // Send Email button
        sendEmailBtn_.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openEmailApp();
            }
        });

        // Upload File to Drive
        uploadToDriveBtn_.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(new Intent(MediaStore.ACTION_IMAGE_CAPTURE),
                        REQUEST_CODE_CAPTURE_IMAGE);
            }
        });

        // Send Task Request
        sendTaskRequestBtn_.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toaster.makeLongToast(SendActivity.this,  getResources().getString(R.string.choose_send_folder_tip),15000);
                // Toast.makeText(SendActivity.this, getResources().getString(R.string.choose_send_folder_tip), Toast.LENGTH_LONG).show();
                openTaskFolder();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!(mGoogleApiClient == null)) {
            mGoogleApiClient.connect();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    // Opens all app which able to send Email
    private void openEmailApp() {
        Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
        emailIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        emailIntent.setType("vnd.android.cursor.item/email");
        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"will@cloudbookkeep.com"});
        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "My Email Subject");
        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, "My email content");
        startActivity(Intent.createChooser(emailIntent, "Send mail using..."));
    }

    // Opens Google Drive file chooser where user can open "To Do" Folder and select Google Form with to do tasks
    private void openTaskFolder() {
        Drive.DriveApi.newDriveContents(mGoogleApiClient)
                .setResultCallback(new ResultCallback<DriveApi.DriveContentsResult>() {

                    @Override
                    public void onResult(DriveApi.DriveContentsResult result) {
                        if (!result.getStatus().isSuccess()) {
                            Log.i(TAG, "Failed to create new contents.");
                            return;
                        }

                        // Create an intent for the file chooser, and start it.
                        IntentSender intentSender = Drive.DriveApi
                                .newOpenFileActivityBuilder()
                                .setMimeType(new String[]{"application/vnd.google-apps.form", "application/vnd.google-apps.spreadsheet"})
                                .build(mGoogleApiClient);
                        try {
                            startIntentSenderForResult(
                                    intentSender, REQUEST_CODE_OPEN_TODO_FORM, null, 0, 0, 0);
                        } catch (IntentSender.SendIntentException e) {
                            Log.i(TAG, "Failed to launch file chooser.");
                        }
                    }
                });
    }


    // Opens google form in the web browser
    private void openGoogleForm(String link) {
        Uri url = Uri.parse(link);
        Intent intent = new Intent(Intent.ACTION_VIEW, url);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Log.d("MainActivity", "Couldn't call because no receiving apps installed!");
        }
    }
}

