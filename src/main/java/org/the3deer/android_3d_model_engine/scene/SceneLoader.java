
package org.the3deer.android_3d_model_engine.scene;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import org.the3deer.android_3d_model_engine.camera.CameraUtils;
import org.the3deer.android_3d_model_engine.model.Camera;
import org.the3deer.android_3d_model_engine.model.Material;
import org.the3deer.android_3d_model_engine.model.Node;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.model.Scene;
import org.the3deer.android_3d_model_engine.model.Screen;
import org.the3deer.android_3d_model_engine.services.LoadListener;
import org.the3deer.android_3d_model_engine.services.collada.ColladaLoaderTask;
import org.the3deer.android_3d_model_engine.services.fbx.FbxLoaderTask;
import org.the3deer.android_3d_model_engine.services.gltf.GltfLoaderTask;
import org.the3deer.android_3d_model_engine.services.stl.STLLoaderTask;
import org.the3deer.android_3d_model_engine.services.wavefront.WavefrontLoaderTask;
import org.the3deer.util.android.ContentUtils;
import org.the3deer.util.bean.BeanFactory;
import org.the3deer.util.bean.BeanInit;
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
import javax.inject.Named;

import kotlin.text.Charsets;

/**
 * This component loads the model into the engine
 */
public class SceneLoader implements LoadListener {

    private final static String TAG = SceneLoader.class.getSimpleName();

    // dependencies
    @Inject
    private BeanFactory beanFactory;
    @Inject @Named("bundle")
    private Bundle bundle;
    @Inject @Named("extras")
    private Bundle extras;
    @Inject
    private Activity activity;
    @Inject
    private SceneManager sceneManager;
    @Inject
    private Camera defaultCamera;
    @Inject
    private EventManager eventManager;
    @Inject
    private Screen screen;

    //private Handler handler;
    /**
     * Sets whether the Demo Objects should be loaded
     */
    private boolean isDemo;
    /**
     * The file to load. Passed as input parameter
     */
    private URI modelUri;
    /**
     * Type of model if file name has no extension (provided though content provider)
     */
    private String modelType;
    /**
     * Background GL clear color. Default is light gray
     */
    private float[] backgroundColor = new float[]{0.0f, 0.0f, 0.0f, 1.0f};

    // other variables
    private long startTime;

    public SceneLoader() {


    }

    @BeanFactory.OnBeanUpdate
    public boolean onBeanUpdate(String id, Object updated){
        Log.v(TAG, "onBeanUpdate: " + id);
        if ("extras".equals(id)){
            loadFromBundle(extras);
        }
        return true;
    }

    @BeanInit
    public void setUp() {

        loadFromBundle(extras);
    }

