package com.pandian.samuvel.geofencingprac;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Intent;
import android.graphics.Color;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.ArrayList;
import java.util.List;


public class GeofenceTransitionService extends IntentService {

    private int mGeofenceTransition;
    private static final int GEOFENCE_NOTIFICATION_ID = 0;
    public GeofenceTransitionService() {
        super("GeofenceTransitionService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        GeofencingEvent mGeofencingEvent = GeofencingEvent.fromIntent(intent);
        mGeofenceTransition = mGeofencingEvent.getGeofenceTransition();
        if((mGeofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) || (mGeofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT)){
            List<Geofence> triggeringGeofences = mGeofencingEvent.getTriggeringGeofences();
            String geofenceTransitionDetails = getGeofenceTransitionDetails(mGeofenceTransition,triggeringGeofences);
            sendNotification(geofenceTransitionDetails);
        }
    }

    private String getGeofenceTransitionDetails(int geofenceTransition,List<Geofence> trigerringGeofences){
        ArrayList<String> mTriggeringGeofenceList = new ArrayList<>();
        for(Geofence geofence : trigerringGeofences){
            mTriggeringGeofenceList.add(geofence.getRequestId());
        }
        String status = null;
        if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER)
            status = "Check In,You have entered ";
        else if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT)
            status = "Check Out, You have exited ";
        return status + TextUtils.join(" ",mTriggeringGeofenceList);
    }

    private void sendNotification(String message){
        Intent notificationIntent = MainActivity.makeNotificationIntent(getApplicationContext(),message);
        TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(getApplicationContext());
        taskStackBuilder.addParentStack(MainActivity.class);
        taskStackBuilder.addNextIntent(notificationIntent);
        PendingIntent notificationPendindIntent = taskStackBuilder.getPendingIntent(0,PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(GEOFENCE_NOTIFICATION_ID, createNotification(message,notificationPendindIntent));

    }
    private Notification createNotification(String message, PendingIntent notificationPendingIntent){
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext(),"channelID");
        notificationBuilder
                .setSmallIcon(R.drawable.know)
                .setColor(Color.RED)
                .setContentTitle(message)
                .setContentText("Know notification!")
                .setContentIntent(notificationPendingIntent)
                .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND)
                .setAutoCancel(true);
        return notificationBuilder.build();

    }
}
