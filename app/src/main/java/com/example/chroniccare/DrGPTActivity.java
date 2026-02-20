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
import com.example.chroniccare.utils.ProfileImageHelper;

import java.util.List;
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
            profileImage.setOnClickListener(v -> 
                startActivity(new Intent(this, ProfileActivity.class))
            );
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
        showLoading(true);
        
        ChatRequest request = new ChatRequest(sessionId, message);
        
        apiService.sendMessage(request).enqueue(new Callback<ChatResponse>() {
            @Override
            public void onResponse(Call<ChatResponse> call, Response<ChatResponse> response) {
                showLoading(false);
                
                if (response.isSuccessful() && response.body() != null) {
                    String botResponse = response.body().getResponse();
                    if (botResponse != null && !botResponse.isEmpty()) {
                        addBotMessage(botResponse);
                        saveMessageToDb("assistant", botResponse);
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
