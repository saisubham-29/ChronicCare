package com.example.chroniccare;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.chroniccare.utils.PrescriptionScanner;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class AddMedications extends AppCompatActivity {

    private EditText medNameEditText, doseEditText;
    private CheckBox checkMorning, checkAfternoon, checkNight;
    private TextView tvMorningTime, tvAfternoonTime, tvNightTime;
    private RadioButton rbMorningBF, rbMorningAF, rbAfternoonBF, rbAfternoonAF, rbNightBF, rbNightAF;
    private Button btnAddMedication;
    private View cardScanPrescription;

    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private AlarmManager alarmManager;
    private PrescriptionScanner prescriptionScanner;
    private List<PrescriptionScanner.MedicationInfo> scannedMedications;
    private int currentMedicationIndex = 0;

    private int morningHour = 8, morningMinute = 0;
    private int afternoonHour = 14, afternoonMinute = 0;
    private int nightHour = 21, nightMinute = 0;

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_medications);

        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        prescriptionScanner = new PrescriptionScanner();

        initActivityLaunchers();
        checkAndRequestPermissions();
        initViews();
        setupListeners();
    }

    private void initActivityLaunchers() {
        cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bitmap bitmap = (Bitmap) result.getData().getExtras().get("data");
                    if (bitmap != null) {
                        processPrescriptionImage(bitmap);
                    }
                }
            }
        );

        galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            if (bitmap != null) {
                                processPrescriptionImage(bitmap);
                            }
                        } catch (Exception e) {
                            Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                            Log.e("AddMedications", "Error loading image", e);
                        }
                    }
                }
            }
        );
    }
    
    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                if (!notificationManager.canUseFullScreenIntent()) {
                    startActivity(new Intent(this, PermissionsActivity.class));
                }
            } else {
                Toast.makeText(this, "Enable 'Display over other apps' in Settings for alarms to work when locked", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initViews() {
        medNameEditText = findViewById(R.id.medNameEditText);
        doseEditText = findViewById(R.id.doseEditText);
        
        checkMorning = findViewById(R.id.checkMorning);
        checkAfternoon = findViewById(R.id.checkAfternoon);
        checkNight = findViewById(R.id.checkNight);
        
        tvMorningTime = findViewById(R.id.tvMorningTime);
        tvAfternoonTime = findViewById(R.id.tvAfternoonTime);
        tvNightTime = findViewById(R.id.tvNightTime);
        
        rbMorningBF = findViewById(R.id.rbMorningBF);
        rbMorningAF = findViewById(R.id.rbMorningAF);
        rbAfternoonBF = findViewById(R.id.rbAfternoonBF);
        rbAfternoonAF = findViewById(R.id.rbAfternoonAF);
        rbNightBF = findViewById(R.id.rbNightBF);
        rbNightAF = findViewById(R.id.rbNightAF);
        
        btnAddMedication = findViewById(R.id.btnAddMedication);
        cardScanPrescription = findViewById(R.id.cardScanPrescription);
    }

    private void setupListeners() {
        cardScanPrescription.setOnClickListener(v -> showImageSourceDialog());
        tvMorningTime.setOnClickListener(v -> showTimePicker("Morning", morningHour, morningMinute, (h, m) -> {
            morningHour = h;
            morningMinute = m;
            tvMorningTime.setText(formatTime(h, m));
        }));

        tvAfternoonTime.setOnClickListener(v -> showTimePicker("Afternoon", afternoonHour, afternoonMinute, (h, m) -> {
            afternoonHour = h;
            afternoonMinute = m;
            tvAfternoonTime.setText(formatTime(h, m));
        }));

        tvNightTime.setOnClickListener(v -> showTimePicker("Night", nightHour, nightMinute, (h, m) -> {
            nightHour = h;
            nightMinute = m;
            tvNightTime.setText(formatTime(h, m));
        }));

        btnAddMedication.setOnClickListener(v -> saveMedications());
    }

    private void showTimePicker(String period, int hour, int minute, TimeSetListener listener) {
        new TimePickerDialog(this, (view, h, m) -> listener.onTimeSet(h, m), hour, minute, false).show();
    }

    private String formatTime(int hour, int minute) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        return new SimpleDateFormat("h:mm a", Locale.getDefault()).format(cal.getTime());
    }

    private void saveMedications() {
        String userId = getUserId();
        if (userId == null) {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show();
            return;
        }

        String medName = medNameEditText.getText().toString().trim();
        String dose = doseEditText.getText().toString().trim();

        if (medName.isEmpty() || dose.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!checkMorning.isChecked() && !checkAfternoon.isChecked() && !checkNight.isChecked()) {
            Toast.makeText(this, "Select at least one time", Toast.LENGTH_SHORT).show();
            return;
        }

        String fullName = medName + " " + dose;
        int count = 0;

        if (checkMorning.isChecked()) {
            String mealTime = rbMorningBF.isChecked() ? "B/F" : "A/F";
            saveMedicationToFirestore(userId, fullName, morningHour, morningMinute, mealTime, "Morning");
            scheduleAlarm(fullName, morningHour, morningMinute, mealTime, count++);
        }

        if (checkAfternoon.isChecked()) {
            String mealTime = rbAfternoonBF.isChecked() ? "B/F" : "A/F";
            saveMedicationToFirestore(userId, fullName, afternoonHour, afternoonMinute, mealTime, "Afternoon");
            scheduleAlarm(fullName, afternoonHour, afternoonMinute, mealTime, count++);
        }

        if (checkNight.isChecked()) {
            String mealTime = rbNightBF.isChecked() ? "B/F" : "A/F";
            saveMedicationToFirestore(userId, fullName, nightHour, nightMinute, mealTime, "Night");
            scheduleAlarm(fullName, nightHour, nightMinute, mealTime, count++);
        }

        Toast.makeText(this, "Medication reminders set!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private String getUserId() {
        if (auth.getCurrentUser() != null) return auth.getCurrentUser().getUid();
        com.google.android.gms.auth.api.signin.GoogleSignInAccount account = 
            com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(this);
        return account != null ? account.getId() : null;
    }

    private void saveMedicationToFirestore(String userId, String name, int hour, int minute, String mealTime, String period) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);

        Map<String, Object> medication = new HashMap<>();
        medication.put("name", name);
        medication.put("time", formatTime(hour, minute));
        medication.put("timestamp", new Timestamp(cal.getTime()));
        medication.put("taken", false);
        medication.put("takenAt", null);
        medication.put("mealTime", mealTime);
        medication.put("period", period);
        
        // NEW: Timezone tracking fields
        medication.put("originTimezone", TimeZone.getDefault().getID());
        medication.put("strictInterval", true); // Default to strict 24h gap for safety

        firestore.collection("users").document(userId).collection("medications")
                .add(medication)
                .addOnSuccessListener(docRef -> initializeDailyTotal(userId));
    }
    
    private void initializeDailyTotal(String userId) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dateKey = sdf.format(new Date());
        
        firestore.collection("users").document(userId)
                .collection("medicationAdherence").document(dateKey)
                .get()
                .addOnSuccessListener(doc -> {
                    int total = doc.exists() && doc.getLong("total") != null ? doc.getLong("total").intValue() : 0;
                    
                    Map<String, Object> data = new HashMap<>();
                    data.put("total", total + 1);
                    data.put("taken", doc.exists() && doc.getLong("taken") != null ? doc.getLong("taken").intValue() : 0);
                    data.put("date", new Timestamp(new Date()));
                    
                    firestore.collection("users").document(userId)
                            .collection("medicationAdherence").document(dateKey)
                            .set(data);
                });
    }

    private void scheduleAlarm(String medName, int hour, int minute, String mealTime, int requestCode) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (calendar.before(Calendar.getInstance())) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.setAction("com.example.chroniccare.MEDICATION_ALARM");
        intent.putExtra("medicationName", medName);
        intent.putExtra("mealTime", mealTime);
        intent.putExtra("time", formatTime(hour, minute));

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                medName.hashCode() + requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
        
        scheduleNextDayAlarm(medName, hour, minute, mealTime, requestCode, calendar);
    }
    
    private void scheduleNextDayAlarm(String medName, int hour, int minute, String mealTime, int requestCode, Calendar currentAlarm) {
        Calendar nextDay = (Calendar) currentAlarm.clone();
        nextDay.add(Calendar.DAY_OF_MONTH, 1);
        
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.setAction("com.example.chroniccare.MEDICATION_ALARM");
        intent.putExtra("medicationName", medName);
        intent.putExtra("mealTime", mealTime);
        intent.putExtra("time", formatTime(hour, minute));
        intent.putExtra("reschedule", true);
        intent.putExtra("hour", hour);
        intent.putExtra("minute", minute);
        intent.putExtra("requestCode", requestCode);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                (medName + "_next").hashCode() + requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextDay.getTimeInMillis(), pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextDay.getTimeInMillis(), pendingIntent);
        }
    }

    interface TimeSetListener {
        void onTimeSet(int hour, int minute);
    }

    private void showImageSourceDialog() {
        String[] options = {"Take Photo", "Choose from Gallery"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Scan Prescription");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                cameraLauncher.launch(cameraIntent);
            } else {
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                galleryLauncher.launch(galleryIntent);
            }
        });
        builder.show();
    }

    private void processPrescriptionImage(Bitmap bitmap) {
        Toast.makeText(this, "Processing prescription...", Toast.LENGTH_SHORT).show();
        
        prescriptionScanner.scanPrescription(bitmap, new PrescriptionScanner.ScanCallback() {
            @Override
            public void onSuccess(List<PrescriptionScanner.MedicationInfo> medications) {
                runOnUiThread(() -> {
                    scannedMedications = medications;
                    currentMedicationIndex = 0;
                    
                    if (medications.size() == 1) {
                        populateFormWithMedication(medications.get(0));
                        Toast.makeText(AddMedications.this, 
                            "Found: " + medications.get(0).name, 
                            Toast.LENGTH_SHORT).show();
                    } else {
                        showMedicationSelector(medications);
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    Toast.makeText(AddMedications.this, errorMessage, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showMedicationSelector(List<PrescriptionScanner.MedicationInfo> medications) {
        String[] medicationNames = new String[medications.size()];
        for (int i = 0; i < medications.size(); i++) {
            medicationNames[i] = medications.get(i).name + " " + medications.get(i).dosage;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Medication");
        builder.setItems(medicationNames, (dialog, which) -> {
            currentMedicationIndex = which;
            populateFormWithMedication(medications.get(which));
        });
        builder.setNeutralButton("Add All", (dialog, which) -> {
            Toast.makeText(this, "First medication added. Save and add more.", Toast.LENGTH_SHORT).show();
            populateFormWithMedication(medications.get(0));
        });
        builder.show();
    }

    private void populateFormWithMedication(PrescriptionScanner.MedicationInfo medication) {
        medNameEditText.setText(medication.name);
        doseEditText.setText(medication.dosage);

        checkMorning.setChecked(false);
        checkAfternoon.setChecked(false);
        checkNight.setChecked(false);

        if (medication.times != null) {
            for (String time : medication.times) {
                String timeLower = time.toLowerCase();
                if (timeLower.contains("morning") || timeLower.contains("breakfast")) {
                    checkMorning.setChecked(true);
                } else if (timeLower.contains("afternoon") || timeLower.contains("lunch")) {
                    checkAfternoon.setChecked(true);
                } else if (timeLower.contains("night") || timeLower.contains("dinner") || timeLower.contains("bedtime") || timeLower.contains("evening")) {
                    checkNight.setChecked(true);
                }
            }
        }

        if (!checkMorning.isChecked() && !checkAfternoon.isChecked() && !checkNight.isChecked()) {
            checkMorning.setChecked(true);
        }

        Toast.makeText(this, "Medication details filled", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (prescriptionScanner != null) {
            prescriptionScanner.close();
        }
    }
}
