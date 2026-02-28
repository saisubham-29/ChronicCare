package com.example.chroniccare.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import java.util.Locale;

public class LocaleHelper {
    private static final String PREF_LANGUAGE = "language_preference";
    private static final String DEFAULT_LANGUAGE = "en";

    public static Context setLocale(Context context, String language) {
        persist(context, language);
        return updateResources(context, language);
    }

    public static Context setLocale(Context context) {
        return setLocale(context, getPersistedLanguage(context));
    }

    public static String getPersistedLanguage(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("ChronicCarePrefs", Context.MODE_PRIVATE);
        return prefs.getString(PREF_LANGUAGE, DEFAULT_LANGUAGE);
    }

    private static void persist(Context context, String language) {
        SharedPreferences prefs = context.getSharedPreferences("ChronicCarePrefs", Context.MODE_PRIVATE);
        prefs.edit().putString(PREF_LANGUAGE, language).apply();
    }

    private static Context updateResources(Context context, String language) {
        Locale locale = new Locale(language);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration configuration = new Configuration(resources.getConfiguration());
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale);
            return context.createConfigurationContext(configuration);
        } else {
            configuration.locale = locale;
            resources.updateConfiguration(configuration, resources.getDisplayMetrics());
            return context;
        }
    }
}
