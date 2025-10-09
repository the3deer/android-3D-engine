package org.the3deer.android_3d_model_engine.camera;


import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import org.the3deer.android_3d_model_engine.preferences.PreferenceAdapter;

import javax.inject.Inject;

public final class CameraPreferences implements PreferenceAdapter {

    private final static String TAG = CameraPreferences.class.getSimpleName();

    @Inject
    private CameraManager cameraManager;


    public CameraPreferences() {
    }


    // In CameraPreferences.java


    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey, Context context, PreferenceGroup screen) {

        Log.v(TAG, "onCreatePreferences");

        // get camera names
        final String[] cameraNames = cameraManager.getCameraNames();

        // check
        if (cameraNames == null || cameraNames.length == 0) return;

        // --- FIX 1: Create an array of indices as strings ---
        // The preference system needs string values to save its state.
        // We create an array like ["0", "1", "2", ...]
        final String[] entryValues = new String[cameraNames.length];
        for (int i = 0; i < cameraNames.length; i++) {
            entryValues[i] = String.valueOf(i);
        }

        // build
        final ListPreference cameraListPreference = new ListPreference(context);
        cameraListPreference.setIconSpaceReserved(screen.isIconSpaceReserved());
        cameraListPreference.setKey("camera_selection"); // Good to use a descriptive key
        cameraListPreference.setTitle("Camera");
        cameraListPreference.setEntries(cameraNames);

        // --- FIX 2: Use the newly created string array of indices ---
        cameraListPreference.setEntryValues(entryValues);

        // Optional: Set a default value, e.g., the first camera
        cameraListPreference.setDefaultValue(entryValues[0]);

        cameraListPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            try {
                // --- FIX 3: Safely parse the string value back to an integer ---
                // 'newValue' will be a string like "0", "1", etc.
                int selectedIndex = Integer.parseInt((String) newValue);

                // Tell the manager to update the active camera
                cameraManager.setActiveCamera(selectedIndex);

                // Use the manager to get the name for logging, ensuring it's the single source of truth
                Log.i(TAG, "Active camera changed to: " + cameraManager.getActiveCamera().getName());
                return true;

            } catch (NumberFormatException e) {
                Log.e(TAG, "Failed to parse camera index from preference", e);
                return false;
            }
        });

        // Add the fully configured preference to the screen
        screen.addPreference(cameraListPreference);
    }

}
