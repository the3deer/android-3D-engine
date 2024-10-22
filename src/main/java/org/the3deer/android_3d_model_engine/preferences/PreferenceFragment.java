package org.the3deer.android_3d_model_engine.preferences;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class PreferenceFragment extends PreferenceFragmentCompat {

    private final static String TAG = PreferenceFragment.class.getSimpleName();

    @Inject
    private Activity activity;
    @Inject
    private Context context;
    @Inject
    private List<PreferenceAdapter> adapters;

    public PreferenceFragment() {
        setEnterTransition(new android.transition.Slide(Gravity.RIGHT));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        view.setBackgroundColor(getResources().getColor(android.R.color.background_light));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.RIGHT;
        view.setLayoutParams(params);
        return view;
    }

    public void setUp() {

        // check
        if (context == null) return;

        // get
        final SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(context);
        final Map<String, ?> all = sharedPreferences.getAll();

        // load
        for (PreferenceAdapter a : adapters){
            a.onRestorePreferences(all);
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        // check
        if (adapters == null || adapters.isEmpty()) return;

        // create screen
        Context context = getPreferenceManager().getContext();
        //setPreferencesFromResource(R.xml.preferences, rootKey);
        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(context);

        // setup screen
        screen.setIconSpaceReserved(false);

        // global options
        this.onCreatePreferences(savedInstanceState, rootKey, context, screen);

        // inflate
        for (PreferenceAdapter a : adapters){
            a.onCreatePreferences(savedInstanceState,rootKey, context, screen);
        }

        // update
        setPreferenceScreen(screen);
    }

    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey, Context context, PreferenceScreen screen) {

        SwitchPreference immersiveSwitch = new SwitchPreference(context);
        immersiveSwitch.setKey("activity.immersive");
        immersiveSwitch.setTitle("Immersive View");
        immersiveSwitch.setIconSpaceReserved(screen.isIconSpaceReserved());
        immersiveSwitch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                // perform
                Log.i(TAG,"Clicked! "+newValue);
                Bundle result = new Bundle();
                result.putString("action", "load");
                getParentFragmentManager().setFragmentResult("immersive", result);
                return true;
            }
        });
        screen.addPreference(immersiveSwitch);
    }

    public void onSaveInstanceState(Bundle outState) {

        Log.v(TAG, "Saving state... ");

        // assert
        if (outState == null || this.adapters == null) return;

        // inform listeners
        for (PreferenceAdapter l : this.adapters) {
            if (l == this) continue;
            l.onSaveInstanceState(outState);
        }
    }

    public void onRestoreInstanceState(Bundle state) {

        Log.v(TAG,"Restoring state... "+state);

        // assert
        if (state == null || this.adapters == null) return;

        // inform listeners
        for (PreferenceAdapter l : this.adapters){
            if (l == this) continue;
            l.onRestoreInstanceState(state);
        }
    }

    private @NonNull SwitchPreference getActivityPrefs() {
        SwitchPreference activityPrefs = new SwitchPreference(getContext());
        activityPrefs.setKey("immersive");
        activityPrefs.setTitle("Load");
        activityPrefs.setOnPreferenceChangeListener((preference, newValue) -> {
            // perform
            Log.i("PreferenceFragment","Clicked! "+newValue);
            Bundle result = new Bundle();
            result.putString("action", "load");
            getParentFragmentManager().setFragmentResult("app", result);
            return true;
        });
        return activityPrefs;
    }
}