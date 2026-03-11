package org.the3deer.android_3d_model_engine.model;

import androidx.annotation.NonNull;

import org.the3deer.android_3d_model_engine.animation.Animation;
import org.the3deer.android_3d_model_engine.animation.Animator;
import org.the3deer.util.event.EventManager;

import java.util.ArrayList;
import java.util.EventListener;
import java.util.EventObject;
import java.util.List;

public interface Scene extends EventListener {

    default boolean isEnabled() {
        return false;
    }

    default void setEnabled(boolean enabled){
    };

    Camera getCamera();

    void addObject(Object3DData obj);

    void addRootNode(Node node);

    void setRootNodes(List<Node> nodes);

    List<Node> getRootNodes();

    void addSkeleton(Skin skin);

    List<Skin> getSkeletons();

    void addObjects(List<Object3DData> objs);

    void setObjects(List<Object3DData> objs);

    List<Object3DData> getObjects();

    Object3DData getSelectedObject();

    void setCamera(Camera camera);

    void addAnimation(Animation animation);

    List<Animation> getAnimations();

    void setCurrentAnimation(Animation animation);

    Animation getCurrentAnimation();

    void onLoadComplete();

    @NonNull
    default String getName() {
        return "Scene ("+System.identityHashCode(this)+")";
    }

    void setName(String name);

    default void reset() {}

    Animator getAnimator();

    void setMaterials(ArrayList<Material> materials);

    void setAnimations(List<Animation> sceneAnimations);

    void merge(Scene other);

    String getId();

    void setCameras(List<Camera> cameras);

    List<Camera> getCameras();

    void setEventManager(EventManager eventManager);

    void setDefaultCamera(Camera defaultCamera);

    void setSelectedObject(Object3DData object);

    boolean onEvent(EventObject eventObject);
}
