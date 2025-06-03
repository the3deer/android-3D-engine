package org.the3deer.android_3d_model_engine.services;

import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.model.Scene;

public interface LoadListener {

    void onStart();

    void onProgress(String progress);

    void onLoadError(Exception ex);

    void onLoad(Object3DData data);

    default void onLoad(Scene scene) {}

    void onLoadComplete();
}
