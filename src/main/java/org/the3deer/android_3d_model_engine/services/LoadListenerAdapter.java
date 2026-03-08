package org.the3deer.android_3d_model_engine.services;

import org.the3deer.android_3d_model_engine.model.Camera;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.model.Scene;

public class LoadListenerAdapter implements LoadListener {

    public LoadListenerAdapter() {
    }

    @Override
    public void onStart() {

    }

    @Override
    public void onLoad(Scene scene) {
    }

    @Override
    public void onLoad(Scene scene, Object3DData data) {
        scene.addObject(data);
    }

    @Override
    public void onLoadComplete(Scene scene) {
        scene.onLoadComplete();
    }

    @Override
    public void onLoadError(Exception ex) {

    }

    @Override
    public void onLoad(Scene scene, Camera camera) {
    }

    @Override
    public void onLoadComplete() {
    }
}
