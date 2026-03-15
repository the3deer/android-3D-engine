package org.the3deer.android_3d_model_engine.preferences;

import android.app.Activity;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.the3deer.util.bean.BeanInit;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

public class Preferences {

    private final static String TAG = Preferences.class.getSimpleName();

    @Inject
    private Activity activity;

    @Inject
    private List<PreferenceAdapter> adapters;

    public Preferences() {
    }

    @BeanInit
    public void init() {
        try {
            if (adapters == null || adapters.isEmpty()){
                Log.e(TAG, "onCreate: adapters is null or empty");
            } else {
                Log.d(TAG, "onCreate: adapters list: " +adapters);
            }

            // debug
            Log.i(TAG, "Restoring preferences..." + adapters);

            // get
            final SharedPreferences sharedPreferences =
                    PreferenceManager.getDefaultSharedPreferences(activity);
            final Map<String, ?> all = sharedPreferences.getAll();

            // load
            for (PreferenceAdapter a : adapters) {
                a.onRestorePreferences(all);
            }

            // debug
            Log.i(TAG, "Preferences restored");
        } catch (Exception ex) {
            Log.e(TAG, "There was a problem loading the preferences", ex);
            throw ex;
        }
    }
}