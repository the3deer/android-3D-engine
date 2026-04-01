package org.the3deer.android.engine;

import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.the3deer.android.engine.model.Model;
import org.the3deer.android.engine.model.ModelEvent;
import org.the3deer.android.engine.model.Node;
import org.the3deer.android.engine.model.Object3D;
import org.the3deer.android.engine.model.Scene;
import org.the3deer.android.engine.model.Screen;
import org.the3deer.util.event.EventListener;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class ModelEngineViewModel extends AndroidViewModel implements ComponentCallbacks2 {

    private final String TAG = "ModelEngineViewModel";

    public enum MemoryStatus {
        OK, WARNING, CRITICAL
    }

    /**
     * OpenGL Screen. Shared across all models to ensure consistent UI.
     */
    private final MutableLiveData<Screen> _glScreen = new MutableLiveData<>(new Screen(640, 480));
    public final LiveData<Screen> glScreen = _glScreen;
    /**
     * Model
     */
    private final MutableLiveData<Map<String, Model>> _models = new MutableLiveData<>(new LinkedHashMap<>());
    /**
     * Engine
     */
    private final MutableLiveData<Map<String, ModelEngine>> _engines = new MutableLiveData<>(new LinkedHashMap<>());
    /**
     * Engine selected by the user.
     */
    private final MutableLiveData<ModelEngine> _activeEngine = new MutableLiveData<>();
    public final LiveData<ModelEngine> activeEngine = _activeEngine;

    /**
     * Loading state per URI. Value is the loading message or null if not loading.
     */
    private final MutableLiveData<Map<String, String>> _loadingState = new MutableLiveData<>(Collections.emptyMap());
    public final LiveData<Map<String, String>> loadingState = _loadingState;

    /**
     * Memory info
     */
    private final MutableLiveData<String> _memoryInfo = new MutableLiveData<>("Memory info...");
    public final LiveData<String> memoryInfo = _memoryInfo;

    /**
     * Memory status for UI feedback (e.g. button color)
     */
    private final MutableLiveData<MemoryStatus> _memoryStatus = new MutableLiveData<>(MemoryStatus.OK);
    public final LiveData<MemoryStatus> memoryStatus = _memoryStatus;

    /**
     * Periodic memory updater
     */
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable memoryUpdater = new Runnable() {
        @Override
        public void run() {
            updateMemoryInfo();
            handler.postDelayed(this, 1000); // Update every second
        }
    };

    public ModelEngineViewModel(Application application) {
        super(application);
        application.registerComponentCallbacks(this);
        initTestModels();
        
        // Start periodic update
        handler.post(memoryUpdater);
    }

    private void initTestModels() {
        // Simple shapes for testing
        Model triangle = createModelForTest("triangle", new float[]{
                0.0f, 0.622008459f, 0.0f,
                -0.5f, -0.311004243f, 0.0f,
                0.5f, -0.311004243f, 0.0f
        });

        Model cube = createModelForTest("cube", new float[]{
                // Front face
                -0.3f, 0.3f, 0.3f, -0.3f, -0.3f, 0.3f, 0.3f, -0.3f, 0.3f,
                -0.3f, 0.3f, 0.3f, 0.3f, -0.3f, 0.3f, 0.3f, 0.3f, 0.3f,
                // Back face
                -0.3f, 0.3f, -0.3f, 0.3f, -0.3f, -0.3f, -0.3f, -0.3f, -0.3f,
                -0.3f, 0.3f, -0.3f, 0.3f, 0.3f, -0.3f, 0.3f, -0.3f, -0.3f,
                // Top face
                -0.3f, 0.3f, -0.3f, -0.3f, 0.3f, 0.3f, 0.3f, 0.3f, 0.3f,
                -0.3f, 0.3f, -0.3f, 0.3f, 0.3f, 0.3f, 0.3f, 0.3f, -0.3f,
                // Bottom face
                -0.3f, -0.3f, -0.3f, 0.3f, -0.3f, -0.3f, 0.3f, -0.3f, 0.3f,
                -0.3f, -0.3f, -0.3f, 0.3f, -0.3f, 0.3f, -0.3f, -0.3f, 0.3f,
                // Left face
                -0.3f, 0.3f, -0.3f, -0.3f, -0.3f, -0.3f, -0.3f, -0.3f, 0.3f,
                -0.3f, 0.3f, -0.3f, -0.3f, -0.3f, 0.3f, -0.3f, 0.3f, 0.3f,
                // Right face
                0.3f, 0.3f, -0.3f, 0.3f, -0.3f, 0.3f, 0.3f, -0.3f, -0.3f,
                0.3f, 0.3f, -0.3f, 0.3f, 0.3f, 0.3f, 0.3f, -0.3f, 0.3f
        });

        Model square = createModelForTest("square", new float[]{
                -0.5f, 0.5f, 0.0f,
                -0.5f, -0.5f, 0.0f,
                0.5f, -0.5f, 0.0f,
                -0.5f, 0.5f, 0.0f,
                0.5f, -0.5f, 0.0f,
                0.5f, 0.5f, 0.0f
        });

        Map<String, Model> models = _models.getValue();
        if (models != null) {
            models.put("triangle", triangle);
            models.put("cube", cube);
            models.put("square", square);
        }
    }

    public ModelEngine getEngine(String uri) {
        Map<String, ModelEngine> engines = _engines.getValue();
        return engines != null ? engines.get(uri) : null;
    }

    public ModelEngine initEngine(String uriString) {
        return initEngine(uriString, null, null);
    }

    public ModelEngine initEngine(String uriString, String name, String type) {

        // get
        Model model = getModel(uriString);

        // init if needed
        if (model == null) {
            model = createModel(uriString, name, type);
        }

        ModelEngine engine = getEngine(uriString);
        if (engine == null) {
            engine = getOrCreateEngine(uriString, model);
        }

        Log.d(TAG, "Model Engine Initialized. uri: " + uriString);
        return engine;
    }

    public void activateEngine(String uriString, Runnable callback) {
        try {
            // Check memory before attempting to load
            if (!isMemoryAvailable()) {

                // make space
                freeMemory(uriString);

                if (!isMemoryAvailable()) {
                    Log.e(TAG, "Critical memory state. Aborting load for: " + uriString);
                    setLoading(uriString, "Error: Critical memory limit reached");
                    _memoryStatus.postValue(MemoryStatus.CRITICAL);
                    return;
                }
            }

            // Initialize engine components (Lightweight)
            final ModelEngine modelEngine = initEngine(uriString);
            if (modelEngine == null) throw new IllegalArgumentException("Engine not initialized");

            // Load 3D model data (Heavyweight) - now handled by the engine itself
            modelEngine.loadAsync(() -> {
                // Update active engine on the UI thread
                _activeEngine.setValue(modelEngine);
                updateMemoryInfo();
            });

            //
            if (callback != null) {
                handler.post(callback);
            }
        } catch (OutOfMemoryError e) {
            // We don't call the callback here to avoid further operations on a failed engine
            Log.e(TAG, "OutOfMemoryError while activating engine for " + uriString, e);
            setLoading(uriString, "Error: Out of memory");
            _memoryStatus.postValue(MemoryStatus.CRITICAL);
            clearCache();
        } catch (Exception e) {
            Log.e(TAG, "Failed to activate engine for " + uriString, e);
            setLoading(uriString, "Error: " + e.getMessage());
        }
    }

    public void startEngine(String uriString, Runnable callback){
        try {

            ModelEngine active = _activeEngine.getValue();
            if (active != null) {

                Log.i(TAG, "Starting engine for " + uriString);

                active.start();

                if (callback != null) {
                    handler.post(callback);
                }
            }
        }catch(Exception ex){
            Log.e(TAG, "Failed to start engine for " + uriString, ex);
        }
    }

    private void freeMemory(String uriString) {
        Log.w(TAG, "Low memory detected. Unloading other models to proceed with: " + uriString);
        setLoading(uriString, "Low memory. Unloading other models...");

        handler.post(() ->
            Toast.makeText(getApplication(), "Unloading inactive models to free memory...", Toast.LENGTH_SHORT).show()
        );

        // Clear cache and try again
        clearCache();
    }

    private boolean isMemoryAvailable() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        long availableMemory = maxMemory - usedMemory;

        // Safety threshold: 32MB
        return availableMemory > 32 * 1024 * 1024;
    }

    private void updateMemoryInfo() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory());
        long maxMemory = runtime.maxMemory();
        long availableMemory = maxMemory - usedMemory;

        long modelMemory = 0;
        ModelEngine active = _activeEngine.getValue();
        if (active != null && active.getModel() != null) {
            modelMemory = active.getModel().getMemoryUsage();
        }

        String info = String.format(Locale.getDefault(), "Memory: %d/%d MB\nModel: %d MB", 
            usedMemory / 1024 / 1024, maxMemory / 1024 / 1024, modelMemory / 1024 / 1024);
        _memoryInfo.postValue(info);

        // Update status based on available memory
        if (availableMemory < 32 * 1024 * 1024) {
            _memoryStatus.postValue(MemoryStatus.CRITICAL);
        } else if (availableMemory < 64 * 1024 * 1024) {
            _memoryStatus.postValue(MemoryStatus.WARNING);
        } else {
            _memoryStatus.postValue(MemoryStatus.OK);
        }
    }

    private ModelEngine getOrCreateEngine(String uriString, Model model) {
        Map<String, ModelEngine> engines = _engines.getValue();
        if (engines == null) return null;

        Log.i(TAG, "Building Engine... uri: " + uriString);

        ModelEngine engine = engines.get(uriString);
        if (engine != null) return engine;

        engine = new ModelEngine(uriString, _glScreen.getValue(), model, getApplication());
        engines.put(uriString, engine);
        _engines.setValue(engines);

        final String finalUriString = uriString;
        engine.add("modelEngineViewModelListener", (EventListener) event -> {
            if (event instanceof ModelEvent) {
                final ModelEvent modelEvent = (ModelEvent) event;
                final String message = modelEvent.getData() != null && modelEvent.getData().containsKey("message") ? Objects.requireNonNull(modelEvent.getData().get("message")).toString() : "Loading...";
                if (modelEvent.getCode() == ModelEvent.Code.LOADING || modelEvent.getCode() == ModelEvent.Code.PROGRESS) {
                    setLoading(finalUriString, message);
                } else if (modelEvent.getCode() == ModelEvent.Code.LOADED || modelEvent.getCode() == ModelEvent.Code.LOAD_ERROR) {
                    setLoading(finalUriString, null);
                    updateMemoryInfo();
                    if (modelEvent.getCode() == ModelEvent.Code.LOAD_ERROR) {
                        _memoryStatus.postValue(MemoryStatus.CRITICAL);
                    }
                }
            }
            return false;
        });

        return engine;
    }

    public void setLoading(String uri, String message) {
        Map<String, String> current = new LinkedHashMap<>(_loadingState.getValue());
        if (message == null) {
            current.remove(uri);
        } else {
            current.put(uri, message);
        }
        _loadingState.postValue(current);
    }

    public Model getModel(String uriString) {
        Map<String, Model> models = _models.getValue();
        return models != null ? models.get(uriString) : null;
    }

    public Model createModel(String uriString) {
        return createModel(uriString, null, null);
    }


    public Model createModel(String uriString, String name, String type) {

        // debug
        Log.i(TAG, "Building Model... uri: " + uriString);

        // create
        Model model = new Model(Uri.parse(uriString), name, type);

        // update
        Map<String, Model> models = _models.getValue();
        if (models != null) {
            models.put(uriString, model);
            _models.setValue(models);
        }

        return model;
    }

    public Model createModelForTest(String uriString, float[] vertices) {
        Model model = new Model(Uri.parse(uriString), uriString, "bin");
        Scene scene = new Scene("Default_" + uriString);
        Node node = new Node("Root");
        node.setMesh(new Object3D(vertices));
        scene.getRootNodes().add(node);
        model.addScene(scene);
        return model;
    }

    public void onSurfaceChanged(int width, int height) {

        // debug
        Log.d(TAG, "onSurfaceChanged: " + width + " x " + height);

        // get screen
        Screen screen = _glScreen.getValue();

        // check
        if (screen == null) throw new IllegalStateException("screen is null at this point");

        // update model
        screen.setSize(width, height);

        // notify
        _glScreen.postValue(screen);

    }

    public ModelEngine getActiveEngine() {
        return activeEngine.getValue();
    }

    public void clearCache() {
        Log.i(TAG, "Clearing inactive engines and models from cache...");
        ModelEngine active = _activeEngine.getValue();
        String activeUri = active != null ? active.id : null;

        Map<String, ModelEngine> engines = _engines.getValue();
        if (engines != null) {
            Iterator<Map.Entry<String, ModelEngine>> it = engines.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, ModelEngine> entry = it.next();
                if (!entry.getKey().equals(activeUri)) {
                    Log.d(TAG, "Closing engine: " + entry.getKey());
                    entry.getValue().close();
                    it.remove();
                }
            }
            _engines.postValue(engines);
        }

        Map<String, Model> models = _models.getValue();
        if (models != null) {
            Iterator<Map.Entry<String, Model>> it = models.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Model> entry = it.next();
                if (!entry.getKey().equals(activeUri)) {
                    Log.d(TAG, "Removing model: " + entry.getKey());
                    // Note: Object3D.dispose() is called in ModelEngine.close()
                    it.remove();
                }
            }
            _models.postValue(models);
        }
        System.gc();
        updateMemoryInfo();
    }

    @Override
    public void onTrimMemory(int level) {
        Log.i(TAG, "onTrimMemory level: " + level);
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW ||
                level == ComponentCallbacks2.TRIM_MEMORY_MODERATE ||
                level == ComponentCallbacks2.TRIM_MEMORY_COMPLETE) {
            clearCache();
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
    }

    @Override
    public void onLowMemory() {
        Log.w(TAG, "onLowMemory received!");
        clearCache();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        handler.removeCallbacks(memoryUpdater);
        getApplication().unregisterComponentCallbacks(this);
        // Shut down all engines to release resources
        Map<String, ModelEngine> engines = _engines.getValue();
        if (engines != null) {
            for (ModelEngine engine : engines.values()) {
                engine.close();
            }
        }
    }
}
