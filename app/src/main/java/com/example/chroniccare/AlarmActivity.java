package com.example.chroniccare;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;

public class AlarmActivity extends AppCompatActivity {

    private CardView swipeButton;
    private TextView tvAlarmTime, alarmMedicationName, tvMealInfo, btnDismiss;
    private float initialX;
    private int screenWidth;
    private String medicationName, mealTime, time;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private boolean snoozed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        );

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        medicationName = getIntent().getStringExtra("medicationName");
        if (medicationName == null) medicationName = getIntent().getStringExtra("MEDICATION_NAME");
        
        mealTime = getIntent().getStringExtra("mealTime");
        time = getIntent().getStringExtra("time");

        tvAlarmTime = findViewById(R.id.tvAlarmTime);
        alarmMedicationName = findViewById(R.id.alarmMedicationName);
        tvMealInfo = findViewById(R.id.tvMealInfo);
        swipeButton = findViewById(R.id.swipeButton);
        btnDismiss = findViewById(R.id.btnDismiss);

        tvAlarmTime.setText(time != null ? time : "");
        alarmMedicationName.setText(medicationName != null ? medicationName : "");
        tvMealInfo.setText(mealTime != null ? mealTime : "");

        screenWidth = getResources().getDisplayMetrics().widthPixels;

        setupSwipeListener();
        btnDismiss.setOnClickListener(v -> finish());
    }

    private void setupSwipeListener() {
        swipeButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = event.getRawX();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float deltaX = event.getRawX() - initialX;
                        if (Math.abs(deltaX) < screenWidth * 0.6) {
                            swipeButton.setX(swipeButton.getX() + deltaX);
                            initialX = event.getRawX();
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        float finalX = swipeButton.getX();
                        float threshold = screenWidth * 0.3f;

                        if (finalX < -threshold) {
                            handleSnooze();
                        } else if (finalX > threshold) {
                            handleTaken();
                        } else {
                            swipeButton.animate().x(8).setDuration(200).start();
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private void handleTaken() {
        markMedicationTaken();
        Toast.makeText(this, "Medication marked as taken", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void handleSnooze() {
        if (snoozed) {
            Toast.makeText(this, "Already snoozed once", Toast.LENGTH_SHORT).show();
            swipeButton.animate().x(8).setDuration(200).start();
            return;
        }

        snoozed = true;
        Toast.makeText(this, "Reminder in 5 minutes", Toast.LENGTH_SHORT).show();
        scheduleSnooze(5);
        scheduleFollowUp(10);
        finish();
    }

    private void scheduleSnooze(int minutes) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, minutes);

        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("medicationName", medicationName);
        intent.putExtra("mealTime", mealTime);
        intent.putExtra("time", time);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                (medicationName + "_snooze").hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
    }

    private void scheduleFollowUp(int minutes) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, minutes);

        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("medicationName", medicationName + " - Follow Up");
        intent.putExtra("mealTime", "Did you take your medication?");
        intent.putExtra("time", time);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                (medicationName + "_followup").hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
    }

    private void markMedicationTaken() {
        String userId = null;
        
        if (auth.getCurrentUser() != null) {
            userId = auth.getCurrentUser().getUid();
        } else {
            com.google.android.gms.auth.api.signin.GoogleSignInAccount account = 
                com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(this);
            if (account != null) {
                userId = account.getId();
            }
        }
        
        if (userId == null) return;

        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);

        firestore.collection("users").document(userId).collection("medications")
                .whereEqualTo("name", medicationName)
                .whereGreaterThanOrEqualTo("timestamp", new Timestamp(today.getTime()))
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        querySnapshot.getDocuments().get(0).getReference()
                                .update("taken", true, "takenAt", Timestamp.now());
                    }
                });
    }

    @Override
    public void onBackPressed() {
        Toast.makeText(this, "Please swipe to respond", Toast.LENGTH_SHORT).show();
    }
}