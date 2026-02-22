// MainVideoActivity.java
package com.example.chroniccare;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

public class MainVideoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fit_hub);

        setupVideoCards();
    }

    private void setupVideoCards() {
        // Video cards now use video1, video2, etc. IDs
        setupVideoCard(R.id.video1, "vCq-qy1v6Bc");
        setupVideoCard(R.id.video2, "Sr8aCh3SNHQ");
        setupVideoCard(R.id.video3, "PwXUHMKamP8");
        setupVideoCard(R.id.video4, "aV-vgqCQFbU");
        setupVideoCard(R.id.video5, "QbmPxLWmWr8");
        setupVideoCard(R.id.video6, "UTRsLReOKzg");
        setupVideoCard(R.id.video7, "yGMPQliSBCo");
        setupVideoCard(R.id.video9, "-nCVxDwanEM");
        setupVideoCard(R.id.video10, "CKnlEt5n3Sk");
        
        // Video 8 is a playlist
        View video8 = findViewById(R.id.video8);
        if (video8 != null) {
            video8.setOnClickListener(v -> openUrl("https://www.youtube.com/playlist?list=PLBU6uF21RTAAgyoH2InY3GENbAsMpTPgO"));
        }
    }
    
    private void setupVideoCard(int cardId, String videoId) {
        View card = findViewById(cardId);
        if (card != null) {
            card.setOnClickListener(v -> openYouTubeVideo(videoId));
        }
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
    
    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}