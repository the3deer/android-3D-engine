package org.the3deer.android.engine;

import android.app.Application;
import android.net.Uri;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.the3deer.android.engine.model.Model;
import org.the3deer.android.engine.model.ModelEvent;
import org.the3deer.android.engine.model.Node;
import org.the3deer.android.engine.model.Object3D;
import org.the3deer.android.engine.model.Scene;
import org.the3deer.android.engine.model.Screen;
import org.the3deer.util.event.EventListener;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ModelEngineViewModel extends AndroidViewModel {

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
     * Loading state per URI. Value is the loading message or null if not loading.
     */
    private final MutableLiveData<Map<String, String>> _loadingState = new MutableLiveData<>(Collections.emptyMap());
    public final LiveData<Map<String, String>> loadingState = _loadingState;

    public ModelEngineViewModel(Application application) {
        super(application);
        initTestModels();
    }

    private void initTestModels() {
        // Simple shapes for testing
        Model triangle = createModelForTest("triangle", new float[]{
                0.0f, 0.622008459f, 0.0f,
                -0.5f, -0.311004243f, 0.0f,
                0.5f, -0.311004243f, 0.0f
        });

        Model cube = createModelForTest("cube", new float[]{
                // Front face
                -0.3f, 0.3f, 0.3f, -0.3f, -0.3f, 0.3f, 0.3f, -0.3f, 0.3f,
                -0.3f, 0.3f, 0.3f, 0.3f, -0.3f, 0.3f, 0.3f, 0.3f, 0.3f,
                // Back face
                -0.3f, 0.3f, -0.3f, 0.3f, -0.3f, -0.3f, -0.3f, -0.3f, -0.3f,
                -0.3f, 0.3f, -0.3f, 0.3f, 0.3f, -0.3f, 0.3f, -0.3f, -0.3f,
                // Top face
                -0.3f, 0.3f, -0.3f, -0.3f, 0.3f, 0.3f, 0.3f, 0.3f, 0.3f,
                -0.3f, 0.3f, -0.3f, 0.3f, 0.3f, 0.3f, 0.3f, 0.3f, -0.3f,
                // Bottom face
                -0.3f, -0.3f, -0.3f, 0.3f, -0.3f, -0.3f, 0.3f, -0.3f, 0.3f,
                -0.3f, -0.3f, -0.3f, 0.3f, -0.3f, 0.3f, -0.3f, -0.3f, 0.3f,
                // Left face
                -0.3f, 0.3f, -0.3f, -0.3f, -0.3f, -0.3f, -0.3f, -0.3f, 0.3f,
                -0.3f, 0.3f, -0.3f, -0.3f, -0.3f, 0.3f, -0.3f, 0.3f, 0.3f,
                // Right face
                0.3f, 0.3f, -0.3f, 0.3f, -0.3f, 0.3f, 0.3f, -0.3f, -0.3f,
                0.3f, 0.3f, -0.3f, 0.3f, 0.3f, 0.3f, 0.3f, -0.3f, 0.3f
        });

        Model square = createModelForTest("square", new float[]{
                -0.5f, 0.5f, 0.0f,
                -0.5f, -0.5f, 0.0f,
                0.5f, -0.5f, 0.0f,
                -0.5f, 0.5f, 0.0f,
                0.5f, -0.5f, 0.0f,
                0.5f, 0.5f, 0.0f
        });

        Map<String, Model> models = _models.getValue();
        if (models != null) {
            models.put("triangle", triangle);
            models.put("cube", cube);
            models.put("square", square);
        }
    }

    public ModelEngine getEngine(String uri) {
        Map<String, ModelEngine> engines = _engines.getValue();
        return engines != null ? engines.get(uri) : null;
    }

    public ModelEngine initEngine(String uriString) {
        return initEngine(uriString, null);
    }

    public ModelEngine initEngine(String uriString, String type) {

        // get
        Model model = getModel(uriString);

        // init if needed
        if (model == null) {
            model = createModel(uriString, type);
        }

        ModelEngine engine = getEngine(uriString);
        if (engine == null) {
            engine = getOrCreateEngine(uriString, model);
        }

        Log.d(TAG, "Model Engine Initialized. uri: " + uriString);
        return engine;
    }

    public void activateEngine(String uriString) {
        try {
            // Initialize engine components (Lightweight)
            final ModelEngine modelEngine = initEngine(uriString);
            if (modelEngine == null) throw new IllegalArgumentException("Engine not initialized");

            // Load 3D model data (Heavyweight) - now handled by the engine itself
            modelEngine.loadAsync(() -> {
                // Update active engine on the UI thread
                _activeEngine.setValue(modelEngine);
            });

        } catch (Exception e) {
            Log.e(TAG, "Failed to activate engine for " + uriString, e);
            // TODO: error handling in UI
        }
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
                ModelEvent modelEvent = (ModelEvent) event;
                if (modelEvent.getCode() == ModelEvent.Code.LOADING || modelEvent.getCode() == ModelEvent.Code.PROGRESS) {
                    setLoading(finalUriString, "Loading...");
                } else if (modelEvent.getCode() == ModelEvent.Code.LOADED || modelEvent.getCode() == ModelEvent.Code.LOAD_ERROR) {
                    setLoading(finalUriString, null);
                }
            }
            return false;
        });

        return engine;
    }

    private void setLoading(String uri, String message) {
        Map<String, String> current = new LinkedHashMap<>(_loadingState.getValue());
        if (message == null) {
            current.remove(uri);
        } else {
            current.put(uri, message);
        }
        _loadingState.postValue(current);
    }

    public Model getModel(String uriString) {
        Map<String, Model> models = _models.getValue();
        return models != null ? models.get(uriString) : null;
    }

    public Model createModel(String uriString) {
        return createModel(uriString, null);
    }


    public Model createModel(String uriString, String type) {

        // debug
        Log.i(TAG, "Building Model... uri: " + uriString);

        // create
        Model model = new Model(Uri.parse(uriString), type);

        // update
        Map<String, Model> models = _models.getValue();
        if (models != null) {
            models.put(uriString, model);
            _models.setValue(models);
        }

        return model;
    }

    public Model createModelForTest(String uriString, float[] vertices) {
        Model model = new Model(Uri.parse(uriString));
        Scene scene = new Scene("Default_" + uriString);
        Node node = new Node("Root");
        node.setMesh(new Object3D(vertices));
        scene.getRootNodes().add(node);
        model.addScene(scene);
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

    @Override
    protected void onCleared() {
        super.onCleared();
        // Shut down all engines to release resources
        Map<String, ModelEngine> engines = _engines.getValue();
        if (engines != null) {
            for (ModelEngine engine : engines.values()) {
                engine.close();
            }
        }
    }
}
