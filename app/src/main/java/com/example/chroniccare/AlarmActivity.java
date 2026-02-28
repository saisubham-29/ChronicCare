package com.example.chroniccare;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AlarmActivity extends AppCompatActivity {

    private CardView swipeButton;
    private View swipeOverlay;
    private ImageView ivBell, ivSwipeArrow;
    private LinearLayout leftAction, rightAction;
    private TextView tvAlarmTime, alarmMedicationName, tvMealInfo;
    private MaterialButton btnDismiss;
    
    private float initialX;
    private float originalX = -1;
    private int screenWidth;
    private String medicationName, mealTime, time;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private boolean snoozed = false;
    
    private ObjectAnimator pulseAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            if (keyguardManager != null) {
                keyguardManager.requestDismissKeyguard(this, null);
            }
        }
        
        setContentView(R.layout.activity_alarm);

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
        swipeOverlay = findViewById(R.id.swipeOverlay);
        ivBell = findViewById(R.id.ivBell);
        ivSwipeArrow = findViewById(R.id.ivSwipeArrow);
        leftAction = findViewById(R.id.leftAction);
        rightAction = findViewById(R.id.rightAction);
        btnDismiss = findViewById(R.id.btnDismiss);

        tvAlarmTime.setText(time != null ? time : "");
        alarmMedicationName.setText(medicationName != null ? medicationName : "");
        tvMealInfo.setText(mealTime != null ? mealTime : "");

        screenWidth = getResources().getDisplayMetrics().widthPixels;

        setupSwipeListener();
        startBellAnimation();
        startPulseAnimation();
        
        btnDismiss.setOnClickListener(v -> finish());
    }

    private void startBellAnimation() {
        RotateAnimation rotate = new RotateAnimation(-15, 15, 
            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0f);
        rotate.setDuration(500);
        rotate.setRepeatMode(Animation.REVERSE);
        rotate.setRepeatCount(Animation.INFINITE);
        ivBell.startAnimation(rotate);
    }

    private void startPulseAnimation() {
        pulseAnimator = ObjectAnimator.ofPropertyValuesHolder(swipeButton,
                PropertyValuesHolder.ofFloat("scaleX", 1.05f),
                PropertyValuesHolder.ofFloat("scaleY", 1.05f));
        pulseAnimator.setDuration(800);
        pulseAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        pulseAnimator.setRepeatMode(ObjectAnimator.REVERSE);
        pulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        pulseAnimator.start();
    }

    private void setupSwipeListener() {
        swipeButton.post(() -> {
            if (originalX == -1) {
                originalX = swipeButton.getX();
            }
        });

        swipeButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = event.getRawX();
                        if (pulseAnimator != null) pulseAnimator.pause();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float deltaX = event.getRawX() - initialX;
                        float newX = swipeButton.getX() + deltaX;
                        swipeButton.setX(newX);
                        initialX = event.getRawX();

                        updateVisualFeedback(newX - originalX);
                        return true;

                    case MotionEvent.ACTION_UP:
                        float currentX = swipeButton.getX();
                        float displacement = currentX - originalX;
                        float threshold = screenWidth * 0.25f;

                        if (displacement < -threshold) {
                            handleSnooze();
                        } else if (displacement > threshold) {
                            handleTaken();
                        } else {
                            resetButton();
                        }
                        return true;
                }
                return false;
            }
        });
    }

    private void updateVisualFeedback(float displacement) {
        float threshold = screenWidth * 0.25f;
        float progress = Math.min(Math.abs(displacement) / threshold, 1.0f);
        
        swipeOverlay.setAlpha(progress * 0.3f);
        
        if (displacement < 0) { // Snooze - Orange
            swipeOverlay.setBackgroundColor(Color.parseColor("#FF9800"));
            leftAction.setScaleX(1.0f + (progress * 0.2f));
            leftAction.setScaleY(1.0f + (progress * 0.2f));
            rightAction.setScaleX(1.0f);
            rightAction.setScaleY(1.0f);
            
            // Rotate arrow to point left (180 is original, we want to point more "left")
            // Since it's already at 180, rotating it back to 0 or 360 would point right.
            // Let's keep it simple: 180 is left, 0 is right.
            ivSwipeArrow.setRotation(180); 
        } else { // Taken - Green
            swipeOverlay.setBackgroundColor(Color.parseColor("#4CAF50"));
            rightAction.setScaleX(1.0f + (progress * 0.2f));
            rightAction.setScaleY(1.0f + (progress * 0.2f));
            leftAction.setScaleX(1.0f);
            leftAction.setScaleY(1.0f);
            
            // Rotate arrow to point right
            ivSwipeArrow.setRotation(0);
        }
        
        // Bonus: Make the arrow "push" harder towards the direction
        ivSwipeArrow.setTranslationX(displacement * 0.1f);
    }

    private void resetButton() {
        swipeButton.animate().x(originalX).setDuration(200).start();
        swipeOverlay.animate().alpha(0).setDuration(200).start();
        leftAction.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
        rightAction.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
        
        ivSwipeArrow.animate().rotation(180).translationX(0).setDuration(200).start();

        if (pulseAnimator != null) pulseAnimator.resume();
    }

    private void handleTaken() {
        stopAlarmService();
        markMedicationTaken();
        Toast.makeText(this, "Medication marked as taken", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void handleSnooze() {
        if (snoozed) {
            Toast.makeText(this, "Already snoozed once", Toast.LENGTH_SHORT).show();
            resetButton();
            return;
        }

        stopAlarmService();
        snoozed = true;
        Toast.makeText(this, "Reminder in 5 minutes", Toast.LENGTH_SHORT).show();
        scheduleSnooze(5);
        scheduleFollowUp(10);
        finish();
    }
    
    private void stopAlarmService() {
        Intent serviceIntent = new Intent(this, AlarmForegroundService.class);
        stopService(serviceIntent);
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
        if (alarmManager != null) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
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
        if (alarmManager != null) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
    }

    private void markMedicationTaken() {
        String userId = null;
        if (auth.getCurrentUser() != null) {
            userId = auth.getCurrentUser().getUid();
        }
        if (userId == null) return;

        final String finalUserId = userId;
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);

        firestore.collection("users").document(finalUserId).collection("medications")
                .whereEqualTo("name", medicationName)
                .whereGreaterThanOrEqualTo("timestamp", new Timestamp(today.getTime()))
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        Timestamp now = Timestamp.now();
                        querySnapshot.getDocuments().get(0).getReference()
                                .update("taken", true, "takenAt", now)
                                .addOnSuccessListener(aVoid -> updateDailyAdherence(finalUserId, now));
                    }
                });
    }
    
    private void updateDailyAdherence(String userId, Timestamp timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dateKey = sdf.format(timestamp.toDate());
        
        firestore.collection("users").document(userId)
                .collection("medicationAdherence").document(dateKey)
                .get()
                .addOnSuccessListener(doc -> {
                    int taken = doc.exists() && doc.getLong("taken") != null ? doc.getLong("taken").intValue() : 0;
                    int total = doc.exists() && doc.getLong("total") != null ? doc.getLong("total").intValue() : 0;
                    
                    java.util.Map<String, Object> data = new java.util.HashMap<>();
                    data.put("taken", taken + 1);
                    data.put("total", total > 0 ? total : taken + 1);
                    data.put("date", timestamp);
                    
                    firestore.collection("users").document(userId)
                            .collection("medicationAdherence").document(dateKey)
                            .set(data);
                });
    }

    @Override
    public void onBackPressed() {
        Toast.makeText(this, "Please swipe to respond", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    protected void onDestroy() {
        if (pulseAnimator != null) pulseAnimator.cancel();
        super.onDestroy();
        stopAlarmService();
    }
}