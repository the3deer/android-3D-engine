package org.the3deer.android_3d_model_engine.camera;


import android.util.Log;

import org.the3deer.android_3d_model_engine.model.Camera;
import org.the3deer.util.bean.BeanFactory;
import org.the3deer.util.bean.BeanInit;

import java.util.List;

import javax.inject.Inject;

public final class CameraManager {

    private final static String TAG = CameraManager.class.getSimpleName();

    // vars
    @Inject
    private List<Camera> cameraList;

    // state
    private Camera currentCamera;

    public CameraManager() {
    }

    @BeanInit
    public void init() {
        if (cameraList != null && !cameraList.isEmpty()) {
            currentCamera = cameraList.get(0);
        }
    }

    public Camera getActiveCamera() {
        return currentCamera;
    }

    public void setActiveCamera(int index) {
        if (cameraList == null || cameraList.isEmpty() || index < 0 || index >= cameraList.size())
            return;;
        this.currentCamera = cameraList.get(index);
    }

    public String[] getCameraNames() {
        if (cameraList == null || cameraList.isEmpty()) return null;
        String[] names = new String[cameraList.size()];
        for (int i = 0; i < cameraList.size(); i++) {
            names[i] = cameraList.get(i).getName();
        }
        return names;
    }

    @BeanFactory.OnBeanUpdate
    public void onBeanUpdate(String id, Object updated) {
        if (updated instanceof Camera) {
            Log.i(TAG, "Camera loaded: " + updated);
        }
    }
}

