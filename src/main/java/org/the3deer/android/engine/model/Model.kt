package org.the3deer.android.engine.model

import android.net.Uri
import android.util.Log

class Model(val uri: Uri) {

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
}
