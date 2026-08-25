package org.the3deer.android.engine.api;

import android.graphics.Bitmap;

import org.the3deer.android.engine.Model;
import org.the3deer.android.engine.api.dto.Scene;
import org.the3deer.util.bean.BeanInfo;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Technical contract for interacting with the 3D Engine.
 */
public interface EngineAPI {

    // --- VIEWING ---
    
    void zoom(float factor);
    
    void rotateCamera(float x, float y, float z);
    
    void setWireframe(boolean enabled);

    Bitmap getScreenshot();

    // --- EDITION (CRUD) ---

    void createObject(String type, String id, Map<String, Object> attributes);

    Map<String, Object> readObject(String id);

    void updateObject(String id, Map<String, Object> attributes);

    void deleteObject(String id);

    void executeSceneCommand(String command);

    // --- DISCOVERY & SYSTEM ---

    Scene getScene();

    List<BeanInfo> getManagedBeans();

    List<URI> getLoadedModels();

    void setConfig(String key, Object value);

    String getSceneSummary();

    // --- AI & ENGINEERING ---

    void create(String prompt);

    void update(String prompt);

    // --- MODEL MANAGEMENT ---

    void createModel(Model model);

    void createModel(Model model, List<Map<String, Object>> initialObjects, List<Map<String, Object>> operations);

    void loadModel(Model model);

    void updateModel(Model model);

    String exportToGltf();
}
