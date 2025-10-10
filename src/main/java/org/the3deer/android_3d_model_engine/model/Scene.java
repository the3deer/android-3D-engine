package org.the3deer.android_3d_model_engine.model;

import androidx.annotation.NonNull;

import org.the3deer.android_3d_model_engine.animation.Animator;

import java.util.List;

public interface Scene {

    default boolean isEnabled() {
        return false;
    }

    default void setEnabled(boolean enabled){
    };

    Camera getCamera();

    void addObject(Object3DData obj);

    void addRootNode(Node node);

    List<Node> getRootNodes();

    void addObjects(List<Object3DData> objs);

    List<Object3DData> getObjects();

    Object3DData getSelectedObject();

    void setCamera(Camera camera);

    void onLoadComplete();

    @NonNull
    default String getName() {
        return "Scene ("+System.identityHashCode(this)+")";
    }

    void setName(String name);

    default void reset() {}

    Animator getAnimator();
}
