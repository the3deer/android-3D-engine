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

import org.jetbrains.annotations.NotNull;
import org.the3deer.android.engine.event.EngineEvent;
import org.the3deer.android.engine.model.ModelEvent;
import org.the3deer.android.engine.model.Screen;
import org.the3deer.util.event.EventListener;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ModelEngineViewModel extends AndroidViewModel implements ComponentCallbacks2 {

    private final String TAG = "ModelEngineViewModel";

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
     * Memory info
     */
    private final MutableLiveData<String> _memoryInfo = new MutableLiveData<>("Memory info...");
    public final LiveData<String> memoryInfo = _memoryInfo;

    /**
     * Background executor for heavy loading operations.
     */
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
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
        //initTestModels();

        // Start periodic update
        handler.post(memoryUpdater);
    }

    public ModelEngine getEngine(@NotNull String uri) {
        Map<String, ModelEngine> engines = _engines.getValue();
        return engines != null ? engines.get(uri) : null;
    }

    public ModelEngine initEngine(@NotNull String uriString) {
        return initEngine(uriString, null, null);
    }

    public ModelEngine initEngine(@NotNull String uriString, String name, String type) {

        // get
        Model model = getModel(uriString);

        // init if needed
        if (model == null) {
            model = createModel(uriString, name, type);
        }

        ModelEngine engine = getEngine(uriString);
        if (engine == null) {
            engine = getOrCreateEngine(uriString, model);
            Log.i(TAG, "Model Engine Initialized. uri: " + uriString);
        }

        return engine;
    }

    public void loadEngine(@NotNull String uriString, Runnable callback) {

        // Check memory before attempting to load
        if (isMemoryExhausted()) {

            // make space
            freeMemory(uriString);

                if (isMemoryExhausted()) {
                    Log.e(TAG, "Critical memory state. Aborting load for: " + uriString);
                    updateEngineStatus(uriString, ModelEngine.Status.ERROR, "Error: Critical memory limit reached");
                    return;
                }
            }

        executor.execute(() -> {

            try {
                // Initialize engine components (Lightweight)
                final ModelEngine modelEngine = initEngine(uriString);
                if (modelEngine == null)
                    throw new IllegalArgumentException("Engine not initialized");

                // update status
                updateEngineStatus(uriString, ModelEngine.Status.LOADING, "Info: Loading Engine...");

                // Load 3D model data (Heavyweight) - now handled by the engine itself
                modelEngine.load();

                if (callback != null) {
                    handler.post(callback);
                }

                updateMemoryInfo();

                // log success
                Log.i(TAG, "Engine loaded. uri: " + uriString);

                // update engine status
                updateEngineStatus(uriString, ModelEngine.Status.OK, "Info: Engine loaded successfully");

            } catch (OutOfMemoryError e) {
                // We don't call the callback here to avoid further operations on a failed engine
                Log.e(TAG, "OutOfMemoryError while activating engine for " + uriString, e);
                updateEngineStatus(uriString, ModelEngine.Status.ERROR , "Error: Out of memory");
                clearCache();
            } catch (Exception e) {
                Log.e(TAG, "Failed to activate engine for " + uriString, e);
                updateEngineStatus(uriString, ModelEngine.Status.ERROR, "Error: " + e.getMessage());
            }
        });
    }

    public void startEngine(@NotNull String uriString, Runnable callback) {

        final ModelEngine engine = getEngine(uriString);
        if (engine == null) throw new IllegalStateException("Info: Engine not initialized");

        try {
            executor.execute(() -> {

                // start engine
                engine.start();

                // invoke callback
                if (callback != null) {
                    handler.post(callback);
                }

                // update engine status
                updateEngineStatus(uriString, ModelEngine.Status.OK, "Info: Engine started successfully");

                Log.i(TAG, "Engine started. uri: " + uriString);

                // Start the model loading process (previously in SceneLoader)
                engine.getModel().load();
            });
        } catch (Exception ex) {
            Log.e(TAG, "Failed to start engine for " + uriString, ex);

            // update engine status
            updateEngineStatus(uriString, ModelEngine.Status.ERROR, "Error: "+ex.getMessage());
        }
    }

    public void setActiveEngine(@NotNull String uriString) {

        ModelEngine engine = getEngine(uriString);
        if (engine == null) throw new IllegalStateException("Engine not initialized");

        // notify observers
        _activeEngine.postValue(engine);

        // log success
        Log.i(TAG, "Engine activated. uri: " + uriString);
    }

    private void freeMemory(String uriString) {
        Log.w(TAG, "Low memory detected. Unloading other models to proceed with: " + uriString);
        updateEngineStatus(uriString, ModelEngine.Status.ERROR, "Low memory. Unloading other models...");

        // FIXME: make this an EngineEvent
        handler.post(() ->
                Toast.makeText(getApplication(), "Unloading inactive models to free memory...", Toast.LENGTH_SHORT).show()
        );

        // Clear cache and try again
        clearCache();
    }

    private boolean isMemoryExhausted() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        long availableMemory = maxMemory - usedMemory;

        // Safety threshold: 32MB
        return availableMemory <= 32 * 1024 * 1024;
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

            // Update status based on available memory
            if (availableMemory < 32 * 1024 * 1024) {
                active.status = ModelEngine.Status.ERROR;
            } else if (availableMemory < 64 * 1024 * 1024) {
                active.status = ModelEngine.Status.WARNING;
            } else {
                active.status = ModelEngine.Status.OK;
            }
        }

        String info = String.format(Locale.getDefault(), "Memory: %d/%d MB\nModel: %d MB",
                usedMemory / 1024 / 1024, maxMemory / 1024 / 1024, modelMemory / 1024 / 1024);
        _memoryInfo.postValue(info);
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
                final Model.Status status = modelEvent.getData("status", Model.Status.class, Model.Status.UNKNOWN);
                if (modelEvent.getCode() == ModelEvent.Code.STATUS_CHANGED) {
                    switch (status) {
                        case LOADING:
                            notifyStatusChange(finalUriString);
                            break;
                        case OK:
                        case ERROR:
                            notifyStatusChange(finalUriString);
                            updateMemoryInfo();
                    }
                }
            }
            return false;
        });

        return engine;
    }

    /**
     * Update the status of the engine
     * @param uri
     * @param status
     */
    private void updateEngineStatus(String uri, ModelEngine.Status status, String message) {

        // get engine
        ModelEngine engine = getEngine(uri);
        if (engine == null) throw new IllegalArgumentException("Engine not initialized");

        // set status
        engine.status = status;
        engine.message = message;

        // notify
        _engines.postValue(_engines.getValue());

        // notify if active
        if (engine == _activeEngine.getValue()) {
            _activeEngine.postValue(engine);
        }

        // fire event
        engine.controller().propagate(new EngineEvent(this, EngineEvent.Code.STATUS_CHANGED).setData("status", status));
    }

    /**
     * Update the status of the model
     * @param uri
     */
    private void notifyStatusChange(String uri) {

        // get engine
        Model model = getModel(uri);
        if (model == null) throw new IllegalArgumentException("Engine not initialized");

        // notify
        _models.postValue(_models.getValue());

        // get engine
        ModelEngine engine = getEngine(uri);

        // process
        if (engine != null) {

            // notify observers
            _engines.postValue(_engines.getValue());

            // notify observers
            if (engine == _activeEngine.getValue()) {
                _activeEngine.postValue(engine);
            }
        };
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
