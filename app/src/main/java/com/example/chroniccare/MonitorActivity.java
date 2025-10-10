// MonitorActivity.java
package com.example.chroniccare;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MonitorActivity extends BottomNavActivity {
    private EditText etReadingType, etGlucoseReading, etDateTime;
    private Button btnSaveReading;
    private Calendar selectedDateTime;
    private String[] readingTypes = {"Blood Glucose", "Blood Pressure", "Heart Rate", "Weight", "Temperature", "Oxygen Saturation"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeViews();
        setupReadingTypeDropdown();
        setupDateTimePicker();
        setupClickListeners();
    }

    private void initializeViews() {
        etReadingType = findViewById(R.id.etReadingType);
        etGlucoseReading = findViewById(R.id.etGlucoseReading);
        etDateTime = findViewById(R.id.etDateTime);
        btnSaveReading = findViewById(R.id.btnSaveReading);

        selectedDateTime = Calendar.getInstance();
    }

    private void setupReadingTypeDropdown() {
        // Set up the reading type dropdown
        etReadingType.setInputType(InputType.TYPE_NULL); // Disable keyboard input
        etReadingType.setOnClickListener(v -> showReadingTypeDialog());

        // Enable dropdown when clicked
        etReadingType.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                showReadingTypeDialog();
            }
        });
    }

    private void showReadingTypeDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Select Reading Type")
                .setItems(readingTypes, (dialog, which) -> {
                    String selectedType = readingTypes[which];
                    etReadingType.setText(selectedType);
                    updateReadingField(selectedType);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateReadingField(String selectedType) {
        // Update the input field based on selected reading type
        switch (selectedType) {
            case "Blood Glucose":
                etGlucoseReading.setHint("Enter Glucose Reading (mg/dL)");
                etGlucoseReading.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                break;
            case "Blood Pressure":
                etGlucoseReading.setHint("Enter Blood Pressure (e.g., 120/80)");
                etGlucoseReading.setInputType(InputType.TYPE_CLASS_TEXT);
                break;
            case "Heart Rate":
                etGlucoseReading.setHint("Enter Heart Rate (bpm)");
                etGlucoseReading.setInputType(InputType.TYPE_CLASS_NUMBER);
                break;
            case "Weight":
                etGlucoseReading.setHint("Enter Weight (kg)");
                etGlucoseReading.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                break;
            case "Temperature":
                etGlucoseReading.setHint("Enter Temperature (°C)");
                etGlucoseReading.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                break;
            case "Oxygen Saturation":
                etGlucoseReading.setHint("Enter Oxygen Saturation (%)");
                etGlucoseReading.setInputType(InputType.TYPE_CLASS_NUMBER);
                break;
        }
        // Clear any previous input when type changes
        etGlucoseReading.setText("");
    }

    private void setupDateTimePicker() {
        etDateTime.setInputType(InputType.TYPE_NULL);
        etDateTime.setOnClickListener(v -> showDateTimePicker());
        etDateTime.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                showDateTimePicker();
            }
        });

        // Set current date and time as default
        updateDateTimeField();
    }

    private void showDateTimePicker() {
        final Calendar currentDate = Calendar.getInstance();

        // Date Picker
        DatePickerDialog datePicker = new DatePickerDialog(this,
                (view, year, monthOfYear, dayOfMonth) -> {
                    selectedDateTime.set(Calendar.YEAR, year);
                    selectedDateTime.set(Calendar.MONTH, monthOfYear);
                    selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    // Time Picker
                    TimePickerDialog timePicker = new TimePickerDialog(this,
                            (view1, hourOfDay, minute) -> {
                                selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                selectedDateTime.set(Calendar.MINUTE, minute);
                                updateDateTimeField();
                            },
                            selectedDateTime.get(Calendar.HOUR_OF_DAY),
                            selectedDateTime.get(Calendar.MINUTE),
                            false
                    );
                    timePicker.show();
                },
                selectedDateTime.get(Calendar.YEAR),
                selectedDateTime.get(Calendar.MONTH),
                selectedDateTime.get(Calendar.DAY_OF_MONTH)
        );
        datePicker.show();
    }

    private void updateDateTimeField() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        etDateTime.setText(dateFormat.format(selectedDateTime.getTime()));
    }

    private void setupClickListeners() {
        btnSaveReading.setOnClickListener(v -> saveReading());

        // Microphone icon for voice input (placeholder functionality)
        View parentLayout = (View) findViewById(R.id.etGlucoseReading).getParent();
        if (parentLayout != null) {
            ImageView micIcon = parentLayout.findViewById(R.id.mic_icon);
            if (micIcon != null) {
                micIcon.setOnClickListener(v -> {
                    Toast.makeText(this, "Voice input feature would be implemented here", Toast.LENGTH_SHORT).show();
                    // Simulate voice input with sample value
                    simulateVoiceInput();
                });
            }
        }
    }

    private void simulateVoiceInput() {
        String currentType = etReadingType.getText().toString();
        String sampleValue = "";

        switch (currentType) {
            case "Blood Glucose":
                sampleValue = "125";
                break;
            case "Blood Pressure":
                sampleValue = "120/80";
                break;
            case "Heart Rate":
                sampleValue = "72";
                break;
            case "Weight":
                sampleValue = "68.5";
                break;
            case "Temperature":
                sampleValue = "36.6";
                break;
            case "Oxygen Saturation":
                sampleValue = "98";
                break;
            default:
                sampleValue = "100";
        }

        etGlucoseReading.setText(sampleValue);
        Toast.makeText(this, "Voice input simulated: " + sampleValue, Toast.LENGTH_SHORT).show();
    }

    private void saveReading() {
        String readingType = etReadingType.getText().toString().trim();
        String readingValue = etGlucoseReading.getText().toString().trim();
        String dateTime = etDateTime.getText().toString().trim();

        // Validation
        if (readingType.isEmpty()) {
            showError("Please select a reading type");
            return;
        }

        if (readingValue.isEmpty()) {
            showError("Please enter a reading value");
            return;
        }

        if (dateTime.isEmpty()) {
            showError("Please select date and time");
            return;
        }

        // Validate reading value based on type
        if (!isValidReading(readingType, readingValue)) {
            showError("Please enter a valid " + readingType + " reading");
            return;
        }

        // Save the reading
        saveReadingToDatabase(readingType, readingValue, dateTime);
    }

    private boolean isValidReading(String type, String value) {
        try {
            switch (type) {
                case "Blood Glucose":
                    double glucose = Double.parseDouble(value);
                    return glucose >= 20 && glucose <= 600; // mg/dL range

                case "Blood Pressure":
                    if (value.contains("/")) {
                        String[] parts = value.split("/");
                        if (parts.length == 2) {
                            int systolic = Integer.parseInt(parts[0].trim());
                            int diastolic = Integer.parseInt(parts[1].trim());
                            return systolic >= 50 && systolic <= 250 && diastolic >= 30 && diastolic <= 150;
                        }
                    }
                    return false;

                case "Heart Rate":
                    int heartRate = Integer.parseInt(value);
                    return heartRate >= 30 && heartRate <= 250;

                case "Weight":
                    double weight = Double.parseDouble(value);
                    return weight >= 10 && weight <= 300;

                case "Temperature":
                    double temp = Double.parseDouble(value);
                    return temp >= 25 && temp <= 45;

                case "Oxygen Saturation":
                    int oxygen = Integer.parseInt(value);
                    return oxygen >= 70 && oxygen <= 100;

                default:
                    return true;
            }
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private void saveReadingToDatabase(String type, String value, String dateTime) {
        // Here you would save to your database (Room, SQLite, etc.)
        // For now, we'll just show a success message

        String message = "Reading saved successfully!\n" +
                "Type: " + type + "\n" +
                "Value: " + value + "\n" +
                "Time: " + dateTime;

        Toast.makeText(this, message, Toast.LENGTH_LONG).show();

        // Clear form but keep reading type for quick entry
        clearForm();

        // Update dashboard with analysis
        updateDashboard(type, value);
    }

    private void clearForm() {
        // Clear only the reading value, keep type and date for quick entry
        etGlucoseReading.setText("");

        // Reset input type to default
        etGlucoseReading.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        etGlucoseReading.setHint("Enter Reading Value");
    }

    private void updateDashboard(String type, String value) {
        // Update any dashboard or summary view with the new reading
        switch (type) {
            case "Blood Glucose":
                analyzeGlucoseLevel(Double.parseDouble(value));
                break;
            case "Blood Pressure":
                analyzeBloodPressure(value);
                break;
            case "Heart Rate":
                analyzeHeartRate(Integer.parseInt(value));
                break;
            case "Weight":
                analyzeWeight(Double.parseDouble(value));
                break;
            case "Temperature":
                analyzeTemperature(Double.parseDouble(value));
                break;
            case "Oxygen Saturation":
                analyzeOxygenSaturation(Integer.parseInt(value));
                break;
        }
    }

    private void analyzeGlucoseLevel(double glucose) {
        String analysis;
        if (glucose < 70) {
            analysis = "⚠️ Low glucose level. Consider having a snack.";
        } else if (glucose > 180) {
            analysis = "⚠️ High glucose level. Monitor carefully.";
        } else {
            analysis = "✓ Glucose level is within normal range.";
        }
        showAnalysisToast(analysis);
    }

    private void analyzeBloodPressure(String bp) {
        try {
            String[] parts = bp.split("/");
            if (parts.length == 2) {
                int systolic = Integer.parseInt(parts[0].trim());
                int diastolic = Integer.parseInt(parts[1].trim());

                String analysis;
                if (systolic < 90 || diastolic < 60) {
                    analysis = "⚠️ Low blood pressure. Stay hydrated.";
                } else if (systolic > 140 || diastolic > 90) {
                    analysis = "⚠️ High blood pressure. Monitor and consult doctor.";
                } else {
                    analysis = "✓ Blood pressure is within normal range.";
                }
                showAnalysisToast(analysis);
            }
        } catch (NumberFormatException e) {
            showAnalysisToast("Blood pressure reading recorded");
        }
    }

    private void analyzeHeartRate(int heartRate) {
        String analysis;
        if (heartRate < 60) {
            analysis = "⚠️ Low heart rate (bradycardia). Consult doctor if symptomatic.";
        } else if (heartRate > 100) {
            analysis = "⚠️ High heart rate (tachycardia). Rest and monitor.";
        } else {
            analysis = "✓ Heart rate is within normal range.";
        }
        showAnalysisToast(analysis);
    }

    private void analyzeWeight(double weight) {
        showAnalysisToast("Weight recorded: " + weight + " kg");
    }

    private void analyzeTemperature(double temp) {
        String analysis;
        if (temp < 36.0) {
            analysis = "⚠️ Low body temperature. Keep warm.";
        } else if (temp > 37.5) {
            analysis = "⚠️ Fever detected. Rest and monitor.";
        } else {
            analysis = "✓ Temperature is normal.";
        }
        showAnalysisToast(analysis);
    }

    private void analyzeOxygenSaturation(int oxygen) {
        String analysis;
        if (oxygen < 95) {
            analysis = "⚠️ Low oxygen saturation. Breathe deeply and monitor.";
        } else if (oxygen < 90) {
            analysis = "🚨 Very low oxygen. Seek medical attention.";
        } else {
            analysis = "✓ Oxygen saturation is normal.";
        }
        showAnalysisToast(analysis);
    }

    private void showAnalysisToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_monitor;
    }

    @Override
    protected int getBottomNavMenuItemId() {
        return R.id.nav_monitor;
    }
}