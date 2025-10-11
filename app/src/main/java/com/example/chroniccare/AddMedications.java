package com.example.chroniccare;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddMedications extends AppCompatActivity {

    private EditText medNameEditText, doseEditText;
    private RadioGroup mealRadioGroup;
    private RadioButton beforeFoodRadio, afterFoodRadio;
    private Switch medTakenSwitch;
    private Button btnAddMedication;

    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_medications);

        firestore = FirebaseFirestore.getInstance();

        medNameEditText = findViewById(R.id.medNameEditText);
        doseEditText = findViewById(R.id.doseEditText);
        mealRadioGroup = findViewById(R.id.mealRadioGroup);
        beforeFoodRadio = findViewById(R.id.beforeFoodRadio);
        afterFoodRadio = findViewById(R.id.afterFoodRadio);
        medTakenSwitch = findViewById(R.id.medTakenSwitch);
        btnAddMedication = findViewById(R.id.btnAddMedication);

        btnAddMedication.setOnClickListener(v -> addMedicationToFirestore());
    }

    private void addMedicationToFirestore() {
        String medName = medNameEditText.getText().toString().trim();
        String dose = doseEditText.getText().toString().trim();
        boolean medTaken = medTakenSwitch.isChecked();

        if (medName.isEmpty() || dose.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Determine Meal boolean
        boolean meal = afterFoodRadio.isChecked(); // true if After Food, false if Before Food

        Map<String, Object> medication = new HashMap<>();
        medication.put("MedName", medName);
        medication.put("dose", dose);
        medication.put("Meal", meal);
        medication.put("MedTaken", medTaken);
        medication.put("MedTime", Timestamp.now());

        firestore.collection("Medications")
                .add(medication)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "Medication added successfully!", Toast.LENGTH_SHORT).show();
                    clearFields();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void clearFields() {
        medNameEditText.setText("");
        doseEditText.setText("");
        mealRadioGroup.clearCheck();
        medTakenSwitch.setChecked(false);
    }
}
