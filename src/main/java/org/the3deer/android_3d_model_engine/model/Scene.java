package org.the3deer.android_3d_model_engine.model;

import java.util.List;

public interface Scene {

    Camera getCamera();

    Light getLight();

    Object3DData getLightBulb();

    void addObject(Object3DData obj);

    List<Object3DData> getObjects();

    Object3DData getSelectedObject();

    void setCamera(Camera camera);

    void onLoadComplete();
}
