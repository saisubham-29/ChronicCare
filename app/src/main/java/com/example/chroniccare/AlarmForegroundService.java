package com.example.chroniccare;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class AlarmForegroundService extends Service {
    
    private static final String CHANNEL_ID = "alarm_service";
    private static final int NOTIFICATION_ID = 999;
    private static final String TAG = "AlarmService";
    private static MediaPlayer mediaPlayer;
    private static boolean isRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");
        
        // Stop any existing alarm first
        stopAlarmSound();
        
        String medicationName = intent != null ? intent.getStringExtra("medicationName") : "Medication";
        String mealTime = intent != null ? intent.getStringExtra("mealTime") : "";
        String time = intent != null ? intent.getStringExtra("time") : "";

        Intent alarmIntent = new Intent(this, AlarmActivity.class);
        alarmIntent.putExtra("medicationName", medicationName);
        alarmIntent.putExtra("mealTime", mealTime);
        alarmIntent.putExtra("time", time);
        alarmIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
                           Intent.FLAG_ACTIVITY_NO_USER_ACTION |
                           Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        alarmIntent.addFlags(Intent.FLAG_FROM_BACKGROUND);

        PendingIntent fullScreenIntent = PendingIntent.getActivity(
                this, 0, alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("💊 " + medicationName)
                .setContentText("Time to take your medication - " + mealTime)
                .setSmallIcon(R.drawable.ic_bell2)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(fullScreenIntent, true)
                .setContentIntent(fullScreenIntent)
                .setAutoCancel(false)
                .setOngoing(true)
                .setVibrate(new long[]{0, 1000, 500, 1000})
                .setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM))
                .build();

        startForeground(NOTIFICATION_ID, notification);
        Log.d(TAG, "Started foreground");
        
        isRunning = true;
        startAlarmSound();
        
        // Try to launch activity
        try {
            startActivity(alarmIntent);
            Log.d(TAG, "Activity launched");
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch activity", e);
        }

        return START_NOT_STICKY;
    }

    private void startAlarmSound() {
        try {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                Log.d(TAG, "Alarm already playing");
                return;
            }
            
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            mediaPlayer = MediaPlayer.create(this, alarmUri);
            if (mediaPlayer != null) {
                mediaPlayer.setLooping(true);
                mediaPlayer.start();
                Log.d(TAG, "Alarm sound started");
            } else {
                Log.e(TAG, "MediaPlayer is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start alarm sound", e);
        }
    }
    
    private void stopAlarmSound() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                Log.d(TAG, "Alarm sound stopped");
            } catch (Exception e) {
                Log.e(TAG, "Error stopping alarm", e);
            }
            mediaPlayer = null;
        }
        isRunning = false;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Medication Alarms",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Critical medication reminders");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            channel.setSound(
                android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM),
                new android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            );
            channel.setBypassDnd(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopAlarmSound();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
