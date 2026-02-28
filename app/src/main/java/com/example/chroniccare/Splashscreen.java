package com.example.chroniccare;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Splashscreen extends AppCompatActivity {

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splashscreen);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        sharedPreferences = getSharedPreferences("ChronicCarePrefs", MODE_PRIVATE);

        new Handler().postDelayed(() -> {
            boolean languageSelected = sharedPreferences.getBoolean("language_selected", false);
            boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);
            
            Intent intent;
            if (!languageSelected) {
                // First time user - show language selection
                intent = new Intent(Splashscreen.this, LanguageSelectionActivity.class);
            } else if (isLoggedIn) {
                intent = new Intent(Splashscreen.this, HomeActivity.class);
            } else {
                intent = new Intent(Splashscreen.this, LogInPage.class);
            }
            
            startActivity(intent);
            finish();
        }, 1500);
    }
}
