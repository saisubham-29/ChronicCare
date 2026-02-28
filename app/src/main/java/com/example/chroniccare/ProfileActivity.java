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
import android.widget.GridLayout;
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
import com.google.firebase.auth.FirebaseAuth;
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
    private MaterialButton btnSaveProfile, btnLogout, btnUploadDocument, btnViewDocuments, btnChangeLanguage;
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

        loadUserData();
        syncProfileFromCloud();
    }

    private void syncProfileFromCloud() {
        com.google.firebase.firestore.FirebaseFirestore firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        firestore.collection("users").document(currentUserId)
            .collection("profile").document("personalInfo")
            .get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
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
                        syncMedicalInfoFromCloud();
                    });
                } else {
                    syncMedicalInfoFromCloud();
                }
            });
    }

    private void syncMedicalInfoFromCloud() {
        com.google.firebase.firestore.FirebaseFirestore firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        firestore.collection("users").document(currentUserId)
            .collection("profile").document("medicalInfo")
            .get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
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
                        syncEmergencyContactFromCloud();
                    });
                } else {
                    syncEmergencyContactFromCloud();
                }
            });
    }

    private void syncEmergencyContactFromCloud() {
        com.google.firebase.firestore.FirebaseFirestore firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        firestore.collection("users").document(currentUserId)
            .collection("profile").document("emergencyContact")
            .get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
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
                        runOnUiThread(() -> loadUserData());
                    });
                } else {
                    runOnUiThread(() -> loadUserData());
                }
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
        btnChangeLanguage = findViewById(R.id.btnChangeLanguage);
        btnUploadDocument = findViewById(R.id.btnUploadDocument);
        btnViewDocuments = findViewById(R.id.btnViewDocuments);
        btnSharePersonal = findViewById(R.id.btnSharePersonal);
        btnShareMedical = findViewById(R.id.btnShareMedical);
        btnShareEmergency = findViewById(R.id.btnShareEmergency);

        loadConsistencyGrid();
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
                        Toast.makeText(this, getString(R.string.profile_photo_updated_toast), Toast.LENGTH_SHORT).show();
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
        btnViewDocuments.setOnClickListener(v -> startActivity(new Intent(this, DocumentsActivity.class)));
        etDOB.setOnClickListener(v -> showDatePicker());
        btnSaveProfile.setOnClickListener(v -> saveProfile());
        btnLogout.setOnClickListener(v -> logout());
        if (btnChangeLanguage != null) {
            btnChangeLanguage.setOnClickListener(v -> showLanguageDialog());
        }
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
                    User user = db.userDao().getUserByUserId(currentUserId);
                    runOnUiThread(() -> {
                        if (user != null) {
                            tvName.setText(user.name != null ? user.name : getString(R.string.profile_default_user_name));
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
                            if (user.gender != null && !user.gender.isEmpty() && spinnerGender != null) spinnerGender.setText(user.gender, false);
                            if (user.bloodGroup != null && !user.bloodGroup.isEmpty() && spinnerBloodGroup != null) spinnerBloodGroup.setText(user.bloodGroup, false);
                        } else {
                            tvName.setText(sharedPreferences.getString("userName", getString(R.string.profile_default_user_name)));
                            tvEmail.setText(sharedPreferences.getString("userEmail", ""));
                        }
                        ProfileImageHelper.loadProfileImage(ProfileActivity.this, profileImage);
                    });
                } catch (Exception e) {
                    Log.e("ProfileActivity", "Error loading user data: " + e.getMessage());
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
                        Toast.makeText(this, getString(R.string.profile_saved_success_toast), Toast.LENGTH_SHORT).show();
                        loadUserData();
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(this, getString(R.string.profile_save_failed_toast), Toast.LENGTH_SHORT).show());
                }
            });
        }

        try {
            java.util.HashMap<String, Object> personalInfo = new java.util.HashMap<>();
            personalInfo.put("name", user.name);
            personalInfo.put("email", user.email);
            personalInfo.put("phone", user.phone);
            personalInfo.put("dob", user.dob);
            personalInfo.put("gender", user.gender);
            personalInfo.put("bloodGroup", user.bloodGroup);

            java.util.HashMap<String, Object> medicalInfo = new java.util.HashMap<>();
            medicalInfo.put("height", user.height);
            medicalInfo.put("weight", user.weight);
            medicalInfo.put("conditions", user.conditions);
            medicalInfo.put("allergies", user.allergies);

            java.util.HashMap<String, Object> emergencyContact = new java.util.HashMap<>();
            emergencyContact.put("name", user.emergencyName);
            emergencyContact.put("phone", user.emergencyPhone);
            emergencyContact.put("relation", user.emergencyRelation);

            if (firebaseSync != null) {
                firebaseSync.syncPersonalInfo(personalInfo);
                firebaseSync.syncMedicalInfo(medicalInfo);
                firebaseSync.syncEmergencyContact(emergencyContact);
            }
        } catch (Exception e) {
            Log.e("ProfileActivity", "Error syncing to Firebase: " + e.getMessage());
        }
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build();
        GoogleSignInClient gsc = GoogleSignIn.getClient(this, gso);
        gsc.signOut().addOnCompleteListener(task -> {
            sharedPreferences.edit().clear().apply();
            Toast.makeText(this, getString(R.string.profile_logout_success_toast), Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(ProfileActivity.this, LogInPage.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void showLanguageDialog() {
        String[] languages = {
            getString(R.string.language_english), getString(R.string.language_hindi),
            getString(R.string.language_bengali), getString(R.string.language_odia),
            getString(R.string.language_tamil), getString(R.string.language_telugu),
            getString(R.string.language_kannada), getString(R.string.language_malayalam)
        };
        String[] languageCodes = {"en", "hi", "bn", "or", "ta", "te", "kn", "ml"};
        
        String currentLang = com.example.chroniccare.utils.LocaleHelper.getPersistedLanguage(this);
        int selectedIndex = 0;
        for (int i = 0; i < languageCodes.length; i++) {
            if (languageCodes[i].equals(currentLang)) {
                selectedIndex = i;
                break;
            }
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.profile_change_language))
            .setSingleChoiceItems(languages, selectedIndex, (dialog, which) -> {
                String selectedLanguage = languageCodes[which];
                com.example.chroniccare.utils.LocaleHelper.setLocale(this, selectedLanguage);
                sharedPreferences.edit().putBoolean("language_selected", true).apply();
                dialog.dismiss();
                
                // CRITICAL FIX: Smooth restart to apply language instantly
                Intent restartIntent = new Intent(this, HomeActivity.class);
                restartIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(restartIntent);
                finish();
            })
            .setNegativeButton(getString(R.string.common_cancel), null)
            .show();
    }

    private void sharePersonalInfo() {
        String name = tvName.getText().toString();
        String phone = etPhone.getText().toString();
        String dob = etDOB.getText().toString();
        String gender = spinnerGender.getText().toString();
        String bloodGroup = spinnerBloodGroup.getText().toString();
        String genderPlaceholder = getResources().getStringArray(R.array.gender_array)[0];
        String bloodGroupPlaceholder = getResources().getStringArray(R.array.blood_group_array)[0];
        StringBuilder message = new StringBuilder(getString(R.string.profile_share_personal_intro));
        message.append(getString(R.string.profile_share_name_label)).append(name).append("\n");
        if (!phone.isEmpty()) message.append(getString(R.string.profile_share_phone_label)).append(phone).append("\n");
        if (!dob.isEmpty()) message.append(getString(R.string.profile_share_dob_label)).append(dob).append("\n");
        if (!gender.isEmpty() && !gender.equals(genderPlaceholder)) message.append(getString(R.string.profile_share_gender_label)).append(gender).append("\n");
        if (!bloodGroup.isEmpty() && !bloodGroup.equals(bloodGroupPlaceholder)) message.append(getString(R.string.profile_share_blood_group_label)).append(bloodGroup);
        shareText(message.toString());
    }

    private void shareMedicalInfo() {
        String height = etHeight.getText().toString();
        String weight = etWeight.getText().toString();
        String conditions = etConditions.getText().toString();
        String allergies = etAllergies.getText().toString();
        StringBuilder message = new StringBuilder(getString(R.string.profile_share_medical_intro));
        if (!height.isEmpty()) message.append(getString(R.string.profile_share_height_label)).append(height).append(getString(R.string.profile_share_height_suffix)).append("\n");
        if (!weight.isEmpty()) message.append(getString(R.string.profile_share_weight_label)).append(weight).append(getString(R.string.profile_share_weight_suffix)).append("\n");
        if (!conditions.isEmpty()) message.append(getString(R.string.profile_share_conditions_label)).append(conditions).append("\n");
        if (!allergies.isEmpty()) message.append(getString(R.string.profile_share_allergies_label)).append(allergies);
        shareText(message.toString());
    }

    private void shareEmergencyInfo() {
        String name = etEmergencyName.getText().toString();
        String phone = etEmergencyPhone.getText().toString();
        String relation = etEmergencyRelation.getText().toString();
        StringBuilder message = new StringBuilder(getString(R.string.profile_share_emergency_intro));
        if (!name.isEmpty()) message.append(getString(R.string.profile_share_contact_name_label)).append(name).append("\n");
        if (!phone.isEmpty()) message.append(getString(R.string.profile_share_contact_phone_label)).append(phone).append("\n");
        if (!relation.isEmpty()) message.append(getString(R.string.profile_share_relation_label)).append(relation);
        shareText(message.toString());
    }

    private void shareText(String text) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(shareIntent, getString(R.string.profile_share_chooser_title)));
    }

    private void showUploadBottomSheet() {
        uploadBottomSheet = new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_upload_document, null);
        uploadBottomSheet.setContentView(view);
        AutoCompleteTextView etDocumentType = view.findViewById(R.id.etDocumentType);
        com.google.android.material.textfield.TextInputEditText etDocumentName = view.findViewById(R.id.etDocumentName);
        MaterialButton btnSelectFile = view.findViewById(R.id.btnSelectFile);
        MaterialButton btnSaveDocument = view.findViewById(R.id.btnSaveDocument);
        String[] types = getResources().getStringArray(R.array.profile_document_types);
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
            if (docType.isEmpty()) { Toast.makeText(this, getString(R.string.profile_select_document_type_toast), Toast.LENGTH_SHORT).show(); return; }
            if (docName.isEmpty()) { Toast.makeText(this, getString(R.string.profile_enter_document_name_toast), Toast.LENGTH_SHORT).show(); return; }
            if (selectedDocumentUri == null) { Toast.makeText(this, getString(R.string.profile_select_file_toast), Toast.LENGTH_SHORT).show(); return; }
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
                tvSelectedFile.setText(getString(R.string.profile_selected_file_label, fileName));
                tvSelectedFile.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            }
        }
    }

    private void uploadDocument(Uri uri, String docType, String docName) {
        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String storedFileName = currentUserId + "_" + timestamp + getFileExtension(getFileName(uri));
            long fileSize = getFileSize(uri);
            android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
            progressDialog.setMessage(getString(R.string.profile_uploading_to_cloud));
            progressDialog.setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setMax(100);
            progressDialog.show();
            com.example.chroniccare.utils.FirebaseStorageHelper storageHelper = new com.example.chroniccare.utils.FirebaseStorageHelper(currentUserId);
            storageHelper.uploadDocument(uri, storedFileName, new com.example.chroniccare.utils.FirebaseStorageHelper.UploadCallback() {
                @Override
                public void onSuccess(String downloadUrl) {
                    progressDialog.dismiss();
                    MedicalDocument document = new MedicalDocument();
                    document.userId = currentUserId; document.documentName = docName; document.documentType = docType;
                    document.documentUri = downloadUrl; document.uploadDate = System.currentTimeMillis(); document.fileSize = fileSize;
                    if (executorService != null && !executorService.isShutdown()) {
                        executorService.execute(() -> {
                            try {
                                db.medicalDocumentDao().insert(document);
                                java.util.HashMap<String, Object> docData = new java.util.HashMap<>();
                                docData.put("documentName", document.documentName); docData.put("documentType", document.documentType);
                                docData.put("downloadUrl", downloadUrl); docData.put("fileName", storedFileName);
                                docData.put("uploadDate", document.uploadDate); docData.put("fileSize", document.fileSize);
                                if (firebaseSync != null) firebaseSync.syncDocument(String.valueOf(System.currentTimeMillis()), docData);
                                runOnUiThread(() -> {
                                    Toast.makeText(ProfileActivity.this, getString(R.string.profile_document_uploaded_toast, docType), Toast.LENGTH_SHORT).show();
                                    selectedDocumentUri = null;
                                });
                            } catch (Exception e) {
                                runOnUiThread(() -> Toast.makeText(ProfileActivity.this, getString(R.string.profile_save_document_failed_toast), Toast.LENGTH_SHORT).show());
                            }
                        });
                    }
                }
                @Override public void onFailure(String error) { progressDialog.dismiss(); Toast.makeText(ProfileActivity.this, getString(R.string.profile_upload_failed_toast, error), Toast.LENGTH_SHORT).show(); }
                @Override public void onProgress(int progress) { progressDialog.setProgress(progress); }
            });
        } catch (Exception e) { Toast.makeText(this, getString(R.string.profile_failed_upload_document_toast), Toast.LENGTH_SHORT).show(); }
    }

    private String getFileExtension(String fileName) {
        return (fileName != null && fileName.contains(".")) ? fileName.substring(fileName.lastIndexOf(".")) : ".pdf";
    }

    private String getFileName(Uri uri) {
        String name = getString(R.string.profile_default_document_name);
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

    private void loadConsistencyGrid() {
        android.widget.GridLayout grid = findViewById(R.id.consistencyGrid);
        if (grid == null) return;
        grid.removeAllViews();
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        int cardPadding = (int) (40 * getResources().getDisplayMetrics().density);
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int availableWidth = screenWidth - cardPadding - (int) (32 * getResources().getDisplayMetrics().density);
        int margin = (int) (2 * getResources().getDisplayMetrics().density);
        int size = (availableWidth - (margin * 2 * 12)) / 12;
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users").document(currentUserId).collection("medicationAdherence")
            .get().addOnSuccessListener(querySnapshot -> {
                java.util.Map<String, Integer> dailyData = new java.util.HashMap<>();
                for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                    String date = doc.getId(); Long taken = doc.getLong("taken");
                    if (taken != null && taken > 0) dailyData.put(date, taken.intValue());
                }
                for (int col = 0; col < 12; col++) {
                    for (int row = 0; row < 7; row++) {
                        int dayOffset = (col * 7) + row;
                        Calendar dayCal = (Calendar) cal.clone(); dayCal.add(Calendar.DAY_OF_YEAR, -(83 - dayOffset));
                        String dateKey = sdf.format(dayCal.getTime());
                        View square = new View(this);
                        android.widget.GridLayout.LayoutParams params = new android.widget.GridLayout.LayoutParams();
                        params.width = size; params.height = size; params.setMargins(margin, margin, margin, margin);
                        params.rowSpec = android.widget.GridLayout.spec(row); params.columnSpec = android.widget.GridLayout.spec(col, 1f);
                        square.setLayoutParams(params);
                        Integer takenCount = dailyData.get(dateKey);
                        square.setBackgroundColor((takenCount != null && takenCount > 0) ? (takenCount >= 3 ? 0xFF87CEEB : 0xFFA5D6A7) : 0xFF008080);
                        grid.addView(square);
                    }
                }
            });
    }

    @Override protected void onDestroy() { super.onDestroy(); if (executorService != null && !executorService.isShutdown()) executorService.shutdown(); }

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
