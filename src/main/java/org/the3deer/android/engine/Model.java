package org.the3deer.android.engine;

import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import org.the3deer.android.engine.camera.CameraUtils;
import org.the3deer.android.engine.model.Camera;
import org.the3deer.android.engine.model.Material;
import org.the3deer.android.engine.model.ModelEvent;
import org.the3deer.android.engine.model.Node;
import org.the3deer.android.engine.model.Object3D;
import org.the3deer.android.engine.model.Scene;
import org.the3deer.android.engine.model.Screen;
import org.the3deer.android.engine.model.Texture;
import org.the3deer.android.engine.services.LoadListener;
import org.the3deer.android.engine.services.collada.ColladaLoaderTask;
import org.the3deer.android.engine.services.fbx.FbxLoaderTask;
import org.the3deer.android.engine.services.gltf.GltfLoaderTask;
import org.the3deer.android.engine.services.stl.STLLoaderTask;
import org.the3deer.android.engine.services.wavefront.WavefrontLoaderTask;
import org.the3deer.android.util.ContentUtils;
import org.the3deer.util.event.EventManager;
import org.the3deer.util.io.IOUtils;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import kotlin.text.Charsets;

/**
 * <p>Main model class representing a 3D model resource and its environment.</p>
 * <p>
 * The model holds the model uri, the arguments and the scene model.  It has also
 * </p>
 */
public class Model implements LoadListener {

    private final static String TAG = Model.class.getSimpleName();

    public enum Status {
        UNKNOWN, LOADING, OK, WARNING, ERROR
    }

    private Status status = Status.UNKNOWN;

    private String message;

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

    private final List<Scene> scenes = new ArrayList<>();
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

    public void setStatus(Status status) {
        setStatus(status, null);
    }

    public void setStatus(Status status, String message) {
        this.status = status;
        this.message = message;

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

    public Map<String, Object> getExtras() {
        return extras;
    }


    public List<Scene> getScenes() {
        return scenes;
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
        Log.i(TAG, "addScene: " + scene.getName());
        scenes.add(scene);
        if (activeScene == null) {
            Log.i(TAG, "Activating scene: " + scene.getName());
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
        long memory = 0;
        for (Scene scene : scenes) {
            memory += scene.getMemoryUsage();
        }
        return memory;
    }

    public void load() {

        Log.i(TAG, "Loading model... uri: "+ getUri()+", type: "+ getType());

        // default uri
        Uri modelUri = getUri();

        // default type
        String modelType = getType();

        // load model
        Log.i(TAG, "Loading model... " + modelUri);

        // update model
        setStatus(Model.Status.LOADING);

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
                    final Uri pseudoUri = Uri.parse("android://org.the3deer.android.engine/binary/" + encodedName);
                    ContentUtils.addUri(encodedName, pseudoUri);

                    encodedName = encodedName.replace("+", "%20");
                    final Uri pseudoUri2 = Uri.parse("android://org.the3deer.android.engine/binary/" + encodedName);
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
                    Log.e(TAG, "Model not found in zip '" + modelUri + "'");
                } else {
                    Log.i(TAG, "Model found in zip: " + modelFile);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading zip file '" + modelUri + "': " + e.getMessage(), e);
            }
        }

        if (modelUri.toString().toLowerCase().endsWith(".obj") || "obj".equalsIgnoreCase(modelType)) {
            new WavefrontLoaderTask(modelUri, this).execute(false);
        } else if (modelUri.toString().toLowerCase().endsWith(".stl") || "stl".equalsIgnoreCase(modelType)) {
            Log.i(TAG, "Loading STL object from: " + modelUri);
            new STLLoaderTask(modelUri, this).execute(false);
        } else if (modelUri.toString().toLowerCase().endsWith(".dae") || "dae".equalsIgnoreCase(modelType)) {
            Log.i(TAG, "Loading Collada object from: " + modelUri);
            new ColladaLoaderTask(modelUri, this).execute(false);
        } else if (modelUri.toString().toLowerCase().endsWith(".gltf") || modelUri.toString().toLowerCase().endsWith(".glb") || "gltf".equalsIgnoreCase(modelType)) {
            Log.i(TAG, "Loading GLTF object from: " + modelUri);
            new GltfLoaderTask(modelUri, this).execute(false);
        } else if (modelUri.toString().toLowerCase().endsWith(".fbx")) {
            new FbxLoaderTask(modelUri, this).execute(false);
        }
    }

    @Override
    public void onLoadStart() {
        // mark start time
        this.startTime = SystemClock.uptimeMillis();
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
        Log.e(TAG, ex.getMessage(), ex);
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
        Log.d(TAG, "Initializing scene... name: " + scene.getName());
        scene.setActiveCamera(defaultCamera);

        // get objects
        final List<Object3D> objects = scene.getObjects();

        // check
        if (objects.isEmpty()) {
            Log.w(TAG, "No objects were loaded");
        } else {
            Log.d(TAG, "onLoadScene: " + scene.getName() + ", Objects: " + objects.size());
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
            Log.i(TAG, "Scene has no root nodes. Creating default nodes for all objects.");
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
        if (this.getScenes().isEmpty()) {
            Log.w(TAG, "No scenes available");
        } else if (this.getActiveScene() == null){
            Log.i(TAG, "No active scene. Setting first scene as active.");
            this.setActiveScene(this.getScenes().get(0));
            this.getActiveScene().update();
        }

        // update model
        this.update();

        // log success
        Log.i(TAG, "Model loaded successfully");

        // update model status
        setStatus(Model.Status.OK);
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
        Log.d(TAG, "Downloading texture... file: " + textureFile + ", uri: " + textureUri);

        // debug
        Log.i(TAG, "Loading texture file: " + textureFile);

        // download texture
        try (InputStream stream = URI.create(textureUri.toString()).toURL().openStream()) {

            // update model
            texture.setData(IOUtils.read(stream));

            // debug
            Log.i(TAG, "Texture successfully loaded: " + textureFile);

        } catch (Exception ex) {
            Log.e(TAG, "Error loading texture file '" + textureFile + "': " + ex.getMessage(), ex);
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
