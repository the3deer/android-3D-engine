package org.the3deer.android_3d_model_engine.preferences;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import org.the3deer.android_3d_model_engine.ModelEngine;
import org.the3deer.android_3d_model_engine.ModelViewModel;

import java.util.List;

public class PreferenceFragment extends PreferenceFragmentCompat {

    private final static String TAG = PreferenceFragment.class.getSimpleName();

    private ModelViewModel viewModel;
    private ModelEngine engine;
    private List<PreferenceAdapter> adapters;

    public PreferenceFragment() {
        //setEnterTransition(new android.transition.Slide(Gravity.RIGHT));
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {

        // get view model
        this.viewModel = new ViewModelProvider(requireActivity()).get(ModelViewModel.class);

        // check we have a model
        if (viewModel.getRecentId().getValue() == null){
            Log.e(TAG, "onCreate: viewModel.getRecentUri().getValue() is null");
            return;
        } else {
            Log.d(TAG, "onCreate: engine id: " +viewModel.getRecentId().getValue());
        }

        // get the correct engine
        this.engine = viewModel.getModelEngine();

        // get preference adapters
        this.adapters =
                engine.getBeanFactory().findAll(PreferenceAdapter.class, null);

        // check
        if (adapters.isEmpty()){
            Log.e(TAG, "onCreate: adapters is empty");
        } else {
            Log.d(TAG, "onCreate: adapters list: " +adapters);
        }

        // invoke super - it will trigger our onCreatePreference()
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        // check
        if (adapters == null || adapters.isEmpty()){
            Log.e(TAG, "onCreate: adapters is null or empty");
            return;
        }

        Log.i(TAG, "Creating the preferences screen...");

        // create screen
        final Context context = requireContext();

        // main screen
        final PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(context);

        // check
        if (adapters == null || adapters.isEmpty()) return;

        // inflate
        for (PreferenceAdapter a : adapters) {
            try {
                a.onCreatePreferences(savedInstanceState, rootKey, context, screen);
            } catch (Exception e) {
                Log.e(TAG, "Issue onCreatePreferences: " + e.getMessage(), e);
            }
        }

        // update
        setPreferenceScreen(screen);

        Log.i(TAG, "Preferences screen set.");
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

        Log.v(TAG, "Restoring state... " + state);

        // assert
        if (state == null || this.adapters == null) return;

        // inform listeners
        for (PreferenceAdapter l : this.adapters) {
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
            Log.i("PreferenceFragment", "Clicked! " + newValue);
            Bundle result = new Bundle();
            result.putString("action", "load");
            getParentFragmentManager().setFragmentResult("app", result);
            return true;
        });
        return activityPrefs;
    }
}