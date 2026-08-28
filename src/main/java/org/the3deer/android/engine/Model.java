package org.the3deer.android.engine;

import org.the3deer.android.engine.model.Camera;
import org.the3deer.android.engine.model.ModelEvent;
import org.the3deer.android.engine.model.Scene;
import org.the3deer.android.engine.model.Screen;
import org.the3deer.util.event.EventManager;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

/**
 * <p>Main model class representing a 3D model resource and its environment.</p>
 * <p>
 * This is the ultimate model entity where the URI is the primary identifier.
 * It is used both by the App (UI/Catalogs) and the Engine (Rendering).
 * </p>
 */
public class Model {


    private final static Logger logger = Logger.getLogger(Model.class.getSimpleName());

    /**
     * ThreadLocal to keep track of the model being processed in the current thread.
     * This allows the global LogInterceptor to associate log messages with a specific model.
     */
    public static final ThreadLocal<Model> CURRENT = new ThreadLocal<>();

    public enum Status {
        UNKNOWN, LOADING, OK, WARNING, ERROR
    }

    private Status status = Status.UNKNOWN;

    private String message;

    /**
     * Map of messages by log level.
     * Sorted by level severity (highest first).
     */
    private final Map<Level, String> messages = new TreeMap<>((l1, l2) -> Integer.compare(l2.intValue(), l1.intValue()));

    // uri of the model (Primary Identifier)
    private final URI uri;
    // resolved model uri (e.g. if extracted from zip)
    private URI uriModel;

    // Discovery & Technical information
    private String name;
    private String type;
    private String gltf;
    private String description;
    private String author;
    private String license;
    private String provider;
    private long size;
    private URI screenshot;

    @Inject
    private EventManager _eventManager;

    //@Inject
    private final Map<String,Camera> cameras = new LinkedHashMap<>();

    @Inject
    private Screen screen;

    // runtime
    private Map<String, Scene> scenesMap = new HashMap<>();
    private Scene activeScene;

    // configuration
    private File cacheDir;
    private File currentDir;
    private final Map<String, URI> includes = new HashMap<>();

    public Model(URI uri) {
        this.uri = uri;
        if (this.uri == null) throw new IllegalArgumentException("Model URI cannot be null");
    }

    public File getCacheDir() {
        return cacheDir;
    }

    public void setCacheDir(File cacheDir) {
        this.cacheDir = cacheDir;
    }

    public File getCurrentDir() {
        return currentDir;
    }

    public void setCurrentDir(File currentDir) {
        this.currentDir = currentDir;
    }

    public URI getUri() {
        return uri;
    }

    public URI getUriModel() {
        if (uriModel != null) {
            return uriModel;
        }
        return uri;
    }

    public void setUriModel(URI uriModel) {
        this.uriModel = uriModel;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public Model setType(String type) {
        this.type = type;
        return this;
    }

    public String getGltf() {
        return gltf;
    }

    public void setGltf(String gltf) {
        this.gltf = gltf;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getLicense() {
        return license;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public String getProvider() {
        return provider;
    }

    public Model setProvider(String provider) {
        this.provider = provider;
        return this;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public URI getScreenshot() {
        return screenshot;
    }

    public void setScreenshot(URI screenshot) {
        this.screenshot = screenshot;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status, String message) {
        this.status = status;
        this.message = message;

        logger.info("Status changed: " + this.status + ", message: " + message);
        if (_eventManager != null) {
            _eventManager.propagate(new ModelEvent(this, ModelEvent.Code.STATUS_CHANGED)
                    .setData("status", status).setData("message", message));
        }
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void addMessage(Level level, String message) {
        this.messages.put(level, message);
    }

    public Map<Level, String> getMessages() {
        return messages;
    }

    public Map<String, Scene> getScenes() {
        return scenesMap;
    }

    public Scene getActiveScene() {
        return activeScene;
    }

    public void setActiveScene(Scene activeScene) {
        this.activeScene = activeScene;
        if (_eventManager != null) {
            _eventManager.propagate(new org.the3deer.android.engine.event.SceneEvent(this, org.the3deer.android.engine.event.SceneEvent.Code.SCENE_UPDATED));
        }
    }

    public void update() {
        if (activeScene != null) {
            activeScene.update();
        }
    }

    public void addScene(Scene scene) {
        if (scenesMap != null && scenesMap.containsKey(scene.getName())) {
            logger.warning("Scene with name '" + scene.getName() + "' already exists");
        }

        logger.info("addScene: " + scene.getName() + ", objects: " + scene.getObjects().size());
        if (scenesMap == null) scenesMap = new TreeMap<>();
        scenesMap.put(scene.getName(), scene);

        if (activeScene == null) {
            logger.info("Activating scene: " + scene.getName());
            activeScene = scene;
        }
    }

    public Map<String,Camera> getCameras() {
        final Map<String, Camera> cameras = new LinkedHashMap<>(this.cameras);
        if (activeScene != null) {
            cameras.putAll(activeScene.getCameras());
        }
        return cameras;
    }

    public void addCamera(Camera camera) {
        if (camera.getName() == null) throw new IllegalArgumentException("camera name is null");
        this.cameras.put(camera.getName(), camera);
    }

    public long getMemoryUsage() {
        if (scenesMap == null) return 0;
        long memory = 0;
        for (Scene scene : scenesMap.values()) {
            memory += scene.getMemoryUsage();
        }
        return memory;
    }

    public Map<String, URI> getIncludes() {
        return includes;
    }

    public void addUri(String name, URI uri) {
        includes.put(name, uri);
    }

    public URI getUri(String name) {
        URI uri = includes.get(name);
        if (uri != null) return uri;
        uri = includes.get(name.replaceAll("\\\\","/"));
        if (uri != null) return uri;

        if (currentDir != null) {
            File file = new File(currentDir, name);
            if (file.exists()) {
                return file.toURI();
            }
        }
        return null;
    }

    public void dispose() {
        this.messages.clear();
    }
}
