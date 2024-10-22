package org.the3deer.android_3d_model_engine.preferences;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceScreen;

import java.util.Map;

public interface PreferenceAdapter {

    default void onRestoreInstanceState(Bundle state) {
    }

    default void onSaveInstanceState(Bundle outState){
    }

    default void onRestorePreferences(@Nullable Map<String,?> preferences){
    }

    default void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey, Context context, PreferenceScreen screen){
    }
}
