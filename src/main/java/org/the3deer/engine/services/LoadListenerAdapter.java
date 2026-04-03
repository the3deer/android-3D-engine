package org.the3deer.engine.services;

import org.the3deer.engine.model.Camera;
import org.the3deer.engine.model.Object3D;
import org.the3deer.engine.model.Scene;

public class LoadListenerAdapter implements LoadListener {

    public LoadListenerAdapter() {
    }

    @Override
    public void onLoadStart() {

    }

    @Override
    public void onProgress(String progress) {

    }

    @Override
    public void onLoadScene(Scene scene) {
    }

    @Override
    public void onLoadObject(Scene scene, Object3D data) {
        scene.addObject(data);
    }

    @Override
    public void onLoadError(Exception ex) {
    }

    @Override
    public void onLoadCamera(Scene scene, Camera camera) {
    }

    @Override
    public void onLoadComplete() {
    }
}
