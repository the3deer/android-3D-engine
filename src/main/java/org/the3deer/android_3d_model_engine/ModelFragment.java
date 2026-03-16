package org.the3deer.android_3d_model_engine;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.SwitchPreference;

import org.the3deer.android_3d_model_engine.preferences.PreferenceAdapter;
import org.the3deer.android_3d_model_engine.view.GLRendererImpl;
import org.the3deer.android_3d_model_engine.view.GLSurfaceView;
import org.the3deer.android_3d_model_engine.view.GLTouchHandler;

/**
 * This activity represents the container for our 3D viewer.
 *
 * @author andresoviedo
 */
public class ModelFragment extends Fragment implements PreferenceAdapter {

    private final static String TAG = ModelFragment.class.getSimpleName();

    private String currentModelUri;
    protected ModelEngine modelEngine;
    protected final Handler handler;

    private GLSurfaceView glSurfaceView;
    /**
     * Enter into Android Immersive mode so the renderer is full screen or not
     */
    private boolean immersiveMode;

    public ModelFragment() {
        super(R.layout.fragment_model);
        // Android UI thread
        this.handler = new Handler(Looper.getMainLooper());
    }

    public static ModelFragment newInstance(String uri, String type, boolean demo) {
        ModelFragment frag = new ModelFragment();
        Bundle args = new Bundle();
        args.putString("uri", uri);
        args.putString("type", type);
        args.putBoolean("demo", demo);
        frag.setArguments(args);
        return frag;
    }

    /**
     * settings:
     * -
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down then this Bundle contains the data it most
     *                           recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate. Loading model fragment...");
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        // log event
        Log.i(TAG, "Creating Model Engine view... state: " + savedInstanceState);

        // call super
        super.onViewCreated(view, savedInstanceState);

        // get gl view
        glSurfaceView = view.findViewById(R.id.gl_surface_view);

        // restore engine (eg: back button)
        try {
            onRestoreEngine(savedInstanceState, getArguments());
        } catch (Exception ex) {
            Log.e(TAG, "Error restoring engine: " + ex.getMessage(), ex);

            // notify user
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(getActivity(), "There was a problem restoring the Engine status. " + ex.getMessage(),
                        Toast.LENGTH_LONG).show();
            });
        }
    }

    private void onRestoreEngine(Bundle savedInstanceState, Bundle arguments) throws Exception {

        // get model
        final ModelViewModel viewModel = new ViewModelProvider(requireActivity()).get(ModelViewModel.class);

        if (savedInstanceState != null) {

            // Fragment is being re-created from a previous state
            Log.d(TAG, "onRestoreEngine: Restoring from savedInstanceState");
            currentModelUri = savedInstanceState.getString("uri");
        } else if (arguments != null) {
                Log.d(TAG, "onRestoreEngine: Initializing from arguments");
                currentModelUri = arguments.getString("uri");
        } else {
            // Optional: Handle case where no arguments were provided if that's an error
            Log.w(TAG, "onRestoreEngine: No arguments provided and no saved state!");
            // You might set default values or throw an exception if arguments are mandatory
        }

        // check
        if (currentModelUri == null){
            Log.e(TAG, "onRestoreEngine: uri is null");
            return;
        }

        // get engine
        modelEngine = viewModel.getModelEngine(currentModelUri);

        // init engine if not found
        if (modelEngine == null) {

            // Run initialization in a background thread to avoid UI contention
            handler.post(() -> {
                try {
                    // inform
                    Log.i(TAG, "ModelEngine not found in ViewModel, Creating new instance... "+ currentModelUri);

                    // create
                    modelEngine = ModelEngine.newInstance(currentModelUri, requireActivity(), savedInstanceState, getArguments());

                    // configure
                    modelEngine.getBeanFactory().addOrReplace("model_fragment", this);

                    // init
                    modelEngine.init();

                    // start
                    modelEngine.start();

                    // configure GL touch handler
                    Log.d(TAG, "Configuring GL view...");
                    GLTouchHandler glTouchHandler = modelEngine.getBeanFactory().find(GLTouchHandler.class);
                    glSurfaceView.setTouchEventHandler(glTouchHandler);

                    // configure GL renderers
                    Log.d(TAG, "Configuring renderer...");
                    final GLRendererImpl renderer = (GLRendererImpl) glSurfaceView.getRenderer();
                    modelEngine.getBeanFactory().configure(renderer);

                    // register engine in the application
                    Log.d(TAG, "Registering engine... ");
                    viewModel.setModelEngine(currentModelUri, modelEngine);
                    viewModel.setRecentUri(currentModelUri);

                    // inform
                    Log.i(TAG, "Model Engine initialized for '"+currentModelUri+"'");

                } catch (Exception ex) {
                    Log.e(TAG, "Error initializing engine in background: " + ex.getMessage(), ex);
                    handler.post(() -> {
                        if (isAdded()) {
                            Toast.makeText(getContext(), "Error: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });
        } else {
            Log.i(TAG, "Resuming ModelEngine from ViewModel... "+ currentModelUri);

            // configure GL touch handler
            Log.d(TAG, "Configuring GL view...");
            GLTouchHandler glTouchHandler = modelEngine.getBeanFactory().find(GLTouchHandler.class);
            glSurfaceView.setTouchEventHandler(glTouchHandler);

            // configure GL renderers
            Log.d(TAG, "Configuring renderer...");
            modelEngine.getBeanFactory().addOrReplace("gl_renderer", glSurfaceView.getRenderer());
        }
    }

    /*private void setupOrientationListener() {
        try {
            //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            //sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR);
            sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
            if (sensor != null) {
                sensorManager.registerListener(new SensorEventListener() {
                                                   @Override
                                                   public void onSensorChanged(SensorEvent event) {
                                                       *//*Log.v("ModelActivity","sensor: "+ Arrays.toString(event.values));
                                                           Quaternion orientation = new Quaternion(event.values);
                                                           orientation.normalize();
                                                           //scene.getSelectedObject().setOrientation(orientation);
                                                           glView.setOrientation(orientation);*//*
                                                   }

                                                   @Override
                                                   public void onAccuracyChanged(Sensor sensor, int accuracy) {

                                                   }
                                               }, sensor,
                        SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
            }
            OrientationEventListener mOrientationListener = new OrientationEventListener(
                    getContext()) {
                @Override
                public void onOrientationChanged(int orientation) {
                    //scene.onOrientationChanged(orientation);
                }
            };

            if (mOrientationListener.canDetectOrientation()) {
                mOrientationListener.enable();
            }
        } catch (Exception e) {
            Log.e("ModelActivity", "There is an issue setting up sensors", e);
        }
    }*/

