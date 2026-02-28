package com.example.chroniccare;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.chroniccare.api.ChatRequest;
import com.example.chroniccare.api.ChatResponse;
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
    private ProgressBar progressBar;
    private EditText messageInput;
    private ScrollView chatScrollView;
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
        
        executorService = Executors.newSingleThreadExecutor();
        
        View root = findViewById(R.id.dr_gpt_root);
        chatContainer = findViewById(R.id.chatContainer);
        messageInput = findViewById(R.id.messageInput);
        chatScrollView = findViewById(R.id.chatScrollView);
        progressBar = findViewById(R.id.progressBar);
        profileImage = findViewById(R.id.profile_image);
        ImageView sendButton = findViewById(R.id.sendButton);
        ImageView btnClearChat = findViewById(R.id.btnClearChat);
        
        if (chatContainer == null || messageInput == null || chatScrollView == null || progressBar == null || sendButton == null) {
            Log.e(TAG, "Failed to initialize views");
            finish();
            return;
        }

        // Advanced keyboard and nav bar management
        if (root != null) {
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime());
                boolean isKeyboardVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
                
                // 1. Hide Nav Bar when keyboard is up
                if (bottomNavigationView != null) {
                    bottomNavigationView.setVisibility(isKeyboardVisible ? View.GONE : View.VISIBLE);
                }

                // 2. Adjust root padding so the input field sits ABOVE the keyboard
                // We add the bottom inset (keyboard height) as padding to the root layout
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), imeInsets.bottom);
                
                if (isKeyboardVisible) {
                    scrollToBottom();
                }
                
                return WindowInsetsCompat.CONSUMED;
            });
        }
        
        if (profileImage != null) {
            ProfileImageHelper.loadProfileImage(this, profileImage);
            profileImage.setOnClickListener(v -> ProfileImageHelper.handleProfileClick(this));
        }
        
        db = com.example.chroniccare.database.AppDatabase.getInstance(this);
        firebaseDb = com.google.firebase.firestore.FirebaseFirestore.getInstance();
        
        SharedPreferences prefs = getSharedPreferences("ChronicCarePrefs", MODE_PRIVATE);
        userId = prefs.getString("userId", null);
        apiService = RetrofitClient.getClient().create(DrGPTApiService.class);
        
        sessionId = prefs.getString("drGptSessionId", null);
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString();
            prefs.edit().putString("drGptSessionId", sessionId).apply();
        }
        
        loadLocalChatHistory();
        
        sendButton.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessage(message);
                messageInput.setText("");
            }
        });
        
        if (btnClearChat != null) {
            btnClearChat.setOnClickListener(v -> showClearChatDialog());
        }
        
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
        sendMessage(message);
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

    private void sendMessageToApi(String payload) {
        com.example.chroniccare.api.ChatRequest request = new com.example.chroniccare.api.ChatRequest(sessionId, payload);

        apiService.sendMessage(request).enqueue(new Callback<com.example.chroniccare.api.ChatResponse>() {
            @Override
            public void onResponse(Call<com.example.chroniccare.api.ChatResponse> call, Response<com.example.chroniccare.api.ChatResponse> response) {
                showLoading(false);
                
                if (response.isSuccessful() && response.body() != null) {
                    String botResponse = response.body().getResponse();
                    if (botResponse != null && !botResponse.isEmpty()) {
                        String safeResponse = applySafetyPostProcessing(botResponse, lastUserMessage);
                        addBotMessage(safeResponse);
                        saveMessageToDb("assistant", safeResponse);
                    }
                } else {
                    handleError("Server error: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<com.example.chroniccare.api.ChatResponse> call, Throwable t) {
                showLoading(false);
                handleError("Connection failed. Please check if the server is running.");
            }
        });
    }

    private void addUserMessage(String message) {
        if (message == null || message.isEmpty()) return;
        View messageView = createMessageView(message, true);
        chatContainer.addView(messageView);
        scrollToBottom();
    }

    private void addBotMessage(String message) {
        if (message == null || message.isEmpty()) return;
        View messageView = createMessageView(message, false);
        chatContainer.addView(messageView);
        scrollToBottom();
    }
    
    private View createMessageView(String message, boolean isUser) {
        TextView textView = new TextView(this);
        String formattedText = parseMarkdown(message);
        textView.setText(android.text.Html.fromHtml(formattedText, android.text.Html.FROM_HTML_MODE_COMPACT));
        textView.setTextSize(14);
        textView.setPadding(32, 24, 32, 24);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 12, 0, 12);
        
        if (isUser) {
            textView.setBackgroundResource(R.drawable.bg_bubble_user);
            textView.setTextColor(Color.BLACK);
            params.gravity = Gravity.END;
            params.setMarginStart(64); // Leave space on the left
        } else {
            textView.setBackgroundResource(R.drawable.bg_bubble_bot);
            textView.setTextColor(Color.parseColor("#1A237E")); // Professional dark blue text
            params.gravity = Gravity.START;
            params.setMarginEnd(64); // Leave space on the right
        }
        
        textView.setLayoutParams(params);
        return textView;
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            if (show) scrollToBottom();
        }
    }

    private void scrollToBottom() {
        if (chatScrollView != null) {
            chatScrollView.post(() -> chatScrollView.fullScroll(ScrollView.FOCUS_DOWN));
        }
    }

    // --- Safety and Context Methods (Remaining existing logic) ---
    private void sendMessageWithMedicalContext(String message) {
        if (userId == null || userId.isEmpty()) {
            sendMessageToApi(message);
            return;
        }

        try {
            com.google.firebase.firestore.DocumentReference personalRef = firebaseDb.collection("users").document(userId).collection("profile").document("personalInfo");
            com.google.firebase.firestore.DocumentReference medicalRef = firebaseDb.collection("users").document(userId).collection("profile").document("medicalInfo");
            com.google.firebase.firestore.DocumentReference emergencyRef = firebaseDb.collection("users").document(userId).collection("profile").document("emergencyContact");
            com.google.firebase.firestore.CollectionReference medsRef = firebaseDb.collection("users").document(userId).collection("medications");

            Tasks.whenAllSuccess(personalRef.get(), medicalRef.get(), emergencyRef.get(), medsRef.get())
                    .addOnSuccessListener(results -> {
                        DocumentSnapshot personalDoc = (DocumentSnapshot) results.get(0);
                        DocumentSnapshot medicalDoc = (DocumentSnapshot) results.get(1);
                        DocumentSnapshot emergencyDoc = (DocumentSnapshot) results.get(2);
                        QuerySnapshot medsSnapshot = (QuerySnapshot) results.get(3);
                        executorService.execute(() -> {
                            User localUser = db.userDao().getUserByUserId(userId);
                            String context = buildUserContext(personalDoc, medicalDoc, emergencyDoc, localUser, medsSnapshot);
                            runOnUiThread(() -> sendMessageToApi(buildMessageWithContext(message, context)));
                        });
                    })
                    .addOnFailureListener(e -> {
                        executorService.execute(() -> {
                            User localUser = db.userDao().getUserByUserId(userId);
                            String context = buildUserContext(null, null, null, localUser, null);
                            runOnUiThread(() -> sendMessageToApi(buildMessageWithContext(message, context)));
                        });
                    });
        } catch (Exception e) {
            sendMessageToApi(message);
        }
    }

    private String buildMessageWithContext(String message, String context) {
        String safetyRules = buildSafetyRules();
        if (context == null || context.isEmpty()) return safetyRules + "\n\nUser question:\n" + message;
        return safetyRules + "\n\nUser profile summary:\n" + context + "\n\nUser question:\n" + message;
    }

    private String buildUserContext(DocumentSnapshot p, DocumentSnapshot m, DocumentSnapshot e, User u, QuerySnapshot ms) {
        StringBuilder builder = new StringBuilder();
        appendSection(builder, "Personal");
        appendContextLine(builder, "Name", getValue(p, "name", u != null ? u.name : null));
        appendContextLine(builder, "Conditions", getValue(m, "conditions", u != null ? u.conditions : null));
        appendSection(builder, "Medications");
        String medSummary = buildMedicationSummary(ms);
        if (medSummary != null && !medSummary.isEmpty()) builder.append(medSummary);
        return builder.toString().trim();
    }

    private String getValue(DocumentSnapshot doc, String key, String fallback) {
        String value = doc != null ? doc.getString(key) : null;
        return (value == null || value.trim().isEmpty()) ? fallback : value;
    }

    private void appendContextLine(StringBuilder b, String l, String v) {
        if (v != null && !v.trim().isEmpty()) b.append("\n").append(l).append(": ").append(v.trim());
    }

    private void appendSection(StringBuilder b, String t) {
        if (b.length() > 0) b.append("\n\n");
        b.append(t).append(":");
    }

    private String buildMedicationSummary(QuerySnapshot medsSnapshot) {
        if (medsSnapshot == null || medsSnapshot.isEmpty()) return "";
        StringBuilder builder = new StringBuilder();
        for (DocumentSnapshot doc : medsSnapshot.getDocuments()) {
            String name = doc.getString("name");
            if (name != null) builder.append("\n- ").append(name.trim());
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
        return false;
    }

    private String applySafetyPostProcessing(String response, String userMessage) {
        return response + "\n\nNote: This is general information, not a medical diagnosis. Consult a doctor for serious concerns.";
    }

    private String buildSafetyRules() {
        return "1) Never diagnosis. 2) India emergency 112. 3) No prescription drug dosing.";
    }

    private boolean isEmergencyMessage(String lower) {
        return lower.contains("chest pain") || lower.contains("difficulty breathing") || lower.contains("emergency");
    }

    private String buildEmergencyResponse() {
        return "⚠️ Possible medical emergency. Please call 112 immediately.";
    }

    private String parseMarkdown(String text) {
        if (text == null) return "";
        return text.replaceAll("\\*\\*(.+?)\\*\\*", "<b>$1</b>").replaceAll("\\n", "<br>");
    }

    private void handleError(String error) {
        runOnUiThread(() -> {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            addBotMessage("Sorry, I encountered an error. Please try again.");
        });
    }

    private void loadLocalChatHistory() {
        if (executorService == null || executorService.isShutdown()) return;
        executorService.execute(() -> {
            try {
                List<com.example.chroniccare.database.ChatMessage> messages = db.chatMessageDao().getMessagesBySession(sessionId);
                runOnUiThread(() -> {
                    if (messages == null || messages.isEmpty()) {
                        addBotMessage("Hello! I'm Dr.GPT, your health assistant. How can I help you today?");
                    } else {
                        for (com.example.chroniccare.database.ChatMessage msg : messages) {
                            if ("user".equals(msg.role)) addUserMessage(msg.content);
                            else addBotMessage(msg.content);
                        }
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> addBotMessage("Hello! I'm Dr.GPT."));
            }
        });
    }
    
    private void saveMessageToDb(String role, String content) {
        if (content == null || content.isEmpty()) return;
        executorService.execute(() -> {
            try {
                com.example.chroniccare.database.ChatMessage message = new com.example.chroniccare.database.ChatMessage();
                message.sessionId = sessionId;
                message.role = role;
                message.content = content;
                message.timestamp = System.currentTimeMillis();
                db.chatMessageDao().insert(message);
            } catch (Exception e) {}
        });
    }
    
    private void showClearChatDialog() {
        new android.app.AlertDialog.Builder(this)
            .setTitle("Clear Chat")
            .setMessage("Continue?")
            .setPositiveButton("Clear", (dialog, which) -> {
                executorService.execute(() -> {
                    db.chatMessageDao().deleteSession(sessionId);
                    runOnUiThread(() -> {
                        chatContainer.removeAllViews();
                        sessionId = UUID.randomUUID().toString();
                        addBotMessage("Hello! I'm Dr.GPT.");
                    });
                });
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) executorService.shutdown();
    }

    @Override
    protected int getLayoutId() { return R.layout.activity_dr_gptactivity; }
    @Override
    protected int getBottomNavMenuItemId() { return R.id.nav_dr_gpt; }
}
