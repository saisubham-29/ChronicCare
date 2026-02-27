package com.example.chroniccare;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

public class LogInPage extends AppCompatActivity {
    TextView skiploggin;
    TextView signupLink;
    LinearLayout btnGoogle;
    EditText etUsername;
    EditText etPassword;
    TextView btnLogin;
    GoogleSignInClient gsc;
    SharedPreferences sharedPreferences;
    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_log_in_page);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        skiploggin=findViewById(R.id.skiploggin);
        skiploggin.setOnClickListener(v->skipLogin());
        
        sharedPreferences = getSharedPreferences("ChronicCarePrefs", MODE_PRIVATE);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        etUsername = findViewById(R.id.etusername);
        etPassword = findViewById(R.id.etpassword);
        btnLogin = findViewById(R.id.btnLogin);
        signupLink = findViewById(R.id.signupLink);

        btnGoogle = findViewById(R.id.btnGoogle);
        
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        gsc = GoogleSignIn.getClient(this, gso);
        
        btnGoogle.setOnClickListener(v -> signIn());
        btnLogin.setOnClickListener(v -> signInWithUsername());
        signupLink.setOnClickListener(v -> startActivity(new Intent(LogInPage.this, SignUpActivity.class)));
    }
    
    private void signIn() {
        Intent signInIntent = gsc.getSignInIntent();
        startActivityForResult(signInIntent, 1000);
    }
    private void skipLogin() {
        Intent skip = new Intent(LogInPage.this, HomeActivity.class);
        startActivity(skip);
    }

    private void signInWithUsername() {
        String usernameInput = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
        String passwordInput = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (usernameInput.isEmpty() || passwordInput.isEmpty()) {
            Toast.makeText(this, "Enter username and password", Toast.LENGTH_SHORT).show();
            return;
        }

        if (usernameInput.contains("@")) {
            signInWithEmail(usernameInput, passwordInput, null);
            return;
        }

        String usernameKey = usernameInput.toLowerCase(Locale.getDefault()).replaceAll("\\s+", "_");
        firestore.collection("usernames").document(usernameKey).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "Username not found", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    String email = doc.getString("email");
                    String username = doc.getString("username");
                    if (email == null || email.trim().isEmpty()) {
                        Toast.makeText(this, "Email not found for username", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    signInWithEmail(email, passwordInput, username != null ? username : usernameInput);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Login failed. Try again.", Toast.LENGTH_SHORT).show());
    }

    private void signInWithEmail(String email, String password, String username) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    String uid = user != null ? user.getUid() : "";
                    saveEmailLoginState(uid, username != null ? username : email, email);
                    Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LogInPage.this, HomeActivity.class));
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Sign-in failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 1000) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    saveLoginState(account);
                    Toast.makeText(this, "Welcome " + account.getDisplayName(), Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LogInPage.this, HomeActivity.class));
                    finish();
                }
            } catch (ApiException e) {
                Toast.makeText(this, "Sign-in failed: " + e.getStatusCode(), Toast.LENGTH_LONG).show();
                android.util.Log.e("GoogleSignIn", "Error code: " + e.getStatusCode() + ", Message: " + e.getMessage());
            }
        }
    }
    
    private void saveLoginState(GoogleSignInAccount account) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isLoggedIn", true);
        editor.putString("userId", account.getId());
        editor.putString("userName", account.getDisplayName());
        editor.putString("userEmail", account.getEmail());
        editor.putString("userPhoto", account.getPhotoUrl() != null ? account.getPhotoUrl().toString() : "");
        editor.apply();
    }

    private void saveEmailLoginState(String userId, String userName, String userEmail) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isLoggedIn", true);
        editor.putString("userId", userId);
        editor.putString("userName", userName);
        editor.putString("userEmail", userEmail);
        editor.putString("userPhoto", "");
        editor.apply();
    }
}
