package com.example.chroniccare;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.os.LocaleListCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

public class LogInPage extends AppCompatActivity {
    TextView skiploggin;
    TextView signupLink;
    MaterialButton btnGoogle, btnInstagram;
    EditText etUsername;
    EditText etPassword;
    MaterialButton btnLogin;




    GoogleSignInClient gsc;
    SharedPreferences sharedPreferences;
    FirebaseAuth firebaseAuth;
    FirebaseFirestore firestore;
    AutoCompleteTextView langSelector;
    String[] languageLabels;
    String[] languageTags = {"en", "hi", "bn", "ta", "te", "mr", "gu", "kn", "ml", "pa"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_log_in_page);
        
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }
        
        skiploggin = findViewById(R.id.skiploggin);
        if (skiploggin != null) {
            skiploggin.setOnClickListener(v -> skipLogin());
        }
        
        sharedPreferences = getSharedPreferences("ChronicCarePrefs", MODE_PRIVATE);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        etUsername = findViewById(R.id.etusername);
        etPassword = findViewById(R.id.etpassword);
        btnLogin = findViewById(R.id.btnLogin);
        signupLink = findViewById(R.id.signupLink);

        btnGoogle = findViewById(R.id.btnGoogle);
        btnInstagram = findViewById(R.id.btnInstagram);
        
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        gsc = GoogleSignIn.getClient(this, gso);
        
        if (btnGoogle != null) {
            btnGoogle.setOnClickListener(v -> signIn());
        }
        
        if (btnLogin != null) {
            btnLogin.setOnClickListener(v -> signInWithUsername());
        }
        
        if (signupLink != null) {
            signupLink.setOnClickListener(v -> startActivity(new Intent(LogInPage.this, SignUpActivity.class)));
        }

        View returnBtn = findViewById(R.id.WelcomePageReturn);
        if (returnBtn != null) {
            returnBtn.setOnClickListener(v -> finish());
        }
        btnGoogle.setOnClickListener(v -> signIn());
        btnLogin.setOnClickListener(v -> signInWithUsername());
        signupLink.setOnClickListener(v -> startActivity(new Intent(LogInPage.this, SignUpActivity.class)));

        setupLanguageSelector();
    }
    
    private void signIn() {
        Intent signInIntent = gsc.getSignInIntent();
        startActivityForResult(signInIntent, 1000);
    }
    
    private void skipLogin() {
        Intent skip = new Intent(LogInPage.this, HomeActivity.class);
        startActivity(skip);
    }

    private void setupLanguageSelector() {
        if (langSelector == null) {
            return;
        }
        languageLabels = getResources().getStringArray(R.array.language_labels);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, languageLabels);
        langSelector.setAdapter(adapter);
        langSelector.setOnClickListener(v -> langSelector.showDropDown());

        String savedTag = sharedPreferences.getString("appLanguage", "en");
        int index = 0;
        for (int i = 0; i < languageTags.length; i++) {
            if (languageTags[i].equals(savedTag)) {
                index = i;
                break;
            }
        }
        langSelector.setText(languageLabels[index], false);

        langSelector.setOnItemClickListener((parent, view, position, id) -> {
            String tag = languageTags[position];
            sharedPreferences.edit().putString("appLanguage", tag).apply();
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag));
            recreate();
        });
    }

    private void signInWithUsername() {
        if (etUsername == null || etPassword == null) return;


        String usernameInput = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
        String passwordInput = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (usernameInput.isEmpty() || passwordInput.isEmpty()) {
            Toast.makeText(this, getString(R.string.login_enter_credentials), Toast.LENGTH_SHORT).show();
            return;
        }

        if (usernameInput.contains("@")) {
            signInWithEmail(usernameInput, passwordInput, null);
            return;
        }

        String usernameKey = usernameInput.toLowerCase(Locale.getDefault()).replaceAll("\\s+", "_");
        firestore.collection("users")
                .whereEqualTo("usernameKey", usernameKey)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        com.google.firebase.firestore.DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                        String email = doc.getString("email");
                        String username = doc.getString("username");
                        if (email == null || email.trim().isEmpty()) {
                            Toast.makeText(this, getString(R.string.login_email_not_found), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        signInWithEmail(email, passwordInput, username != null ? username : usernameInput);
                        return;
                    }

                    firestore.collection("usernames").document(usernameKey).get()
                            .addOnSuccessListener(doc -> {
                                if (!doc.exists()) {
                                    Toast.makeText(this, getString(R.string.login_username_not_found), Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                String email = doc.getString("email");
                                String username = doc.getString("username");
                                if (email == null || email.trim().isEmpty()) {
                                    Toast.makeText(this, getString(R.string.login_email_not_found), Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                signInWithEmail(email, passwordInput, username != null ? username : usernameInput);
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, getString(R.string.login_failed_try_again), Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, getString(R.string.login_failed_try_again), Toast.LENGTH_SHORT).show());
    }

    private void signInWithEmail(String email, String password, String username) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    String uid = user != null ? user.getUid() : "";
                    saveEmailLoginState(uid, username != null ? username : email, email);
                    Toast.makeText(this, getString(R.string.login_welcome_back_toast), Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LogInPage.this, HomeActivity.class));
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, getString(R.string.login_signin_failed_message, e.getMessage()), Toast.LENGTH_LONG).show());
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
                    String displayName = account.getDisplayName() != null ? account.getDisplayName() : "";
                    Toast.makeText(this, getString(R.string.login_welcome_user, displayName), Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(LogInPage.this, HomeActivity.class));
                    finish();
                }
            } catch (ApiException e) {
                Toast.makeText(this, getString(R.string.login_signin_failed_code, e.getStatusCode()), Toast.LENGTH_LONG).show();
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