    public void loadFromBundle(Bundle extras) {

        // check
/*        if (handler == null) {
            Log.e(TAG, "Handler is null");
            return;
        }*/

        // Try to get input parameters
        if (extras == null) {
            Log.i(TAG, "Bundle is null");
            return;
        }


        if (extras.containsKey("uri")) {
            try {

                this.isDemo = extras.getBoolean("demo");
                this.modelUri = new URI(extras.getString("uri"));
                this.modelType = extras.getString("type");

                Log.i(TAG, "uri '" + modelUri + "', type: " + modelType);

                if (extras.getString("backgroundColor") != null) {
                    String[] backgroundColors = extras.getString("backgroundColor").split(" ");
                    backgroundColor[0] = Float.parseFloat(backgroundColors[0]);
                    backgroundColor[1] = Float.parseFloat(backgroundColors[1]);
                    backgroundColor[2] = Float.parseFloat(backgroundColors[2]);
                    backgroundColor[3] = Float.parseFloat(backgroundColors[3]);
                }
            } catch (Exception ex) {
                Log.e(TAG, "Error parsing activity parameters: " + ex.getMessage(), ex);
                activity.runOnUiThread(() -> {
                    Toast.makeText(activity, "There was a problem building the model: " + ex.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        }

        if (modelUri == null && !isDemo){
            return;
        }


        // handler.post(() -> {

            // load model
            Log.i(TAG, "Loading model... " + this.modelUri);

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
                        final Uri pseudoUri = Uri.parse("android://" + activity.getPackageName() + "/binary/" + encodedName);
                        ContentUtils.addUri(encodedName, pseudoUri);

                        encodedName = encodedName.replace("+","%20");
                        final Uri pseudoUri2 = Uri.parse("android://" + activity.getPackageName() + "/binary/" + encodedName);
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
                                modelUri = new URI(pseudoUri.toString());
                                break;
                        }
                    }
                    if (modelFile == null) {
                        Log.e(TAG, "Model not found in zip '" + modelUri + "'");
                        Toast.makeText(activity, "Model not found in zip '" + modelUri + "'", Toast.LENGTH_LONG).show();
                    } else {
                        Log.i(TAG, "Model found in zip: " + modelFile);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error loading zip file '" + modelUri + "': " + e.getMessage(), e);
                    Toast.makeText(activity, "Error loading zip file '" + modelUri + "': " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }

            if (modelUri.toString().toLowerCase().endsWith(".obj") || "obj".equalsIgnoreCase(modelType)) {
                new WavefrontLoaderTask(activity, modelUri, SceneLoader.this).execute();
            } else if (modelUri.toString().toLowerCase().endsWith(".stl") || "stl".equalsIgnoreCase(modelType)) {
                Log.i(TAG, "Loading STL object from: " + modelUri);
                new STLLoaderTask(activity, modelUri, SceneLoader.this).execute();
            } else if (modelUri.toString().toLowerCase().endsWith(".dae") || "dae".equalsIgnoreCase(modelType)) {
                Log.i(TAG, "Loading Collada object from: " + modelUri);
                new ColladaLoaderTask(activity, modelUri, SceneLoader.this).execute();
            } else if (modelUri.toString().toLowerCase().endsWith(".gltf") || modelUri.toString().toLowerCase().endsWith(".glb") || "gltf".equalsIgnoreCase(modelType)) {
                Log.i(TAG, "Loading GLTF object from: " + modelUri);
                new GltfLoaderTask(activity, modelUri, SceneLoader.this).execute();
            } else if (modelUri.toString().toLowerCase().endsWith(".fbx")){
                new FbxLoaderTask(activity, modelUri, SceneLoader.this).execute();
            }
        // });
    }

    @Override
    public void onStart() {

        // mark start time
        this.startTime = SystemClock.uptimeMillis();

        // Android UI thread
        //this.handler = new Handler(Looper.getMainLooper());
        //Looper.prepare();

        // provide context to allow reading resources
        ContentUtils.setThreadActivity(activity);
    }

    @Override
    public void onLoad(Scene scene, Camera camera) {

        // fix aspect ratio
        camera.getProjection().setAspectRatio(screen.getRatio());
    }

    @Override
    public void onLoadError(Exception ex) {
        Log.e(TAG, ex.getMessage(), ex);
        activity.runOnUiThread(() -> {
            Toast.makeText(activity, "There was a problem building the model: " + ex.getMessage(),
                    Toast.LENGTH_LONG).show();
            ContentUtils.setThreadActivity(null);
        });
    }

    @Override
    public void onLoad(Scene scene) {
        //if (this.sceneManager == null) return;

        // configure default camera
        Log.d(TAG, "Initializing scene... name: " + scene.getName());
        scene.setDefaultCamera(defaultCamera);
        scene.setEventManager(eventManager);
    }

    @Override
    public void onLoad(Scene scene, Object3DData data) {
        scene.addObject(data);
    }

    @Override
    public void onLoadComplete(Scene scene) {

        // get objects
        final List<Object3DData> objects = scene.getObjects();

        // check
        if (objects == null || objects.isEmpty()) {
            Log.w(TAG, "No objects were loaded");
        } else {
            Log.i(TAG, "onLoadComplete: " + scene.getName() + ", Objects: " + objects.size());
        }

        for (int i = 0; i < objects.size(); i++) {
            for (int m = 0; m < objects.size(); m++) {
                loadTextureDatas(objects.get(m).getMaterial());
            }
        }

        // initialize scene
        scene.onLoadComplete();

        // show object errors
        List<String> allErrors = new ArrayList<>();
        for (Object3DData data : objects) {
            allErrors.addAll(data.getErrors());
        }
        if (!allErrors.isEmpty()) {
            makeToastText(allErrors.toString(), Toast.LENGTH_LONG);
        }

        // notify user
        final String elapsed = (SystemClock.uptimeMillis() - startTime) / 1000 + " secs";
        makeToastText("Load complete (" + elapsed + ")", Toast.LENGTH_SHORT);

        // Ensure all objects have a parent node to unify the rendering pipeline.
        if (scene.getRootNodes() == null || scene.getRootNodes().isEmpty()) {
            Log.i(TAG, "Scene has no root nodes. Creating default nodes for all objects.");
            List<Node> rootNodes = new ArrayList<>();
            for (Object3DData obj : objects) {
                // Create a new node and assign the object to it.
                Node node = new Node();
                node.setMesh(obj); // Link the visible object to this node.
                node.setMatrix(obj.getModelMatrix());
                obj.setParentNode(node);
                obj.setParentBound(true);
                rootNodes.add(node);
            }
            scene.setRootNodes(rootNodes);
        }

        // fame camera
        CameraUtils.frameModel(scene.getCamera(), scene.getObjects());

        // register scene
        this.sceneManager.addScene(scene);
    }

    @Override
    public void onLoadComplete() {
        ContentUtils.setThreadActivity(null);
    }

    private void loadTextureDatas(Material mat) {
        if (mat == null) return;
        if (mat.getColorTexture() != null && mat.getColorTexture().getFile() != null) {
            String textureFile = mat.getColorTexture().getFile();
            Log.i(TAG, "Loading texture file: " + textureFile);
            try (InputStream stream = ContentUtils.getInputStream(textureFile)) {
                mat.getColorTexture().setData(IOUtils.read(stream));
                Log.i(TAG, "Texture successfully loaded: " + textureFile);
            } catch (Exception ex) {
                Log.e(TAG, "Error loading texture file '" + textureFile + "': " + ex.getMessage(), ex);
                makeToastText("Error loading texture file '" + textureFile + "': " + ex.getMessage(), Toast.LENGTH_LONG);
            }

        }
    }

    private void makeToastText(final String text, final int toastDuration) {
        if (activity == null) return;
        activity.runOnUiThread(() -> Toast.makeText(activity.getApplicationContext(), text, toastDuration).show());
    }
}
