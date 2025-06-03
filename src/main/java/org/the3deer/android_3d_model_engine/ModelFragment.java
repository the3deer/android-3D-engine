package org.the3deer.android_3d_model_engine;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.the3deer.android_3d_model_engine.model.Scene;
import org.the3deer.android_3d_model_engine.preferences.PreferenceFragment;
import org.the3deer.android_3d_model_engine.scene.SceneLoader;
import org.the3deer.android_3d_model_engine.shader.ShaderFactory;
import org.the3deer.android_3d_model_engine.shader.ShaderPreferences;
import org.the3deer.android_3d_model_engine.view.GLFragment;
import org.the3deer.android_3d_model_engine.view.GLRendererImpl;
import org.the3deer.android_3d_model_engine.view.GLSurfaceView;

/**
 * This activity represents the container for our 3D viewer.
 *
 * @author andresoviedo
 */
public class ModelFragment extends Fragment {

    private final static String TAG = ModelFragment.class.getSimpleName();

    private ModelViewModel viewModel;

    protected final Handler handler;

    protected ModelEngine modelEngine;
    /**
     * Enter into Android Immersive mode so the renderer is full screen or not
     */
    private boolean immersiveMode;
    private OnBackPressedCallback onBackPressedCallback;

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
        Log.i(TAG, "Loading model fragment...");
        super.onCreate(savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(ModelViewModel.class);

        // default engine
        modelEngine = viewModel.getModelEngine().getValue();

        // create engine
        if (modelEngine == null) {
            Log.d(TAG, "ModelEngine not found in ViewModel, creating new instance...");
            modelEngine = ModelEngine.newInstance(requireActivity(), savedInstanceState, getArguments());
            //modelEngine.getBeanFactory().addOrReplace("extras", getArguments());
            //modelEngine.getBeanFactory().addOrReplace("scene_0.loader", new SceneLoader());
            /*modelEngine.getBeanFactory().addOrReplace("100.renderer", GLRendererImpl.class);
            modelEngine.getBeanFactory().addOrReplace("100.surface", new GLSurfaceView(requireActivity()));
            modelEngine.getBeanFactory().addOrReplace("100.fragment_gl", new GLFragment());*/
            modelEngine.init();
            modelEngine.start();
            viewModel.setModelEngine(modelEngine);
        } else {
            Log.d(TAG, "Reusing ModelEngine from ViewModel.");
            //modelEngine.getBeanFactory().addOrReplace("extras", getArguments());
            //modelEngine.getBeanFactory().addOrReplace("scene_0.loader", new SceneLoader());
            /*modelEngine.getBeanFactory().addOrReplace("100.renderer", GLRendererImpl.class);
            modelEngine.getBeanFactory().addOrReplace("100.surface", new GLSurfaceView(requireActivity()));
            modelEngine.getBeanFactory().addOrReplace("100.fragment_gl", new GLFragment());*/
        }

        // Ensure modelEngine is not null before proceeding
        if (modelEngine == null) {
            Log.e(TAG, "ModelEngine is still null! ViewModel might not be retaining it correctly.");
            // Handle this error appropriately - perhaps show an error message or finish.
            return;
        }

        Log.i(TAG, "Updating context... "+System.identityHashCode(this));

        modelEngine.getBeanFactory().addOrReplace("99.activity", requireActivity());
        modelEngine.getBeanFactory().addOrReplace("99.bundle", savedInstanceState);
        modelEngine.getBeanFactory().addOrReplace("99.extras", getArguments());
        //modelEngine.getBeanFactory().addOrReplace("shaderFactory", new ShaderFactory(requireActivity()));
        Log.i(TAG, "Adding GL components... "+System.identityHashCode(this));
        modelEngine.getBeanFactory().addOrReplace("99.shaderFactory", new ShaderFactory());
        modelEngine.getBeanFactory().addOrReplace("99.renderer", new GLRendererImpl());
        modelEngine.getBeanFactory().addOrReplace("99.surface", new GLSurfaceView(requireActivity()));
        modelEngine.getBeanFactory().addOrReplace("99.fragment_gl", new GLFragment());
        modelEngine.getBeanFactory().addOrReplace("99.shaderPreferences", new ShaderPreferences());
        modelEngine.getBeanFactory().addOrReplace("99.settings", new PreferenceFragment());

        // restore state
        //modelEngine.getPreferenceFragment().onRestoreInstanceState(savedInstanceState);

    }

    @Override
    public void onStart() {
        super.onStart();

        if (modelEngine == null){
            return;
        }

        final GLFragment glFragment = modelEngine.getGLFragment();
        if (glFragment == null){
            return;
        }

        Log.i(TAG, "Attaching GL fragment... "+System.identityHashCode(glFragment));

        // listen for engine events
        getChildFragmentManager().setFragmentResultListener("app", this, (requestKey, result) -> {
            getParentFragmentManager().setFragmentResult(requestKey, result);
        });
        getChildFragmentManager().setFragmentResultListener("immersive", this, (requestKey, result) -> {
            toggleImmersive();
        });

        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.gl_container, (Fragment) glFragment, "surface")
                .setReorderingAllowed(true)
                .commit();

        // settings view
        createSettings();

        modelEngine.getBeanFactory().addOrReplace("extras", getArguments());
        modelEngine.getBeanFactory().addOrReplace("scene_0.loader", new SceneLoader());
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        onBackPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                Log.v(TAG, "handleOnBackPressed");
                Bundle result = new Bundle();
                result.putString("action", "back");
                getParentFragmentManager().setFragmentResult("app", result);
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(onBackPressedCallback);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (onBackPressedCallback != null) {
            onBackPressedCallback.remove();
            onBackPressedCallback = null;
        }
        if (modelEngine != null){
            final Scene scene = modelEngine.getBeanFactory().find(Scene.class);
            if (scene != null) {
                scene.reset();
            }
        }
        Log.v(TAG, "onDetach");
    }

    private void createSettings() {

        // check
        if (modelEngine.getPreferenceFragment() == null) return;

        Log.v(TAG, "createSettings");

        // toolbar (disabled in favor of settings)
        /*Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);*/
        //surfaceView.setBackgroundColor(backgroundColor);

        // settings button
        final FloatingActionButton fab = getView().findViewById(R.id.button_settings);
        fab.setOnClickListener(view -> {

            // check
            final Fragment settings = getChildFragmentManager().findFragmentByTag("settings");
            if (settings != null) {
                // hide settings
                getChildFragmentManager()
                        .beginTransaction()
                        .remove(settings)
                        .commit();
            } else {

                // show settings
                getChildFragmentManager()
                        .beginTransaction()
                        .replace(R.id.settings, modelEngine.getPreferenceFragment(), "settings")
                        .commit();
            }
        });
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
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // save status
        if (modelEngine.getPreferenceFragment() != null) {
            modelEngine.getPreferenceFragment().onSaveInstanceState(outState);
        }

/*        if (getArguments() != null && getArguments().containsKey("uri")) {
            outState.putString("uri", getArguments().getString("uri"));
        }
        if (getArguments() != null && getArguments().containsKey("type")) {
            outState.putString("type", getArguments().getString("type"));
        }
        if (getArguments() != null && getArguments().containsKey("demo")) {
            outState.putBoolean("type", getArguments().getBoolean("demo"));
        }*/
    }

    private void toggleImmersive() {
        this.immersiveMode = !this.immersiveMode;
        if (this.immersiveMode) {
            hideSystemUI();
        } else {
            showSystemUI();
        }
        Toast.makeText(getContext(), "Fullscreen " + this.immersiveMode, Toast.LENGTH_SHORT).show();
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
