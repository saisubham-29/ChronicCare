package com.example.chroniccare;

import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import java.util.HashMap;
import java.util.Map;

public class AlarmActivity extends AppCompatActivity {

    private String medicationId;
    private String medicationName;
    private FirebaseFirestore firestore;
    private int alarmCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);

        firestore = FirebaseFirestore.getInstance();

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        );

        Intent intent = getIntent();
        medicationId = intent.getStringExtra("MEDICATION_ID");
        medicationName = intent.getStringExtra("MEDICATION_NAME");
        alarmCode = intent.getIntExtra("ALARM_CODE", AlarmService.INITIAL_REMINDER_CODE);

        if (medicationId == null || medicationName == null) {
            Toast.makeText(this, "Reminder data missing.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        TextView medNameView = findViewById(R.id.alarmMedicationName);
        Button btnTakeNow = findViewById(R.id.btnAlarmTakeNow);
        Button btnWillTakeLater = findViewById(R.id.btnAlarmDismiss);

        String alertType = "";
        if (alarmCode == AlarmService.FOLLOWUP_REMINDER_CODE) {
            alertType = " (2nd Alert)";
            btnWillTakeLater.setText("Will Take Later");
        } else if (alarmCode == AlarmService.SNOOZE_REMINDER_CODE) {
            alertType = " (Snoozed)";
            btnWillTakeLater.setText("Snooze (10 min)");
        } else {
            btnWillTakeLater.setText("Will Take Later");
        }
        medNameView.setText(medicationName + alertType);

        btnTakeNow.setOnClickListener(v -> takeMedicationNow());
        btnWillTakeLater.setOnClickListener(v -> willTakeLater());
    }

    private void takeMedicationNow() {
        if (medicationId == null) return;

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("MedTaken", true);

        firestore.collection("Medications").document(medicationId)
                .set(updateData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, medicationName + " taken. Alarm stopped.", Toast.LENGTH_SHORT).show();
                    AlarmService.cancelAllReminders(this, medicationId, medicationName);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update Firestore.", Toast.LENGTH_SHORT).show();
                });
    }

    private void willTakeLater() {
        AlarmService.cancelSpecificReminder(this, medicationId, medicationName, AlarmService.INITIAL_REMINDER_CODE);
        AlarmService.cancelSpecificReminder(this, medicationId, medicationName, AlarmService.FOLLOWUP_REMINDER_CODE);

        AlarmService.scheduleSnoozeAlarm(this, medicationId, medicationName);

        finish();
    }

    @Override
    public void onBackPressed() {
        Toast.makeText(this, "Select 'Take Now' or 'Will Take Later'", Toast.LENGTH_SHORT).show();
    }
}