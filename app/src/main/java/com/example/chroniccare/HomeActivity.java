package com.example.chroniccare;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.cardview.widget.CardView;
import com.google.android.material.card.MaterialCardView;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;

public class HomeActivity extends BottomNavActivity {

    // Core views that must exist
    private TextView currentReadingValue, lastCheckedTime;
    private CardView btnCheckNow, btnTakeNow;
    private TextView nextMedicationName, medTiming;

    private Calendar nextMedicationTime;
    private SimpleDateFormat timeFormat;
    private Random random;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize with error handling
        try {
            initializeBasicViews();
            setupBasicClickListeners();
            updateBasicUI();
        } catch (Exception e) {
            Toast.makeText(this, "Error initializing: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void initializeBasicViews() {
        // Only initialize essential views that definitely exist
        currentReadingValue = findViewById(R.id.currentReadingValue);
        lastCheckedTime = findViewById(R.id.LastCheckedTime);
        btnCheckNow = findViewById(R.id.btn_checknow);
        btnTakeNow = findViewById(R.id.btn_takenow);
        nextMedicationName = findViewById(R.id.NextMedicationName);
        medTiming = findViewById(R.id.MedTiming);

        // Initialize with safe defaults
        nextMedicationTime = Calendar.getInstance();
        nextMedicationTime.add(Calendar.MINUTE, 15);
        timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
        random = new Random();
    }

    private void setupBasicClickListeners() {
        // Safe click listeners with null checks
        if (btnCheckNow != null) {
            btnCheckNow.setOnClickListener(v -> simulateGlucoseCheck());
        }

        if (btnTakeNow != null) {
            btnTakeNow.setOnClickListener(v -> takeMedication());
        }

        // Safe quick actions setup
        setupQuickActions();

        // Safe schedule setup
        setupScheduleClicks();
    }

    private void setupQuickActions() {
        try {
            // Food card
            View foodParent = findViewById(R.id.foodImg);
            if (foodParent != null && foodParent.getParent() != null && foodParent.getParent().getParent() instanceof CardView) {
                CardView foodCard = (CardView) foodParent.getParent().getParent();
                foodCard.setOnClickListener(v -> logFood());
            }

            // Exercise card
            View exerciseParent = findViewById(R.id.gymImg);
            if (exerciseParent != null && exerciseParent.getParent() != null && exerciseParent.getParent().getParent() instanceof CardView) {
                CardView exerciseCard = (CardView) exerciseParent.getParent().getParent();
                exerciseCard.setOnClickListener(v -> logExercise());
            }

            // Report card
            View reportParent = findViewById(R.id.reportImg);
            if (reportParent != null && reportParent.getParent() != null && reportParent.getParent().getParent() instanceof CardView) {
                CardView reportCard = (CardView) reportParent.getParent().getParent();
                reportCard.setOnClickListener(v -> viewReports());
            }

            // Doctor card
            View doctorParent = findViewById(R.id.docImg);
            if (doctorParent != null && doctorParent.getParent() != null && doctorParent.getParent().getParent() instanceof CardView) {
                CardView doctorCard = (CardView) doctorParent.getParent().getParent();
                doctorCard.setOnClickListener(v -> contactDoctor());
            }
        } catch (Exception e) {
            // Ignore quick action errors - they're not critical
        }
    }

    private void setupScheduleClicks() {
        try {
            MaterialCardView scheduleCard = findViewById(R.id.TodaysScheduleCard);
            if (scheduleCard != null && scheduleCard.getChildAt(0) instanceof LinearLayout) {
                LinearLayout scheduleContainer = (LinearLayout) scheduleCard.getChildAt(0);

                for (int i = 0; i < scheduleContainer.getChildCount(); i++) {
                    View scheduleItem = scheduleContainer.getChildAt(i);
                    if (scheduleItem instanceof LinearLayout) {
                        final int position = i;
                        scheduleItem.setOnClickListener(v -> toggleScheduleItem((LinearLayout) v, position));
                    }
                }
            }
        } catch (Exception e) {
            // Ignore schedule errors - they're not critical
        }
    }

    private void toggleScheduleItem(LinearLayout scheduleItem, int position) {
        try {
            // The ImageView is the last child (index 2)
            if (scheduleItem.getChildCount() > 2) {
                ImageView checkIcon = (ImageView) scheduleItem.getChildAt(2);

                // Simple toggle based on current resource
                if (checkIcon.getTag() == null || !checkIcon.getTag().equals("checked")) {
                    checkIcon.setImageResource(R.drawable.ic_checked);
                    checkIcon.setTag("checked");
                    Toast.makeText(this, "Marked as completed", Toast.LENGTH_SHORT).show();
                } else {
                    checkIcon.setImageResource(R.drawable.ic_unchecked);
                    checkIcon.setTag("unchecked");
                    Toast.makeText(this, "Marked as incomplete", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error updating schedule", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateBasicUI() {
        // Update essential UI elements safely
        updateReadingStatus(128);
        updateLastCheckedTime();
        updateNextMedication();
        updateGreetingAndDate();
    }

    private void updateGreetingAndDate() {
        try {
            // Update greeting
            TextView greetingView = findViewById(R.id.mainPageGreeting);
            if (greetingView != null) {
                Calendar calendar = Calendar.getInstance();
                int hour = calendar.get(Calendar.HOUR_OF_DAY);

                String greeting = "Good Morning,";
                if (hour >= 12 && hour < 17) greeting = "Good Afternoon,";
                else if (hour >= 17) greeting = "Good Evening,";

                greetingView.setText(greeting);
            }

            // Update date
            TextView dateView = findViewById(R.id.mainPageDate);
            if (dateView != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMM dd", Locale.getDefault());
                dateView.setText(dateFormat.format(Calendar.getInstance().getTime()));
            }
        } catch (Exception e) {
            // Ignore greeting/date errors
        }
    }

    private void simulateGlucoseCheck() {
        try {
            int newReading = 80 + random.nextInt(120);
            updateReadingStatus(newReading);
            updateLastCheckedTime();
            Toast.makeText(this, "New reading: " + newReading + " mg/dL", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error checking glucose", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateReadingStatus(int reading) {
        if (currentReadingValue != null) {
            currentReadingValue.setText(String.valueOf(reading));
        }

        // Update status indicator safely
        try {
            LinearLayout statusLayout = findViewById(R.id.statusLayout);
            if (statusLayout != null) {
                View indicator = statusLayout.getChildAt(0);
                TextView statusText = (TextView) statusLayout.getChildAt(1);

                if (reading < 70) {
                    if (statusText != null) statusText.setText("Low");
                    if (indicator != null) indicator.setBackgroundColor(0xFFFF0000); // Red
                } else if (reading > 180) {
                    if (statusText != null) statusText.setText("High");
                    if (indicator != null) indicator.setBackgroundColor(0xFFFF0000); // Red
                } else {
                    if (statusText != null) statusText.setText("In Range");
                    if (indicator != null) indicator.setBackgroundColor(0xFF00FF00); // Green
                }
            }
        } catch (Exception e) {
            // Ignore status update errors
        }
    }

    private void updateLastCheckedTime() {
        if (lastCheckedTime != null) {
            String currentTime = new SimpleDateFormat("h:mma", Locale.getDefault())
                    .format(Calendar.getInstance().getTime())
                    .toLowerCase();
            lastCheckedTime.setText("Last checked at " + currentTime);
        }
    }

    private void updateNextMedication() {
        if (nextMedicationName != null) nextMedicationName.setText("Empagliflozin");
        if (medTiming != null) {
            Calendar now = Calendar.getInstance();
            long minutes = (nextMedicationTime.getTimeInMillis() - now.getTimeInMillis()) / (60 * 1000);
            medTiming.setText(minutes <= 0 ? "Due Now -" : "Due In " + minutes + " Mins -");
        }
    }

    private void takeMedication() {
        try {
            Toast.makeText(this, "Empagliflozin 10mg marked as taken", Toast.LENGTH_SHORT).show();

            if (btnTakeNow != null) {
                btnTakeNow.setEnabled(false);
                btnTakeNow.setAlpha(0.5f);

                // Update next medication time
                nextMedicationTime.add(Calendar.HOUR, 4);
                updateNextMedication();

                // Reset button
                new android.os.Handler().postDelayed(() -> {
                    if (btnTakeNow != null) {
                        btnTakeNow.setEnabled(true);
                        btnTakeNow.setAlpha(1f);
                    }
                }, 2000);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error taking medication", Toast.LENGTH_SHORT).show();
        }
    }

    private void logFood() {
        Toast.makeText(this, "Navigate to Food Logging", Toast.LENGTH_SHORT).show();
    }

    private void logExercise() {
        Toast.makeText(this, "Navigate to Exercise Logging", Toast.LENGTH_SHORT).show();
    }

    private void viewReports() {
        Toast.makeText(this, "Navigate to Reports", Toast.LENGTH_SHORT).show();
    }

    private void contactDoctor() {
        Toast.makeText(this, "Navigate to Contact Doctor", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_main; // Make sure this matches your XML file name
    }

    @Override
    protected int getBottomNavMenuItemId() {
        return R.id.nav_home;
    }
}