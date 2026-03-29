package org.the3deer.android.engine

import android.app.Application
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.the3deer.android.engine.model.Model
import org.the3deer.android.engine.model.ModelEvent
import org.the3deer.android.engine.model.Screen
import org.the3deer.android.engine.model.Node
import org.the3deer.android.engine.model.Object3D
import org.the3deer.android.engine.model.Scene
import org.the3deer.android.engine.model.Constants
import org.the3deer.util.event.EventListener
import java.util.EventObject
import java.util.LinkedHashMap

class ModelEngineViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "ModelEngineViewModel"

    /**
     * Model
     */
    private val _models = MutableLiveData<MutableMap<String, Model>>(LinkedHashMap())
    private val _activeModel = MutableLiveData<Model>()
    val activeModel: LiveData<Model> = _activeModel

    /**
     * Engine
     */
    private val _engines = MutableLiveData<MutableMap<String, ModelEngine>>(LinkedHashMap())
    private val _activeEngine = MutableLiveData<ModelEngine>()
    val activeEngine: LiveData<ModelEngine> = _activeEngine

    /**
     * Loading state per URI. Value is the loading message or null if not loading.
     */
    private val _loadingState = MutableLiveData<Map<String, String>>(emptyMap())
    val loadingState: LiveData<Map<String, String>> = _loadingState

    /**
     * OpenGL Screen. Shared across all engines to avoid race conditions.
     */
    private val _glScreen = MutableLiveData(Screen(640, 480))
    val glScreen: LiveData<Screen> = _glScreen

    // Simple shapes for testing
    private val triangle = createModelForTest(
        "triangle", floatArrayOf(
            0.0f, 0.622008459f, 0.0f,
            -0.5f, -0.311004243f, 0.0f,
            0.5f, -0.311004243f, 0.0f
        )
    )

    private val cube = createModelForTest(
        "cube", floatArrayOf(
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
        )
    )

    private val square = createModelForTest(
        "square", floatArrayOf(
            -0.5f, 0.5f, 0.0f,
            -0.5f, -0.5f, 0.0f,
            0.5f, -0.5f, 0.0f,
            -0.5f, 0.5f, 0.0f,
            0.5f, -0.5f, 0.0f,
            0.5f, 0.5f, 0.0f
        )
    )

    init {
        // Pre-load test models
        val models = _models.value!!
        models["triangle"] = triangle
        models["cube"] = cube
        models["square"] = square
    }

    fun getEngine(uri: String): ModelEngine? {
        return _engines.value?.get(uri)
    }

    fun loadEngine(uriString: String): ModelEngine {

        // debug
        Log.i(TAG, "Loading Model Engine... uri: $uriString")

        // Try to get the model from the engine view model
        var model = getModel(uriString)
        if (model == null) {
            model = createModel(uriString);
        }

        // Get engine
        var engine = getEngine(uriString)

        // Initialize engine
        if (engine == null) {
            engine = getOrCreateEngine(uriString, model)
        }

        // debug
        Log.d(TAG, "Enabling Model Engine... uri: $uriString")

        return engine;
    }

    fun setActiveEngine(engine: ModelEngine?) {
        _activeEngine.value = engine
    }

    /**
     * Creates or retrieves a ModelEngine for the given URI.
     */
    private fun getOrCreateEngine(uriString: String, model: Model): ModelEngine {

        val engines = _engines.value!!

        // debug
        Log.i(TAG, "Building Engine... uri: $uriString")

        // get engine
        var engine = engines[uriString]

        // check if engine already exists
        if (engine != null) return engine;

        // Create the heavy engine instance
        engine = ModelEngine(uriString, model, getApplication())

        // Add shared screen to the engine's bean factory before initialization
        //_glScreen.value?.let {
        engine.beanFactory.add(Constants.BEAN_ID_SCREEN, _glScreen.value)
        //}

        engines[uriString] = engine
        _engines.value = engines

        // Register a listener that updates the loading state
        engine.beanFactory.addOrReplace("modelEngineViewModelListener", object : EventListener {
            override fun onEvent(event: EventObject): Boolean {
                if (event is ModelEvent) {
                    when (event.code) {
                        ModelEvent.Code.LOADING -> setLoading(uriString, "Loading...")
                        ModelEvent.Code.PROGRESS -> setLoading(uriString, "Loading...")
                        ModelEvent.Code.LOADED, ModelEvent.Code.LOAD_ERROR -> setLoading(
                            uriString,
                            null
                        )

                        else -> {}
                    }
                }
                return false
            }
        })


        return engine;
    }


    private fun setLoading(uri: String, message: String?) {
        val current = _loadingState.value?.toMutableMap() ?: mutableMapOf()
        if (message == null) {
            current.remove(uri)
        } else {
            current[uri] = message
        }
        _loadingState.postValue(current)
    }

    fun getModel(uriString: String): Model? {
        return _models.value?.get(uriString)
    }

    fun createModel(uriString: String): Model {

        // debug
        Log.i(TAG, "Building Model... uri: $uriString")

        val model = Model(uriString.toUri())

        // register model
        val models = _models.value!!
        models[uriString] = model

        return model;
    }

    fun createModelForTest(uriString: String, vertices: FloatArray): Model {
        val model = Model(uriString.toUri())
        val scene = Scene("Default_${uriString}")
        val node = Node("Root")
        node.mesh = Object3D(vertices)
        scene.rootNodes.add(node)
        model.addScene(scene)
        return model
    }

    fun setActiveModel(model: Model?) {
        _activeModel.value = model
    }


    fun onSurfaceChanged(width: Int, height: Int) {
        Log.d(TAG, "onSurfaceChanged: $width x $height")
        _glScreen.value?.setSize(width, height)
    }
}
