package org.the3deer.android_3d_model_engine;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.SwitchPreference;

import org.the3deer.android_3d_model_engine.preferences.PreferenceAdapter;
import org.the3deer.android_3d_model_engine.view.GLSurfaceView;
import org.the3deer.android_3d_model_engine.view.GLTouchHandler;

/**
 * This activity represents the container for our 3D viewer.
 *
 * @author andresoviedo
 */
public class ModelFragment extends Fragment implements PreferenceAdapter {

    private final static String TAG = ModelFragment.class.getSimpleName();

    private String currentEngineId;
    private String currentModelUri;
    protected ModelEngine modelEngine;
    private OnBackPressedCallback onBackPressedCallback;
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

        // get parameters
        onRestoreEngine(savedInstanceState);
    }

    private void onRestoreEngine(Bundle savedInstanceState) {

        // get model
        final ModelViewModel viewModel = new ViewModelProvider(requireActivity()).get(ModelViewModel.class);

        if (savedInstanceState != null) {
            // Fragment is being re-created from a previous state
            Log.d(TAG, "onRestoreEngine: Restoring from savedInstanceState");
            currentEngineId = savedInstanceState.getString("id");
            currentModelUri = savedInstanceState.getString("uri");
        } else {
            // Fragment is being created for the first time (or arguments are the source of truth)
            Bundle arguments = getArguments();
            if (arguments != null) {
                Log.d(TAG, "onRestoreEngine: Initializing from arguments");
                currentEngineId = arguments.getString("id");
                currentModelUri = arguments.getString("uri");
            } else {
                // Optional: Handle case where no arguments were provided if that's an error
                Log.w(TAG, "onRestoreEngine: No arguments provided and no saved state!");
                // You might set default values or throw an exception if arguments are mandatory
            }
        }

        // get model
        if (currentEngineId == null){
            currentEngineId = currentModelUri;
        }
        if (currentEngineId != null) {
            modelEngine = viewModel.getModelEngine(currentEngineId);
        } else {
            currentEngineId = "android://fragment/"+this.getClass().getSimpleName()
                    .replace("Fragment","");
        }

        // init engine
        if (modelEngine == null) {
            Log.i(TAG, "ModelEngine not found in ViewModel, Creating new instance... "+ currentEngineId);
            modelEngine = ModelEngine.newInstance(currentEngineId, requireActivity(), savedInstanceState, getArguments());
            //modelEngine.getBeanFactory().addOrReplace("_uri", currentModelUri);
            //modelEngine.getBeanFactory().addOrReplace("scene_0.loader", new SceneLoader());
            /*modelEngine.getBeanFactory().addOrReplace("100.renderer", GLRendererImpl.class);
            modelEngine.getBeanFactory().addOrReplace("100.surface", new GLSurfaceView(requireActivity()));
            modelEngine.getBeanFactory().addOrReplace("100.fragment_gl", new GLFragment());*/
            modelEngine.init();
            modelEngine.start();
        } else {
            Log.i(TAG, "Resuming ModelEngine from ViewModel... "+ currentEngineId);
            //modelEngine.getBeanFactory().addOrReplace("_uri", currentModelUri);
            //modelEngine.getBeanFactory().addOrReplace("extras", getArguments());
            //modelEngine.getBeanFactory().addOrReplace("scene_0.loader", new SceneLoader());
            /*modelEngine.getBeanFactory().addOrReplace("100.renderer", GLRendererImpl.class);
            modelEngine.getBeanFactory().addOrReplace("100.surface", new GLSurfaceView(requireActivity()));
            modelEngine.getBeanFactory().addOrReplace("100.fragment_gl", new GLFragment());*/
        }

        //modelEngine.getBeanFactory().addOrReplace("99.activity", requireActivity());
        //modelEngine.getBeanFactory().addOrReplace("bundle", savedInstanceState);
        //modelEngine.getBeanFactory().addOrReplace("extras", getArguments());
        //modelEngine.getBeanFactory().addOrReplace("99.loader", new SceneLoader());
        //modelEngine.getBeanFactory().addOrReplace("shaderFactory", new ShaderFactory(requireActivity()));
        //Log.i(TAG, "Adding GL components... "+System.identityHashCode(this));
        //modelEngine.getBeanFactory().addOrReplace("99.shaderFactory", new ShaderFactory());
        //modelEngine.getBeanFactory().addOrReplace("99.shaderPreferences", new ShaderPreferences());
        //modelEngine.getBeanFactory().addOrReplace("99.renderer", new GLRendererImpl());
        //modelEngine.getBeanFactory().addOrReplace("99.surface", new GLSurfaceView(requireActivity()));
        //modelEngine.getBeanFactory().addOrReplace("99.fragment_gl", new GLFragment());
        //modelEngine.getBeanFactory().addOrReplace("99.settings", new PreferenceFragment());

        // restore state
        //modelEngine.getPreferenceFragment().onRestoreInstanceState(savedInstanceState);
        //SceneManager sceneLoader = modelEngine.getBeanFactory().find(SceneManager.class);
        //sceneLoader.loadFromBundle(getArguments());

        modelEngine.getBeanFactory().addOrReplace("model_fragment", (PreferenceAdapter)this);

        viewModel.setModelEngine(currentEngineId, modelEngine);
        viewModel.setRecentId(currentEngineId);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        // restore engine (eg: back button)
        Log.d(TAG, "onCreateView. Restoring engine...");
        onRestoreEngine(savedInstanceState);

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // register touch handler
        Log.d(TAG, "onViewCreated. Registering touch handler...");
        glSurfaceView = view.findViewById(R.id.gl_surface_view);
        GLTouchHandler glTouchHandler = modelEngine.getBeanFactory().find(GLTouchHandler.class);
        if (glTouchHandler != null) {
            glSurfaceView.setTouchEventHandler(glTouchHandler); // Set the fragment as the callback
            glSurfaceView.setUp(modelEngine);
        } else {
            Log.e(TAG, "Touch handler not found!");
        }
    }

    /* @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);

        modelEngine.getBeanFactory().addOrReplace("gl_fragment", this);

        // settings view
        //createSettings();

        return view;
    }*/

    /*@Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (onBackPressedCallback == null) {
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
    }*/

    private void createSettings() {

        // check
        //if (modelEngine.getPreferenceFragment() == null) return;

        //Log.v(TAG, "createSettings");

        // toolbar (disabled in favor of settings)
        /*Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);*/
        //surfaceView.setBackgroundColor(backgroundColor);

        // settings button
        /*final FloatingActionButton fab = getView().findViewById(R.id.button_settings);
        if (fab == null){
            return;
        }*/

        /*fab.setOnClickListener(view -> {

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
                        .setReorderingAllowed(true)
                        .replace(R.id.settings, modelEngine.getPreferenceFragment(), "settings")
                        .commit();
            }
        });*/
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
