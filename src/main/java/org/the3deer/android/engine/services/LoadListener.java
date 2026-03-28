package org.the3deer.android.engine.services;

import org.the3deer.android.engine.model.Camera;
import org.the3deer.android.engine.model.Object3D;
import org.the3deer.android.engine.model.Scene;

public interface LoadListener {

    void onLoadStart();

    void onProgress(String progress);

    void onLoadCamera(Scene scene, Camera camera);

    void onLoadScene(Scene scene);

    void onLoadObject(Scene scene, Object3D data);

    void onLoadError(Exception ex);

    void onLoadComplete();
}
