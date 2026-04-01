
package org.the3deer.android.engine.scene;

import android.net.Uri;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import org.the3deer.android.engine.camera.CameraUtils;
import org.the3deer.android.engine.model.Camera;
import org.the3deer.android.engine.model.Material;
import org.the3deer.android.engine.model.Model;
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
import org.the3deer.util.bean.BeanFactory;
import org.the3deer.util.bean.BeanInit;
import org.the3deer.util.event.EventManager;
import org.the3deer.util.io.IOUtils;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import kotlin.text.Charsets;

/**
 * This component loads the model into the engine
 */
public class SceneLoader implements LoadListener {

    private final static String TAG = SceneLoader.class.getSimpleName();

    // dependencies
    @Inject
    private Model sceneManager;
    @Inject
    private Camera defaultCamera;
    @Inject
    private EventManager eventManager;
    @Inject
    private Screen screen;
    @Inject
    private Model model;

    // other variables
    private long startTime;

    public SceneLoader() {
    }

    @BeanFactory.OnBeanUpdate
    public boolean onBeanUpdate(String id, Object updated) {
        Log.v(TAG, "onBeanUpdate: " + id);
        if ("extras".equals(id)) {
            //loadFromBundle(extras);
        }
        return true;
    }

    @BeanInit
    public void setUp() throws MalformedURLException {

        Log.i(TAG, "Loading model... uri: "+model.getUri()+", type: "+model.getType());

        // default uri
        Uri modelUri = model.getUri();

        // default type
        String modelType = model.getType();

        // load model
        Log.i(TAG, "Loading model... " + this.sceneManager.getUri());

        // if the model is a zip file, we need to extract it and register the entries as content uris

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
                    //activity.runOnUiThread(() -> Toast.makeText(activity, "Model not found in zip '" + modelUri + "'", Toast.LENGTH_LONG).show());
                } else {
                    Log.i(TAG, "Model found in zip: " + modelFile);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading zip file '" + modelUri + "': " + e.getMessage(), e);
                //activity.runOnUiThread(() -> Toast.makeText(activity, "Error loading zip file '" + modelUri + "': " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }

        if (modelUri.toString().toLowerCase().endsWith(".obj") || "obj".equalsIgnoreCase(modelType)) {
            new WavefrontLoaderTask(modelUri, SceneLoader.this).execute();
        } else if (modelUri.toString().toLowerCase().endsWith(".stl") || "stl".equalsIgnoreCase(modelType)) {
            Log.i(TAG, "Loading STL object from: " + modelUri);
            new STLLoaderTask(modelUri, SceneLoader.this).execute();
        } else if (modelUri.toString().toLowerCase().endsWith(".dae") || "dae".equalsIgnoreCase(modelType)) {
            Log.i(TAG, "Loading Collada object from: " + modelUri);
            new ColladaLoaderTask(modelUri, SceneLoader.this).execute();
        } else if (modelUri.toString().toLowerCase().endsWith(".gltf") || modelUri.toString().toLowerCase().endsWith(".glb") || "gltf".equalsIgnoreCase(modelType)) {
            Log.i(TAG, "Loading GLTF object from: " + modelUri);
            new GltfLoaderTask(modelUri, SceneLoader.this).execute();
        } else if (modelUri.toString().toLowerCase().endsWith(".fbx")) {
            new FbxLoaderTask(modelUri, SceneLoader.this).execute();
        }
        // });
    }

    @Override
    public void onLoadStart() {

        // mark start time
        this.startTime = SystemClock.uptimeMillis();

        // Android UI thread
        //this.handler = new Handler(Looper.getMainLooper());
        //Looper.prepare();

        // provide context to allow reading resources
        //ContentUtils.setContext(activity);

        if (eventManager != null) {
            eventManager.propagate(new ModelEvent(this, ModelEvent.Code.LOADING));
        }
    }

    @Override
    public void onProgress(String progress) {
        if (eventManager != null) {
            eventManager.propagate(new ModelEvent(this, ModelEvent.Code.PROGRESS, Collections.singletonMap("message", progress)));
        }
    }

    @Override
    public void onLoadCamera(Scene scene, Camera camera) {

        // fix aspect ratio
        camera.getProjection().setAspectRatio(screen.getRatio());
    }

    @Override
    public void onLoadError(Exception ex) {
        Log.e(TAG, ex.getMessage(), ex);

        // notify
        if (eventManager != null){
            eventManager.propagate(new ModelEvent(this, ModelEvent.Code.LOAD_ERROR, Collections.singletonMap("message", ex.getMessage())));
        }
    }

    @Override
    public void onLoadObject(Scene scene, Object3D data) {
        scene.addObject(data);
    }

    @Override
    public void onLoadScene(Scene scene) {
        //if (this.sceneManager == null) return;

        // configure default camera
        Log.i(TAG, "Initializing scene... name: " + scene.getName());
        scene.setActiveCamera(defaultCamera);

        // get objects
        final List<Object3D> objects = scene.getObjects();

        // check
        if (objects.isEmpty()) {
            Log.w(TAG, "No objects were loaded");
        } else {
            Log.d(TAG, "onSceneComplete: " + scene.getName() + ", Objects: " + objects.size());
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
        this.sceneManager.addScene(scene);

        // notify user
        final String elapsed = (SystemClock.uptimeMillis() - startTime) / 1000 + " secs";
        makeToastText("Load complete (" + elapsed + ")", Toast.LENGTH_SHORT);
    }

    @Override
    public void onLoadComplete() {

        // initialize model
        if (this.sceneManager.getScenes().isEmpty()) {
            Log.w(TAG, "No scenes available");
        } else if (this.sceneManager.getActiveScene() == null){
            Log.i(TAG, "No active scene. Setting first scene as active.");
            this.sceneManager.setActiveScene(this.sceneManager.getScenes().get(0));
            this.sceneManager.getActiveScene().update();
        }

        this.sceneManager.update();

        // debug
        Log.i(TAG, "Load complete");

        if (eventManager != null) {
            eventManager.propagate(new ModelEvent(this, ModelEvent.Code.LOADED));
        }
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
        final String modelPath = model.getUri().toString();
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
        //if (activity == null) return;
        //activity.runOnUiThread(() -> Toast.makeText(activity.getApplicationContext(), text, toastDuration).show());
    }
}
