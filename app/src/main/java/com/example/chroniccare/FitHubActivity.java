package com.example.chroniccare;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;

import com.example.chroniccare.database.AppDatabase;
import com.example.chroniccare.database.User;
import com.example.chroniccare.model.Article;
import com.example.chroniccare.utils.ArticleProvider;
import com.example.chroniccare.utils.ProfileImageHelper;
import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.concurrent.Executors;

import de.hdodenhof.circleimageview.CircleImageView;

public class FitHubActivity extends BottomNavActivity {
    
    private CircleImageView profileImage;
    private LinearLayout articlesContainer;
    private CardView logFoodCard, logExerciseCard;
    private AppDatabase db;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        profileImage = findViewById(R.id.profile_image);
        articlesContainer = findViewById(R.id.articlesContainer);
        logFoodCard = findViewById(R.id.logFoodCard);
        logExerciseCard = findViewById(R.id.logExerciseCard);
        
        ProfileImageHelper.loadProfileImage(this, profileImage);
        profileImage.setOnClickListener(v -> ProfileImageHelper.handleProfileClick(this));
        
        if (logFoodCard != null) {
            logFoodCard.setOnClickListener(v -> startActivity(new Intent(this, LogFood.class)));
        }
        
        if (logExerciseCard != null) {
            logExerciseCard.setOnClickListener(v -> startActivity(new Intent(this, LogExercise.class)));
        }
        
        setupVideoClickListeners();
        
        db = AppDatabase.getInstance(this);
        loadPersonalizedArticles();
    }
    
    private void setupVideoClickListeners() {
        // Video 1: MadFit - No Jumping Cardio
        setupVideoCard(R.id.video1, "vCq-qy1v6Bc", "No Jumping Cardio", "10 min");
        
        // Video 2: Alex Crockford - Daily Workout
        setupVideoCard(R.id.video2, "Sr8aCh3SNHQ", "Daily Workout", "10 min");
        
        // Video 3: Oliver Sjostrom - HIIT
        setupVideoCard(R.id.video3, "PwXUHMKamP8", "Intense HIIT", "10 min");
        
        // Video 4: BullyJuice - Full Body
        setupVideoCard(R.id.video4, "aV-vgqCQFbU", "Full Body Workout", "10 min");
        
        // Video 5: MadFit - Beginner Full Body
        setupVideoCard(R.id.video5, "QbmPxLWmWr8", "Beginner Full Body", "10 min");
        
        // Video 6: MadFit - AB Workout
        setupVideoCard(R.id.video6, "UTRsLReOKzg", "AB Workout", "10 min");
        
        // Video 7: Oliver Sjostrom - Cardio HIIT
        setupVideoCard(R.id.video7, "yGMPQliSBCo", "Cardio HIIT", "10 min");
        
        // Video 8: Alex Crockford - Playlist
        View video8 = findViewById(R.id.video8);
        if (video8 != null) {
            TextView title8 = video8.findViewById(R.id.videoTitle);
            TextView duration8 = video8.findViewById(R.id.videoDuration);
            ImageView thumb8 = video8.findViewById(R.id.videoThumbnail);

            if (title8 != null && duration8 != null && thumb8 != null) {
                title8.setText("Daily Workout Series");
                duration8.setText("Playlist");
                Picasso.get()
                    .load("https://img.youtube.com/vi/Sr8aCh3SNHQ/maxresdefault.jpg")
                    .placeholder(R.drawable.img_exercise)
                    .into(thumb8);
                video8.setOnClickListener(v -> openUrl("https://www.youtube.com/playlist?list=PLBU6uF21RTAAgyoH2InY3GENbAsMpTPgO"));
            }
        }
        
        // Video 9: General Daily Workout
        setupVideoCard(R.id.video9, "-nCVxDwanEM", "Daily Workout", "10 min");
        
        // Video 10: Lower Body Stretch
        setupVideoCard(R.id.video10, "CKnlEt5n3Sk", "Lower Body Stretch", "10 min");
    }
    
    private void setupVideoCard(int cardId, String videoId, String title, String duration) {
        View card = findViewById(cardId);
        if (card == null) {
            return;
        }
        TextView titleView = card.findViewById(R.id.videoTitle);
        TextView durationView = card.findViewById(R.id.videoDuration);
        ImageView thumbnail = card.findViewById(R.id.videoThumbnail);
        if (titleView == null || durationView == null || thumbnail == null) {
            return;
        }
        
        titleView.setText(title);
        durationView.setText(duration);
        
        // Load YouTube thumbnail
        String thumbnailUrl = "https://img.youtube.com/vi/" + videoId + "/maxresdefault.jpg";
        Picasso.get()
            .load(thumbnailUrl)
            .placeholder(R.drawable.img_exercise)
            .error(R.drawable.img_exercise)
            .into(thumbnail);
        
        card.setOnClickListener(v -> openYouTubeVideo(videoId));
    }
    
    private void loadPersonalizedArticles() {
        Executors.newSingleThreadExecutor().execute(() -> {
            SharedPreferences prefs = getSharedPreferences("ChronicCarePrefs", MODE_PRIVATE);
            String userId = prefs.getString("userId", null);
            
            String userConditions = "";
            if (userId != null) {
                User user = db.userDao().getUserByUserId(userId);
                if (user != null && user.conditions != null) {
                    userConditions = user.conditions;
                }
            }
            
            List<Article> articles = ArticleProvider.getPersonalizedArticles(userConditions);
            
            runOnUiThread(() -> {
                articlesContainer.removeAllViews();
                for (Article article : articles) {
                    addArticleCard(article);
                }
            });
        });
    }
    
    private void addArticleCard(Article article) {
        View cardView = LayoutInflater.from(this).inflate(R.layout.item_article_card, articlesContainer, false);
        
        ImageView articleImage = cardView.findViewById(R.id.articleImage);
        TextView articleTitle = cardView.findViewById(R.id.articleTitle);
        TextView articleReadTime = cardView.findViewById(R.id.articleReadTime);
        
        articleTitle.setText(article.title);
        articleReadTime.setText(article.readTime);
        
        Picasso.get()
            .load(article.imageUrl)
            .placeholder(R.drawable.img_article)
            .error(R.drawable.img_article)
            .into(articleImage);
        
        cardView.setOnClickListener(v -> openUrl(article.url));
        articlesContainer.addView(cardView);
    }
    
    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open link", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        ProfileImageHelper.loadProfileImage(this, profileImage);
    }
    
    private void openYouTubeVideo(String videoId) {
        try {
            Intent appIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + videoId));
            startActivity(appIntent);
        } catch (Exception e) {
            Intent webIntent = new Intent(Intent.ACTION_VIEW, 
                Uri.parse("https://www.youtube.com/watch?v=" + videoId));
            startActivity(webIntent);
        }
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_fit_hub;
    }

    @Override
    protected int getBottomNavMenuItemId() {
        return R.id.nav_fithub;
    }
}
