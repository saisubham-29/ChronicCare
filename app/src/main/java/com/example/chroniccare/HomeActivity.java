package com.example.chroniccare;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.AppCompatButton;
import androidx.cardview.widget.CardView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.auth.FirebaseAuth;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Random;

public class HomeActivity extends BottomNavActivity {

    // Header
    private TextView mainPageGreeting, mainPageName, mainPageDate;
    private de.hdodenhof.circleimageview.CircleImageView profileImage;

    // Live data
    private TextView currentReadingValue, lastCheckedTime;

    // Medication
    private TextView nextMedicationName, nextMedicationDose, medTiming, foodInstruction;

    // Buttons
    private AppCompatButton btnCheckNow, btnTakeNow;

    // Quick actions
    private CardView logFoodCard, logExerciseCard, viewReportsCard, contactDoctorCard;

    // Today's Schedule
    private LinearLayout todaysScheduleContainer;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    // Logic
    private Random random;
    private GoogleSignInAccount account;
    private Handler handler = new Handler();
    private Runnable updateTimeRunnable;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = getSharedPreferences("ChronicCarePrefs", MODE_PRIVATE);
        initializeViews();

        account = GoogleSignIn.getLastSignedInAccount(this);

        updateGreeting();
        updateDate();
        updateUserName(account);
        loadUserPhoto();
        updateInitialReadings();
        loadNextMedication();
        loadTodaysSchedule();

