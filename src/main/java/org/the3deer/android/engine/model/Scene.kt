package org.the3deer.android.engine.model

import android.util.Log
import org.the3deer.android.engine.animation.Animation
import org.the3deer.android.engine.animation.Animator
import org.the3deer.util.math.Math3DUtils

class Scene(var name: String = "Scene_"+System.currentTimeMillis()) {
    
    // Core Hierarchy
    val rootNodes = mutableListOf<Node>()
    val objects = mutableListOf<Object3D>()
    val skins = mutableListOf<Skin>()

    // Object Management
    var selectedObject: Object3D? = null

    // Texture Management
    val textures = mutableListOf<Texture>()

    // Camera Management
    val cameras = mutableListOf<Camera>()
    var activeCamera: Camera? = null
    
    // Animation Management
    var animations= mutableListOf<Animation>()
    var activeAnimation: Animation? = null
    val animator = Animator()
    
    // Rendering Toggles (Config)
    var isBlendingEnabled = true
    var isBlendingForced = false
    var drawWireframe = false
    var drawPoints = false
    var drawBoundingBox = false
    var drawNormals = false
    var drawTextures = true
    var drawColors = true
    var drawSkeleton = false
    
    // Lighting
    enum class LightProfile { Rotating, PointOfView, Off }
    var lightProfile = LightProfile.Rotating
    
    // State
    var doAnimation = true
    var isSmooth = false
    var showBindPose = false
    var isCollision = true
    
    fun update() {
        Log.d("Scene", "Updating scene graph")
        // Recursive update of the scene graph
        rootNodes.forEach { it.updateWorldTransform(Math3DUtils.IDENTITY_MATRIX) }

        if (animations.isNotEmpty() && activeAnimation == null) {
            activeAnimation = animations.get(0);
        }

    }

    fun addObject(data: Object3D) {
        Log.d("Scene", "Adding object: " + data.id)
        objects.add(data)
    }


    fun merge(other: Scene) {

        // merge objects
        objects.addAll(other.objects)

        // merge skeletons
        skins.addAll(other.skins)

        // merge root nodes
        rootNodes.addAll(other.rootNodes)

        // merge animations
        animations.addAll(other.animations)
    }

}