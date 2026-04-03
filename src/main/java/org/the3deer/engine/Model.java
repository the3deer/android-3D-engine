package org.the3deer.engine;

import android.net.Uri;
import android.os.SystemClock;
import android.widget.Toast;

import org.the3deer.engine.android.util.ContentUtils;
import org.the3deer.engine.camera.CameraUtils;
import org.the3deer.engine.model.Camera;
import org.the3deer.engine.model.Material;
import org.the3deer.engine.model.ModelEvent;
import org.the3deer.engine.model.Node;
import org.the3deer.engine.model.Object3D;
import org.the3deer.engine.model.Scene;
import org.the3deer.engine.model.Screen;
import org.the3deer.engine.model.Texture;
import org.the3deer.engine.services.LoadListener;
import org.the3deer.engine.services.collada.ColladaLoaderTask;
import org.the3deer.engine.services.fbx.FbxLoaderTask;
import org.the3deer.engine.services.gltf.GltfLoaderTask;
import org.the3deer.engine.services.stl.STLLoaderTask;
import org.the3deer.engine.services.wavefront.WavefrontLoaderTask;
import org.the3deer.util.event.EventManager;
import org.the3deer.util.io.IOUtils;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

import kotlin.text.Charsets;

/**
 * <p>Main model class representing a 3D model resource and its environment.</p>
 * <p>
 * The model holds the model uri, the arguments and the scene model.  It has also
 * </p>
 */
public class Model implements LoadListener {

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

    private final Uri uri;
    private final String name;
    private final String type;
    private final Map<String, Object> extras;

    @Inject
    private EventManager _eventManager;

    @Inject
    private Camera defaultCamera;

    @Inject
    private Screen screen;

    private Map<String, Scene> scenesMap;
    private Scene activeScene;

    // other variables
    private long startTime;

    public Model(Uri uri, String name, String type) {
        this(uri, name, type, null);
    }

    public Model(Uri uri, String name, String type, Map<String, Object> extras) {
        this.uri = uri;
        this.name = name;
        this.type = type;
        this.extras = extras;

        if (this.uri == null) throw new IllegalArgumentException("Model URI cannot be null");
        if (this.name == null) throw new IllegalArgumentException("Model name cannot be null");
    }

    public String getName() {
        return name;
    }

    public Uri getUri() {
        return uri;
    }

    public String getType() {
        return type;
    }

    public Status getStatus() {
        return status;
    }

