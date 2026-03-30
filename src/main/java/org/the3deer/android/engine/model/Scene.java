package org.the3deer.android.engine.model;

import android.util.Log;

import org.the3deer.android.engine.animation.Animation;
import org.the3deer.util.math.Math3DUtils;

import java.util.ArrayList;
import java.util.List;

public class Scene {

    private String name;

    // Core Hierarchy
    private final List<Node> rootNodes = new ArrayList<>();
    private final List<Object3D> objects = new ArrayList<>();
    private final List<Skin> skins = new ArrayList<>();

    // Object Management
    private Object3D selectedObject;

    // Texture Management
    private final List<Texture> textures = new ArrayList<>();

    // Camera Management
    private final List<Camera> cameras = new ArrayList<>();
    private Camera activeCamera;

    // Animation Management
    private List<Animation> animations = new ArrayList<>();
    private Animation activeAnimation;
    private boolean isSmooth = false;
    private boolean isCollision = true;

    public Scene() {
        this("Scene_" + System.currentTimeMillis());
    }

    public Scene(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Node> getRootNodes() {
        return rootNodes;
    }

    public List<Object3D> getObjects() {
        return objects;
    }

    public List<Skin> getSkins() {
        return skins;
    }

    public Object3D getSelectedObject() {
        return selectedObject;
    }

    public void setSelectedObject(Object3D selectedObject) {
        this.selectedObject = selectedObject;
    }

    public List<Texture> getTextures() {
        return textures;
    }

    public List<Camera> getCameras() {
        return cameras;
    }

    public Camera getActiveCamera() {
        return activeCamera;
    }

    public void setActiveCamera(Camera activeCamera) {
        this.activeCamera = activeCamera;
    }

    public List<Animation> getAnimations() {
        return animations;
    }

    public void setAnimations(List<Animation> animations) {
        this.animations = animations;
    }

    public Animation getActiveAnimation() {
        return activeAnimation;
    }

    public void setActiveAnimation(Animation activeAnimation) {
        this.activeAnimation = activeAnimation;
    }

    public boolean isSmooth() {
        return isSmooth;
    }

    public void setSmooth(boolean smooth) {
        isSmooth = smooth;
    }

    public boolean isCollision() {
        return isCollision;
    }

    public void setCollision(boolean collision) {
        isCollision = collision;
    }

    public void update() {
        Log.d("Scene", "Updating scene graph");
        // Recursive update of the scene graph
        for (Node node : rootNodes) {
            node.updateWorldTransform(Math3DUtils.IDENTITY_MATRIX);
        }

        if (!animations.isEmpty() && activeAnimation == null) {
            activeAnimation = animations.get(0);
        }
    }

    public void addObject(Object3D data) {
        Log.d("Scene", "Adding object: " + data.getId());
        objects.add(data);
    }

    public void merge(Scene other) {
        // merge objects
        objects.addAll(other.objects);

        // merge skeletons
        skins.addAll(other.skins);

        // merge root nodes
        rootNodes.addAll(other.rootNodes);

        // merge animations
        animations.addAll(other.animations);
    }
}
