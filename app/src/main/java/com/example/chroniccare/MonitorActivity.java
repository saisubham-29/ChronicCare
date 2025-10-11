// MonitorActivity.java
package com.example.chroniccare;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MonitorActivity extends BottomNavActivity {

    private EditText etReadingType, etValueReading, etDateTime, etNotes;
    private Button btnSaveReading;
    private ImageView micIcon, dropdownArrow;
    private Calendar selectedDateTime;

    private LineChart chartGlucose;
    private BarChart comparision;

    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance();

        // Initialize views and functionality
        initializeViews();
        setupClickListeners();
        setupCharts();
    }

    private void initializeViews() {
        // Find input fields
        etReadingType = findViewById(R.id.etReadingType);
        EditText etGlucoseReading = findViewById(R.id.etGlucoseReading);
        etDateTime = findViewById(R.id.etDateTime);
        btnSaveReading = findViewById(R.id.btnSaveReading);
        micIcon = findViewById(R.id.mic_icon);

        // Find dropdown arrow (the ImageView next to reading type)
        LinearLayout readingTypeLayout = (LinearLayout) etReadingType.getParent();
        dropdownArrow = (ImageView) readingTypeLayout.getChildAt(1);

        // Find charts
        chartGlucose = findViewById(R.id.chartGlucose);
        comparision = findViewById(R.id.barchart);

        // Initialize calendar
        selectedDateTime = Calendar.getInstance();

        // Rename the glucose reading field to be generic value field
        etValueReading = etGlucoseReading;
        etValueReading.setHint("Enter Value");

        // Add notes field programmatically (or you can add it in XML)
        addNotesField();
    }

    private void addNotesField() {
        // Find the card view layout
        androidx.cardview.widget.CardView cardView = findViewById(R.id.livedata_card);
        LinearLayout cardLayout = (LinearLayout) cardView.getChildAt(0);

        // Create notes EditText
        etNotes = new EditText(this);
        etNotes.setHint("Add Notes (Optional)");
        etNotes.setTextSize(14f);
        etNotes.setTextColor(getResources().getColor(android.R.color.black));
        etNotes.setHintTextColor(getResources().getColor(android.R.color.darker_gray));

        // Create layout params
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 20, 0, 0);

        // Create container for notes
        LinearLayout notesContainer = new LinearLayout(this);
        notesContainer.setOrientation(LinearLayout.HORIZONTAL);
        notesContainer.setBackgroundResource(R.drawable.input_background);
        notesContainer.setPadding(12, 12, 12, 12);
        notesContainer.setLayoutParams(params);

        notesContainer.addView(etNotes, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        ));

        // Add notes field before the save button
        int saveButtonIndex = cardLayout.indexOfChild(findViewById(R.id.btnSaveReading));
        cardLayout.addView(notesContainer, saveButtonIndex);
    }

    private void setupClickListeners() {
        // Reading Type dropdown - click only on the arrow icon
        dropdownArrow.setOnClickListener(v -> showReadingTypeDialog());

        // Date & Time picker - click on the entire layout
        LinearLayout dateTimeLayout = (LinearLayout) etDateTime.getParent();
        dateTimeLayout.setOnClickListener(v -> showDateTimePicker());

        // Save Button
        btnSaveReading.setOnClickListener(v -> saveReadingToFirestore());

        // Mic Icon for voice input (placeholder)
        micIcon.setOnClickListener(v -> {
            Toast.makeText(this, "Voice input feature coming soon!", Toast.LENGTH_SHORT).show();
        });
    }

    private void showReadingTypeDialog() {
        final String[] readingTypes = {
                "Blood Glucose Level (BGL)",
                "Diastolic Blood Pressure",
                "Systolic Blood Pressure",
                "Heart Rate",
                "Body Temperature",
                "SPO2"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Reading Type")
                .setItems(readingTypes, (dialog, which) -> {
                    etReadingType.setText(readingTypes[which]);
                    updateValueFieldHint(readingTypes[which]);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateValueFieldHint(String readingType) {
        switch (readingType) {
            case "Blood Glucose Level (BGL)":
                etValueReading.setHint("Enter Glucose Reading (mg/dL)");
                etValueReading.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                break;
            case "Diastolic Blood Pressure":
                etValueReading.setHint("Enter Diastolic Pressure (mmHg)");
                etValueReading.setInputType(InputType.TYPE_CLASS_NUMBER);
                break;
            case "Systolic Blood Pressure":
                etValueReading.setHint("Enter Systolic Pressure (mmHg)");
                etValueReading.setInputType(InputType.TYPE_CLASS_NUMBER);
                break;
            case "Heart Rate":
                etValueReading.setHint("Enter Heart Rate (bpm)");
                etValueReading.setInputType(InputType.TYPE_CLASS_NUMBER);
                break;
            case "Body Temperature":
                etValueReading.setHint("Enter Body Temperature (°C)");
                etValueReading.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                break;
            case "SPO2":
                etValueReading.setHint("Enter SPO2 (%)");
                etValueReading.setInputType(InputType.TYPE_CLASS_NUMBER);
                break;
            default:
                etValueReading.setHint("Enter Value");
                etValueReading.setInputType(InputType.TYPE_CLASS_NUMBER);
                break;
        }
    }

    private void showDateTimePicker() {
        final Calendar currentDate = Calendar.getInstance();

        // Date Picker
        DatePickerDialog datePicker = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            selectedDateTime.set(Calendar.YEAR, year);
            selectedDateTime.set(Calendar.MONTH, month);
            selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            // Time Picker after date is selected
            TimePickerDialog timePicker = new TimePickerDialog(this, (view1, hourOfDay, minute) -> {
                selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                selectedDateTime.set(Calendar.MINUTE, minute);

                // Format and display the selected date & time
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault());
                etDateTime.setText(dateFormat.format(selectedDateTime.getTime()));

            }, currentDate.get(Calendar.HOUR_OF_DAY), currentDate.get(Calendar.MINUTE), false);

            timePicker.show();

        }, currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH), currentDate.get(Calendar.DAY_OF_MONTH));

        datePicker.show();
    }

    private void saveReadingToFirestore() {
        String readingType = etReadingType.getText().toString().trim();
        String valueReading = etValueReading.getText().toString().trim();
        String dateTime = etDateTime.getText().toString().trim();
        String notes = etNotes != null ? etNotes.getText().toString().trim() : "";

        // Validation
        if (readingType.isEmpty()) {
            Toast.makeText(this, "Please select reading type", Toast.LENGTH_SHORT).show();
            return;
        }

        if (valueReading.isEmpty()) {
            Toast.makeText(this, "Please enter " + getReadingUnit(readingType), Toast.LENGTH_SHORT).show();
            return;
        }

        if (dateTime.isEmpty()) {
            Toast.makeText(this, "Please select date and time", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Parse the value based on reading type
            double value = Double.parseDouble(valueReading);

            // Prepare data map according to your Firestore structure
            Map<String, Object> reading = new HashMap<>();

            // Map reading types to your Firestore field names
            switch (readingType) {
                case "Blood Glucose Level (BGL)":
                    reading.put("BloodSugar", value);
                    break;
                case "Diastolic Blood Pressure":
                    reading.put("DiaBP", value);
                    break;
                case "Systolic Blood Pressure":
                    reading.put("SysBP", value);
                    break;
                case "Heart Rate":
                    reading.put("HeartRate", value);
                    break;
                case "Body Temperature":
                    reading.put("BodyTemp", value);
                    break;
                case "SPO2":
                    reading.put("SpO2", value);
                    break;
            }

            // Add common fields
            reading.put("Notes", notes);
            reading.put("readingTime", Timestamp.now());
            reading.put("readingType", readingType); // Store the reading type for reference

            // Set default values for other fields (0.0)
            if (!reading.containsKey("BloodSugar")) reading.put("BloodSugar", 0.0);
            if (!reading.containsKey("BodyTemp")) reading.put("BodyTemp", 0.0);
            if (!reading.containsKey("DiaBP")) reading.put("DiaBP", 0.0);
            if (!reading.containsKey("SysBP")) reading.put("SysBP", 0.0);
            if (!reading.containsKey("HeartRate")) reading.put("HeartRate", 0.0);
            if (!reading.containsKey("SpO2")) reading.put("SpO2", 0.0);

            // Save to Firestore
            firestore.collection("readingInfo")
                    .add(reading)
                    .addOnSuccessListener(docRef -> {
                        String message = readingType + ": " + valueReading + " " + getReadingUnit(readingType) + " saved successfully!";
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                        clearFields();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
        }
    }

    private String getReadingUnit(String readingType) {
        switch (readingType) {
            case "Blood Glucose Level (BGL)":
                return "mg/dL";
            case "Diastolic Blood Pressure":
            case "Systolic Blood Pressure":
                return "mmHg";
            case "Heart Rate":
                return "bpm";
            case "Body Temperature":
                return "°C";
            case "SPO2":
                return "%";
            default:
                return "value";
        }
    }

    private void clearFields() {
        etReadingType.setText("");
        etValueReading.setText("");
        etDateTime.setText("");
        if (etNotes != null) {
            etNotes.setText("");
        }
    }

    private void setupCharts() {
        // ================= Line Chart (Blood Glucose Trend) =================
        ArrayList<Entry> lineEntries = new ArrayList<>();
        lineEntries.add(new Entry(0, 106));
        lineEntries.add(new Entry(1, 118));
        lineEntries.add(new Entry(2, 99));
        lineEntries.add(new Entry(3, 141));
        lineEntries.add(new Entry(4, 105));
        lineEntries.add(new Entry(5, 115));
        lineEntries.add(new Entry(6, 112));

        LineDataSet lineDataSet = new LineDataSet(lineEntries, "Blood Glucose");
        lineDataSet.setColor(Color.parseColor("#26A69A"));
        lineDataSet.setLineWidth(2f);
        lineDataSet.setCircleColor(Color.parseColor("#26A69A"));
        lineDataSet.setCircleRadius(4f);
        lineDataSet.setDrawCircleHole(false);
        lineDataSet.setDrawValues(false);
        lineDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        chartGlucose.setData(new LineData(lineDataSet));
        chartGlucose.getDescription().setEnabled(false);
        chartGlucose.getLegend().setEnabled(false);

        // Y-Axis (Line chart)
        YAxis yAxisLeft = chartGlucose.getAxisLeft();
        yAxisLeft.setAxisMinimum(0f);
        yAxisLeft.setAxisMaximum(160f);
        chartGlucose.getAxisRight().setEnabled(false);
        yAxisLeft.enableGridDashedLine(10f, 5f, 0f);
        yAxisLeft.setAxisLineColor(Color.BLACK);

        // X-Axis (Line chart)
        XAxis xAxis = chartGlucose.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(
                new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"}));
        xAxis.setDrawLabels(false);
        xAxis.setAxisLineColor(Color.BLACK);

        chartGlucose.invalidate();

        // ================= Bar Chart (Before vs After Meal) - COMPLETE VERSION =================
        ArrayList<BarEntry> beforeMeal = new ArrayList<>();
        ArrayList<BarEntry> afterMeal = new ArrayList<>();

        // Complete data for all 7 days
        beforeMeal.add(new BarEntry(0, 110f)); // Mon
        afterMeal.add(new BarEntry(0, 118f));

        beforeMeal.add(new BarEntry(1, 120f)); // Tue
        afterMeal.add(new BarEntry(1, 118f));

        beforeMeal.add(new BarEntry(2, 135f)); // Wed
        afterMeal.add(new BarEntry(2, 140f));

        beforeMeal.add(new BarEntry(3, 130f)); // Thu
        afterMeal.add(new BarEntry(3, 140f));

        beforeMeal.add(new BarEntry(4, 115f)); // Fri
        afterMeal.add(new BarEntry(4, 107f));

        beforeMeal.add(new BarEntry(5, 125f)); // Sat
        afterMeal.add(new BarEntry(5, 130f));

        beforeMeal.add(new BarEntry(6, 108f)); // Sun
        afterMeal.add(new BarEntry(6, 120f));

        BarDataSet setBefore = new BarDataSet(beforeMeal, "Before Meal");
        setBefore.setColor(ContextCompat.getColor(this, R.color.teal_700));

        BarDataSet setAfter = new BarDataSet(afterMeal, "After Meal");
        setAfter.setColor(ContextCompat.getColor(this, R.color.teal_200));

        BarData barData = new BarData(setBefore, setAfter);
        barData.setBarWidth(0.4f);

        comparision.setData(barData);
        comparision.groupBars(0f, 0.4f, 0.06f); // Adjust group spacing

        comparision.getDescription().setEnabled(false);
        comparision.setDrawGridBackground(false);
        comparision.setDrawBarShadow(false);
        comparision.getLegend().setEnabled(true); // Enable legend to show Before/After colors

        // X-Axis - Show all day labels
        XAxis barXAxis = comparision.getXAxis();
        barXAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        barXAxis.setGranularity(1f);
        barXAxis.setValueFormatter(new IndexAxisValueFormatter(
                new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"}));
        barXAxis.setDrawGridLines(false);
        barXAxis.setLabelCount(7); // Ensure all 7 labels are shown
        barXAxis.setCenterAxisLabels(true);

        // Y-Axis
        YAxis barLeft = comparision.getAxisLeft();
        barLeft.setAxisMinimum(0f);
        barLeft.setAxisMaximum(160f);
        barLeft.setGranularity(20f); // Show grid lines at intervals of 20
        barLeft.setDrawGridLines(true);
        barLeft.setGridColor(Color.parseColor("#E0E0E0"));
        comparision.getAxisRight().setEnabled(false);

        // Custom labels above bars showing the difference
        final int[] diffs = {+8, -2, +5, +10, -8, +5, +12};

        barData.setValueFormatter(new ValueFormatter() {
            @Override
            public String getBarLabel(BarEntry barEntry) {
                int xIndex = (int) barEntry.getX();
                // Show difference label only on AFTER meal bars
                if (barEntry.getData() != null && barEntry.getData().equals("after")) {
                    int diff = diffs[xIndex];
                    return (diff >= 0 ? "+" : "") + diff;
                }
                return "";
            }
        });

        // Mark after meal entries for label display
        for (int i = 0; i < afterMeal.size(); i++) {
            afterMeal.get(i).setData("after");
        }

        barData.setValueTextSize(12f);
        barData.setValueTextColor(Color.BLACK);
        barData.setValueTypeface(Typeface.DEFAULT_BOLD);

        // Additional chart styling
        comparision.setFitBars(true);
        comparision.setDrawValueAboveBar(true);
        comparision.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        comparision.getLegend().setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        comparision.getLegend().setOrientation(Legend.LegendOrientation.HORIZONTAL);
        comparision.getLegend().setDrawInside(false);
        comparision.getLegend().setYOffset(10f);

        comparision.invalidate();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_monitor;
    }

    protected int getBottomNavMenuItemId() {
        return R.id.nav_monitor;
    }
}