package com.example.chroniccare;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chroniccare.database.AppDatabase;
import com.example.chroniccare.database.ExerciseLog;
import com.example.chroniccare.utils.FirebaseSync;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

public class LogExercise extends AppCompatActivity {

    private AppDatabase db;
    private FirebaseSync firebaseSync;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_exercise);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        db = AppDatabase.getInstance(this);
        SharedPreferences prefs = getSharedPreferences("ChronicCarePrefs", MODE_PRIVATE);
        userId = prefs.getString("userId", "");
        firebaseSync = new FirebaseSync(userId);

        setupCategoryClick(R.id.cardCycling, "Cycling");
        setupCategoryClick(R.id.cardCardio, "Cardio");
        setupCategoryClick(R.id.cardWalk, "Walk");
        setupCategoryClick(R.id.cardFlexibility, "Flexibility");
        setupCategoryClick(R.id.cardCore, "Core");
        setupCategoryClick(R.id.cardSwim, "Swim");

        Button btnLogManual = findViewById(R.id.btnLogManual);
        btnLogManual.setOnClickListener(v -> showExerciseDialog(null));
    }

    private void setupCategoryClick(int viewId, String name) {
        View view = findViewById(viewId);
        if (view == null) {
            return;
        }
        view.setOnClickListener(v -> showExerciseDialog(name));
    }

    private void showExerciseDialog(String presetName) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_log_exercise, null);
        TextInputEditText etName = dialogView.findViewById(R.id.etExerciseName);
        TextInputEditText etDuration = dialogView.findViewById(R.id.etDuration);
        TextInputEditText etCalories = dialogView.findViewById(R.id.etCaloriesBurned);

        if (presetName != null && !presetName.isEmpty()) {
            etName.setText(presetName);
        }

        new AlertDialog.Builder(this)
                .setTitle(presetName != null ? "Log " + presetName : "Log Exercise")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = getTrimmedText(etName);
                    if (name.isEmpty()) {
                        Toast.makeText(this, "Please enter an exercise name", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int duration = parseIntSafe(getTrimmedText(etDuration));
                    int calories = parseIntSafe(getTrimmedText(etCalories));
                    saveExercise(name, duration, calories);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveExercise(String name, int durationMinutes, int caloriesBurned) {
        Calendar now = Calendar.getInstance();

        ExerciseLog log = new ExerciseLog();
        log.setExerciseName(name);
        log.setDurationMinutes(durationMinutes);
        log.setCaloriesBurned(caloriesBurned);
        log.setDate(formatStorageDate(now));

        Executors.newSingleThreadExecutor().execute(() -> {
            db.exerciseDao().insertExercise(log);
            syncExerciseToCloud(name, durationMinutes, caloriesBurned, now);

            runOnUiThread(() ->
                    Toast.makeText(this, "Exercise logged", Toast.LENGTH_SHORT).show()
            );
        });
    }

    private void syncExerciseToCloud(String name, int durationMinutes, int caloriesBurned, Calendar dateTime) {
        if (userId == null || userId.isEmpty()) {
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("exerciseName", name);
        data.put("durationMinutes", durationMinutes);
        data.put("caloriesBurned", caloriesBurned);
        data.put("date", formatStorageDate(dateTime));
        data.put("timestamp", dateTime.getTimeInMillis());
        data.put("userId", userId);

        firebaseSync.syncExercise(String.valueOf(dateTime.getTimeInMillis()), data);
    }

    private String formatStorageDate(Calendar dateTime) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(dateTime.getTime());
    }

    private String getTrimmedText(TextInputEditText editText) {
        if (editText.getText() == null) {
            return "";
        }
        return editText.getText().toString().trim();
    }

    private int parseIntSafe(String value) {
        if (value == null || value.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
