package com.example.chroniccare;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class AlarmReceiver extends BroadcastReceiver {
    
    private static final String CHANNEL_ID = "medication_reminders";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        String medicationName = intent.getStringExtra("medicationName");
        String mealTime = intent.getStringExtra("mealTime");
        String time = intent.getStringExtra("time");
        
        Intent alarmIntent = new Intent(context, AlarmActivity.class);
        alarmIntent.putExtra("medicationName", medicationName);
        alarmIntent.putExtra("mealTime", mealTime);
        alarmIntent.putExtra("time", time);
        alarmIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        context.startActivity(alarmIntent);
        
        showNotification(context, medicationName, mealTime);
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
            notificationManager.createNotificationChannel(channel);
        }
        
        Intent intent = new Intent(context, AlarmActivity.class);
        intent.putExtra("medicationName", medicationName);
        intent.putExtra("mealTime", mealTime);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bell2)
            .setContentTitle("Time for " + medicationName)
            .setContentText(mealTime)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent);
        
        notificationManager.notify(medicationName.hashCode(), builder.build());
    }
}
