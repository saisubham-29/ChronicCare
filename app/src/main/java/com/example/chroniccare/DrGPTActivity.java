package com.example.chroniccare;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.chroniccare.api.ChatRequest;
import com.example.chroniccare.api.ChatResponse;
import com.example.chroniccare.api.ChatHistoryResponse;
import com.example.chroniccare.api.StatusResponse;
import com.example.chroniccare.api.DrGPTApiService;
import com.example.chroniccare.api.RetrofitClient;
import com.example.chroniccare.database.User;
import com.example.chroniccare.utils.ProfileImageHelper;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DrGPTActivity extends BottomNavActivity {
    
    private static final String TAG = "DrGPTActivity";
    private LinearLayout chatContainer;
    private EditText messageInput;
    private ScrollView chatScrollView;
    private ProgressBar progressBar;
    private CircleImageView profileImage;
    private com.example.chroniccare.database.AppDatabase db;
    private com.google.firebase.firestore.FirebaseFirestore firebaseDb;
    private String sessionId;
    private String userId;
    private DrGPTApiService apiService;
    private ExecutorService executorService;
    private String lastUserMessage;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize executor
        executorService = Executors.newSingleThreadExecutor();
        
        chatContainer = findViewById(R.id.chatContainer);
        messageInput = findViewById(R.id.messageInput);
        chatScrollView = findViewById(R.id.chatScrollView);
        progressBar = findViewById(R.id.progressBar);
        profileImage = findViewById(R.id.profile_image);
        ImageView sendButton = findViewById(R.id.sendButton);
        ImageView btnClearChat = findViewById(R.id.btnClearChat);
        
        // Null checks
        if (chatContainer == null || messageInput == null || chatScrollView == null || progressBar == null || sendButton == null) {
            Log.e(TAG, "Failed to initialize views");
            finish();
            return;
        }
        
        // Load profile image
        if (profileImage != null) {
            ProfileImageHelper.loadProfileImage(this, profileImage);
            profileImage.setOnClickListener(v -> ProfileImageHelper.handleProfileClick(this));
        }
        
        // Initialize database
        db = com.example.chroniccare.database.AppDatabase.getInstance(this);
        
        // Initialize Firebase Firestore
        firebaseDb = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        
        // Get user ID from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("ChronicCarePrefs", MODE_PRIVATE);
        userId = prefs.getString("userId", null);
        
        // Initialize API service
        apiService = RetrofitClient.getClient().create(DrGPTApiService.class);
        
        // Get or create session ID
        sessionId = prefs.getString("drGptSessionId", null);
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString();
            prefs.edit().putString("drGptSessionId", sessionId).apply();
            Log.d(TAG, "Created new session: " + sessionId);
        } else {
            Log.d(TAG, "Using existing session: " + sessionId);
        }
        
        loadLocalChatHistory();
        
        sendButton.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessage(message);
                messageInput.setText("");
            }
        });
        
        // Clear chat button
        if (btnClearChat != null) {
            btnClearChat.setOnClickListener(v -> showClearChatDialog());
        }
        
        // Preset question buttons
        setupPresetButtons();
    }
    
    private void setupPresetButtons() {
        findViewById(R.id.btnMissedDose).setOnClickListener(v -> 
            sendPresetMessage("I missed my medication dose. What should I do?"));
        
        findViewById(R.id.btnFeelingDizzy).setOnClickListener(v -> 
            sendPresetMessage("I'm feeling dizzy. What could be the reason and what should I do?"));
        
        findViewById(R.id.btnHighSugar).setOnClickListener(v -> 
            sendPresetMessage("My blood sugar level is high. What steps should I take?"));
        
        findViewById(R.id.btnLowSugar).setOnClickListener(v -> 
            sendPresetMessage("My blood sugar level is low. What should I do immediately?"));
        
        findViewById(R.id.btnFeelingTired).setOnClickListener(v -> 
            sendPresetMessage("I'm feeling unusually tired. What could be causing this?"));
    }
    
    private void sendPresetMessage(String message) {
        messageInput.setText(message);
        sendMessage(message);
        messageInput.setText("");
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (profileImage != null) {
            ProfileImageHelper.loadProfileImage(this, profileImage);
        }
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_dr_gptactivity;
    }

    @Override
    protected int getBottomNavMenuItemId() {
        return R.id.nav_dr_gpt;
    }

    private void addWelcomeMessage() {
        addBotMessage("Hello! I'm Dr.GPT, your health assistant. How can I help you today?");
    }

    private void sendMessage(String message) {
        addUserMessage(message);
        saveMessageToDb("user", message);
        lastUserMessage = message;

        if (handleSafetyMessage(message)) {
            showLoading(false);
            return;
        }

        showLoading(true);
        sendMessageWithMedicalContext(message);
    }

    private void sendMessageWithMedicalContext(String message) {
        if (userId == null || userId.isEmpty()) {
            sendMessageToApi(message);
            return;
        }

        try {
            com.google.firebase.firestore.DocumentReference personalRef = firebaseDb
                    .collection("users").document(userId)
                    .collection("profile").document("personalInfo");
            com.google.firebase.firestore.DocumentReference medicalRef = firebaseDb
                    .collection("users").document(userId)
                    .collection("profile").document("medicalInfo");
            com.google.firebase.firestore.DocumentReference emergencyRef = firebaseDb
                    .collection("users").document(userId)
                    .collection("profile").document("emergencyContact");
            com.google.firebase.firestore.CollectionReference medsRef = firebaseDb
                    .collection("users").document(userId)
                    .collection("medications");

            Tasks.whenAllSuccess(personalRef.get(), medicalRef.get(), emergencyRef.get(), medsRef.get())
                    .addOnSuccessListener(results -> {
                        DocumentSnapshot personalDoc = (DocumentSnapshot) results.get(0);
                        DocumentSnapshot medicalDoc = (DocumentSnapshot) results.get(1);
                        DocumentSnapshot emergencyDoc = (DocumentSnapshot) results.get(2);
                        QuerySnapshot medsSnapshot = (QuerySnapshot) results.get(3);
                        User localUser = db.userDao().getUserByUserId(userId);
                        String context = buildUserContext(personalDoc, medicalDoc, emergencyDoc, localUser, medsSnapshot);
                        sendMessageToApi(buildMessageWithContext(message, context));
                    })
                    .addOnFailureListener(e -> {
                        User localUser = db.userDao().getUserByUserId(userId);
                        String context = buildUserContext(null, null, null, localUser, null);
                        sendMessageToApi(buildMessageWithContext(message, context));
                    });
        } catch (Exception e) {
            Log.e(TAG, "Failed to load medical context", e);
            sendMessageToApi(message);
        }
    }

    private void sendMessageToApi(String payload) {
        ChatRequest request = new ChatRequest(sessionId, payload);

        apiService.sendMessage(request).enqueue(new Callback<ChatResponse>() {
            @Override
            public void onResponse(Call<ChatResponse> call, Response<ChatResponse> response) {
                showLoading(false);
                
                if (response.isSuccessful() && response.body() != null) {
                    String botResponse = response.body().getResponse();
                    if (botResponse != null && !botResponse.isEmpty()) {
                        String safeResponse = applySafetyPostProcessing(botResponse, lastUserMessage);
                        addBotMessage(safeResponse);
                        saveMessageToDb("assistant", safeResponse);
                    }
                    Log.d(TAG, "API Response: " + botResponse);
                } else {
                    handleError("Server error: " + response.code());
                    Log.e(TAG, "API Error: " + response.code() + " - " + response.message());
                }
            }
            
            @Override
            public void onFailure(Call<ChatResponse> call, Throwable t) {
                showLoading(false);
                handleError("Connection failed. Please check if the server is running.");
                Log.e(TAG, "API Failure: " + t.getMessage(), t);
            }
        });
    }

    private String buildMessageWithContext(String message, String context) {
        String safetyRules = buildSafetyRules();
        if (context == null || context.isEmpty()) {
            return safetyRules + "\n\nUser question:\n" + message;
        }
        return safetyRules + "\n\nUser profile summary:\n" + context + "\n\nUser question:\n" + message;
    }

    private String buildUserContext(
            DocumentSnapshot personalDoc,
            DocumentSnapshot medicalDoc,
            DocumentSnapshot emergencyDoc,
            User localUser,
            QuerySnapshot medsSnapshot
    ) {
        String name = getValue(personalDoc, "name", localUser != null ? localUser.name : null);
        String email = getValue(personalDoc, "email", localUser != null ? localUser.email : null);
        String phone = getValue(personalDoc, "phone", localUser != null ? localUser.phone : null);
        String dob = getValue(personalDoc, "dob", localUser != null ? localUser.dob : null);
        String gender = getValue(personalDoc, "gender", localUser != null ? localUser.gender : null);
        String bloodGroup = getValue(personalDoc, "bloodGroup", localUser != null ? localUser.bloodGroup : null);
        String height = getValue(medicalDoc, "height", localUser != null ? localUser.height : null);
        String weight = getValue(medicalDoc, "weight", localUser != null ? localUser.weight : null);
        String conditions = getValue(medicalDoc, "conditions", localUser != null ? localUser.conditions : null);
        String allergies = getValue(medicalDoc, "allergies", localUser != null ? localUser.allergies : null);
        String emergencyName = getValue(emergencyDoc, "name", localUser != null ? localUser.emergencyName : null);
        String emergencyPhone = getValue(emergencyDoc, "phone", localUser != null ? localUser.emergencyPhone : null);
        String emergencyRelation = getValue(emergencyDoc, "relation", localUser != null ? localUser.emergencyRelation : null);

        StringBuilder builder = new StringBuilder();
        appendSection(builder, "Personal");
        appendContextLine(builder, "Name", name);
        appendContextLine(builder, "Email", email);
        appendContextLine(builder, "Phone", phone);
        appendContextLine(builder, "DOB", dob);
        appendContextLine(builder, "Gender", gender);
        appendContextLine(builder, "Blood Group", bloodGroup);

        appendSection(builder, "Medical");
        appendContextLine(builder, "Height", height);
        appendContextLine(builder, "Weight", weight);
        appendContextLine(builder, "Conditions", conditions);
        appendContextLine(builder, "Allergies", allergies);

        appendSection(builder, "Emergency Contact");
        appendContextLine(builder, "Name", emergencyName);
        appendContextLine(builder, "Phone", emergencyPhone);
        appendContextLine(builder, "Relation", emergencyRelation);

        appendSection(builder, "Medications");
        String medicationSummary = buildMedicationSummary(medsSnapshot);
        if (medicationSummary == null || medicationSummary.isEmpty()) {
            appendContextLine(builder, "Current", "No medications found");
        } else {
            builder.append(medicationSummary);
        }

        return builder.toString().trim();
    }

    private String getValue(DocumentSnapshot doc, String key, String fallback) {
        String value = doc != null ? doc.getString(key) : null;
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value;
    }

    private void appendContextLine(StringBuilder builder, String label, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append("\n");
        }
        builder.append(label).append(": ").append(value.trim());
    }

    private void appendSection(StringBuilder builder, String title) {
        if (builder.length() > 0) {
            builder.append("\n\n");
        }
        builder.append(title).append(":");
    }

    private String buildMedicationSummary(QuerySnapshot medsSnapshot) {
        if (medsSnapshot == null || medsSnapshot.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        int index = 1;
        for (DocumentSnapshot doc : medsSnapshot.getDocuments()) {
            String name = doc.getString("name");
            String time = doc.getString("time");
            String mealTime = doc.getString("mealTime");
            String period = doc.getString("period");
            Boolean taken = doc.getBoolean("taken");

            if (name == null || name.trim().isEmpty()) {
                continue;
            }

            builder.append("\n")
                    .append(index++)
                    .append(". ")
                    .append(name.trim());

            if (time != null && !time.trim().isEmpty()) {
                builder.append(" at ").append(time.trim());
            }

            if (period != null && !period.trim().isEmpty()) {
                builder.append(" (").append(period.trim()).append(")");
            }

            if (mealTime != null && !mealTime.trim().isEmpty()) {
                builder.append(" ").append(mealTime.trim());
            }

            builder.append(" - ");
            builder.append(Boolean.TRUE.equals(taken) ? "Taken" : "Pending");
        }

        return builder.toString().trim();
    }

    private boolean handleSafetyMessage(String message) {
        String lower = message != null ? message.toLowerCase(Locale.getDefault()) : "";
        if (isEmergencyMessage(lower)) {
            addBotMessage(buildEmergencyResponse());
            saveMessageToDb("assistant", buildEmergencyResponse());
            return true;
        }
        if (isSelfHarmMessage(lower)) {
            addBotMessage(buildSelfHarmResponse());
            saveMessageToDb("assistant", buildSelfHarmResponse());
            return true;
        }
        return false;
    }

    private String applySafetyPostProcessing(String response, String userMessage) {
        String safeResponse = response;
        boolean medicalQuery = isMedicalQuery(userMessage);
        boolean medicationQuery = isMedicationQuery(userMessage) || containsMedicationHints(response);

        if (medicationQuery) {
            safeResponse = redactDosing(safeResponse);
            safeResponse = safeResponse + "\n\nNeeds doctor's review: This involves medication. Please consult a qualified doctor before making changes.";
        }

        if (medicalQuery || medicationQuery) {
            safeResponse = safeResponse + "\n\nI'm not a doctor. This is general information, not a diagnosis. Consider consulting a healthcare professional.";
            if (mentionsSensitiveGroups(userMessage)) {
                safeResponse = safeResponse + "\nBe extra cautious for children, pregnancy, or older adults—seek medical advice.";
            }
        }

        if (containsOutOfScopeIndicators(safeResponse)) {
            safeResponse = safeResponse + "\nIf you want, share more details and I can try to help, or you can consult a doctor.";
        }

        return safeResponse.trim();
    }

    private String buildSafetyRules() {
        return "Safety rules (must follow):" +
                "\n1) Never diagnose or impersonate a doctor. Use cautious language (e.g., 'this could be')." +
                "\n2) If emergency symptoms are mentioned, stop normal response and advise emergency care. Provide India emergency number 112. No home remedies." +
                "\n3) Never prescribe prescription drugs or give exact dosing. If medication is discussed, recommend doctor review." +
                "\n4) If uncertain or info is missing, say 'I don't know' and ask for clarification or suggest a doctor." +
                "\n5) For self-harm signals, respond empathetically and provide India helpline 1800-599-0019 and 112 for immediate danger." +
                "\n6) Answer only using verified medical sources. If not found, say you don't have enough information." +
                "\n7) Do not expose sensitive user info unless necessary. Ask for consent if needed." +
                "\n8) Always offer escalation to a medical professional for serious concerns.";
    }

    private boolean isEmergencyMessage(String lower) {
        return containsAny(lower,
                "chest pain",
                "pressure in chest",
                "shortness of breath",
                "difficulty breathing",
                "not breathing",
                "severe bleeding",
                "unconscious",
                "passed out",
                "fainting",
                "seizure",
                "stroke",
                "slurred speech",
                "face drooping",
                "sudden weakness",
                "severe allergic reaction",
                "anaphylaxis",
                "severe burn",
                "emergency",
                "ambulance",
                "heart attack"
        );
    }

    private boolean isSelfHarmMessage(String lower) {
        return containsAny(lower,
                "suicide",
                "kill myself",
                "end my life",
                "self harm",
                "hurt myself",
                "want to die"
        );
    }

    private boolean isMedicationQuery(String message) {
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase(Locale.getDefault());
        return containsAny(lower,
                "medication",
                "medicine",
                "drug",
                "tablet",
                "pill",
                "dose",
                "dosage",
                "mg",
                "ml",
                "antibiotic",
                "insulin",
                "metformin",
                "paracetamol",
                "ibuprofen",
                "prescription"
        );
    }

    private boolean containsMedicationHints(String response) {
        if (response == null) {
            return false;
        }
        String lower = response.toLowerCase(Locale.getDefault());
        return containsAny(lower,
                "mg",
                "ml",
                "tablet",
                "pill",
                "dose",
                "dosage",
                "medication",
                "medicine",
                "drug"
        );
    }

    private boolean isMedicalQuery(String message) {
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase(Locale.getDefault());
        return containsAny(lower,
                "symptom",
                "pain",
                "fever",
                "cough",
                "cold",
                "dizzy",
                "nausea",
                "vomit",
                "headache",
                "rash",
                "infection",
                "diabetes",
                "blood pressure",
                "sugar",
                "heart",
                "breath",
                "medication",
                "medicine",
                "drug"
        );
    }

    private boolean mentionsSensitiveGroups(String message) {
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase(Locale.getDefault());
        return containsAny(lower,
                "pregnant",
                "pregnancy",
                "child",
                "baby",
                "infant",
                "elderly",
                "senior"
        );
    }

    private boolean containsOutOfScopeIndicators(String response) {
        if (response == null) {
            return false;
        }
        String lower = response.toLowerCase(Locale.getDefault());
        return containsAny(lower,
                "i don't know",
                "not sure",
                "cannot determine",
                "insufficient information"
        );
    }

    private String redactDosing(String response) {
        if (response == null) {
            return "";
        }
        return response.replaceAll("\\b\\d+(?:\\.\\d+)?\\s*(mg|ml|mcg|g)\\b", "[dose omitted]");
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String buildEmergencyResponse() {
        return "⚠️ This could be a medical emergency. Please seek urgent care now.\n"
                + "Call emergency services in India: 112.\n"
                + "I can't provide medical instructions for emergencies. Please get help immediately.";
    }

    private String buildSelfHarmResponse() {
        return "I'm really sorry you're feeling this way. You deserve support, and help is available.\n"
                + "If you're in India, you can call the mental health helpline: 1800-599-0019.\n"
                + "If you feel in immediate danger, please call emergency services: 112.\n"
                + "I'm not a doctor, but I care about your safety—please reach out to someone you trust or a professional.";
    }

    private void addUserMessage(String message) {
        if (message == null || message.isEmpty()) return;
        TextView textView = createMessageView(message, true);
        chatContainer.addView(textView);
        scrollToBottom();
    }

    private void addBotMessage(String message) {
        if (message == null || message.isEmpty()) return;
        TextView textView = createMessageView(message, false);
        chatContainer.addView(textView);
        scrollToBottom();
    }
    
    private TextView createMessageView(String message, boolean isUser) {
        TextView textView = new TextView(this);
        
        // Parse markdown formatting
        String formattedText = parseMarkdown(message);
        textView.setText(android.text.Html.fromHtml(formattedText, android.text.Html.FROM_HTML_MODE_COMPACT));
        
        textView.setTextSize(14);
        textView.setPadding(24, 16, 24, 16);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 8, 0, 8);
        
        if (isUser) {
            textView.setBackgroundColor(Color.parseColor("#26A69A"));
            textView.setTextColor(Color.WHITE);
            params.gravity = Gravity.END;
        } else {
            textView.setBackgroundColor(Color.parseColor("#F5F5F5"));
            textView.setTextColor(Color.BLACK);
            params.gravity = Gravity.START;
        }
        
        textView.setLayoutParams(params);
        return textView;
    }
    
    private String parseMarkdown(String text) {
        if (text == null) return "";
        
        // Bold: **text** or __text__ -> <b>text</b>
        text = text.replaceAll("\\*\\*(.+?)\\*\\*", "<b>$1</b>");
        text = text.replaceAll("__(.+?)__", "<b>$1</b>");
        
        // Italic: *text* or _text_ -> <i>text</i>
        text = text.replaceAll("\\*(.+?)\\*", "<i>$1</i>");
        text = text.replaceAll("_(.+?)_", "<i>$1</i>");
        
        // Code: `text` -> <tt>text</tt>
        text = text.replaceAll("`(.+?)`", "<tt>$1</tt>");
        
        // Line breaks
        text = text.replaceAll("\\n", "<br>");
        
        return text;
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? android.view.View.VISIBLE : android.view.View.GONE);
        }
    }

    private void handleError(String error) {
        runOnUiThread(() -> {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            addBotMessage("Sorry, I encountered an error. Please try again.");
        });
    }

    private void scrollToBottom() {
        if (chatScrollView != null) {
            chatScrollView.post(() -> chatScrollView.fullScroll(ScrollView.FOCUS_DOWN));
        }
    }
    
    private void loadLocalChatHistory() {
        if (executorService == null || executorService.isShutdown()) return;
        
        executorService.execute(() -> {
            try {
                List<com.example.chroniccare.database.ChatMessage> messages = db.chatMessageDao().getMessagesBySession(sessionId);
                runOnUiThread(() -> {
                    if (messages == null || messages.isEmpty()) {
                        addWelcomeMessage();
                    } else {
                        for (com.example.chroniccare.database.ChatMessage msg : messages) {
                            if ("user".equals(msg.role)) {
                                addUserMessage(msg.content);
                            } else {
                                addBotMessage(msg.content);
                            }
                        }
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading chat history: " + e.getMessage(), e);
                runOnUiThread(this::addWelcomeMessage);
            }
        });
    }
    
    private void saveMessageToDb(String role, String content) {
        if (content == null || content.isEmpty()) return;
        
        // Save to local Room database
        if (executorService != null && !executorService.isShutdown()) {
            executorService.execute(() -> {
                try {
                    com.example.chroniccare.database.ChatMessage message = new com.example.chroniccare.database.ChatMessage();
                    message.sessionId = sessionId;
                    message.role = role;
                    message.content = content;
                    message.timestamp = System.currentTimeMillis();
                    db.chatMessageDao().insert(message);
                } catch (Exception e) {
                    Log.e(TAG, "Error saving to local DB: " + e.getMessage(), e);
                }
            });
        }
        
        // Save to Firebase
        if (userId != null && !userId.isEmpty()) {
            saveMessageToFirebase(role, content);
        }
    }
    
    private void saveMessageToFirebase(String role, String content) {
        try {
            java.util.HashMap<String, Object> messageData = new java.util.HashMap<>();
            messageData.put("role", role);
            messageData.put("content", content);
            messageData.put("timestamp", System.currentTimeMillis());
            
            firebaseDb.collection("users").document(userId)
                .collection("drGptChats").document(sessionId)
                .collection("messages").add(messageData)
                .addOnSuccessListener(documentReference -> Log.d(TAG, "✅ Message saved to Firestore"))
                .addOnFailureListener(e -> Log.e(TAG, "❌ Failed to save to Firestore: " + e.getMessage()));
        } catch (Exception e) {
            Log.e(TAG, "Error saving to Firestore: " + e.getMessage(), e);
        }
    }
    
    private void clearLocalSession() {
        // Clear local database
        if (executorService != null && !executorService.isShutdown()) {
            executorService.execute(() -> {
                try {
                    db.chatMessageDao().deleteSession(sessionId);
                    runOnUiThread(() -> {
                        if (chatContainer != null) {
                            chatContainer.removeAllViews();
                            addWelcomeMessage();
                        }
                        Toast.makeText(DrGPTActivity.this, "Chat cleared", Toast.LENGTH_SHORT).show();
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error clearing local session: " + e.getMessage(), e);
                }
            });
        }
        
        // Clear Firestore
        if (userId != null && !userId.isEmpty()) {
            try {
                firebaseDb.collection("users").document(userId)
                    .collection("drGptChats").document(sessionId)
                    .collection("messages").get()
                    .addOnSuccessListener(querySnapshot -> {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            doc.getReference().delete();
                        }
                        Log.d(TAG, "✅ Firestore chat cleared");
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "❌ Failed to clear Firestore: " + e.getMessage()));
            } catch (Exception e) {
                Log.e(TAG, "Error clearing Firestore: " + e.getMessage(), e);
            }
        }
    }
    
    private void showClearChatDialog() {
        new android.app.AlertDialog.Builder(this)
            .setTitle("Clear Chat")
            .setMessage("This will clear all chat history and start a new session. Continue?")
            .setPositiveButton("Clear", (dialog, which) -> {
                clearLocalSession();
                // Create new session
                sessionId = UUID.randomUUID().toString();
                getSharedPreferences("ChronicCarePrefs", MODE_PRIVATE)
                    .edit()
                    .putString("drGptSessionId", sessionId)
                    .apply();
                Log.d(TAG, "New session created: " + sessionId);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}
