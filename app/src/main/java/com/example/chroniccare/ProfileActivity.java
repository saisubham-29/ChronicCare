package com.example.chroniccare;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chroniccare.database.AppDatabase;
import com.example.chroniccare.database.MedicalDocument;
import com.example.chroniccare.database.User;
import com.example.chroniccare.utils.ProfileImageHelper;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends BaseActivity {

    private CircleImageView profileImage, btnChangePhoto;
    private TextView tvName, tvEmail;
    private TextInputEditText etPhone, etDOB, etHeight, etWeight, etConditions, etAllergies;
    private TextInputEditText etEmergencyName, etEmergencyPhone, etEmergencyRelation;
    private AutoCompleteTextView spinnerGender, spinnerBloodGroup;
    private MaterialButton btnSaveProfile, btnLogout, btnUploadDocument, btnViewDocuments;
    private ImageView btnSharePersonal, btnShareMedical, btnShareEmergency;
    private MaterialToolbar toolbar;
    private SharedPreferences sharedPreferences;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Intent> documentPickerLauncher;
    private AppDatabase db;
    private String currentUserId;
    private Uri selectedDocumentUri;
    private com.google.android.material.bottomsheet.BottomSheetDialog uploadBottomSheet;
    private com.example.chroniccare.utils.FirebaseSync firebaseSync;
    private java.util.concurrent.ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        sharedPreferences = getSharedPreferences("ChronicCarePrefs", MODE_PRIVATE);
        currentUserId = sharedPreferences.getString("userId", "");
        
        if (currentUserId == null || currentUserId.isEmpty()) {
            startActivity(new Intent(this, LogInPage.class));
            finish();
            return;
        }
        
        setContentView(R.layout.activity_profile);
        
        executorService = java.util.concurrent.Executors.newSingleThreadExecutor();
        db = AppDatabase.getInstance(this);
        firebaseSync = new com.example.chroniccare.utils.FirebaseSync(currentUserId);
        
        initializeViews();
        setupToolbar();
        setupDropdowns();
        setupListeners();
        
        // Load local data first, then sync from cloud
        loadUserData();
        syncProfileFromCloud();
    }
    
    private void syncProfileFromCloud() {
        com.google.firebase.firestore.FirebaseFirestore firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        
        // Sync personal info from Firebase to local DB
        firestore.collection("users").document(currentUserId)
            .collection("profile").document("personalInfo")
            .get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    Log.d("ProfileActivity", "✅ Synced personal info from cloud");
                    
                    // Save to local database
                    executorService.execute(() -> {
                        User user = db.userDao().getUserByUserId(currentUserId);
                        if (user == null) {
                            user = new User();
                            user.userId = currentUserId;
                        }
                        
                        user.name = doc.getString("name");
                        user.email = doc.getString("email");
                        user.phone = doc.getString("phone");
                        user.dob = doc.getString("dob");
                        user.gender = doc.getString("gender");
                        user.bloodGroup = doc.getString("bloodGroup");
                        
                        db.userDao().insert(user);
                        
                        // Now sync medical info
                        syncMedicalInfoFromCloud();
                    });
                } else {
                    Log.d("ProfileActivity", "No personal info in cloud");
                    syncMedicalInfoFromCloud();
                }
            })
            .addOnFailureListener(e -> {
                Log.e("ProfileActivity", "Failed to sync personal info from cloud", e);
            });
    }
    
    private void syncMedicalInfoFromCloud() {
        com.google.firebase.firestore.FirebaseFirestore firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        
        firestore.collection("users").document(currentUserId)
            .collection("profile").document("medicalInfo")
            .get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    Log.d("ProfileActivity", "✅ Synced medical info from cloud");
                    
                    executorService.execute(() -> {
                        User user = db.userDao().getUserByUserId(currentUserId);
                        if (user == null) {
                            user = new User();
                            user.userId = currentUserId;
                        }
                        
                        user.height = doc.getString("height");
                        user.weight = doc.getString("weight");
                        user.conditions = doc.getString("conditions");
                        user.allergies = doc.getString("allergies");
                        
                        db.userDao().insert(user);
                        
                        // Now sync emergency contact
                        syncEmergencyContactFromCloud();
                    });
                } else {
                    Log.d("ProfileActivity", "No medical info in cloud");
                    syncEmergencyContactFromCloud();
                }
            })
            .addOnFailureListener(e -> {
                Log.e("ProfileActivity", "Failed to sync medical info from cloud", e);
            });
    }
    
    private void syncEmergencyContactFromCloud() {
        com.google.firebase.firestore.FirebaseFirestore firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        
        firestore.collection("users").document(currentUserId)
            .collection("profile").document("emergencyContact")
            .get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    Log.d("ProfileActivity", "✅ Synced emergency contact from cloud");
                    
                    executorService.execute(() -> {
                        User user = db.userDao().getUserByUserId(currentUserId);
                        if (user == null) {
                            user = new User();
                            user.userId = currentUserId;
                        }
                        
                        user.emergencyName = doc.getString("name");
                        user.emergencyPhone = doc.getString("phone");
                        user.emergencyRelation = doc.getString("relation");
                        
                        db.userDao().insert(user);
                        
                        // Reload UI after all syncs complete
                        runOnUiThread(() -> loadUserData());
                    });
                } else {
                    Log.d("ProfileActivity", "No emergency contact in cloud");
                    runOnUiThread(() -> loadUserData());
                }
            })
            .addOnFailureListener(e -> {
                Log.e("ProfileActivity", "Failed to sync emergency contact from cloud", e);
                runOnUiThread(() -> loadUserData());
            });
    }

    private void initializeViews() {
        toolbar = findViewById(R.id.toolbar);
        profileImage = findViewById(R.id.profileImage);
        tvName = findViewById(R.id.tvProfileName);
        tvEmail = findViewById(R.id.tvProfileEmail);
        etPhone = findViewById(R.id.etPhone);
        etDOB = findViewById(R.id.etDOB);
        etHeight = findViewById(R.id.etHeight);
        etWeight = findViewById(R.id.etWeight);
        etConditions = findViewById(R.id.etConditions);
        etAllergies = findViewById(R.id.etAllergies);
        etEmergencyName = findViewById(R.id.etEmergencyName);
        etEmergencyPhone = findViewById(R.id.etEmergencyPhone);
        etEmergencyRelation = findViewById(R.id.etEmergencyRelation);
        spinnerGender = findViewById(R.id.spinnerGender);
        spinnerBloodGroup = findViewById(R.id.spinnerBloodGroup);
        btnChangePhoto = findViewById(R.id.btnChangePhoto);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnLogout = findViewById(R.id.btnLogout);
        btnUploadDocument = findViewById(R.id.btnUploadDocument);
        btnViewDocuments = findViewById(R.id.btnViewDocuments);
        btnSharePersonal = findViewById(R.id.btnSharePersonal);
        btnShareMedical = findViewById(R.id.btnShareMedical);
        btnShareEmergency = findViewById(R.id.btnShareEmergency);
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupDropdowns() {
        String[] genders = getResources().getStringArray(R.array.gender_array);
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, genders);
        spinnerGender.setAdapter(genderAdapter);

        String[] bloodGroups = getResources().getStringArray(R.array.blood_group_array);
        ArrayAdapter<String> bloodAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, bloodGroups);
        spinnerBloodGroup.setAdapter(bloodAdapter);
    }

    private void setupListeners() {
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        try {
                            getContentResolver().takePersistableUriPermission(
                                imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } catch (Exception ignored) {}
                        profileImage.setImageURI(imageUri);
                        sharedPreferences.edit().putString("userPhoto", imageUri.toString()).apply();
                        Toast.makeText(this, "Profile photo updated", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        );

        documentPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri docUri = result.getData().getData();
                    if (docUri != null) {
                        selectedDocumentUri = docUri;
                        updateBottomSheetWithFile(docUri);
                    }
                }
            }
        );

        btnChangePhoto.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        btnUploadDocument.setOnClickListener(v -> showUploadBottomSheet());

        btnViewDocuments.setOnClickListener(v -> 
            startActivity(new Intent(this, DocumentsActivity.class))
        );

        etDOB.setOnClickListener(v -> showDatePicker());
        btnSaveProfile.setOnClickListener(v -> saveProfile());
        btnLogout.setOnClickListener(v -> logout());
        btnSharePersonal.setOnClickListener(v -> sharePersonalInfo());
        btnShareMedical.setOnClickListener(v -> shareMedicalInfo());
        btnShareEmergency.setOnClickListener(v -> shareEmergencyInfo());
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            String date = String.format("%02d/%02d/%d", day, month + 1, year);
            etDOB.setText(date);
        }, cal.get(Calendar.YEAR) - 30, cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void loadUserData() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.execute(() -> {
                try {
                    Log.d("ProfileActivity", "Loading user data for userId: " + currentUserId);
                    User user = db.userDao().getUserByUserId(currentUserId);
                    
                    if (user != null) {
                        Log.d("ProfileActivity", "User found in DB: " + user.name);
                        Log.d("ProfileActivity", "Conditions: " + user.conditions);
                        Log.d("ProfileActivity", "Emergency Name: " + user.emergencyName);
                        Log.d("ProfileActivity", "Height: " + user.height);
                    } else {
                        Log.d("ProfileActivity", "User NOT found in DB");
                    }
                    
                    runOnUiThread(() -> {
                        if (user != null) {
                            // Load from database
                            tvName.setText(user.name != null ? user.name : "User");
                            tvEmail.setText(user.email != null ? user.email : "");
                            
                            if (etPhone != null) etPhone.setText(user.phone != null ? user.phone : "");
                            if (etDOB != null) etDOB.setText(user.dob != null ? user.dob : "");
                            if (etHeight != null) etHeight.setText(user.height != null ? user.height : "");
                            if (etWeight != null) etWeight.setText(user.weight != null ? user.weight : "");
                            if (etConditions != null) etConditions.setText(user.conditions != null ? user.conditions : "");
                            if (etAllergies != null) etAllergies.setText(user.allergies != null ? user.allergies : "");
                            if (etEmergencyName != null) etEmergencyName.setText(user.emergencyName != null ? user.emergencyName : "");
                            if (etEmergencyPhone != null) etEmergencyPhone.setText(user.emergencyPhone != null ? user.emergencyPhone : "");
                            if (etEmergencyRelation != null) etEmergencyRelation.setText(user.emergencyRelation != null ? user.emergencyRelation : "");
                            
                            if (user.gender != null && !user.gender.isEmpty() && spinnerGender != null) 
                                spinnerGender.setText(user.gender, false);
                            if (user.bloodGroup != null && !user.bloodGroup.isEmpty() && spinnerBloodGroup != null) 
                                spinnerBloodGroup.setText(user.bloodGroup, false);
                                
                            Log.d("ProfileActivity", "UI updated with user data");
                        } else {
                            // Load from SharedPreferences (first time)
                            tvName.setText(sharedPreferences.getString("userName", "User"));
                            tvEmail.setText(sharedPreferences.getString("userEmail", ""));
                            Log.d("ProfileActivity", "No user data in DB, loaded from SharedPreferences");
                        }
                        ProfileImageHelper.loadProfileImage(ProfileActivity.this, profileImage);
                    });
                } catch (Exception e) {
                    Log.e("ProfileActivity", "Error loading user data: " + e.getMessage(), e);
                }
            });
        }
    }

    private void saveProfile() {
        User user = new User();
        user.userId = currentUserId;
        user.name = sharedPreferences.getString("userName", "");
        user.email = sharedPreferences.getString("userEmail", "");
        user.phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
        user.dob = etDOB.getText() != null ? etDOB.getText().toString().trim() : "";
        user.gender = spinnerGender.getText() != null ? spinnerGender.getText().toString() : "";
        user.bloodGroup = spinnerBloodGroup.getText() != null ? spinnerBloodGroup.getText().toString() : "";
        user.height = etHeight.getText() != null ? etHeight.getText().toString().trim() : "";
        user.weight = etWeight.getText() != null ? etWeight.getText().toString().trim() : "";
        user.conditions = etConditions.getText() != null ? etConditions.getText().toString().trim() : "";
        user.allergies = etAllergies.getText() != null ? etAllergies.getText().toString().trim() : "";
        user.emergencyName = etEmergencyName.getText() != null ? etEmergencyName.getText().toString().trim() : "";
        user.emergencyPhone = etEmergencyPhone.getText() != null ? etEmergencyPhone.getText().toString().trim() : "";
        user.emergencyRelation = etEmergencyRelation.getText() != null ? etEmergencyRelation.getText().toString().trim() : "";

        if (executorService != null && !executorService.isShutdown()) {
            executorService.execute(() -> {
                try {
                    db.userDao().insert(user);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Profile saved successfully", Toast.LENGTH_SHORT).show();
                        loadUserData(); // Reload to show saved data
                    });
                } catch (Exception e) {
                    Log.e("ProfileActivity", "Error saving profile: " + e.getMessage(), e);
                    runOnUiThread(() -> Toast.makeText(this, "Failed to save profile", Toast.LENGTH_SHORT).show());
                }
            });
        }
        
        // Sync to Firebase with proper structure
        try {
            Log.d("ProfileActivity", "Preparing to sync profile to Firebase...");
            
            // Personal Info
            java.util.HashMap<String, Object> personalInfo = new java.util.HashMap<>();
            personalInfo.put("name", user.name);
            personalInfo.put("email", user.email);
            personalInfo.put("phone", user.phone);
            personalInfo.put("dob", user.dob);
            personalInfo.put("gender", user.gender);
            personalInfo.put("bloodGroup", user.bloodGroup);
            
            // Medical Info
            java.util.HashMap<String, Object> medicalInfo = new java.util.HashMap<>();
            medicalInfo.put("height", user.height);
            medicalInfo.put("weight", user.weight);
            medicalInfo.put("conditions", user.conditions);
            medicalInfo.put("allergies", user.allergies);
            
            // Emergency Contact
            java.util.HashMap<String, Object> emergencyContact = new java.util.HashMap<>();
            emergencyContact.put("name", user.emergencyName);
            emergencyContact.put("phone", user.emergencyPhone);
            emergencyContact.put("relation", user.emergencyRelation);
            
            if (firebaseSync != null) {
                firebaseSync.syncPersonalInfo(personalInfo);
                firebaseSync.syncMedicalInfo(medicalInfo);
                firebaseSync.syncEmergencyContact(emergencyContact);
                Log.d("ProfileActivity", "Firebase sync triggered");
            } else {
                Log.e("ProfileActivity", "FirebaseSync is null!");
            }
        } catch (Exception e) {
            Log.e("ProfileActivity", "Error syncing to Firebase: " + e.getMessage(), e);
        }
    }

    private void logout() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail().build();
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

    private void sharePersonalInfo() {
        String name = tvName.getText().toString();
        String phone = etPhone.getText().toString();
        String dob = etDOB.getText().toString();
        String gender = spinnerGender.getText().toString();
        String bloodGroup = spinnerBloodGroup.getText().toString();

        StringBuilder message = new StringBuilder("Hello, Here is my Personal Info:\n\n");
        message.append("Name: ").append(name).append("\n");
        if (!phone.isEmpty()) message.append("Phone: ").append(phone).append("\n");
        if (!dob.isEmpty()) message.append("DOB: ").append(dob).append("\n");
        if (!gender.isEmpty() && !gender.equals("Select Gender")) message.append("Gender: ").append(gender).append("\n");
        if (!bloodGroup.isEmpty() && !bloodGroup.equals("Select Blood Group")) message.append("Blood Group: ").append(bloodGroup);

        shareText(message.toString());
    }

    private void shareMedicalInfo() {
        String height = etHeight.getText().toString();
        String weight = etWeight.getText().toString();
        String conditions = etConditions.getText().toString();
        String allergies = etAllergies.getText().toString();

        StringBuilder message = new StringBuilder("Hello, Here is my Medical Info:\n\n");
        if (!height.isEmpty()) message.append("Height: ").append(height).append(" cm\n");
        if (!weight.isEmpty()) message.append("Weight: ").append(weight).append(" kg\n");
        if (!conditions.isEmpty()) message.append("Chronic Conditions: ").append(conditions).append("\n");
        if (!allergies.isEmpty()) message.append("Allergies: ").append(allergies);

        shareText(message.toString());
    }

    private void shareEmergencyInfo() {
        String name = etEmergencyName.getText().toString();
        String phone = etEmergencyPhone.getText().toString();
        String relation = etEmergencyRelation.getText().toString();

        StringBuilder message = new StringBuilder("Hello, Here is my Emergency Contact Info:\n\n");
        if (!name.isEmpty()) message.append("Contact Name: ").append(name).append("\n");
        if (!phone.isEmpty()) message.append("Contact Phone: ").append(phone).append("\n");
        if (!relation.isEmpty()) message.append("Relationship: ").append(relation);

        shareText(message.toString());
    }

    private void shareText(String text) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }

    private void showUploadBottomSheet() {
        uploadBottomSheet = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_upload_document, null);
        uploadBottomSheet.setContentView(view);

        AutoCompleteTextView etDocumentType = view.findViewById(R.id.etDocumentType);
        com.google.android.material.textfield.TextInputEditText etDocumentName = view.findViewById(R.id.etDocumentName);
        MaterialButton btnSelectFile = view.findViewById(R.id.btnSelectFile);
        TextView tvSelectedFile = view.findViewById(R.id.tvSelectedFile);
        MaterialButton btnSaveDocument = view.findViewById(R.id.btnSaveDocument);

        String[] types = {"Lab Report", "Prescription", "Medical Certificate", "X-Ray/Scan", "Insurance Document", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, types);
        etDocumentType.setAdapter(adapter);

        btnSelectFile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            String[] mimeTypes = {"application/pdf", "image/*"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            documentPickerLauncher.launch(intent);
        });

        btnSaveDocument.setOnClickListener(v -> {
            String docType = etDocumentType.getText().toString().trim();
            String docName = etDocumentName.getText().toString().trim();

            if (docType.isEmpty()) {
                Toast.makeText(this, "Please select document type", Toast.LENGTH_SHORT).show();
                return;
            }
            if (docName.isEmpty()) {
                Toast.makeText(this, "Please enter document name", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedDocumentUri == null) {
                Toast.makeText(this, "Please select a file", Toast.LENGTH_SHORT).show();
                return;
            }

            uploadDocument(selectedDocumentUri, docType, docName);
            uploadBottomSheet.dismiss();
        });

        uploadBottomSheet.show();
    }

    private void updateBottomSheetWithFile(Uri uri) {
        if (uploadBottomSheet != null && uploadBottomSheet.isShowing()) {
            TextView tvSelectedFile = uploadBottomSheet.findViewById(R.id.tvSelectedFile);
            if (tvSelectedFile != null) {
                String fileName = getFileName(uri);
                tvSelectedFile.setText("Selected: " + fileName);
                tvSelectedFile.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            }
        }
    }

    private void uploadDocument(Uri uri, String docType, String docName) {
        try {
            Log.d("ProfileActivity", "Starting upload for URI: " + uri.toString());
            
            String extension = getFileExtension(getFileName(uri));
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String storedFileName = currentUserId + "_" + timestamp + extension;
            long fileSize = getFileSize(uri);
            
            Log.d("ProfileActivity", "File name: " + storedFileName);
            Log.d("ProfileActivity", "File size: " + fileSize);
            
            // Show progress
            android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
            progressDialog.setMessage("Uploading to cloud...");
            progressDialog.setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setMax(100);
            progressDialog.show();
            
            // Upload to Firebase Storage
            com.example.chroniccare.utils.FirebaseStorageHelper storageHelper = 
                new com.example.chroniccare.utils.FirebaseStorageHelper(currentUserId);
            
            storageHelper.uploadDocument(uri, storedFileName, new com.example.chroniccare.utils.FirebaseStorageHelper.UploadCallback() {
                @Override
                public void onSuccess(String downloadUrl) {
                    Log.d("ProfileActivity", "✅ Upload successful! Download URL: " + downloadUrl);
                    progressDialog.dismiss();
                    
                    // Save to local database
                    MedicalDocument document = new MedicalDocument();
                    document.userId = currentUserId;
                    document.documentName = docName;
                    document.documentType = docType;
                    document.documentUri = downloadUrl; // Store Firebase URL
                    document.uploadDate = System.currentTimeMillis();
                    document.fileSize = fileSize;
                    
                    Log.d("ProfileActivity", "Saving to local DB with URL: " + downloadUrl);
                    
                    if (executorService != null && !executorService.isShutdown()) {
                        executorService.execute(() -> {
                            try {
                                db.medicalDocumentDao().insert(document);
                                Log.d("ProfileActivity", "✅ Saved to local database");
                                
                                // Sync metadata to Firestore
                                java.util.HashMap<String, Object> docData = new java.util.HashMap<>();
                                docData.put("documentName", document.documentName);
                                docData.put("documentType", document.documentType);
                                docData.put("downloadUrl", downloadUrl);
                                docData.put("fileName", storedFileName);
                                docData.put("uploadDate", document.uploadDate);
                                docData.put("fileSize", document.fileSize);
                                
                                if (firebaseSync != null) {
                                    firebaseSync.syncDocument(String.valueOf(System.currentTimeMillis()), docData);
                                    Log.d("ProfileActivity", "✅ Synced to Firestore");
                                }
                                
                                runOnUiThread(() -> {
                                    Toast.makeText(ProfileActivity.this, docType + " uploaded to cloud successfully", Toast.LENGTH_SHORT).show();
                                    selectedDocumentUri = null;
                                });
                            } catch (Exception e) {
                                Log.e("ProfileActivity", "❌ Error saving document: " + e.getMessage(), e);
                                runOnUiThread(() -> Toast.makeText(ProfileActivity.this, "Failed to save document", Toast.LENGTH_SHORT).show());
                            }
                        });
                    }
                }
                
                @Override
                public void onFailure(String error) {
                    Log.e("ProfileActivity", "❌ Upload failed: " + error);
                    progressDialog.dismiss();
                    Toast.makeText(ProfileActivity.this, "Upload failed: " + error, Toast.LENGTH_SHORT).show();
                }
                
                @Override
                public void onProgress(int progress) {
                    progressDialog.setProgress(progress);
                    Log.d("ProfileActivity", "Upload progress: " + progress + "%");
                }
            });
        } catch (Exception e) {
            Log.e("ProfileActivity", "❌ Upload error: " + e.getMessage(), e);
            Toast.makeText(this, "Failed to upload document", Toast.LENGTH_SHORT).show();
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf("."));
        }
        return ".pdf";
    }

    private String getFileName(Uri uri) {
        String name = "Document";
        try {
            android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1) name = cursor.getString(nameIndex);
                cursor.close();
            }
        } catch (Exception ignored) {}
        return name;
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

    private long getFileSize(Uri uri) {
        long size = 0;
        try {
            android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
                if (sizeIndex != -1) size = cursor.getLong(sizeIndex);
                cursor.close();
            }
        } catch (Exception ignored) {}
        return size;
    }
}
