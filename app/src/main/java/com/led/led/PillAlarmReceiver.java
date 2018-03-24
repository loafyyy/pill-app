package com.led.led;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class PillAlarmReceiver extends BroadcastReceiver {

    private int NOTIFICATION_ID;

    public PillAlarmReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        /* todo get intents
        String roomName = intent.getStringExtra(context.getResources().getString(R.string.laundry_room_name));
        String machineType = intent.getStringExtra(context.getResources().getString(R.string.laundry_machine_type));
        int id = intent.getIntExtra(context.getResources().getString(R.string.laundry_machine_id), -1);

        // checks for errors
        if (roomName == null || machineType == null || id == -1) {
            return;
        }*/

        int broadcastId = 0;

        NOTIFICATION_ID = 1;

        // build notification
        Notification.Builder mBuilder = new Notification.Builder(context)
                .setSmallIcon(R.drawable.ic_alert)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText("Time to take your pill");
        mBuilder.setAutoCancel(true);
        mBuilder.setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // intent to go to HomeActivity todo change to BT
        Intent laundryIntent = new Intent(context, HomeActivity.class);
        laundryIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent notifyIntent = PendingIntent.getActivity(context, NOTIFICATION_ID, laundryIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(notifyIntent);
        notificationManager.notify(NOTIFICATION_ID, mBuilder.build());

        // todo send alarm to pill box

        // cancel intent after notification/alarm goes off
        PendingIntent fromIntent = PendingIntent.getBroadcast(context, broadcastId, intent, PendingIntent.FLAG_NO_CREATE);
        fromIntent.cancel();
    }
}
