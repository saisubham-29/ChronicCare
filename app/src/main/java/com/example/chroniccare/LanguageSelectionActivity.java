package com.example.chroniccare;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chroniccare.utils.LocaleHelper;

public class LanguageSelectionActivity extends AppCompatActivity {

    private RadioGroup languageGroup;
    private Button btnContinue;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_language_selection);

        sharedPreferences = getSharedPreferences("ChronicCarePrefs", MODE_PRIVATE);
        
        languageGroup = findViewById(R.id.languageGroup);
        btnContinue = findViewById(R.id.btnContinue);

        // Set default selection to English
        RadioButton englishRadio = findViewById(R.id.radioEnglish);
        englishRadio.setChecked(true);

        btnContinue.setOnClickListener(v -> {
            int selectedId = languageGroup.getCheckedRadioButtonId();
            String languageCode = "en"; // default

            if (selectedId == R.id.radioHindi) {
                languageCode = "hi";
            } else if (selectedId == R.id.radioBengali) {
                languageCode = "bn";
            } else if (selectedId == R.id.radioOdia) {
                languageCode = "or";
            } else if (selectedId == R.id.radioTamil) {
                languageCode = "ta";
            } else if (selectedId == R.id.radioTelugu) {
                languageCode = "te";
            } else if (selectedId == R.id.radioKannada) {
                languageCode = "kn";
            } else if (selectedId == R.id.radioMalayalam) {
                languageCode = "ml";
            }

            // Save language preference
            LocaleHelper.setLocale(this, languageCode);
            sharedPreferences.edit().putBoolean("language_selected", true).apply();

            // Navigate to next screen
            Intent intent = new Intent(LanguageSelectionActivity.this, LogInPage.class);
            startActivity(intent);
            finish();
        });
    }
}
