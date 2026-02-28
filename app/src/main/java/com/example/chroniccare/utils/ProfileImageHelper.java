package com.example.chroniccare.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;

import com.example.chroniccare.LogInPage;
import com.example.chroniccare.ProfileActivity;
import com.example.chroniccare.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.squareup.picasso.Picasso;

public class ProfileImageHelper {
    private static final String TAG = "ProfileImageHelper";
    
    /**
     * Loads the profile image into the provided ImageView.
     * Supports both CircleImageView and ShapeableImageView as they both extend ImageView.
     */
    public static void loadProfileImage(Context context, ImageView imageView) {
        if (context == null || imageView == null) {
            Log.e(TAG, "Context or ImageView is null");
            return;
        }
        
        try {
            SharedPreferences prefs = context.getSharedPreferences("ChronicCarePrefs", Context.MODE_PRIVATE);
            String photoUrl = prefs.getString("userPhoto", "");
            
            if (!photoUrl.isEmpty()) {
                try {
                    if (photoUrl.startsWith("http")) {
                        Picasso.get()
                            .load(photoUrl)
                            .placeholder(R.drawable.ic_profile)
                            .error(R.drawable.ic_profile)
                            .into(imageView);
                    } else {
                        Uri uri = Uri.parse(photoUrl);
                        imageView.setImageURI(uri);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error loading photo", e);
                    loadGooglePhoto(context, imageView);
                }
            } else {
                loadGooglePhoto(context, imageView);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in loadProfileImage", e);
            if (imageView != null) {
                imageView.setImageResource(R.drawable.ic_profile);
            }
        }
    }
    
    private static void loadGooglePhoto(Context context, ImageView imageView) {
        if (context == null || imageView == null) return;
        
        try {
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
            if (account != null && account.getPhotoUrl() != null) {
                Picasso.get()
                    .load(account.getPhotoUrl().toString())
                    .placeholder(R.drawable.ic_profile)
                    .error(R.drawable.ic_profile)
                    .into(imageView);
            } else {
                imageView.setImageResource(R.drawable.ic_profile);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading Google photo", e);
            if (imageView != null) {
                imageView.setImageResource(R.drawable.ic_profile);
            }
        }
    }
    
    public static void handleProfileClick(Context context) {
        if (context == null) return;
        
        try {
            SharedPreferences prefs = context.getSharedPreferences("ChronicCarePrefs", Context.MODE_PRIVATE);
            boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);
            String userId = prefs.getString("userId", "");
            
            Intent intent;
            if (isLoggedIn && userId != null && !userId.isEmpty()) {
                intent = new Intent(context, ProfileActivity.class);
            } else {
                intent = new Intent(context, LogInPage.class);
            }
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error handling profile click", e);
        }
    }
}
