package com.example.chroniccare;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

public class FitHubActivity extends BottomNavActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        findViewById(R.id.webView_gentle_yoga).setOnClickListener(v -> {
            openYouTubeVideo("EvMTrP8eRvM");
        });
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
