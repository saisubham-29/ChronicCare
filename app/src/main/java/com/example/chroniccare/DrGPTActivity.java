package com.example.chroniccare;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class DrGPTActivity extends BottomNavActivity {
    
    private LinearLayout chatContainer;
    private EditText messageInput;
    private ScrollView chatScrollView;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        chatContainer = findViewById(R.id.chatContainer);
        messageInput = findViewById(R.id.messageInput);
        chatScrollView = findViewById(R.id.chatScrollView);
        ImageView sendButton = findViewById(R.id.sendButton);
        
        addWelcomeMessage();
        
        sendButton.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty()) {
                addUserMessage(message);
                messageInput.setText("");
                simulateBotResponse(message);
            }
        });
    }
    
    private void addWelcomeMessage() {
        addBotMessage("Hello! I'm Dr.GPT, your AI health assistant. I can help you understand medical reports, medications, and general health information.\n\nImportant: I don't replace a doctor. Always consult healthcare professionals for medical decisions.");
    }
    
    private void addUserMessage(String message) {
        TextView textView = createMessageView(message, true);
        chatContainer.addView(textView);
        scrollToBottom();
    }
    
    private void addBotMessage(String message) {
        TextView textView = createMessageView(message, false);
        chatContainer.addView(textView);
        scrollToBottom();
    }
    
    private TextView createMessageView(String message, boolean isUser) {
        TextView textView = new TextView(this);
        textView.setText(message);
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
    
    private void simulateBotResponse(String userMessage) {
        chatScrollView.postDelayed(() -> {
            String response = generateResponse(userMessage);
            addBotMessage(response);
        }, 1000);
    }
    
    private String generateResponse(String message) {
        String lower = message.toLowerCase();
        
        if (lower.contains("blood sugar") || lower.contains("glucose") || lower.contains("diabetes")) {
            return "Blood sugar monitoring is important for diabetes management. Normal fasting levels are 70-100 mg/dL. Always consult your doctor for personalized targets and treatment adjustments.";
        } else if (lower.contains("medication") || lower.contains("medicine") || lower.contains("drug")) {
            return "Always take medications as prescribed by your doctor. Never stop or change doses without consulting them. If you experience side effects, contact your healthcare provider immediately.";
        } else if (lower.contains("pressure") || lower.contains("bp") || lower.contains("hypertension")) {
            return "Normal blood pressure is around 120/80 mmHg. High blood pressure requires medical attention. Lifestyle changes like diet, exercise, and stress management can help, but follow your doctor's treatment plan.";
        } else if (lower.contains("exercise") || lower.contains("workout")) {
            return "Regular exercise is beneficial for chronic disease management. Start slowly and consult your doctor before beginning any new exercise program, especially if you have heart or joint conditions.";
        } else if (lower.contains("diet") || lower.contains("food") || lower.contains("eat")) {
            return "A balanced diet is crucial for managing chronic conditions. Focus on whole foods, vegetables, lean proteins, and limit processed foods. Consider consulting a dietitian for personalized meal plans.";
        } else {
            return "I can help with general health information, but for specific medical advice, diagnosis, or treatment, please consult your healthcare provider. What would you like to know about?";
        }
    }
    
    private void scrollToBottom() {
        chatScrollView.post(() -> chatScrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_dr_gptactivity;
    }

    @Override
    protected int getBottomNavMenuItemId() {
        return R.id.nav_dr_gpt;
    }
}