    private void setStatus(Status status, String message) {
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

    public Map<String, Object> getExtras() {
        return extras;
    }


    public List<Scene> getScenes() {

        // check
        if (scenesMap == null) return Collections.emptyList();

        // return scenes
        return new ArrayList<>(scenesMap.values());
    }

    public Scene getActiveScene() {
        return activeScene;
    }

    public void setActiveScene(Scene activeScene) {
        this.activeScene = activeScene;
    }

    public void update() {
        if (activeScene != null) {
            activeScene.update();
        }
    }

    public void addScene(Scene scene) {

        // check duplicates
        if (scenesMap != null && scenesMap.containsKey(scene.getName())) {
            logger.severe("Scene with name '" + scene.getName() + "' already exists");
            throw new IllegalArgumentException("Scene with name '" + scene.getName() + "' already exists");
        }

        // log event
        logger.info("addScene: " + scene.getName()+", objects: "+scene.getObjects().size());

        // init map
        if (scenesMap == null) scenesMap = new TreeMap<>();

        // register
        scenesMap.put(scene.getName(), scene);

        // check active scene
        if (activeScene == null) {

            // log event
            logger.info("Activating scene: " + scene.getName());

            // activate default scene
            activeScene = scene;
        }
    }

    public List<Camera> getCameras() {
        List<Camera> cameras = new ArrayList<>();
        if (defaultCamera != null) {
            cameras.add(defaultCamera);
        }
        if (activeScene != null) {
            cameras.addAll(activeScene.getCameras());
        }
        return cameras;
    }

    public long getMemoryUsage() {
        // check
        if (scenesMap == null) return 0;

        // calculate memory usage
        long memory = 0;
        for (Scene scene : scenesMap.values()) {
            memory += scene.getMemoryUsage();
        }
        return memory;
    }

    public void load() {

        logger.info("Loading model... uri: "+ getUri()+", type: "+ getType()+" ---------------------------------- ");

        // register current model to the thread
        CURRENT.set(this);

        try {

            // default uri
            Uri modelUri = getUri();

            // default type
            String modelType = getType();

            // load model
            logger.info("Loading model... uri: " + modelUri);

            // update model
            setStatus(Model.Status.LOADING, "Loading model: " + modelUri);

            // if the model is a zip file, we need to extract it and register the entries as content uris
            if (modelUri.toString().toLowerCase().endsWith(".zip")) {
                final Map<String, byte[]> zipFiles;
                try {
                    zipFiles = ContentUtils.readFiles(new URL(modelUri.toString()));
                    Uri modelFile = null;
                    for (Map.Entry<String, byte[]> zipFile : zipFiles.entrySet()) {

                        final String zipFilename = zipFile.getKey();
                        final int dotIndex = zipFilename.lastIndexOf('.');
                        final String fileExtension;
                        if (dotIndex != -1) {
                            fileExtension = zipFilename.substring(dotIndex);
                        } else {
                            fileExtension = "?";
                        }

                        // register all zip entries

                        String encodedName = URLEncoder.encode(zipFilename, Charsets.UTF_8.name());
                        final Uri pseudoUri = Uri.parse("android://org.the3deer.engine/binary/" + encodedName);
                        ContentUtils.addUri(encodedName, pseudoUri);

                        encodedName = encodedName.replace("+", "%20");
                        final Uri pseudoUri2 = Uri.parse("android://org.the3deer.engine/binary/" + encodedName);
                        ContentUtils.addUri(encodedName, pseudoUri2);

                        ContentUtils.addData(pseudoUri, zipFile.getValue());
                        ContentUtils.addData(pseudoUri2, zipFile.getValue());

                        // detect model
                        switch (fileExtension.toLowerCase()) {
                            case ".obj":
                            case ".stl":
                            case ".dae":
                            case ".gltf":
                            case ".glb":
                            case ".fbx":
                                modelFile = pseudoUri;
                                modelUri = Uri.parse(pseudoUri.toString());
                                break;
                        }
                    }
                    if (modelFile == null) {
                        logger.severe("Model not found in zip '" + modelUri + "'");
                    } else {
                        logger.info("Model found in zip: " + modelFile);
                    }
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error loading zip file '" + modelUri + "': " + e.getMessage(), e);
                }
            }

            if (modelUri.toString().toLowerCase().endsWith(".obj") || "obj".equalsIgnoreCase(modelType)) {
                new WavefrontLoaderTask(modelUri, this).execute(false);
            } else if (modelUri.toString().toLowerCase().endsWith(".stl") || "stl".equalsIgnoreCase(modelType)) {
                logger.info("Loading STL object from: " + modelUri);
                new STLLoaderTask(modelUri, this).execute(false);
            } else if (modelUri.toString().toLowerCase().endsWith(".dae") || "dae".equalsIgnoreCase(modelType)) {
                logger.info("Loading Collada object from: " + modelUri);
                new ColladaLoaderTask(modelUri, this).execute(false);
            } else if (modelUri.toString().toLowerCase().endsWith(".gltf") || modelUri.toString().toLowerCase().endsWith(".glb") || "gltf".equalsIgnoreCase(modelType)) {
                logger.info("Loading GLTF object from: " + modelUri);
                new GltfLoaderTask(modelUri, this).execute(false);
            } else if (modelUri.toString().toLowerCase().endsWith(".fbx")) {
                new FbxLoaderTask(modelUri, this).execute(false);
            }

            // log success
            logger.info("Loading model finished -------------------------------------- ");

            // update model
            setStatus(Model.Status.OK, "Loading Model finished successfully");

        } finally {
            // unregister current model from the thread
            // we don't remove it here because the loader tasks might still be running in background threads
            // FIXME: CURRENT.remove();
        }
    }

    @Override
    public void onLoadStart() {
        // register current model to the thread
        CURRENT.set(this);

        // mark start time
        this.startTime = SystemClock.uptimeMillis();

        setStatus(Model.Status.LOADING, "Loading started...");
    }

    @Override
    public void onProgress(String progress) {
        // update model
        setStatus(Model.Status.LOADING, progress);
    }

    @Override
    public void onLoadCamera(Scene scene, Camera camera) {
        // fix aspect ratio
        if (screen != null) {
            camera.getProjection().setAspectRatio(screen.getRatio());
        }
    }

    @Override
    public void onLoadError(Exception ex) {
        logger.log(Level.SEVERE, ex.getMessage(), ex);
        // update model
        setStatus(Model.Status.ERROR, ex.getMessage());
    }

    @Override
    public void onLoadObject(Scene scene, Object3D data) {
        scene.addObject(data);
    }

    @Override
    public void onLoadScene(Scene scene) {
        // configure default camera
        logger.fine("Initializing scene... name: " + scene.getName());
        scene.setActiveCamera(defaultCamera);

        // get objects
        final List<Object3D> objects = scene.getObjects();

        // check
        if (objects.isEmpty()) {
            logger.warning("No objects were loaded");
        } else {
            logger.fine("onLoadScene: " + scene.getName() + ", Objects: " + objects.size());
        }

        for (int i = 0; i < objects.size(); i++) {
            for (int m = 0; m < objects.size(); m++) {

                // get material
                final Material material = objects.get(m).getMaterial();
                if (material == null) continue;

                // init color texture
                final Texture colorTexture = material.getColorTexture();
                if (colorTexture != null) loadTextureDatas(colorTexture);

                final Texture normalTexture = material.getNormalTexture();
                if (normalTexture != null) loadTextureDatas(normalTexture);
            }
        }

        // initialize scene
        scene.update();

        // show object errors
        List<String> allErrors = new ArrayList<>();
        for (Object3D data : objects) {
            allErrors.addAll(data.getErrors());
        }
        if (!allErrors.isEmpty()) {
            makeToastText(allErrors.toString(), Toast.LENGTH_LONG);
        }

        // Ensure all objects have a parent node to unify the rendering pipeline.
        if (scene.getRootNodes() == null || scene.getRootNodes().isEmpty()) {
            logger.info("Scene has no root nodes. Creating default nodes for all objects.");
            List<Node> rootNodes = new ArrayList<>();
            for (Object3D obj : objects) {
                // Create a new node and assign the object to it.
                Node node = new Node();
                node.setMesh(obj); // Link the visible object to this node.
                node.setMatrix(obj.getModelMatrix());
                obj.setParentNode(node);
                obj.setParentBound(true);
                rootNodes.add(node);
            }
            scene.getRootNodes().addAll(rootNodes);
        }

        // fame camera
        CameraUtils.frameModel(scene.getActiveCamera(), scene.getObjects());

        // register scene
        this.addScene(scene);

        // notify user
        final String elapsed = (SystemClock.uptimeMillis() - startTime) / 1000 + " secs";
        makeToastText("Load complete (" + elapsed + ")", Toast.LENGTH_SHORT);
    }

    @Override
    public void onLoadComplete() {
        // initialize model
        if (scenesMap == null || scenesMap.isEmpty()) {
            logger.warning("No scenes available");
        } else if (this.getActiveScene() == null){
            logger.info("No active scene. Setting first scene as active.");
            this.setActiveScene(scenesMap.values().iterator().next());
            this.getActiveScene().update();
        }

        // update model
        this.update();

        // log success
        logger.info("Model loaded successfully");

        // unregister current model from the thread
        CURRENT.remove();
    }

    private void loadTextureDatas(Texture texture) {

        // check texture
        if(texture == null) throw new IllegalArgumentException("Texture cannot be null");;

        // check texture data
        if(texture.getData() != null) return; // already loaded

        // check file
        if (texture.getFile() == null) return;

        // get file
        final String textureFile = texture.getFile().replace('\\','/').replace(' ', '+');

        // Resolve texture URI relative to the model's location
        // Extracting the parent path manually from the model's URI
        final String modelPath = getUri().toString();
        int lastSlash = modelPath.lastIndexOf('/');
        final String parentPath = (lastSlash != -1) ? modelPath.substring(0, lastSlash + 1) : "";

        // Create the full texture URI
        final Uri textureUri = Uri.parse(parentPath + textureFile);

        // update model
        texture.setUri(textureUri);

        // debug
        logger.fine("Downloading texture... file: " + textureFile + ", uri: " + textureUri);

        // debug
        logger.info("Loading texture file: " + textureFile);

        // download texture
        try (InputStream stream = URI.create(textureUri.toString()).toURL().openStream()) {

            // update model
            texture.setData(IOUtils.read(stream));

            // debug
            logger.info("Texture successfully loaded: " + textureFile);

        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error loading texture file '" + textureFile + "': " + ex.getMessage(), ex);
            makeToastText("Error loading texture file '" + textureFile + "': " + ex.getMessage(), Toast.LENGTH_LONG);
        }
    }

    private void makeToastText(final String text, final int toastDuration) {
        // FIXME: how to show toast from here?
    }

    public static class Metadata {
        public final String type;
        public final Map<String, Object> extras;

        public Metadata(String type, Map<String, Object> extras) {
            this.type = type;
            this.extras = extras;
        }
    }
}