        setupClickListeners();
        startTimeUpdater();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTodaysSchedule();
        loadNextMedication();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (updateTimeRunnable != null) {
            handler.removeCallbacks(updateTimeRunnable);
        }
    }

    private void initializeViews() {
        mainPageGreeting = findViewById(R.id.mainPageGreeting);
        mainPageName = findViewById(R.id.mainPageName);
        mainPageDate = findViewById(R.id.mainPageDate);
        profileImage = findViewById(R.id.profile_image);

        currentReadingValue = findViewById(R.id.currentReadingValue);
        lastCheckedTime = findViewById(R.id.LastCheckedTime);

        nextMedicationName = findViewById(R.id.NextMedicationName);
        nextMedicationDose = findViewById(R.id.NextMedicationDose);
        medTiming = findViewById(R.id.MedTiming);
        foodInstruction = findViewById(R.id.FoodInstrcution);

        btnCheckNow = findViewById(R.id.btn_checknow);
        btnTakeNow  = findViewById(R.id.btn_takenow);

        logFoodCard = findViewById(R.id.LogFood);
        logExerciseCard = findViewById(R.id.LogExercise);
        viewReportsCard = findViewById(R.id.ViewReports);
        contactDoctorCard = findViewById(R.id.ContactDoctor);

        todaysScheduleContainer = findViewById(R.id.todaysScheduleContainer);
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        random = new Random();
    }

    private void updateGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) mainPageGreeting.setText("Good Morning,");
        else if (hour < 17) mainPageGreeting.setText("Good Afternoon,");
        else mainPageGreeting.setText("Good Evening,");
    }

    private void updateDate() {
        String date = new SimpleDateFormat(
                "EEEE, MMM dd",
                Locale.getDefault()
        ).format(Calendar.getInstance().getTime());
        mainPageDate.setText(date);
    }

    private void updateUserName(GoogleSignInAccount account) {

        String fullName = "User";

        if (account != null) {
            if (account.getDisplayName() != null)
                fullName = account.getDisplayName();
            else if (account.getGivenName() != null)
                fullName = account.getGivenName();
            else if (account.getEmail() != null)
                fullName = account.getEmail();
        } else {
            fullName = sharedPreferences.getString("userName", "User");
        }

        String displayName = formatName(fullName);
        mainPageName.setText(displayName);
    }
    
    private void loadUserPhoto() {
        String photoUrl = sharedPreferences.getString("userPhoto", "");
        
        if (!photoUrl.isEmpty()) {
            if (photoUrl.startsWith("http")) {
                Picasso.get().load(photoUrl).placeholder(R.drawable.ic_profile).into(profileImage);
            } else {
                profileImage.setImageURI(Uri.parse(photoUrl));
            }
        }
        
        profileImage.setOnClickListener(v -> 
            startActivity(new Intent(this, ProfileActivity.class))
        );
    }
    
    private String formatName(String name) {

        if (name == null || name.trim().isEmpty())
            return "User";

        name = name.trim();

        // If there is no space, return as-is
        if (!name.contains(" "))
            return name;

        // Extract first name
        String firstName = name.substring(0, name.indexOf(" "));

        // If name is longer than first name, add dots
        return firstName + "...";
    }


    private void updateInitialReadings() {
        currentReadingValue.setText("128");
        updateLastCheckedTime();
    }

    private void updateLastCheckedTime() {
        String time = new SimpleDateFormat("h:mma", Locale.getDefault())
                .format(Calendar.getInstance().getTime()).toLowerCase();
        lastCheckedTime.setText("Last checked at " + time);
    }

    private void loadNextMedication() {
        String userId = null;
        
        if (auth.getCurrentUser() != null) {
            userId = auth.getCurrentUser().getUid();
        } else {
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
            if (account != null) {
                userId = account.getId();
            }
        }
        
        if (userId == null) {
            nextMedicationName.setText("No medications");
            nextMedicationDose.setText("");
            medTiming.setText("");
            foodInstruction.setText("");
            return;
        }

        Calendar now = Calendar.getInstance();
        
        firestore.collection("users").document(userId).collection("medications")
                .whereEqualTo("taken", false)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    QueryDocumentSnapshot nextMed = null;
                    long minDiff = Long.MAX_VALUE;
                    
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Timestamp timestamp = doc.getTimestamp("timestamp");
                        if (timestamp != null) {
                            Calendar medTime = Calendar.getInstance();
                            medTime.setTime(timestamp.toDate());
                            
                            // Get time difference
                            long diff = medTime.getTimeInMillis() - now.getTimeInMillis();
                            
                            // If in future and closer than current min
                            if (diff > 0 && diff < minDiff) {
                                minDiff = diff;
                                nextMed = doc;
                            }
                        }
                    }
                    
                    if (nextMed != null) {
                        updateNextMedicationUI(nextMed, minDiff);
                    } else {
                        nextMedicationName.setText("No upcoming");
                        nextMedicationDose.setText("");
                        medTiming.setText("");
                        foodInstruction.setText("");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading next medication", e);
                    nextMedicationName.setText("Error loading");
                    nextMedicationDose.setText("");
                    medTiming.setText("");
                    foodInstruction.setText("");
                });
    }

    private void updateNextMedicationUI(QueryDocumentSnapshot doc, long millisUntil) {
        String fullName = doc.getString("name");
        String mealTime = doc.getString("mealTime");
        
        // Split name and dose (e.g., "Metformin 500mg" -> "Metformin" + "500mg")
        if (fullName != null && fullName.contains(" ")) {
            int lastSpace = fullName.lastIndexOf(" ");
            String medName = fullName.substring(0, lastSpace);
            String dose = fullName.substring(lastSpace + 1);
            
            nextMedicationName.setText(medName);
            nextMedicationDose.setText(dose);
        } else {
            nextMedicationName.setText(fullName != null ? fullName : "Medication");
            nextMedicationDose.setText("");
        }
        
        // Calculate time remaining
        long minutes = millisUntil / 60000;
        long hours = minutes / 60;
        long days = hours / 24;
        
        String timeText;
        if (days > 0) {
            timeText = "Due in " + days + " day" + (days > 1 ? "s" : "");
        } else if (hours > 0) {
            timeText = "Due in " + hours + " hr" + (hours > 1 ? "s" : "");
        } else if (minutes > 0) {
            timeText = "Due in " + minutes + " min" + (minutes > 1 ? "s" : "");
        } else {
            timeText = "Due Now";
        }
        
        medTiming.setText(timeText + " -");
        foodInstruction.setText(mealTime != null ? mealTime : "");
    }

    private void startTimeUpdater() {
        updateTimeRunnable = new Runnable() {
            @Override
            public void run() {
                loadNextMedication();
                handler.postDelayed(this, 60000); // Update every minute
            }
        };
        handler.postDelayed(updateTimeRunnable, 60000);
    }

    private void setupClickListeners() {

        btnCheckNow.setOnClickListener(v -> {
            int reading = 80 + random.nextInt(120);
            currentReadingValue.setText(String.valueOf(reading));
            updateLastCheckedTime();
            Toast.makeText(this, "Reading logged", Toast.LENGTH_SHORT).show();
        });

        btnTakeNow.setOnClickListener(v -> {
            Toast.makeText(this, "Mark medications from schedule below", Toast.LENGTH_SHORT).show();
        });

        logExerciseCard.setOnClickListener(v ->
                startActivity(new Intent(this, LogExercise.class)));

        findViewById(R.id.AddMediaction).setOnClickListener(v ->
                startActivity(new Intent(this, AddMedications.class)));
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected int getBottomNavMenuItemId() {
        return R.id.nav_home;
    }

    //today's schedule section
    private void loadTodaysSchedule() {
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

        // Load all medications
        firestore.collection("users").document(userId).collection("medications")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    todaysScheduleContainer.removeAllViews();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        addMedicationRow(document);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading medications", e);
                    Toast.makeText(this, "Error loading medications", Toast.LENGTH_SHORT).show();
                });
    }

    private void addMedicationRow(QueryDocumentSnapshot document) {
        View rowView = LayoutInflater.from(this).inflate(R.layout.schedule_row_item, todaysScheduleContainer, false);
        
        TextView scheduleTime = rowView.findViewById(R.id.scheduleTime);
        TextView scheduleTitle = rowView.findViewById(R.id.scheduleTitle);
        ImageView scheduleStatusIcon = rowView.findViewById(R.id.scheduleStatusIcon);
        
        String name = document.getString("name");
        String time = document.getString("time");
        Boolean taken = document.getBoolean("taken");
        Timestamp takenAt = document.getTimestamp("takenAt");
        
        scheduleTime.setText(time != null ? time : "");
        scheduleTitle.setText(name != null ? name : "");
        scheduleStatusIcon.setImageResource(Boolean.TRUE.equals(taken) ? R.drawable.ic_checked : R.drawable.ic_unchecked);
        
        scheduleStatusIcon.setOnClickListener(v -> handleMedicationClick(document, taken, takenAt));
        
        todaysScheduleContainer.addView(rowView);
    }

    private void handleMedicationClick(QueryDocumentSnapshot document, Boolean taken, Timestamp takenAt) {
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
        
        // Toggle taken status
        if (Boolean.TRUE.equals(taken)) {
            // Unmark as taken
            firestore.collection("users").document(userId).collection("medications")
                    .document(document.getId())
                    .update("taken", false, "takenAt", null)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Medication unmarked", Toast.LENGTH_SHORT).show();
                        loadTodaysSchedule();
                        loadNextMedication();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error updating medication", e);
                        Toast.makeText(this, "Error updating medication", Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Mark as taken
            Timestamp now = Timestamp.now();
            firestore.collection("users").document(userId).collection("medications")
                    .document(document.getId())
                    .update("taken", true, "takenAt", now)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Medication taken", Toast.LENGTH_SHORT).show();
                        loadTodaysSchedule();
                        loadNextMedication();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error updating medication", e);
                        Toast.makeText(this, "Error updating medication", Toast.LENGTH_SHORT).show();
                    });
        }
    }

}
