package org.the3deer.android_3d_model_engine.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import javax.inject.Inject;

/**
 * This is the OpenGL fragment of the engine.
 *
 * It requires a @{@link android.opengl.GLSurfaceView} implementation
 *
 */
public class GLFragment extends Fragment
{
    @Inject
    private GLSurfaceView glSurfaceView;

    public GLFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return glSurfaceView;
    }

    /*@Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey, Context context, PreferenceScreen screen) {

        SwitchPreference immersiveSwitch = new SwitchPreference(context);
        immersiveSwitch.setKey("activity.immersive");
        immersiveSwitch.setTitle("Immersive View");
        immersiveSwitch.setIconSpaceReserved(screen.isIconSpaceReserved());
        immersiveSwitch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                // FIXME:
                //toggleImmersive();
                return true;
            }
        });
        screen.addPreference(immersiveSwitch);
    }*/

    @Override
    public void onPause() {
        super.onPause();
        if (glSurfaceView != null) {
            glSurfaceView.onPause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (glSurfaceView != null) {
            glSurfaceView.onResume();
        }

    }
}