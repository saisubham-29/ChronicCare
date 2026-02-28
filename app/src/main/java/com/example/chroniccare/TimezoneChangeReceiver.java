package com.example.chroniccare;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Calendar;
import java.util.TimeZone;

public class TimezoneChangeReceiver extends BroadcastReceiver {
    private static final String TAG = "TimezoneReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction())) {
            String newTimezone = TimeZone.getDefault().getID();
            Log.d(TAG, "Timezone changed to: " + newTimezone);

            // Notify the user that the app is adjusting to the new timezone
            showTimezoneNotification(context, newTimezone);

            // Reschedule all alarms based on the new timezone logic
            rescheduleMedications(context);
        }
    }

    private void rescheduleMedications(Context context) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) return;

        String userId = auth.getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(userId).collection("medications")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        syncMedicationAlarm(context, doc);
                    }
                });
    }

    private void syncMedicationAlarm(Context context, QueryDocumentSnapshot doc) {
        String name = doc.getString("name");
        String originTz = doc.getString("originTimezone");
        Boolean strict = doc.getBoolean("strictInterval");
        Timestamp originalTimestamp = doc.getTimestamp("timestamp");

        if (name == null || originalTimestamp == null) return;

        Calendar calendar = Calendar.getInstance();
        
        if (Boolean.TRUE.equals(strict) && originTz != null) {
            // SAFE INTERVAL LOGIC (Absolute Time)
            // We maintain the exact UTC moment. 
            // If it was 5 PM India, it remains that same biological moment globally.
            calendar.setTime(originalTimestamp.toDate());
            Log.d(TAG, "Strict Sync: Maintaining absolute time for " + name);
        } else {
            // WALL CLOCK LOGIC (Local Time)
            // We shift the alarm so it stays at the user's preferred local time (e.g. 5 PM London)
            Calendar originCal = Calendar.getInstance(TimeZone.getTimeZone(originTz != null ? originTz : "UTC"));
            originCal.setTime(originalTimestamp.toDate());
            
            calendar.set(Calendar.HOUR_OF_DAY, originCal.get(Calendar.HOUR_OF_DAY));
            calendar.set(Calendar.MINUTE, originCal.get(Calendar.MINUTE));
            calendar.set(Calendar.SECOND, 0);
            Log.d(TAG, "Clock Sync: Shifting to new local 5:00 PM for " + name);
        }

        // Ensure alarm is in the future
        if (calendar.before(Calendar.getInstance())) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        scheduleSystemAlarm(context, name, calendar, doc.getString("mealTime"), doc.getString("time"));
    }

    private void scheduleSystemAlarm(Context context, String medName, Calendar calendar, String mealTime, String displayTime) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction("com.example.chroniccare.MEDICATION_ALARM");
        intent.putExtra("medicationName", medName);
        intent.putExtra("mealTime", mealTime);
        intent.putExtra("time", displayTime);

        // Standard requestCode logic used in AddMedications
        int requestCode = medName.hashCode(); 

        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pi);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pi);
        }
    }

    private void showTimezoneNotification(Context context, String tzId) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "timezone_sync";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, "Timezone Sync", NotificationManager.IMPORTANCE_LOW);
            nm.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_clock2)
                .setContentTitle("Timezone Updated")
                .setContentText("ChronicCare adjusted your reminders to " + tzId)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true);

        nm.notify(999, builder.build());
    }
}
