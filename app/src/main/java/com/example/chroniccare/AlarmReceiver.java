package com.example.chroniccare;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.Calendar;

public class AlarmReceiver extends BroadcastReceiver {
    
    private static final String CHANNEL_ID = "medication_reminders";
    private static final String TAG = "AlarmReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "AlarmReceiver triggered!");
        
        String medicationName = intent.getStringExtra("medicationName");
        String mealTime = intent.getStringExtra("mealTime");
        String time = intent.getStringExtra("time");
        
        Log.d(TAG, "Medication: " + medicationName);
        
        // Safety check - don't trigger if medication data is missing
        if (medicationName == null || medicationName.trim().isEmpty()) {
            Log.e(TAG, "Medication name is null or empty - ignoring alarm");
            return;
        }
        
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "ChronicCare:AlarmWakeLock"
        );
        wakeLock.acquire(60000); // 1 minute
        
        boolean reschedule = intent.getBooleanExtra("reschedule", false);
        if (reschedule) {
            int hour = intent.getIntExtra("hour", 0);
            int minute = intent.getIntExtra("minute", 0);
            int requestCode = intent.getIntExtra("requestCode", 0);
            rescheduleAlarm(context, medicationName, hour, minute, mealTime, requestCode);
        }
        
        // Start foreground service
        Intent serviceIntent = new Intent(context, AlarmForegroundService.class);
        serviceIntent.putExtra("medicationName", medicationName);
        serviceIntent.putExtra("mealTime", mealTime);
        serviceIntent.putExtra("time", time);
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
            Log.d(TAG, "Service started");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start service", e);
        }
        
        showNotification(context, medicationName, mealTime);
        
        wakeLock.release();
    }
    
    private void rescheduleAlarm(Context context, String medName, int hour, int minute, String mealTime, int requestCode) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("medicationName", medName);
        intent.putExtra("mealTime", mealTime);
        intent.putExtra("time", String.format("%02d:%02d", hour, minute));
        intent.putExtra("reschedule", true);
        intent.putExtra("hour", hour);
        intent.putExtra("minute", minute);
        intent.putExtra("requestCode", requestCode);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                (medName + "_next").hashCode() + requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.getTimeInMillis(),
                pendingIntent
        );
    }
    
    private void showNotification(Context context, String medicationName, String mealTime) {
        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Medication Reminders",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Reminders for medication schedule");
            channel.enableVibration(true);
            channel.setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                new android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                    .build()
            );
            notificationManager.createNotificationChannel(channel);
        }
        
        Intent intent = new Intent(context, AlarmActivity.class);
        intent.putExtra("medicationName", medicationName);
        intent.putExtra("mealTime", mealTime);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        PendingIntent fullScreenIntent = PendingIntent.getActivity(
            context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bell2)
            .setContentTitle("💊 TIME TO TAKE MEDICATION")
            .setContentText(medicationName + " - " + mealTime)
            .setStyle(new NotificationCompat.BigTextStyle()
                .bigText(medicationName + "\n" + mealTime + "\n\nTap to open alarm"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            .setVibrate(new long[]{0, 1000, 500, 1000})
            .setAutoCancel(false)
            .setOngoing(true)
            .setFullScreenIntent(fullScreenIntent, true)
            .setContentIntent(pendingIntent);
        
        notificationManager.notify(medicationName.hashCode(), builder.build());
    }
}
