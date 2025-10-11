package com.example.chroniccare;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class AlarmService {

    private static final String TAG = "AlarmService";
    public static final int INITIAL_REMINDER_CODE = 1001;
    public static final int FOLLOWUP_REMINDER_CODE = 1002;
    public static final int SNOOZE_REMINDER_CODE = 1003;

    private static final long FOLLOWUP_DELAY_MS = 5 * 60 * 1000;
    private static final long SNOOZE_DELAY_MS = 10 * 60 * 1000;

    public static void scheduleInitialAlarm(Context context, String medicationId, String medName, long triggerTimeMs) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        scheduleSingleAlarm(context, alarmManager, medicationId, medName, triggerTimeMs, INITIAL_REMINDER_CODE);
        scheduleSingleAlarm(context, alarmManager, medicationId, medName, triggerTimeMs + FOLLOWUP_DELAY_MS, FOLLOWUP_REMINDER_CODE);

        Toast.makeText(context, medName + " reminder set!", Toast.LENGTH_SHORT).show();
    }

    public static void scheduleSnoozeAlarm(Context context, String medicationId, String medName) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        cancelSpecificReminder(context, medicationId, medName, SNOOZE_REMINDER_CODE);

        long snoozeTriggerTimeMs = System.currentTimeMillis() + SNOOZE_DELAY_MS;

        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                snoozeTriggerTimeMs,
                SNOOZE_DELAY_MS,
                getPendingIntent(context, medicationId, medName, SNOOZE_REMINDER_CODE)
        );

        Log.d(TAG, "Snooze alarm scheduled.");
        Toast.makeText(context, "Snoozing for 10 minutes...", Toast.LENGTH_SHORT).show();
    }

    private static void scheduleSingleAlarm(Context context, AlarmManager alarmManager, String medicationId, String medName, long triggerTimeMs, int requestCode) {
        alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMs,
                getPendingIntent(context, medicationId, medName, requestCode)
        );
    }

    private static PendingIntent getPendingIntent(Context context, String medicationId, String medName, int requestCode) {
        Intent intent = new Intent(context, AlarmActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("MEDICATION_ID", medicationId);
        intent.putExtra("MEDICATION_NAME", medName);
        intent.putExtra("ALARM_CODE", requestCode);

        return PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );
    }

    private static void cancelSpecificReminder(Context context, String medicationId, String medName, int requestCode) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;
        PendingIntent pendingIntent = getPendingIntent(context, medicationId, medName, requestCode);
        alarmManager.cancel(pendingIntent);
    }

    public static void cancelAllReminders(Context context, String medicationId, String medName) {
        cancelSpecificReminder(context, medicationId, medName, INITIAL_REMINDER_CODE);
        cancelSpecificReminder(context, medicationId, medName, FOLLOWUP_REMINDER_CODE);
        cancelSpecificReminder(context, medicationId, medName, SNOOZE_REMINDER_CODE);
    }
}