    @Override
    public void onPause() {
        Log.v(TAG, "onPause");
        super.onPause();
        if (glSurfaceView != null) {
            Log.v(TAG, "onPause surface");
            glSurfaceView.onPause();
        }
    }

    @Override
    public void onResume() {
        Log.v(TAG, "onResume");
        super.onResume();
        if (glSurfaceView != null) {
            Log.v(TAG, "onResume surface");
            glSurfaceView.onResume();
        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // update status
        if (currentModelUri != null) {
            Log.v(TAG,"saving url: "+currentModelUri);
            //outState.putString("uri", currentModelUri);
        }

        // save status
        if (modelEngine.getPreferenceFragment() != null) {
            modelEngine.getPreferenceFragment().onSaveInstanceState(outState);
        }
    }

    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey, Context context, PreferenceGroup screen) {

        PreferenceGroup category = screen.findPreference(this.getClass().getName());
        if (category == null){
            category = new PreferenceCategory(context);
            category.setKey(getClass().getName());
            category.setTitle(getClass().getSimpleName());
            category.setLayoutResource(R.layout.preference_category);
            screen.addPreference(category);
        }

        SwitchPreference immersiveSwitch = new SwitchPreference(context);
        immersiveSwitch.setKey("activity.immersive");
        immersiveSwitch.setTitle("Immersive View");
        immersiveSwitch.setIconSpaceReserved(screen.isIconSpaceReserved());
        immersiveSwitch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
                ModelFragment.this.immersiveMode = (Boolean) newValue;
                return handleImmersive();
            }
        });
        category.addPreference(immersiveSwitch);
    }

    private boolean handleImmersive() {
        if (this.immersiveMode) {
            hideSystemUI();
        } else {
            showSystemUI();
        }
        Toast.makeText(getContext(), "Fullscreen " + this.immersiveMode, Toast.LENGTH_SHORT).show();
        return true;
    }

    void hideSystemUI() {
        if (!this.immersiveMode) {
            return;
        }
        // Set the IMMERSIVE flag.
        // Set the content to appear under the system bars so that the content
        // doesn't resize when the system bars hide and show.
        final View decorView = getActivity().getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    // This snippet shows the system bars. It does this by removing all the flags
    // except for the ones that make the content appear under the system bars.
    void showSystemUI() {
        handler.removeCallbacksAndMessages(null);
        final View decorView = getActivity().getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
    }
}
