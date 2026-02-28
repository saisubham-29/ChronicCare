package com.example.chroniccare;

import static android.content.ContentValues.TAG;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.cardview.widget.CardView;

import com.example.chroniccare.utils.ProfileImageHelper;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.auth.FirebaseAuth;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

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
    private MaterialButton btnDelete, btnCancelEdit;
    private View actionButtonsContainer;

    // Quick actions
    private CardView logFoodCard, logExerciseCard, viewReportsCard, contactDoctorCard;

    // Today's Schedule
    private LinearLayout todaysScheduleContainer;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private ListenerRegistration medicationListener;

    // Logic
    private Random random;
    private GoogleSignInAccount account;
    private Handler handler = new Handler();
    private Runnable updateTimeRunnable;
    private SharedPreferences sharedPreferences;
    
    // Selection state
    private boolean isInDeleteMode = false;
    private final Set<String> selectedMedIds = new HashSet<>();
    private final List<QueryDocumentSnapshot> currentMeds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = getSharedPreferences("ChronicCarePrefs", MODE_PRIVATE);
        initializeViews();

        account = GoogleSignIn.getLastSignedInAccount(this);

        updateGreeting();
        updateDate();
        updateUserName(account);
        ProfileImageHelper.loadProfileImage(this, profileImage);
        updateInitialReadings();
        
        loadTodaysSchedule();

        setupClickListeners();
        startTimeUpdater();
        checkAlarmPermission();
    }
    
    private void checkAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                new AlertDialog.Builder(this)
                    .setTitle(R.string.home_permission_title)
                    .setMessage(R.string.home_permission_message)
                    .setPositiveButton(R.string.home_permission_grant, (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    })
                    .setNegativeButton(R.string.home_permission_later, null)
                    .show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTodaysSchedule();
        ProfileImageHelper.loadProfileImage(this, profileImage);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (updateTimeRunnable != null) {
            handler.removeCallbacks(updateTimeRunnable);
        }
        if (medicationListener != null) {
            medicationListener.remove();
        }
    }

    private void initializeViews() {
        mainPageGreeting = findViewById(R.id.mainPageGreeting);
        mainPageName = findViewById(R.id.mainPageName);
        mainPageDate = findViewById(R.id.mainPageDate);
        profileImage = findViewById(R.id.profile_image);
        profileImage.setOnClickListener(v -> ProfileImageHelper.handleProfileClick(this));

        currentReadingValue = findViewById(R.id.currentReadingValue);
        lastCheckedTime = findViewById(R.id.LastCheckedTime);

        nextMedicationName = findViewById(R.id.NextMedicationName);
        nextMedicationDose = findViewById(R.id.NextMedicationDose);
        medTiming = findViewById(R.id.MedTiming);
        foodInstruction = findViewById(R.id.FoodInstrcution);

        btnCheckNow = findViewById(R.id.btn_checknow);
        btnTakeNow  = findViewById(R.id.btn_takenow);
        btnDelete = findViewById(R.id.btnDelete);
        btnCancelEdit = findViewById(R.id.btnCancelEdit);
        actionButtonsContainer = findViewById(R.id.actionButtonsContainer);

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
        if (hour < 12) mainPageGreeting.setText(R.string.home_greeting_morning);
        else if (hour < 17) mainPageGreeting.setText(R.string.home_greeting_afternoon);
        else mainPageGreeting.setText(R.string.home_greeting_evening);
    }

    private void updateDate() {
        String date = new SimpleDateFormat("EEEE, MMM dd", Locale.getDefault()).format(Calendar.getInstance().getTime());
        mainPageDate.setText(date);
    }

    private void updateUserName(GoogleSignInAccount account) {
        String fullName = getString(R.string.home_default_user_name);
        if (account != null) {
            if (account.getDisplayName() != null) fullName = account.getDisplayName();
            else if (account.getGivenName() != null) fullName = account.getGivenName();
        } else {
            fullName = sharedPreferences.getString("userName", getString(R.string.home_default_user_name));
        }
        mainPageName.setText(formatName(fullName));
    }
    
    private String formatName(String name) {
        if (name == null || name.trim().isEmpty()) return getString(R.string.home_default_user_name);
        name = name.trim();
        if (!name.contains(" ")) return name;
        return name.substring(0, name.indexOf(" ")) + "...";
    }

    private void updateInitialReadings() {
        currentReadingValue.setText(R.string.home_default_reading);
        updateLastCheckedTime();
    }

    private void updateLastCheckedTime() {
        String time = new SimpleDateFormat("h:mma", Locale.getDefault()).format(Calendar.getInstance().getTime()).toLowerCase();
        lastCheckedTime.setText(getString(R.string.home_last_checked_at, time));
    }

    private void updateNextMedicationFromSnapshot(List<QueryDocumentSnapshot> medications) {
        Calendar now = Calendar.getInstance();
        QueryDocumentSnapshot nextMed = null;
        long minDiff = Long.MAX_VALUE;
        
        for (QueryDocumentSnapshot doc : medications) {
            if (Boolean.TRUE.equals(doc.getBoolean("taken"))) continue;
            Timestamp timestamp = doc.getTimestamp("timestamp");
            if (timestamp != null) {
                Calendar medTime = Calendar.getInstance();
                medTime.setTime(timestamp.toDate());
                long diff = medTime.getTimeInMillis() - now.getTimeInMillis();
                if (diff > 0 && diff < minDiff) {
                    minDiff = diff;
                    nextMed = doc;
                }
            }
        }
        
        if (nextMed != null) updateNextMedicationUI(nextMed, minDiff);
        else {
            nextMedicationName.setText(R.string.home_no_upcoming);
            nextMedicationDose.setText("");
            medTiming.setText("");
            foodInstruction.setText("");
        }
    }

    private void updateNextMedicationUI(QueryDocumentSnapshot doc, long millisUntil) {
        String fullName = doc.getString("name");
        String mealTime = doc.getString("mealTime");
        if (fullName != null && fullName.contains(" ")) {
            int lastSpace = fullName.lastIndexOf(" ");
            nextMedicationName.setText(fullName.substring(0, lastSpace));
            nextMedicationDose.setText(fullName.substring(lastSpace + 1));
        } else {
            nextMedicationName.setText(fullName != null ? fullName : getString(R.string.home_default_medication));
            nextMedicationDose.setText("");
        }
        long minutes = millisUntil / 60000;
        long hours = minutes / 60;
        long days = hours / 24;
        
        String timeText;
        if (days > 0) {
            timeText = getResources().getQuantityString(R.plurals.home_due_days, (int) days, (int) days);
        } else if (hours > 0) {
            timeText = getResources().getQuantityString(R.plurals.home_due_hours, (int) hours, (int) hours);
        } else if (minutes > 0) {
            timeText = getResources().getQuantityString(R.plurals.home_due_minutes, (int) minutes, (int) minutes);
        } else {
            timeText = getString(R.string.home_due_now);
        }
        
        medTiming.setText(getString(R.string.home_due_with_separator, timeText));
        foodInstruction.setText(mealTime != null ? mealTime : "");
    }

    private void startTimeUpdater() {
        updateTimeRunnable = () -> {
            loadTodaysSchedule(); 
            handler.postDelayed(updateTimeRunnable, 60000);
        };
        handler.postDelayed(updateTimeRunnable, 60000);
    }

    private void setupClickListeners() {
        btnCheckNow.setOnClickListener(v -> {
            currentReadingValue.setText(String.valueOf(80 + random.nextInt(120)));
            updateLastCheckedTime();
            Toast.makeText(this, R.string.home_reading_logged_toast, Toast.LENGTH_SHORT).show();
        });

        btnTakeNow.setOnClickListener(v -> Toast.makeText(this, R.string.home_mark_medications_toast, Toast.LENGTH_SHORT).show());
        
        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> confirmDeletion());
        }
        
        if (btnCancelEdit != null) {
            btnCancelEdit.setOnClickListener(v -> {
                isInDeleteMode = false;
                selectedMedIds.clear();
                if (actionButtonsContainer != null) {
                    actionButtonsContainer.setVisibility(View.GONE);
                }
                loadTodaysSchedule();
            });
        }

        logFoodCard.setOnClickListener(v -> startActivity(new Intent(this, LogFood.class)));
        logExerciseCard.setOnClickListener(v -> startActivity(new Intent(this, LogExercise.class)));
        viewReportsCard.setOnClickListener(v -> startActivity(new Intent(this, ReportsActivity.class)));
        findViewById(R.id.AddMediaction).setOnClickListener(v -> startActivity(new Intent(this, AddMedications.class)));
    }

    private void loadTodaysSchedule() {
        String userId = getUserId();
        if (userId == null) return;

        if (medicationListener != null) medicationListener.remove();
        medicationListener = firestore.collection("users").document(userId).collection("medications")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;
                    todaysScheduleContainer.removeAllViews();
                    currentMeds.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        currentMeds.add(doc);
                        addMedicationRow(doc);
                    }
                    updateNextMedicationFromSnapshot(currentMeds);
                });
    }

    private void addMedicationRow(QueryDocumentSnapshot doc) {
        View rowView = LayoutInflater.from(this).inflate(R.layout.schedule_row_item, todaysScheduleContainer, false);
        TextView scheduleTime = rowView.findViewById(R.id.scheduleTime);
        TextView scheduleTitle = rowView.findViewById(R.id.scheduleTitle);
        ImageView statusIcon = rowView.findViewById(R.id.scheduleStatusIcon);
        CheckBox deleteCheckbox = rowView.findViewById(R.id.deleteCheckbox);
        ImageView editIcon = rowView.findViewById(R.id.editMedIcon);
        
        String id = doc.getId();
        scheduleTime.setText(doc.getString("time"));
        scheduleTitle.setText(doc.getString("name"));
        statusIcon.setImageResource(Boolean.TRUE.equals(doc.getBoolean("taken")) ? R.drawable.ic_checked : R.drawable.ic_unchecked);
        
        if (isInDeleteMode) {
            statusIcon.setVisibility(View.GONE);
            deleteCheckbox.setVisibility(View.VISIBLE);
            editIcon.setVisibility(View.VISIBLE);
            deleteCheckbox.setChecked(selectedMedIds.contains(id));
            
            deleteCheckbox.setOnCheckedChangeListener((btn, isChecked) -> {
                if (isChecked) selectedMedIds.add(id);
                else selectedMedIds.remove(id);
            });
            
            editIcon.setOnClickListener(v -> showEditOptionsMenu(doc));
        } else {
            statusIcon.setVisibility(View.VISIBLE);
            deleteCheckbox.setVisibility(View.GONE);
            editIcon.setVisibility(View.GONE);
            statusIcon.setOnClickListener(v -> handleMedicationClick(doc));
        }
        
        rowView.setOnLongClickListener(v -> {
            showMiniPopup(doc);
            return true;
        });
        
        todaysScheduleContainer.addView(rowView);
    }

    private void showMiniPopup(QueryDocumentSnapshot doc) {
        new AlertDialog.Builder(this)
                .setItems(new String[]{getString(R.string.common_select), getString(R.string.common_cancel)}, (dialog, which) -> {
                    if (which == 0) {
                        isInDeleteMode = true;
                        selectedMedIds.clear();
                        if (actionButtonsContainer != null) {
                            actionButtonsContainer.setVisibility(View.VISIBLE);
                        }
                        loadTodaysSchedule(); 
                    }
                }).show();
    }

    private void showEditOptionsMenu(QueryDocumentSnapshot doc) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_edit_med, null);
        bottomSheetDialog.setContentView(view);

        view.findViewById(R.id.optionEditName).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            showEditMedNameDialog(doc);
        });

        view.findViewById(R.id.optionEditTime).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            showEditTimeDialog(doc);
        });

        bottomSheetDialog.show();
    }

    private void showEditMedNameDialog(QueryDocumentSnapshot doc) {
        String currentName = doc.getString("name");
        EditText input = new EditText(this);
        input.setText(currentName);
        input.setPadding(50, 20, 50, 20);

        new AlertDialog.Builder(this)
                .setTitle(R.string.common_edit)
                .setView(input)
                .setPositiveButton(R.string.common_save, (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        updateMedicationName(doc.getId(), newName);
                    }
                })
                .setNegativeButton(R.string.common_cancel, null)
                .show();
    }

    private void showEditTimeDialog(QueryDocumentSnapshot doc) {
        Calendar cal = Calendar.getInstance();
        Timestamp timestamp = doc.getTimestamp("timestamp");
        if (timestamp != null) cal.setTime(timestamp.toDate());

        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            updateMedicationTime(doc, hourOfDay, minute);
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show();
    }

    private void updateMedicationName(String id, String newName) {
        String userId = getUserId();
        if (userId == null) return;

        firestore.collection("users").document(userId).collection("medications").document(id)
                .update("name", newName)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, R.string.common_success, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, R.string.common_failed, Toast.LENGTH_SHORT).show());
    }

    private void updateMedicationTime(QueryDocumentSnapshot doc, int hour, int minute) {
        String userId = getUserId();
        if (userId == null) return;

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);

        String newTimeStr = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(cal.getTime());
        String medName = doc.getString("name");

        firestore.collection("users").document(userId).collection("medications").document(doc.getId())
                .update("time", newTimeStr, "timestamp", new Timestamp(cal.getTime()))
                .addOnSuccessListener(aVoid -> {
                    cancelAlarmForMed(medName); // Cancel old
                    scheduleNewAlarm(medName, hour, minute, doc.getString("mealTime")); // Schedule new
                    Toast.makeText(this, getString(R.string.common_success), Toast.LENGTH_SHORT).show();
                });
    }

    private void scheduleNewAlarm(String medName, int hour, int minute, String mealTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.before(Calendar.getInstance())) calendar.add(Calendar.DAY_OF_MONTH, 1);

        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.setAction("com.example.chroniccare.MEDICATION_ALARM");
        intent.putExtra("medicationName", medName);
        intent.putExtra("mealTime", mealTime);
        intent.putExtra("time", new SimpleDateFormat("h:mm a", Locale.getDefault()).format(calendar.getTime()));

        PendingIntent pi = PendingIntent.getBroadcast(this, medName.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pi);
        } else {
            am.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pi);
        }
    }

    private void confirmDeletion() {
        if (selectedMedIds.isEmpty()) {
            Toast.makeText(this, R.string.home_no_medications_selected, Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.common_delete)
                .setMessage(R.string.medications_delete_confirm_message)
                .setPositiveButton(R.string.common_delete, (dialog, which) -> executeDeletion())
                .setNegativeButton(R.string.common_cancel, null)
                .show();
    }

    private void executeDeletion() {
        String userId = getUserId();
        if (userId == null) return;

        for (String id : selectedMedIds) {
            QueryDocumentSnapshot doc = findDocById(id);
            if (doc != null) {
                cancelAlarmForMed(doc.getString("name"));
                firestore.collection("users").document(userId).collection("medications").document(id).delete();
            }
        }
        
        isInDeleteMode = false;
        if (actionButtonsContainer != null) {
            actionButtonsContainer.setVisibility(View.GONE);
        }
        selectedMedIds.clear();
        Toast.makeText(this, R.string.home_medications_deleted, Toast.LENGTH_SHORT).show();
    }

    private void cancelAlarmForMed(String medName) {
        if (medName == null) return;
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.setAction("com.example.chroniccare.MEDICATION_ALARM");
        
        for (int i = 0; i < 3; i++) {
            PendingIntent pi = PendingIntent.getBroadcast(this, medName.hashCode() + i, intent, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (pi != null) {
                am.cancel(pi);
                pi.cancel();
            }
            PendingIntent piNext = PendingIntent.getBroadcast(this, (medName + "_next").hashCode() + i, intent, PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (piNext != null) {
                am.cancel(piNext);
                piNext.cancel();
            }
        }
    }

    private String getUserId() {
        if (auth.getCurrentUser() != null) return auth.getCurrentUser().getUid();
        GoogleSignInAccount acc = GoogleSignIn.getLastSignedInAccount(this);
        return acc != null ? acc.getId() : null;
    }

    private QueryDocumentSnapshot findDocById(String id) {
        for (QueryDocumentSnapshot doc : currentMeds) if (doc.getId().equals(id)) return doc;
        return null;
    }

    private void handleMedicationClick(QueryDocumentSnapshot doc) {
        String userId = getUserId();
        if (userId == null) return;
        boolean taken = Boolean.TRUE.equals(doc.getBoolean("taken"));
        firestore.collection("users").document(userId).collection("medications").document(doc.getId())
                .update("taken", !taken, "takenAt", !taken ? Timestamp.now() : null);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected int getBottomNavMenuItemId() {
        return R.id.nav_home;
    }
}
