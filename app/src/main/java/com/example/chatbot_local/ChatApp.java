package com.example.chatbot_local;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public class ChatApp extends Application {
    public static final String PREFS = "settings";
    public static final String DEFAULT_MODEL = "qwen3.5:4b";
    public static final String DEFAULT_SERVER = "http://10.0.2.2:11434";

    @Override
    public void onCreate() {
        super.onCreate();
        applySavedTheme(this);
    }

    public static void applySavedTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, MODE_PRIVATE);
        String theme = prefs.getString("theme", "system");
        int mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        if ("dark".equals(theme)) {
            mode = AppCompatDelegate.MODE_NIGHT_YES;
        } else if ("light".equals(theme)) {
            mode = AppCompatDelegate.MODE_NIGHT_NO;
        }
        AppCompatDelegate.setDefaultNightMode(mode);
    }
}
