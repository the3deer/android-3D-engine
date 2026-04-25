package org.the3deer.android.engine;

import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.res.Configuration;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.jetbrains.annotations.NotNull;
import org.the3deer.android.engine.event.EngineEvent;
import org.the3deer.android.engine.model.ModelEvent;
import org.the3deer.android.engine.model.Screen;
import org.the3deer.util.event.EventListener;

import java.net.URI;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ModelEngineViewModel extends AndroidViewModel implements ComponentCallbacks2 {

    private static final Logger logger = Logger.getLogger(ModelEngineViewModel.class.getSimpleName());

    /**
     * OpenGL Screen. Shared across all models to ensure consistent UI. Initialized with some safe value
     */
    private final Screen glScreen = new Screen(640, 480);
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
     * Background executor for heavy loading operations.
     */
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    /**
     * Main thread handler for UI updates and callbacks.
     */
    private final Handler handler = new Handler(Looper.getMainLooper());

    public ModelEngineViewModel(Application application) {
        super(application);
        application.registerComponentCallbacks(this);
        //initTestModels();
    }

    public ModelEngine getEngine(@NotNull String uri) {
        Map<String, ModelEngine> engines = _engines.getValue();
        return engines != null ? engines.get(uri) : null;
    }

    /**
     * Initialize the engine for the given URI. If <code>callback</code> is <code>null</code>, the engine will be initialized synchronously. Otherwise, it will be initialized asynchronously
     *
     * @param uriString the model id
     * @param name the model name
     * @param type the model type
     * @param callback the callback to invoke when the engine is initialized or <code>null</code>
     */
    public ModelEngine initEngine(@NotNull String uriString, String name, String type, Runnable callback) {

        if (callback == null) {
            // sync
            return initEngineImpl(uriString, name, type);
        } else {
            // async
            executor.execute(() -> {
                initEngineImpl(uriString, name, type);
                handler.post(callback);
            });
            return null;
        }
    }

    @Nullable
    private ModelEngine initEngineImpl(@NonNull String uriString, String name, String type) {
        // get
        Model model = getModel(uriString);

        // init if needed
        if (model == null) {
            model = createModel(uriString, name, type);
        }

        ModelEngine engine = getEngine(uriString);
        if (engine == null) {
            engine = getOrCreateEngine(uriString, model);
            logger.info("Model Engine Initialized. uri: " + uriString);
        }

        return engine;
    }

    public void loadEngine(@NotNull String uriString, Runnable callback) {

        // get engine
        ModelEngine engine = getEngine(uriString);
        if (engine == null) throw new IllegalStateException("Engine not initialized");

        // check status
        if (engine.isLoaded()) {
            logger.info("Engine already loaded. uri: " + uriString);
            handler.post(callback);
            return;
        }

        if (isMemoryExhausted()){
            logger.warning("Engine load aborted. Low memory (MB): "+getAvailableMemory());
            return;
        }

        executor.execute(() -> {

            try {
                // Initialize engine components (Lightweight)
                final ModelEngine modelEngine = getEngine(uriString);
                if (modelEngine == null)
                    throw new IllegalArgumentException("Engine not initialized");

                // update status
                updateEngineStatus(uriString, ModelEngine.Status.LOADING, "Info: Loading Engine...");

                // Load 3D model data (Heavyweight) - now handled by the engine itself
                modelEngine.load();

                if (isMemoryExhausted()){
                    logger.warning("Engine load aborted. Low memory (MB): "+getAvailableMemory());
                    return;
                }

                // update engine status
                updateEngineStatus(uriString, ModelEngine.Status.OK, "Info: Engine loaded successfully");

                // log success
                logger.info("Engine loaded. uri: " + uriString);

                // notify
                if (callback != null) {
                    handler.post(callback);
                }

                // collect garbage
                gc();

            } catch (OutOfMemoryError e) {
                logger.log(Level.SEVERE, "OutOfMemoryError while activating engine for " + uriString, e);
                updateEngineStatus(uriString, ModelEngine.Status.ERROR, "Error: Out of memory");
                freeMemory(false);
            } catch (Throwable t) {
                logger.log(Level.SEVERE, "Failed to load engine for " + uriString, t);
                updateEngineStatus(uriString, ModelEngine.Status.ERROR, "Error: " + t.getMessage());
            }
        });
    }

    private boolean isMemoryExhausted() {

        // Check memory before attempting to load
        long availableMemory = getAvailableMemory();

        // Safety threshold: 32MB
        if (availableMemory <= 32) {

            // make space
            freeMemory(false);

            if (getAvailableMemory() <= 32) {
                logger.log(Level.SEVERE, "Critical memory state. Free Memory (MB): "+getAvailableMemory());

                freeMemory(true);

                logger.info("Critical memory state. Free Memory (MB): "+getAvailableMemory());

                return getAvailableMemory() <= 32;
            }
        }
        return false;
    }

    public void startEngine(@NotNull String uriString, Runnable callback) {

        // get engine
        final ModelEngine engine = getEngine(uriString);
        if (engine == null) throw new IllegalStateException("Info: Engine not initialized");

        // check status
        if (engine.isStarted()) {
            logger.info("Engine already started");
            handler.post(callback);
            return;
        }

        try {
            executor.execute(() -> {

                // start engine
                engine.start();

                // invoke callback
                if (callback != null) {
                    handler.post(callback);
                }

                logger.info("Engine started. uri: " + uriString);

                // update engine status
                updateEngineStatus(uriString, ModelEngine.Status.OK, "Info: Engine started successfully");

                try {
                    // Start the model loading process (previously in SceneLoader)
                    engine.getModel().load();
                } catch (OutOfMemoryError e) {
                    logger.log(Level.SEVERE, "OutOfMemoryError during model load: " + uriString, e);
                    updateEngineStatus(uriString, ModelEngine.Status.ERROR, "Error: Out of memory");
                    freeMemory(false);
                } catch (Throwable t) {
                    logger.log(Level.SEVERE, "Critical error during model load: " + t.getMessage(), t);
                    updateEngineStatus(uriString, ModelEngine.Status.ERROR, "Error: " + t.getMessage());
                }

            });
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Failed to start engine for " + uriString, ex);

            // update engine status
            updateEngineStatus(uriString, ModelEngine.Status.ERROR, "Error: " + ex.getMessage());
        }
    }

    public void setActiveEngine(@NotNull String uriString) {

        ModelEngine engine = getEngine(uriString);
        if (engine == null) throw new IllegalStateException("Engine not initialized");

        // notify observers
        _activeEngine.postValue(engine);

        // log success
        logger.info("Engine activated. uri: " + uriString);
    }

    /**
     * Reset the engine resources (GPU and memory).
     * @param uriString the engine uri
     */
    public void resetEngine(@NotNull String uriString) {
        final ModelEngine engine = getEngine(uriString);
        if (engine == null) return;

        logger.info("Requesting engine reset: " + uriString);

        // Try to reset on GL Thread if surface is available
        final Object surface = engine.getBeanFactory().get("gl.surfaceView");
        if (surface instanceof GLSurfaceView) {
            ((GLSurfaceView) surface).queueEvent(engine::reset);
        } else {
            // Fallback to current thread (might miss some GL resource deletion if no context)
            engine.reset();
        }
    }

    private void freeMemory(boolean freeActive) {
        logger.warning("Low memory detected");


        // FIXME: make this an EngineEvent
        handler.post(() ->
                Toast.makeText(getApplication(), "Unloading inactive models to free memory...", Toast.LENGTH_SHORT).show()
        );

        // Clear cache and try again
        clearCache(freeActive);
    }

    private static long getAvailableMemory() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        long availableMemory = maxMemory - usedMemory;
        return availableMemory / (1024 * 1024);
    }

    public void updateMemoryStatus() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory());
        long maxMemory = runtime.maxMemory();
        long availableMemory = maxMemory - usedMemory;

        ModelEngine active = _activeEngine.getValue();
        if (active != null && active.getModel() != null) {
            // Update status based on available memory
            if (availableMemory < 32 * 1024 * 1024) {
                active.setStatus(ModelEngine.Status.ERROR);
            } else if (availableMemory < 64 * 1024 * 1024) {
                active.setStatus(ModelEngine.Status.WARNING);
            } else {
                active.setStatus(ModelEngine.Status.OK);
            }
        }
    }

    private ModelEngine getOrCreateEngine(String uriString, Model model) {
        Map<String, ModelEngine> engines = _engines.getValue();
        if (engines == null) return null;

        logger.info("Building Engine... uri: " + uriString);

        ModelEngine engine = engines.get(uriString);
        if (engine != null) return engine;

        engine = new ModelEngine(uriString, glScreen, model, getApplication());
        engines.put(uriString, engine);


        final String finalUriString = uriString;

        engine.add("modelEngineViewModelListener", (EventListener) event -> {
            if (event instanceof ModelEvent) {
                final ModelEvent modelEvent = (ModelEvent) event;
                if (modelEvent.getCode() == ModelEvent.Code.STATUS_CHANGED) {
                    final Model.Status status = modelEvent.getData("status", Model.Status.class, Model.Status.UNKNOWN);
                    if (status == Model.Status.ERROR) {
                        handler.post(this::updateMemoryStatus);
                    } else if (status == Model.Status.WARNING) {
                        handler.post(this::updateMemoryStatus);
                    }
                    notifyStatusChange(finalUriString);
                }
            }
            return false;
        });

        // update model
        _engines.postValue(engines);

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
        if (engine == null) {
            logger.severe("Engine not initialized. uri: "+uri);
            return;
        }

        // set status
        engine.setStatus(status);
        engine.setMessage(message);

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
     *
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
        }
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
        logger.info("Building Model... uri: " + uriString);

        // create
        Model model = null;
        try {
            model = new Model(URI.create(uriString), name, type);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to create model from uri: " + uriString, e);
        }

        // update
        Map<String, Model> models = _models.getValue();
        if (models != null && model != null) {
            models.put(uriString, model);
            _models.postValue(models);
        }

        return model;
    }

    public ModelEngine getActiveEngine() {
        return activeEngine.getValue();
    }

    public void clearCache(boolean deleteActive) {

        final ModelEngine active = _activeEngine.getValue();
        final String activeUri = active != null ? active.id : null;

        final Map<String, ModelEngine> engines = _engines.getValue();
        if (engines != null) {

            logger.info("Clearing inactive engines and models from cache...");
            Iterator<Map.Entry<String, ModelEngine>> it = engines.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, ModelEngine> entry = it.next();
                if (!entry.getKey().equals(activeUri) || deleteActive) {
                    logger.info("Closing engine: " + entry.getKey());
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
                if (!entry.getKey().equals(activeUri) || deleteActive) {
                    logger.info("Removing model: " + entry.getKey());
                    // Note: Object3D.dispose() is called in ModelEngine.close()
                    entry.getValue().dispose();
                    it.remove();
                }
            }
            _models.postValue(models);
        }


        if (deleteActive) {
            _activeEngine.postValue(null);
        }

        gc();
    }

    private void gc() {
        // [SAFE APPLY] Trigger GC and delay the UI update to reflect actual memory state
        System.gc();
        System.runFinalization();

        // Wait 500ms before updating the UI so the GC has a chance to run
        handler.postDelayed(this::updateMemoryStatus, 500);
    }

    @Override
    public void onTrimMemory(int level) {
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW ||
                level == ComponentCallbacks2.TRIM_MEMORY_MODERATE ||
                level == ComponentCallbacks2.TRIM_MEMORY_COMPLETE) {
            logger.info("onTrimMemory level: " + level);
            clearCache(false);
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
    }

    @Override
    public void onLowMemory() {
        logger.warning("onLowMemory received!");
        clearCache(false);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        getApplication().unregisterComponentCallbacks(this);
        // Shut down all engines to release resources
        Map<String, ModelEngine> engines = _engines.getValue();
        if (engines != null) {
            for (ModelEngine engine : engines.values()) {
                engine.close();
            }
        }
    }

    public Screen getGlScreen() {
        return glScreen;
    }
}
