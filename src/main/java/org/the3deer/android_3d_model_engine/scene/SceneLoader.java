
package org.the3deer.android_3d_model_engine.scene;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.model.Scene;
import org.the3deer.android_3d_model_engine.services.LoadListener;
import org.the3deer.android_3d_model_engine.services.collada.ColladaLoaderTask;
import org.the3deer.android_3d_model_engine.services.gltf.GltfLoaderTask;
import org.the3deer.android_3d_model_engine.services.stl.STLLoaderTask;
import org.the3deer.android_3d_model_engine.services.wavefront.WavefrontLoaderTask;
import org.the3deer.util.android.ContentUtils;
import org.the3deer.util.bean.BeanFactory;

import java.net.URI;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * This component loads the model into the Scene
 * This component can also load Demo objects
 */
public class SceneLoader implements LoadListener {

    private final static String TAG = SceneLoader.class.getSimpleName();

    // dependencies
    @Inject @Named("bundle")
    private Bundle bundle;
    @Inject @Named("extras")
    private Bundle extras;
    @Inject
    private Activity activity;
    @Inject
    private Scene scene;
    @Inject
    private SceneManager sceneManager;

    // variables
    private final Handler handler;
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
    private int modelType = -1;
    /**
     * Background GL clear color. Default is light gray
     */
    private float[] backgroundColor = new float[]{0.0f, 0.0f, 0.0f, 1.0f};

    // other variables
    private long startTime;

    public SceneLoader() {

        // Android UI thread
        this.handler = new Handler(Looper.getMainLooper());
    }


    public void setUp() {

        Log.i(TAG, "Starting up...");

        // check
        if (handler == null) {
            Log.e(TAG, "Handler is null");
            return;
        }

        // Try to get input parameters
        if (extras == null) {
            return;
        }

        try {
            if (extras.getString("uri") != null) {
                this.modelUri = new URI(extras.getString("uri"));
                this.modelType = extras.getString("type") != null ? Integer.parseInt(extras.getString("type")) : -1;
                Log.i(TAG, "uri '" + modelUri + "', type: "+modelType);
            }

            if (extras.getString("backgroundColor") != null) {
                String[] backgroundColors = extras.getString("backgroundColor").split(" ");
                backgroundColor[0] = Float.parseFloat(backgroundColors[0]);
                backgroundColor[1] = Float.parseFloat(backgroundColors[1]);
                backgroundColor[2] = Float.parseFloat(backgroundColors[2]);
                backgroundColor[3] = Float.parseFloat(backgroundColors[3]);
            }

            isDemo = extras.getBoolean("demo");

            if (this.modelUri == null && bundle != null){
                this.modelUri = new URI(bundle.getString("uri"));
                this.modelType = extras.getString("type") != null ? Integer.parseInt(bundle.getString("type")) : -1;
                Log.i(TAG, "uri '" + modelUri + "', type: "+modelType);
            }

        } catch (Exception ex) {
            Log.e(TAG, "Error parsing activity parameters: " + ex.getMessage(), ex);
        }

        if (modelUri == null && !isDemo){
            return;
        }



        handler.post(() -> {

            // load model
            Log.i(TAG, "Loading model... " + this.modelUri);

            if (isDemo) {
                new DemoLoaderTask(activity, null, this).execute();
            } else if (modelUri.toString().toLowerCase().endsWith(".obj") || modelType == 0) {
                new WavefrontLoaderTask(activity, modelUri, SceneLoader.this).execute();
            } else if (modelUri.toString().toLowerCase().endsWith(".stl") || modelType == 1) {
                Log.i(TAG, "Loading STL object from: " + modelUri);
                new STLLoaderTask(activity, modelUri, SceneLoader.this).execute();
            } else if (modelUri.toString().toLowerCase().endsWith(".dae") || modelType == 2) {
                Log.i(TAG, "Loading Collada object from: " + modelUri);
                new ColladaLoaderTask(activity, modelUri, SceneLoader.this).execute();
            } else if (modelUri.toString().toLowerCase().endsWith(".gltf") || modelUri.toString().toLowerCase().endsWith(".glb") || modelType == 3) {
                Log.i(TAG, "Loading GLTF object from: " + modelUri);
                new GltfLoaderTask(activity, modelUri, SceneLoader.this).execute();
            } else if (modelUri.toString().toLowerCase().endsWith(".fbx")){
                //new FBXLoaderTask(activity, modelUri, this).execute();
            }
        });
    }

    @Override
    public void onStart() {

        // mark start time
        startTime = SystemClock.uptimeMillis();

        // provide context to allow reading resources
        ContentUtils.setThreadActivity(activity);
    }

    @Override
    public void onLoadError(Exception ex) {
        Log.e(TAG, ex.getMessage(), ex);
        Toast.makeText(activity, "There was a problem building the model: " + ex.getMessage(),
                Toast.LENGTH_LONG).show();
        ContentUtils.setThreadActivity(null);
    }

    @Override
    public void onProgress(String progress) {

    }

    @Override
    public void onLoad(Scene scene) {
        if (this.sceneManager == null) return;

        this.sceneManager.addScene(scene);

    }

    @Override
    public void onLoad(Object3DData data) {
        if (this.sceneManager != null) {
            this.sceneManager.addObject(data);
        } else {
            scene.addObject(data);
        }
    }

    @Override
    public void onLoadComplete() {


        scene.onLoadComplete();
    }

}
