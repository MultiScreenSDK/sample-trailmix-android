package com.samsung.trailmix.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.samsung.trailmix.App;

/**
 * Created by bliu on 6/19/2015.
 */
public enum  Settings {
    instance;

    private static final String NAME = "com.samsung.trailmix.settings";

    private SharedPreferences preferences;

    private Settings() {
        preferences = App.getInstance().getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public void writePlaybackPosition(String id, float position) {
        preferences.edit().putFloat(id, position).apply();
    }

    public float readPlaybackPosition(String id) {
        return preferences.getFloat(id, 0);
    }
}
