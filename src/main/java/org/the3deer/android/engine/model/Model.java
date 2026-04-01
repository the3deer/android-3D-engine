package org.the3deer.android.engine.model;

import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

/**
 * <p>Main model class representing a 3D model resource and its environment.</p>
 * <p>
 * The model holds the model uri, the arguments and the scene model.  It has also
 * </p>
 */
public class Model {

    private final Uri uri;
    private final String name;
    private final String type;
    private final Map<String, Object> extras;

    @Inject
    private Camera defaultCamera;

    private final List<Scene> scenes = new ArrayList<>();
    private Scene activeScene;

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
        Log.i("Model", "addScene: " + scene.getName());
        scenes.add(scene);
        if (activeScene == null) {
            Log.i("Model", "Activating scene: " + scene.getName());
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

    public static class Metadata {
        public final String type;
        public final Map<String, Object> extras;

        public Metadata(String type, Map<String, Object> extras) {
            this.type = type;
            this.extras = extras;
        }
    }
}
