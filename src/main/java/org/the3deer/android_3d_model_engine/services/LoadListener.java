package org.the3deer.android_3d_model_engine.services;

import org.the3deer.android_3d_model_engine.model.Camera;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.model.Scene;

public interface LoadListener {

    void onStart();

    default void onProgress(String progress){};

    void onLoad(Camera camera);

    void onLoad(Scene scene);

    void onLoad(Scene scene, Object3DData data);

    void onLoadComplete(Scene scene);

    void onLoadError(Exception ex);

    void onLoadComplete();
}
