package org.the3deer.android.engine.services;

import org.the3deer.android.engine.model.Camera;
import org.the3deer.android.engine.model.Object3D;
import org.the3deer.android.engine.model.Scene;

public interface LoadListener {

    void onStart();

    void onProgress(String progress);

    void onLoad(Scene scene, Camera camera);

    void onLoad(Scene scene);

    void onLoad(Scene scene, Object3D data);

    void onLoadComplete(Scene scene);

    void onLoadError(Exception ex);

    void onLoadComplete();
}
