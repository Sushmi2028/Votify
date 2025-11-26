package com.example.collegeelectionsystem;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";
    public static final String CHANNEL_ID_ANN = "votify_announcements";
    public static final String CHANNEL_ID_RESULTS = "votify_results";

    // simple incrementing ID for notifications
    private static final AtomicInteger notifId = new AtomicInteger(0);

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Called when message is received.
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Prefer data payload (custom)
        Map<String, String> data = remoteMessage.getData();
        String title = null;
        String body = null;
        String type = null; // e.g., "announcement" or "results"

        if (data != null && !data.isEmpty()) {
            // Example expected keys: title, body, type, click_action
            title = data.get("title");
            body = data.get("body");
            type = data.get("type");
        }

        // If notification payload present, it can be used as fallback
        if (remoteMessage.getNotification() != null) {
            if (title == null) title = remoteMessage.getNotification().getTitle();
            if (body == null) body = remoteMessage.getNotification().getBody();
        }

        // Default fallback
        if (title == null) title = "Votify";
        if (body == null) body = "You have a new message.";

        // Decide channel
        String channel = CHANNEL_ID_ANN;
        if ("results".equalsIgnoreCase(type)) channel = CHANNEL_ID_RESULTS;
        if ("announcement".equalsIgnoreCase(type)) channel = CHANNEL_ID_ANN;

        // Build and show notification
        sendNotification(title, body, channel, data);
    }

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d(TAG, "Refreshed token: " + token);
        // Optionally send token to your server if you manage device tokens
        // sendRegistrationToServer(token);
    }

    private void sendNotification(String title, String messageBody, String channelId, Map<String,String> data) {
        Context ctx = getApplicationContext();

        // Create intent for when user taps notification.
        // Default: open AnnouncementsActivity. If payload gives click_action, handle it.
        Intent intent = new Intent(this, AnnouncementsActivity.class);
        // If data contains "detailId" or "type", you can put extras to route user.
        if (data != null) {
            for (Map.Entry<String, String> e : data.entrySet()) {
                intent.putExtra(e.getKey(), e.getValue());
            }
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int requestCode = notifId.incrementAndGet();
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0)
        );

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.drawable.ic_votify_logo) // create or change this drawable
                        .setContentTitle(title)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent)
                        .setPriority(NotificationCompat.PRIORITY_HIGH);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // notificationId unique for each notification
        int id = notifId.incrementAndGet();
        if (notificationManager != null) {
            notificationManager.notify(id, notificationBuilder.build());
        }
    }
}
