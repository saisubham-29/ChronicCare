package com.example.chroniccare;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    private CircleImageView profileImage;
    private TextView tvName, tvEmail;
    private Button btnChangePhoto, btnLogout;
    private SharedPreferences sharedPreferences;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        sharedPreferences = getSharedPreferences("ChronicCarePrefs", MODE_PRIVATE);

        profileImage = findViewById(R.id.profileImage);
        tvName = findViewById(R.id.tvProfileName);
        tvEmail = findViewById(R.id.tvProfileEmail);
        btnChangePhoto = findViewById(R.id.btnChangePhoto);
        btnLogout = findViewById(R.id.btnLogout);

        loadUserData();

        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        profileImage.setImageURI(imageUri);
                        sharedPreferences.edit().putString("userPhoto", imageUri.toString()).apply();
                        Toast.makeText(this, "Profile photo updated", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        );

        btnChangePhoto.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        btnLogout.setOnClickListener(v -> logout());
    }

    private void loadUserData() {
        String name = sharedPreferences.getString("userName", "User");
        String email = sharedPreferences.getString("userEmail", "");
        String photoUrl = sharedPreferences.getString("userPhoto", "");

        tvName.setText(name);
        tvEmail.setText(email);

        if (!photoUrl.isEmpty()) {
            if (photoUrl.startsWith("http")) {
                Picasso.get().load(photoUrl).placeholder(R.drawable.ic_profile).into(profileImage);
            } else {
                profileImage.setImageURI(Uri.parse(photoUrl));
            }
        }
    }

    private void logout() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        GoogleSignInClient gsc = GoogleSignIn.getClient(this, gso);
        gsc.signOut().addOnCompleteListener(task -> {
            sharedPreferences.edit().clear().apply();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(ProfileActivity.this, LogInPage.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
