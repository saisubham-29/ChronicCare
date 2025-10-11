package com.example.chroniccare;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Calendar;

public class AlarmActivity extends AppCompatActivity {

    private Ringtone ringtone;
    private Button btnTakeNow, btnTakeLater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_alarm);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnTakeNow = findViewById(R.id.btnTakeNow);
        btnTakeLater = findViewById(R.id.btnTakeLater);

        // Play the alarm sound immediately when the activity opens
        playAlarmSound();

        btnTakeNow.setOnClickListener(v -> {
            stopAlarm();
            updateMedicationStatus(true); // mock DB update
            Toast.makeText(this, "✅ Medication marked as taken.", Toast.LENGTH_SHORT).show();
            finish();
        });

        btnTakeLater.setOnClickListener(v -> {
            stopAlarm();
            scheduleSnooze(5); // snooze for 5 minutes
            Toast.makeText(this, "⏰ Reminder snoozed for 5 minutes.", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void playAlarmSound() {
        Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmUri == null)
            alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        ringtone = RingtoneManager.getRingtone(getApplicationContext(), alarmUri);
        ringtone.play();
    }

    private void stopAlarm() {
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
    }

    private void scheduleSnooze(int minutes) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        long triggerTime = System.currentTimeMillis() + minutes * 60 * 1000L;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }
    }

    // Mock database update — replace with actual Room DB logic
    private void updateMedicationStatus(boolean taken) {
        // Example:
        // medicationDao.updateStatus(medicationId, "Taken");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAlarm();
    }
}
