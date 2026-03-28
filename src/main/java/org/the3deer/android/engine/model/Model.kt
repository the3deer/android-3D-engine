package org.the3deer.android.engine.model

import android.net.Uri
import android.util.Log
import javax.inject.Inject

class Model(val uri: Uri) {

    @Inject
    var defaultCamera: Camera? = null

    val scenes = mutableListOf<Scene>()
    var activeScene: Scene? = null

    fun update() {
        activeScene?.update()
    }

    fun addScene(scene: Scene) {

        Log.i("Model", "addScene: " + scene.name)

        scenes.add(scene)

        if (activeScene == null) {

            Log.i("Model", "Activating scene: " + scene.name)

            activeScene = scene
        }
    }

    fun getCameras() : List<Camera> {

        val cameras = mutableListOf<Camera>()

        if (defaultCamera != null) cameras.add(defaultCamera!!)

        if (activeScene == null) return cameras

        cameras.addAll(activeScene!!.cameras)

        return cameras
    }
}
