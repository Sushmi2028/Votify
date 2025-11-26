package com.example.collegeelectionsystem;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.util.Log;

public class MyApp extends Application {

    private static final String TAG = "MyApp";

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel announcements = new NotificationChannel(
                    MyFirebaseMessagingService.CHANNEL_ID_ANN,
                    "Announcements",
                    NotificationManager.IMPORTANCE_HIGH
            );
            announcements.setDescription("Important announcements about elections");

            NotificationChannel results = new NotificationChannel(
                    MyFirebaseMessagingService.CHANNEL_ID_RESULTS,
                    "Results",
                    NotificationManager.IMPORTANCE_HIGH
            );
            results.setDescription("Notifications when results are published");

            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(announcements);
                nm.createNotificationChannel(results);
                Log.d(TAG, "Notification channels created");
            }
        }
    }
}
