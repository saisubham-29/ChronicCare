package com.example.chroniccare;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private EditText etUsername;
    private EditText etEmail;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private TextView btnSignUp;
    private TextView linkLogin;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        sharedPreferences = getSharedPreferences("ChronicCarePrefs", MODE_PRIVATE);

        etUsername = findViewById(R.id.etSignupUsername);
        etEmail = findViewById(R.id.etSignupEmail);
        etPassword = findViewById(R.id.etSignupPassword);
        etConfirmPassword = findViewById(R.id.etSignupConfirmPassword);
        btnSignUp = findViewById(R.id.btnSignUp);
        linkLogin = findViewById(R.id.linkLogin);

        btnSignUp.setOnClickListener(v -> createAccount());
        linkLogin.setOnClickListener(v -> finish());
    }

    private void createAccount() {
        String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        String confirm = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirm)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        String usernameKey = username.toLowerCase(Locale.getDefault()).replaceAll("\\s+", "_");
        firestore.collection("usernames").document(usernameKey).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Toast.makeText(this, "Username already taken", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    firebaseAuth.createUserWithEmailAndPassword(email, password)
                            .addOnSuccessListener(authResult -> {
                                FirebaseUser user = authResult.getUser();
                                if (user == null) {
                                    Toast.makeText(this, "Signup failed", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                String uid = user.getUid();

                                Map<String, Object> personalInfo = new HashMap<>();
                                personalInfo.put("name", username);
                                personalInfo.put("email", email);
                                personalInfo.put("username", username);

                                firestore.collection("users").document(uid)
                                        .collection("profile").document("personalInfo")
                                        .set(personalInfo);

                                Map<String, Object> usernameMap = new HashMap<>();
                                usernameMap.put("userId", uid);
                                usernameMap.put("email", email);
                                usernameMap.put("username", username);
                                usernameMap.put("createdAt", System.currentTimeMillis());

                                firestore.collection("usernames").document(usernameKey)
                                        .set(usernameMap);

                                saveLoginState(uid, username, email);
                                Toast.makeText(this, "Account created", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(SignUpActivity.this, HomeActivity.class));
                                finish();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Signup failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Signup failed. Try again.", Toast.LENGTH_SHORT).show());
    }

    private void saveLoginState(String userId, String userName, String userEmail) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isLoggedIn", true);
        editor.putString("userId", userId);
        editor.putString("userName", userName);
        editor.putString("userEmail", userEmail);
        editor.putString("userPhoto", "");
        editor.apply();
    }
}
