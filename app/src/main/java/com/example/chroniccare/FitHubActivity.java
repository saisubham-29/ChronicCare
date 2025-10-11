// MonitorActivity.java

package com.example.chroniccare;

import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.example.chroniccare.R; // Assuming R is accessible

public class FitHubActivity extends BottomNavActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Once the layout is set in the parent class, we can find and configure the WebView.
        setupGentleYogaVideo();
    }

    /**
     * Finds the WebView for the Gentle Yoga card, configures it, and loads
     * the embedded YouTube video link.
     */
    private void setupGentleYogaVideo() {
        // Find the WebView element by its ID
        WebView webView = findViewById(R.id.webView_gentle_yoga);

        // --- MANDATORY: Replace this ID with the actual YouTube Video ID you want to embed ---
        // Example: This placeholder ID is from a sample 15-minute gentle yoga video.
        final String youtubeVideoId = "<iframe width=\"560\" height=\"315\" src=\"https://www.youtube.com/embed/EvMTrP8eRvM?si=ZPSXWfAUKzFRQKjH\" title=\"YouTube video player\" frameborder=\"0\" allow=\"accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share\" referrerpolicy=\"strict-origin-when-cross-origin\" allowfullscreen></iframe>";

        // 1. Configure WebView settings for media playback
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true); // Recommended for better iframe/YouTube compatibility

        // Ensure links clicked inside the iframe/WebView stay within the WebView
        webView.setWebViewClient(new WebViewClient());

        // 2. HTML template for embedding a YouTube video using an iframe
        // The iframe is sized to 100% of the CardView dimensions.
        // Parameters: autoplay=0 (don't auto-start), controls=0 (minimal controls)
        String frameVideo = "https://youtu.be/EvMTrP8eRvM?si=dmP5w_9rc967GGhO";

        // 3. Load the HTML content
        webView.loadData(frameVideo, "text/html", "utf-8");
    }

    // --- Required abstract methods from BottomNavActivity ---

    @Override
    protected int getLayoutId() {
        // Assumed to be R.layout.activity_fit_hub as per context
        return R.layout.activity_fit_hub;
    }

    @Override
    protected int getBottomNavMenuItemId() {
        return R.id.nav_fithub;
    }
}
