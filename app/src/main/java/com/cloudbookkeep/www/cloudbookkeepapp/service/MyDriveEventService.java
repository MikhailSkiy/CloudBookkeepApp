package com.cloudbookkeep.www.cloudbookkeepapp.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.cloudbookkeep.www.cloudbookkeepapp.R;
import com.cloudbookkeep.www.cloudbookkeepapp.activity.ReceiveActivity;
import com.cloudbookkeep.www.cloudbookkeepapp.activity.SettingsActivity;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.events.ChangeEvent;
import com.google.android.gms.drive.events.DriveEventService;


/**
 * Created by Mikhail Valuyskiy on 06.10.2015.
 */
public class MyDriveEventService extends DriveEventService {

    private static final int REPORTS_NOTIFICATION = 1;
    private static final int QUERIES_NOTIFICATION = 2;

    @Override
    public void onChange(ChangeEvent event){

        Log.d("MyDriveEventService", "Change event: " + event);
        DriveId resourseId = event.getDriveId();
        String id = event.getDriveId().toString();

        if (resourseId.equals(SettingsActivity.selectedFolderId_)){
            sendNotification(REPORTS_NOTIFICATION,id);
        } else if (resourseId.equals(SettingsActivity.bankQueryFileId_)){
            sendNotification(QUERIES_NOTIFICATION,id);
        }

    }

    private void sendNotification(int notificationType,String id) {
        Intent intent = new Intent(this,  ReceiveActivity.class);
        intent.putExtra(getString(R.string.id_key),id);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.drawable.ic_launcher);
        // Set the intent that will fire when the user taps the notification.
        builder.setContentIntent(pendingIntent);
        // Set the notification to auto-cancel. This means that the notification will disappear
        // after the user taps it, rather than remaining until it's explicitly dismissed.
        builder.setAutoCancel(true);
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));
        builder.setDefaults(Notification.DEFAULT_ALL);

        if (notificationType==1) {
            builder.setContentTitle("Reports notification");
            builder.setContentText("The reports in Reports folder were updated!");
            builder.setSubText("Tap to view changes.");
        } else if (notificationType ==2){
            if (notificationType==2) {
                builder.setContentTitle("Queries notification");
                builder.setContentText("The queries in Bank queries folder were updated!");
                builder.setSubText("Tap to view changes.");
            }
        }

        NotificationManager notificationManager = (NotificationManager)this.getSystemService(
                Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, builder.build());
    }
}
