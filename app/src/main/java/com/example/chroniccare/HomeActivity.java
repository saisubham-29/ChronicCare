package com.example.chroniccare;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.cardview.widget.CardView;
import com.google.android.material.card.MaterialCardView;
import androidx.appcompat.widget.AppCompatButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;

public class HomeActivity extends BottomNavActivity {

    private TextView currentReadingValue;
    private TextView lastCheckedTime;
    private CardView btnCheckNow, btnTakeNow;
    private AppCompatButton btnTestAlarm;
    private TextView nextMedicationName, medTiming;
    private CardView logFoodCard;

    private Calendar nextMedicationTime;
    private Random random;

    private static final String NEXT_DUE_MED_ID = "MED_DOC_ID_FOR_DEMO";
    private static final String NEXT_DUE_MED_NAME = "Empagliflozin 10mg";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            initializeViews();
            setupClickListeners();
            updateBasicUI();
        } catch (Exception e) {
            Toast.makeText(this, "Application setup error.", Toast.LENGTH_LONG).show();
        }
    }

    private void initializeViews() {
        currentReadingValue = findViewById(R.id.currentReadingValue);
        lastCheckedTime = findViewById(R.id.LastCheckedTime);
        btnCheckNow = findViewById(R.id.btn_checknow);
        btnTakeNow = findViewById(R.id.btn_takenow);
        nextMedicationName = findViewById(R.id.NextMedicationName);
        medTiming = findViewById(R.id.MedTiming);
        logFoodCard = findViewById(R.id.LogFood);
        btnTestAlarm = findViewById(R.id.btnTestAlarm);

        nextMedicationTime = Calendar.getInstance();
        nextMedicationTime.add(Calendar.MINUTE, 15);
        random = new Random();
    }

    private void setupClickListeners() {
        if (btnCheckNow != null) {
            btnCheckNow.setOnClickListener(v -> simulateGlucoseCheck());
        }

        if (btnTakeNow != null) {
            takeMedication();
        }

        if (btnTestAlarm != null) {
            btnTestAlarm.setOnClickListener(v -> triggerAlarmForDueMedication());
        }

        setupScheduleClicks();
        setupQuickActions();

        TextView addMedication = findViewById(R.id.AddMediaction);
        if (addMedication != null) {
            addMedication.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, AddMedications.class);
                startActivity(intent);
            });
        }
    }

    private void triggerAlarmForDueMedication() {
        String DUE_MEDICATION_ID = NEXT_DUE_MED_ID;
        String DUE_MEDICATION_NAME = NEXT_DUE_MED_NAME;
        long triggerTime = System.currentTimeMillis() + 5000;
        AlarmService.scheduleInitialAlarm(this, DUE_MEDICATION_ID, DUE_MEDICATION_NAME, triggerTime);
    }

    private void takeMedication() {
        if (btnTakeNow != null) {
            btnTakeNow.setOnClickListener(v -> {
                AlarmService.cancelAllReminders(this, NEXT_DUE_MED_ID, NEXT_DUE_MED_NAME);
                Toast.makeText(this, NEXT_DUE_MED_NAME + " marked as taken (Home Button)", Toast.LENGTH_SHORT).show();

                btnTakeNow.setEnabled(false);
                btnTakeNow.setAlpha(0.5f);
                nextMedicationTime = Calendar.getInstance();
                nextMedicationTime.add(Calendar.HOUR, 4);
                updateNextMedication();

                new android.os.Handler().postDelayed(() -> {
                    if (btnTakeNow != null) {
                        btnTakeNow.setEnabled(true);
                        btnTakeNow.setAlpha(1f);
                        updateNextMedication();
                    }
                }, 5000);
            });
        }
    }

    private void simulateGlucoseCheck() {
        try {
            int newReading = 80 + random.nextInt(120);
            updateReadingStatus(newReading);
            updateLastCheckedTime();
            Toast.makeText(this, "New reading logged: " + newReading + " mg/dL", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error checking glucose", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateBasicUI() {
        updateReadingStatus(128);
        updateLastCheckedTime();
        updateNextMedication();
        updateGreetingAndDate();
    }

    // --- UI Update Methods (Required but trimmed) ---

    private void updateReadingStatus(int reading) {
        if (currentReadingValue != null) {
            currentReadingValue.setText(String.valueOf(reading));
        }
        // ... (Status indicator color logic) ...
    }

    private void updateLastCheckedTime() {
        if (lastCheckedTime != null) {
            String currentTime = new SimpleDateFormat("h:mma", Locale.getDefault()).format(Calendar.getInstance().getTime()).toLowerCase();
            lastCheckedTime.setText("Last checked at " + currentTime);
        }
    }

    private void updateNextMedication() {
        if (nextMedicationName != null) nextMedicationName.setText(NEXT_DUE_MED_NAME);
        if (medTiming != null) {
            Calendar now = Calendar.getInstance();
            long minutes = (nextMedicationTime.getTimeInMillis() - now.getTimeInMillis()) / (60 * 1000);

            String timingText;
            if (minutes <= 0) {
                timingText = "Due Now -";
            } else if (minutes < 60) {
                timingText = "Due In " + minutes + " Mins -";
            } else {
                long hours = minutes / 60;
                minutes = minutes % 60;
                timingText = "Due In " + hours + " Hr " + minutes + " Mins -";
            }
            medTiming.setText(timingText);
        }
    }

    private void updateGreetingAndDate() {
        // ... (Greeting and Date logic) ...
    }

    private void setupScheduleClicks() {
        // ... (Schedule click logic) ...
    }

    private void toggleScheduleItem(LinearLayout scheduleItem, int position) {
        // ... (Toggle logic) ...
    }

    private void setupQuickActions() {
        if (logFoodCard != null) logFoodCard.setOnClickListener(v -> logFood());
        CardView logExerciseCard = findViewById(R.id.LogExercise);
        if (logExerciseCard != null) logExerciseCard.setOnClickListener(v -> logExercise());
        CardView viewReportsCard = findViewById(R.id.ViewReports);
        if (viewReportsCard != null) viewReportsCard.setOnClickListener(v -> viewReports());
        CardView contactDoctorCard = findViewById(R.id.ContactDoctor);
        if (contactDoctorCard != null) contactDoctorCard.setOnClickListener(v -> contactDoctor());
    }

    private void logFood() { Toast.makeText(this, "Log Food", Toast.LENGTH_SHORT).show(); }
    private void logExercise() { Toast.makeText(this, "Log Exercise", Toast.LENGTH_SHORT).show(); }
    private void viewReports() { Toast.makeText(this, "View Reports", Toast.LENGTH_SHORT).show(); }
    private void contactDoctor() { Toast.makeText(this, "Contact Doctor", Toast.LENGTH_SHORT).show(); }

    @Override
    protected int getLayoutId() { return R.layout.activity_main; }

    @Override
    protected int getBottomNavMenuItemId() { return R.id.nav_home; }
}