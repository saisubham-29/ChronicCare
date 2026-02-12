package com.example.chroniccare;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddMedications extends AppCompatActivity {

    private EditText medNameEditText, doseEditText, timeEditText;
    private RadioGroup mealRadioGroup;
    private RadioButton beforeFoodRadio, afterFoodRadio;
    private Button btnAddMedication;

    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private Calendar selectedDateTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_medications);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        selectedDateTime = Calendar.getInstance();

        medNameEditText = findViewById(R.id.medNameEditText);
        doseEditText = findViewById(R.id.doseEditText);
        timeEditText = findViewById(R.id.timeEditText);
        mealRadioGroup = findViewById(R.id.mealRadioGroup);
        beforeFoodRadio = findViewById(R.id.beforeFoodRadio);
        afterFoodRadio = findViewById(R.id.afterFoodRadio);
        btnAddMedication = findViewById(R.id.btnAddMedication);

        timeEditText.setOnClickListener(v -> showDateTimePicker());
        btnAddMedication.setOnClickListener(v -> addMedicationToFirestore());
    }

    private void addMedicationToFirestore() {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (userId == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        String medName = medNameEditText.getText().toString().trim();
        String dose = doseEditText.getText().toString().trim();
        String timeText = timeEditText.getText().toString().trim();

        // Hardened validation
        if (medName.isEmpty()) {
            Toast.makeText(this, "Please enter medication name", Toast.LENGTH_SHORT).show();
            medNameEditText.requestFocus();
            return;
        }
        if (dose.isEmpty()) {
            Toast.makeText(this, "Please enter dose", Toast.LENGTH_SHORT).show();
            doseEditText.requestFocus();
            return;
        }
        if (timeText.isEmpty()) {
            Toast.makeText(this, "Please select time", Toast.LENGTH_SHORT).show();
            timeEditText.performClick();
            return;
        }

        String name = medName + " " + dose;

        // Guarantee Firestore consistency - never write takenAt from Add Medication
        Map<String, Object> medication = new HashMap<>();
        medication.put("name", name);
        medication.put("time", timeText);
        medication.put("timestamp", new Timestamp(selectedDateTime.getTime()));
        medication.put("taken", false);  // Always false
        medication.put("takenAt", null); // Always null

        firestore.collection("users").document(userId).collection("medications")
                .add(medication)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "Medication scheduled successfully!", Toast.LENGTH_SHORT).show();
                    finish(); // Return to HomeActivity
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to save medication", Toast.LENGTH_SHORT).show()
                );
    }

    private void showDateTimePicker() {
        DatePickerDialog datePicker = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            selectedDateTime.set(Calendar.YEAR, year);
            selectedDateTime.set(Calendar.MONTH, month);
            selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            TimePickerDialog timePicker = new TimePickerDialog(this, (view1, hourOfDay, minute) -> {
                selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                selectedDateTime.set(Calendar.MINUTE, minute);

                SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
                timeEditText.setText(timeFormat.format(selectedDateTime.getTime()));

            }, selectedDateTime.get(Calendar.HOUR_OF_DAY), selectedDateTime.get(Calendar.MINUTE), false);

            timePicker.show();

        }, selectedDateTime.get(Calendar.YEAR), selectedDateTime.get(Calendar.MONTH), selectedDateTime.get(Calendar.DAY_OF_MONTH));

        datePicker.show();
    }

    private void clearFields() {
        medNameEditText.setText("");
        doseEditText.setText("");
        timeEditText.setText("");
        mealRadioGroup.clearCheck();
        selectedDateTime = Calendar.getInstance();
    }
}
