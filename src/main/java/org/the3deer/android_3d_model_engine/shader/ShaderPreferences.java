package org.the3deer.android_3d_model_engine.shader;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;

import org.the3deer.android_3d_model_engine.R;
import org.the3deer.android_3d_model_engine.preferences.PreferenceAdapter;

import java.util.Map;

import javax.inject.Inject;

public class ShaderPreferences implements PreferenceAdapter {

    // Preference Keys (define these as constants)
    public static final String KEY_SHADER_LIGHTING_TYPE = "shader_lighting_type";
    public static final String KEY_SHADER_ANIMATION_ENABLED = "shader_animation_enabled"; // Example for a boolean
    public static final String KEY_SHADER_TEXTURE_SELECTION = "shader_texture_enabled";

    // Keep a list of ShaderImpl instances to control.
    // This allows one controller to manage multiple shaders if needed,
    // or you can simplify it to control a single instance.
    @Inject
    private ShaderFactory shaderFactory;

    // Or, if controlling a single, globally accessible shader (less ideal but possible):
    // private ShaderImpl targetShader;

    // Default SharedPreferences
    private SharedPreferences sharedPreferences;
    private Context context;

    // Method to add a ShaderImpl instance to be managed by this controller
    public void setUp() {
        if (shaderFactory != null && sharedPreferences != null) {
            final Map<String, Shader> shaders = shaderFactory.getShaders();
            for (Map.Entry<String, Shader> entry : shaders.entrySet()) {
                // Optionally, apply current preferences to this newly added shader
                applyPreferencesToShader(entry.getValue(), sharedPreferences.getAll());
            }
        }
    }

    // --- PreferenceAdapter Implementation ---

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey, Context context, PreferenceGroup screen) {
        this.context = context;
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        // 1. Create the "Shader" Submenu (as a PreferenceScreen)
        //setPreferencesFromResource(R.xml.preferences, rootKey);
        // add submenu
        PreferenceCategory category = new PreferenceCategory(context);
        category.setKey(this.getClass().getName());
        category.setTitle("Shaders");
        category.setLayoutResource(R.layout.preference_category);
        category.setInitialExpandedChildrenCount(0);
        screen.addPreference(category);

        final Map<String, Shader> shaders = shaderFactory.getShaders();
        for (Map.Entry<String, Shader> entry : shaders.entrySet()) {
            // You'll need methods in ShaderImpl to set these properties
            final Shader shader = entry.getValue();
            if (shader instanceof PreferenceAdapter){
                ((PreferenceAdapter) shader).onCreatePreferences(savedInstanceState, rootKey, context, category);
            }
        }

        /*// Example: ListPreference for Lighting Type
        ListPreference lightingPreference = new ListPreference(context);
        lightingPreference.setKey(KEY_SHADER_LIGHTING_TYPE);
        lightingPreference.setTitle("Lighting");
        lightingPreference.setSummary("Select the type of lighting effect");
        lightingPreference.setEntries(new CharSequence[]{"Simple", "No Lighting"});
        lightingPreference.setEntryValues(new CharSequence[]{"on", "off"});
        lightingPreference.setDefaultValue("on"); // Set a default
        category.addPreference(lightingPreference);

        // Example: ListPreference for Texture (assuming texture names or paths are strings)
        ListPreference texturePreference = new ListPreference(context);
        texturePreference.setKey(KEY_SHADER_TEXTURE_SELECTION);
        texturePreference.setTitle("Texture");
        texturePreference.setSummary("Select the texture to apply");
        texturePreference.setEntries(new CharSequence[]{"Textures Enabled", "No Textures"});
        texturePreference.setEntryValues(new CharSequence[]{"on", "off"});
        texturePreference.setDefaultValue("on");
        category.addPreference(texturePreference);

        // Example: SwitchPreference for Animation (if you had a boolean)
        SwitchPreferenceCompat animationPreference = new SwitchPreferenceCompat(context);
        animationPreference.setKey(KEY_SHADER_ANIMATION_ENABLED);
        animationPreference.setTitle("Animation");
        animationPreference.setDefaultValue(true);
        category.addPreference(animationPreference);



        // Set listeners to react to preference changes immediately
        lightingPreference.setOnPreferenceChangeListener((preference, newValue) -> {
            String newLightingValue = (String) newValue;
            final Map<String, Shader> shaders = shaderFactory.getShaders();
            for (Map.Entry<String, Shader> entry : shaders.entrySet()) {
                // You'll need methods in ShaderImpl to set these properties
                final Shader shader = entry.getValue();
                shader.setLightingEnabled(newLightingValue.equals("on"));
            }
            // Update summary or other UI elements if needed
            preference.setSummary("Current: " + newLightingValue);
            return true; // True to update the preference's state
        });

        texturePreference.setOnPreferenceChangeListener((preference, newValue) -> {
            String newTextureValue = (String) newValue;
            final Map<String, Shader> shaders = shaderFactory.getShaders();
            for (Map.Entry<String, Shader> entry : shaders.entrySet()) {
                // You'll need methods in ShaderImpl to set these properties
                final Shader shader = entry.getValue();
                shader.setTexturesEnabled(newTextureValue.equals("on")); // Example method
            }
            preference.setSummary("Current: " + newTextureValue);
            return true;
        });*/

        // Initialize summaries based on current values
        updatePreferenceSummaries();
    }

    @Override
    public void onRestorePreferences(@Nullable Map<String, ?> preferences) {

        if (preferences == null || shaderFactory == null) return;

        final Map<String, Shader> shaders = shaderFactory.getShaders();
        for (Map.Entry<String, Shader> entry : shaders.entrySet()) {
            applyPreferencesToShader(entry.getValue(), preferences);
        }
        // If preferences were loaded by the PreferenceFragmentCompat itself,
        // the onChangeListeners might have already fired or will fire.
        // If you are setting them programmatically, you might need to update summaries here too.
        // updatePreferenceSummaries(); // Call this if summaries are not updated by listeners on initial load
    }

    private void applyPreferencesToShader(Shader shader, Map<String, ?> preferences) {
        if (shader == null || preferences == null) return;

        /*Object lightingValue = preferences.get(KEY_SHADER_LIGHTING_TYPE);
        if (lightingValue instanceof String) {
            shader.setLightingEnabled("on".equals(lightingValue));
        } else if (sharedPreferences != null) { // Fallback to current preference if not in map
            shader.setLightingEnabled("on".equals(sharedPreferences.getString(KEY_SHADER_LIGHTING_TYPE, "simple")));
        }


        Object textureValue = preferences.get(KEY_SHADER_TEXTURE_SELECTION);
        if (textureValue instanceof String) {
            shader.setTexturesEnabled("on".equals((String) textureValue));
        } else if (sharedPreferences != null) {
            shader.setTexturesEnabled("on".equals(sharedPreferences.getString(KEY_SHADER_TEXTURE_SELECTION, "none")));
        }*/

        // Apply other preferences...
    }

    private void updatePreferenceSummaries() {
        if (context == null || sharedPreferences == null) return;
        // This is a bit more involved if preferences are added dynamically to the screen.
        // If you had a PreferenceFragment, you could do:
        // ListPreference lightingPref = findPreference(KEY_SHADER_LIGHTING_TYPE);
        // if (lightingPref != null) lightingPref.setSummary(getEntryForValue(lightingPref, sharedPreferences.getString(KEY_SHADER_LIGHT
    }
}