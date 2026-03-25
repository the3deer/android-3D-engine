package org.the3deer.android.engine.services;

import org.the3deer.android.engine.model.Camera;
import org.the3deer.android.engine.model.Object3D;
import org.the3deer.android.engine.model.Scene;

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
    public void onLoad(Scene scene, Object3D data) {
        scene.addObject(data);
    }

    @Override
    public void onLoadComplete(Scene scene) {
        scene.update();
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
