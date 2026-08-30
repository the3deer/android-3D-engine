package org.the3deer.android.engine;

import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.jetbrains.annotations.NotNull;
import org.the3deer.android.engine.api.EngineAPI;
import org.the3deer.android.engine.camera.CameraUtils;
import org.the3deer.android.engine.event.CameraEvent;
import org.the3deer.android.engine.event.EngineEvent;
import org.the3deer.android.engine.logger.LogInterceptor;
import org.the3deer.android.engine.model.Camera;
import org.the3deer.android.engine.model.ModelEvent;
import org.the3deer.android.engine.model.Node;
import org.the3deer.android.engine.model.Object3D;
import org.the3deer.android.engine.model.Scene;
import org.the3deer.android.engine.model.Screen;
import org.the3deer.android.engine.model.Texture;
import org.the3deer.android.engine.services.LoadListener;
import org.the3deer.android.engine.services.LoaderRegistry;
import org.the3deer.android.engine.services.LoaderTask;
import org.the3deer.util.event.EventListener;
import org.the3deer.util.event.EventManager;
import org.the3deer.util.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ModelEngineViewModel extends AndroidViewModel implements ComponentCallbacks2 {

    private static final Logger logger = Logger.getLogger(ModelEngineViewModel.class.getSimpleName());

    static {
        // Configure Global Java Logging Interceptor
        final Logger rootLogger = Logger.getLogger("");
        rootLogger.setLevel(Level.ALL);
        // Remove existing instances if any (e.g. during dev/re-deploy)
        for (java.util.logging.Handler handler : rootLogger.getHandlers()) {
            if (handler instanceof LogInterceptor) {
                rootLogger.removeHandler(handler);
            }
        }
        rootLogger.addHandler(new LogInterceptor());
    }

    /**
     * Singleton-like access for non-UI components (e.g. AI Assistants, AppFunctions)
     */
    public static ModelEngineViewModel INSTANCE;

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
     * Request to load a new model and navigate to it.
     */
    private final MutableLiveData<Model> _loadRequest = new MutableLiveData<>();
    public final LiveData<Model> loadRequest = _loadRequest;


    /**
     * Background executor for heavy loading operations.
     */
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    /**
     * Main thread handler for UI updates and callbacks.
     */
    private final Handler handler = new Handler(Looper.getMainLooper());

    /**
     * Screenshot request
     */
    private CompletableFuture<Bitmap> screenshotRequest;

    public ModelEngineViewModel(Application application) {
        super(application);
        application.registerComponentCallbacks(this);
        INSTANCE = this;
    }

    public ModelEngine getEngine(@NotNull String uri) {
        Map<String, ModelEngine> engines = _engines.getValue();
        return engines != null ? engines.get(uri) : null;
    }

    /**
     * Initialize the engine for the given model.
     *
     * @param model the model entity
     * @param callback the callback to invoke when the engine is initialized
     */
    public ModelEngine initEngine(@NotNull Model model, Runnable callback) {

        if (callback == null) {
            // sync
            return initEngineImpl(model);
        } else {
            // async
            executor.execute(() -> {
                initEngineImpl(model);
                handler.post(callback);
            });
            return null;
        }
    }

    @Nullable
    private ModelEngine initEngineImpl(@NonNull Model model) {
        final String uriString = model.getUri().toString();

        // Ensure the model is registered in our map
        Map<String, Model> models = _models.getValue();
        if (models != null && !models.containsKey(uriString)) {
            models.put(uriString, model);
            _models.postValue(models);
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
                // update status
                updateEngineStatus(uriString, ModelEngine.Status.LOADING, "Info: Loading Engine...");

                // Load engine beans
                engine.load();

                if (isMemoryExhausted()){
                    logger.warning("Engine load aborted. Low memory (MB): "+getAvailableMemory());
                    return;
                }

                // update status
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
                    // Start the model loading process
                    loadModel(engine);
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
        final GLSurfaceView surface = engine.getBeanFactory().find(GLSurfaceView.class);
        if (surface != null) {
            surface.queueEvent(engine::reset);
        } else {
            // Fallback to current thread (might miss some GL resource deletion if no context)
            engine.reset();
        }
    }

    private void freeMemory(boolean freeActive) {
        logger.warning("Low memory detected");
        handler.post(() ->
                Toast.makeText(getApplication(), "Unloading inactive models to free memory...", Toast.LENGTH_SHORT).show()
        );
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
        long availableMemory = getAvailableMemory() * 1024 * 1024;
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
                    if (status == Model.Status.ERROR || status == Model.Status.WARNING) {
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
        logger.info("Building Model... uri: " + uriString);

        // create
        Model model = null;
        try {
            model = new Model(URI.create(uriString));
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

    @NotNull
    public List<URI> getLoadedModels() {
        final Map<String, ModelEngine> engines = _engines.getValue();
        final List<URI> ret = new ArrayList<>();
        if (engines != null) {
            for (String uri : engines.keySet()) {
                try {
                    ret.add(URI.create(uri));
                } catch (Exception ignored) {
                }
            }
        }
        return ret;
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

    public synchronized CompletableFuture<Bitmap> requestScreenshot() {
        this.screenshotRequest = new CompletableFuture<>();
        return this.screenshotRequest;
    }

    public synchronized CompletableFuture<Bitmap> getScreenshotRequest() {
        return this.screenshotRequest;
    }

    public synchronized void fulfillScreenshotRequest(Bitmap bitmap) {
        if (this.screenshotRequest != null) {
            this.screenshotRequest.complete(bitmap);
            this.screenshotRequest = null;
        }
    }

    // --- Helpers ---

    /**
     * Returns the EngineAPI for the currently active engine.
     */
    @Nullable
    public EngineAPI getApi() {
        ModelEngine active = getActiveEngine();
        return active != null ? active.getBeanFactory().find(EngineAPI.class) : null;
    }

    /**
     * Request to load a new model. This will trigger navigation to HomeFragment in the UI.
     * @param model the model to load
     */
    public void loadModel(@NotNull Model model) {
        _loadRequest.postValue(model);
    }

    /**
     * Triggers a reload of the current model for the given engine.
     * Useful when model technical data (like URI or glTF content) has changed.
     */
    public void reloadModel(@NotNull ModelEngine engine) {
        executor.execute(() -> {
            try {
                loadModel(engine);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to reload model", e);
                updateEngineStatus(engine.id, ModelEngine.Status.ERROR, "Error: " + e.getMessage());
            }
        });
    }

    // --- Loading Implementation ---

    private void loadModel(ModelEngine engine) throws IOException {
        Model model = engine.getModel();

        if (model.getType() == null || model.getType().isEmpty()) {
            String fileName = getFileName(model.getUri());
            if (fileName != null && fileName.contains(".")) {
                model.setType(fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase());
            }
        }

        logger.info("Loading model... uri: " + model.getUriModel() + ", type: " + model.getType());
        Model.CURRENT.set(model);
        model.setStatus(Model.Status.LOADING, "Loading model: " + model.getUriModel());

        // ZIP extraction logic
        if ("zip".equalsIgnoreCase(model.getType()) || model.getUriModel().toString().toLowerCase().endsWith(".zip")) {

            if (model.getCacheDir() == null) throw new IllegalStateException("Cache directory not set for ZIP extraction");

            // Create a unique cache directory for this zip
            final String folderName = (model.getName() != null) 
                ? model.getName().replace(".", "_") : "temp_zip_" + System.currentTimeMillis();
            final File zipCacheDir = new File(model.getCacheDir(), "models/" + folderName);
            if (!zipCacheDir.exists() && !zipCacheDir.mkdirs()) throw new IOException("Failed to create cache directory");

            try (InputStream is = model.getUriModel().toURL().openStream()) {
                IOUtils.extract(is, zipCacheDir);
            }

            URI modelFile = null;
            List<File> allFiles = new ArrayList<>();
            scanFiles(zipCacheDir, allFiles);
            for (File file : allFiles) {
                final String relativePath = zipCacheDir.toURI().relativize(file.toURI()).getPath();
                if (relativePath.startsWith("__MACOSX") || relativePath.contains("/._")) continue;
                model.addUri(relativePath, file.toURI());
                if (modelFile == null && file.getName().matches("(?i).*\\.(obj|stl|dae|gltf|fbx|glb)")) {
                    modelFile = file.toURI();
                    model.setType(file.getName().substring(file.getName().lastIndexOf('.') + 1).toLowerCase());
                }
            }
            if (modelFile != null) {
                model.setUriModel(modelFile);
                model.setCurrentDir(new File(modelFile.getPath()).getParentFile());
            } else {
                throw new IllegalArgumentException("No supported model found in zip");
            }
        } else if ("file".equals(model.getUri().getScheme())) {
            model.setCurrentDir(new File(model.getUri().getPath()).getParentFile());
        }

        final LoadListener listener = new LoadListener() {
            @Override public void onLoadStart() { model.setStatus(Model.Status.LOADING, "Loading started..."); }
            @Override public void onProgress(String progress) { model.setStatus(Model.Status.LOADING, progress); }
            @Override public void onLoadCamera(Scene scene, Camera camera) {
                if (glScreen != null) {
                    camera.getProjection().setAspectRatio(-1);
                }
            }
            @Override public void onLoadError(Exception ex) { model.setStatus(Model.Status.ERROR, ex.getMessage()); Model.CURRENT.remove(); }
            @Override public void onLoadObject(Scene scene, Object3D data) { scene.addObject(data); }
            @Override public void onLoadScene(Scene scene) {
                // Ensure the scene has a camera to work with
                if (scene.getActiveCamera() == null) {
                    final Map<String, Camera> cameras = model.getCameras();
                    if (!cameras.isEmpty()) {
                        final Camera defaultCamera = cameras.values().iterator().next();
                        scene.setActiveCamera(defaultCamera);
                        scene.addCamera(defaultCamera);
                    }
                }

                for (Object3D obj : scene.getObjects()) {
                    if (obj.getMaterial() != null) {
                        if (obj.getMaterial().getColorTexture() != null) loadTextureDatas(model, obj.getMaterial().getColorTexture());
                        if (obj.getMaterial().getNormalTexture() != null) loadTextureDatas(model, obj.getMaterial().getNormalTexture());
                    }
                }
                if (scene.getRootNodes().isEmpty()) {
                    for (Object3D obj : scene.getObjects()) {
                        Node node = new Node();
                        node.setMesh(obj);
                        obj.setParentNode(node);
                        scene.getRootNodes().add(node);
                    }
                }

                scene.update();

                model.addScene(scene);
            }
            @Override public void onLoadComplete() {

                if (model.getScenes().isEmpty()) {
                    logger.warning("No scenes available");
                } else if (model.getActiveScene() == null) {
                    model.setActiveScene(model.getScenes().values().iterator().next());
                    model.getActiveScene().update();
                }

                if (model.getActiveScene() != null && model.getActiveScene().getActiveCamera() == null) {
                    if (model.getCameras().isEmpty()) {
                        logger.warning("No cameras available");
                    } else {
                        model.getActiveScene().setActiveCamera(model.getCameras().values().iterator().next());
                    }
                }

                // frame system cameras
                if (model.getActiveScene() != null){
                    final EventManager eventManager = engine.getBeanFactory().find(EventManager.class);
                    for (Camera camera : model.getCameras().values()) {
                        CameraUtils.frameModel(camera, model.getActiveScene().getObjects(), false);
                        if (eventManager != null) {
                            eventManager.propagate(new CameraEvent(this, camera, CameraEvent.Code.CAMERA_UPDATED));
                        }
                    }
                }

                model.update();

                model.setStatus(Model.Status.OK, "Loading Model finished successfully");

                Model.CURRENT.remove();
            }
        };

        final LoaderTask loaderTask = LoaderRegistry.get(model, listener);

        if (loaderTask != null) {
            loaderTask.execute(false);
        } else if ("custom".equalsIgnoreCase(model.getType()) || !model.getScenes().isEmpty()) {
            // Manual trigger for pre-populated models (CSG/AI)
            logger.info("Triggering manual load sequence for custom model...");
            listener.onLoadStart();
            if (model.getScenes().isEmpty()) {
                logger.log(Level.WARNING, "No scenes loaded. Creating 'default' scene...");
                model.getScenes().put("default", new Scene("default"));
            }
            for (Scene scene : model.getScenes().values()) {
                listener.onLoadScene(scene);
            }
            listener.onLoadComplete();
        } else {
            model.setStatus(Model.Status.ERROR, "No loader registered for type: " + model.getType());
        }
    }

    private String getFileName(URI uri) {
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getApplication().getContentResolver().query(android.net.Uri.parse(uri.toString()), null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) return cursor.getString(index);
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error querying content filename", e);
            }
        }
        return IOUtils.getFileName(uri);
    }

    private void scanFiles(File dir, List<File> result) {
        File[] files = dir.listFiles();
        if (files != null) for (File file : files) if (file.isDirectory()) scanFiles(file, result); else result.add(file);
    }

    private void loadTextureDatas(Model model, Texture texture) {
        if (texture == null || texture.getData() != null || texture.getFile() == null) return;
        final URI textureUri = model.getUriModel().resolve(texture.getFile().replace('\\', '/').replace(' ', '+'));
        try (InputStream stream = textureUri.toURL().openStream()) {
            texture.setUri(textureUri);
            texture.setData(IOUtils.read(stream));
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error loading texture file", ex);
        }
    }
}
