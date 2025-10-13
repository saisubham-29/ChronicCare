package com.example.chroniccare;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Splashscreen extends AppCompatActivity {

    private ImageView Logo;
    private static final int SPLASH_DURATION = 1500; // milliseconds
    private SharedPreferences sharedPreferences;

    private static final String TAG = "SplashScreen";
    private static final String PREF_NAME = "LoginPrefs";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "userId";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splashscreen);

        // Handle insets properly for edge-to-edge layout
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize views and preferences
        Logo = findViewById(R.id.Logo);
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        // Start animation
        animateLogo();

        // Delay navigation based on login status
        new Handler().postDelayed(this::checkLoginStatus, SPLASH_DURATION);
    }

    /**
     * Animate logo pop-in and scale effect
     */
    private void animateLogo() {
        Logo.setScaleX(0f);
        Logo.setScaleY(0f);
        Logo.animate()
                .scaleX(1.2f)
                .scaleY(1.2f)
                .setDuration(200)
                .withEndAction(() -> Logo.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(200)
                        .start())
                .start();
    }

    /**
     * Check if user is logged in and navigate accordingly
     */
    private void checkLoginStatus() {
        boolean isLoggedIn = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
        String userId = sharedPreferences.getString(KEY_USER_ID, null);

        Log.d(TAG, "SharedPreferences - isLoggedIn: " + isLoggedIn + ", userId: " + userId);

        Intent intent;
        if (isLoggedIn && userId != null) {
            // User is logged in → go to WelcomePage
            intent = new Intent(Splashscreen.this, WelcomePage.class);
            Log.d(TAG, "User logged in, going to WelcomePage");
        } else {
            // User not logged in → go to LogInPage
            intent = new Intent(Splashscreen.this, LogInPage.class);
            Log.d(TAG, "User not logged in, going to LogInPage");
        }

        startActivity(intent);
        finish();
    }

    /**
     * Save login data after successful login
     */
    public static void saveLoginData(SharedPreferences sharedPreferences, String userId) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_ID, userId);
        editor.apply();
        Log.d(TAG, "Login data saved for user: " + userId);
    }

    /**
     * Clear login data on logout
     */
    public static void clearLoginData(SharedPreferences sharedPreferences) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_IS_LOGGED_IN);
        editor.remove(KEY_USER_ID);
        editor.apply();
        Log.d(TAG, "Login data cleared");
    }
}
