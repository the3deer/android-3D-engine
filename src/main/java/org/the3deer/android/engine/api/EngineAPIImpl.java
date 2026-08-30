package org.the3deer.android.engine.api;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.the3deer.android.engine.Model;
import org.the3deer.android.engine.ModelEngine;
import org.the3deer.android.engine.ModelEngineViewModel;
import org.the3deer.android.engine.animation.Animation;
import org.the3deer.android.engine.api.dto.Mapper;
import org.the3deer.android.engine.model.Camera;
import org.the3deer.android.engine.model.Constants;
import org.the3deer.android.engine.model.Material;
import org.the3deer.android.engine.model.Object3D;
import org.the3deer.android.engine.model.Scene;
import org.the3deer.android.engine.model.Screen;
import org.the3deer.android.engine.objects.PrimitiveManager;
import org.the3deer.android.engine.text.FontManager;
import org.the3deer.android.engine.text.Text;
import org.the3deer.util.bean.Bean;
import org.the3deer.util.bean.BeanInfo;
import org.the3deer.util.bean.BeanManager;
import org.the3deer.util.bean.BeanPropertyInfo;
import org.the3deer.util.bean.BeanUtils;
import org.the3deer.util.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

/**
 * Technical implementation of the EngineAPI.
 * Handles 3D operations and exposes technical metadata.
 */
@Bean
public class EngineAPIImpl implements EngineAPI {

    private static final String TAG = EngineAPIImpl.class.getSimpleName();

    @Inject
    private Model model;

    @Inject
    private ModelEngine engine;

    @Inject
    private Screen screen;

    @Inject
    private BeanManager beanManager;

    @Inject
    private PrimitiveManager primitiveManager;

    @Inject
    private FontManager fontManager;

    public EngineAPIImpl() {
    }

/*    @Override
    public List<Function> getCapabilities() {
        List<Function> capabilities = new ArrayList<>();
        capabilities.add(new Function("Spatial Manipulation", "Rotate, scale, and translate objects in 3D space."));
        capabilities.add(new Function("Object Creation", "Create primitives like cubes, pyramids, and text labels."));
        capabilities.add(new Function("Custom Geometry", "Generate custom triangulated meshes from vertices."));
        capabilities.add(new Function("glTF Support", "Process complete glTF 2.0 models or delta updates."));
        capabilities.add(new Function("Scene Management", "Control visibility, colors, and animations."));
        return capabilities;
    }*/

    @Override
    public void executeSceneCommand(String response) {
        Log.i(TAG, "Executing scene command: " + response);
        try {
            String cleanJson = cleanJson(response);

            if (cleanJson.contains("\"asset\"") && cleanJson.contains("\"version\":\"2.0\"")) {
                loadGltf(cleanJson);
            } else if (cleanJson.startsWith("[") || cleanJson.startsWith("{")) {
                processJsonCommands(cleanJson);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to execute scene command", e);
        }
    }

    private String cleanJson(String json) {
        if (json == null) return "";
        String clean = json.trim();
        if (clean.startsWith("```json")) {
            clean = clean.substring(7);
        } else if (clean.startsWith("```")) {
            clean = clean.substring(3);
        }
        if (clean.endsWith("```")) {
            clean = clean.substring(0, clean.length() - 3);
        }
        return clean.trim();
    }

    @Override
    public void create(String prompt) {
        invokeAssistant("createGltfModel", prompt);
    }

    @Override
    public void update(String prompt) {
        invokeAssistant("updateGltfModel", prompt);
    }

    private void invokeAssistant(String methodName, String prompt) {
        Object assistant = beanManager.get("ai.assistant");
        if (assistant != null) {
            try {
                assistant.getClass().getMethod(methodName, String.class).invoke(assistant, prompt);
            } catch (Exception e) {
                Log.e(TAG, "Error invoking assistant method: " + methodName, e);
            }
        } else {
            Log.e(TAG, "AI Assistant not available");
        }
    }

    @Override
    public void createModel(Model model) {
        createModel(model, null, null);
    }

    @Override
    public void createModel(Model model, List<Map<String, Object>> initialObjects, List<Map<String, Object>> operations) {
        // Initialize an empty model with a default scene
        if (model.getActiveScene() == null) {
            Scene scene = new Scene();
            scene.setId("default");
            scene.setName("Default Scene");
            model.addScene(scene);
            model.setActiveScene(scene);
        }
        if (model.getActiveScene().getActiveCamera() == null){
            model.getActiveScene().setActiveCamera(new Camera("default", Constants.DEFAULT_CAMERA_POSITION));
        }

        // Populate initial objects (Primitives)
        if (initialObjects != null) {
            Log.i(TAG, "Populating model with " + initialObjects.size() + " initial objects");
            for (Map<String, Object> attrs : initialObjects) {
                try {
                    String type = (String) attrs.get("type");
                    String id = (String) attrs.get("id");
                    if (id == null) id = "obj_" + System.currentTimeMillis();
                    createObject(model.getActiveScene(), type, id, attrs);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to create initial object", e);
                }
            }
        }

        // TODO: Handle CSG Operations (Union, Subtract, Intersect)
        if (operations != null && !operations.isEmpty()) {
            Log.i(TAG, "CSG Operations requested: " + operations.size() + " (Logic pending integration)");
        }

        loadModel(model);
    }

    @Override
    public void loadModel(Model model) {
        try {
            Log.i(TAG, "Reconciling full model document...");
            
            String gltfJson = cleanJson(model.getGltf());
            if (gltfJson != null && !gltfJson.isEmpty()) {
                File gltfFile = new File(this.model.getCacheDir(), "ai_generated.gltf");
                try (FileOutputStream fos = new FileOutputStream(gltfFile)) {
                    fos.write(gltfJson.getBytes(StandardCharsets.UTF_8));
                }
                model.setUriModel(gltfFile.toURI());
                model.setCacheDir(this.model.getCacheDir());
                model.setGltf(gltfJson); // Store the cleaned version
            }

            // Trigger system load and navigation
            if (ModelEngineViewModel.INSTANCE != null) {
                ModelEngineViewModel.INSTANCE.loadModel(model);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load model content", e);
        }
    }

    @Override
    public void updateModel(Model model) {
        if (model.getGltf() != null) {
            //updateGltf(model.getGltf());
        }
    }

    private void loadGltf(String gltfJson) {
        Model aiModel = new Model(java.net.URI.create("ai://generated"));
        aiModel.setName("AI Generated");
        aiModel.setType("gltf");
        aiModel.setGltf(gltfJson);
        loadModel(aiModel);
    }

    public void processJsonCommands(String json) {
        try {
            String clean = cleanJson(json);
            if (clean.startsWith("[")) {
                JSONArray commands = new JSONArray(clean);
                for (int i = 0; i < commands.length(); i++) {
                    processAutoCommand(commands.getJSONObject(i));
                }
            } else {
                processAutoCommand(new JSONObject(clean));
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to process scene commands", e);
        }
    }

    private void processAutoCommand(JSONObject cmd) throws Exception {
        final String action = cmd.optString("action");
        if ("update".equalsIgnoreCase(action) || "edit".equalsIgnoreCase(action) || cmd.has("node")) {
            processDelta(cmd);
        } else {
            processCommand(cmd);
        }
    }

    private void processDelta(JSONObject delta) throws Exception {
        final String nodeName = delta.optString("node", delta.optString("target", delta.optString("id")));
        List<Object3D> targets = getTargetObjects(nodeName);
        
        for (Object3D target : targets) {
            if (delta.has("translation") || delta.has("location") || delta.has("position")) {
                Object posObj = delta.opt("translation");
                if (posObj == null) posObj = delta.opt("location");
                if (posObj == null) posObj = delta.opt("position");
                applyLocation(target, posObj);
            }
            if (delta.has("rotation")) {
                applyRotation(target, delta.get("rotation"));
            }
            if (delta.has("scale")) {
                applyScale(target, delta.get("scale"));
            }
            if (delta.has("color")) {
                applyColor(target, delta.optString("color"));
            }
            if (delta.has("material")) {
                JSONObject mat = delta.getJSONObject("material");
                if (mat.has("baseColorFactor")) {
                    JSONArray color = mat.getJSONArray("baseColorFactor");
                    float[] colorArray = new float[] {
                        (float)color.getDouble(0), (float)color.getDouble(1), 
                        (float)color.getDouble(2), (float)color.getDouble(3)
                    };
                    if (target.getMaterial() == null) target.setMaterial(new Material());
                    target.getMaterial().setColor(colorArray);
                }
            }
        }
    }

    @Override
    public void zoom(float factor) {
        if (model.getActiveScene() != null && model.getActiveScene().getActiveCamera() != null) {
            model.getActiveScene().getActiveCamera().zoom(factor);
        }
    }

    @Override
    public void rotateCamera(float x, float y, float z) {
        if (model.getActiveScene() != null && model.getActiveScene().getActiveCamera() != null) {
            final Camera camera = model.getActiveScene().getActiveCamera();
            // Orbiting: y is horizontal (yaw), x is vertical (pitch)
            camera.move((float)Math.toRadians(y), (float)Math.toRadians(x));
            // Roll: z is rotation around the camera's look axis
            camera.rotate((float)Math.toRadians(z));
        }
    }

    @Override
    public void setWireframe(boolean enabled) {
    }

    @Override
    public Bitmap getScreenshot() {
        try {
            if (ModelEngineViewModel.INSTANCE != null) {
                CompletableFuture<Bitmap> bitmapCompletableFuture = ModelEngineViewModel.INSTANCE.requestScreenshot();
                return bitmapCompletableFuture.get();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    public void createObject(String type, String id, Map<String, Object> attributes) {
        createObject(model.getActiveScene(), type, id, attributes);
    }

    private void createObject(Scene scene, String type, String id, Map<String, Object> attributes) {
        Log.i(TAG, "Creating object in scene " + (scene != null ? scene.getName() : "null") + ": type=" + type + ", id=" + id + ", attrs=" + attributes);
        create(scene, type, id, (String)attributes.get("color"), attributes.get("location"), attributes.get("scale"), attributes.get("rotation"), (String)attributes.get("text"));
    }

    @Override
    public Map<String, Object> readObject(String id) {
        return null;
    }

    @Override
    public void updateObject(String id, Map<String, Object> attributes) {
        if (attributes.containsKey("visible")) {
            visibility(id, (Boolean)attributes.get("visible"));
        }
        if (attributes.containsKey("color")) {
            color(id, (String)attributes.get("color"));
        }
    }

    @Override
    public void deleteObject(String id) {
        delete(id);
    }

    @Override
    public void setConfig(String key, Object value) {
        if (engine != null) {
            engine.setConfig(key, value);
        }
    }

    @Override
    public org.the3deer.android.engine.api.dto.Scene getScene() {
        return Mapper.fromDomain(model.getActiveScene());
    }

    @Override
    public List<BeanInfo> getManagedBeans() {
        if (beanManager == null) return Collections.emptyList();

        final List<BeanInfo> ret = new ArrayList<>();
        final Map<String, Object> beans = beanManager.getBeans();
        
        for (Object bean : beans.values()) {
            final Map<String, BeanPropertyInfo> props = beanManager.getProperties(bean);
            if (props.isEmpty()) continue;

            final Class<?> beanClass = bean.getClass();
            final Bean beanAnn = beanClass.getAnnotation(Bean.class);
            final String technicalName = (beanAnn != null && !beanAnn.name().isEmpty()) ? beanAnn.name() : BeanUtils.getSnakeCase(beanClass);
            
            final BeanInfo beanInfo = new BeanInfo(beanClass.getName(), technicalName, null);
            
            for (BeanPropertyInfo prop : props.values()) {
                try {
                    // Populate current value before sending to UI/AI
                    prop.setValue(prop.getValue(bean));

                    // Populate dynamic options if any
                    final String[] values = prop.getValues(bean);
                    if (values != null) {
                        prop.setOptions(java.util.Arrays.asList(values));
                    }
                } catch (Exception ignored) {}

                beanInfo.addProperty(prop.getId(), prop);
            }
            ret.add(beanInfo);
        }
        return ret;
    }

    @Override
    public List<java.net.URI> getLoadedModels() {
        if (org.the3deer.android.engine.ModelEngineViewModel.INSTANCE == null) return Collections.emptyList();
        return org.the3deer.android.engine.ModelEngineViewModel.INSTANCE.getLoadedModels();
    }

    @Override
    public String getSceneSummary() {
        try {
            org.the3deer.android.engine.api.dto.Scene scene = getScene();
            if (scene == null) return "{}";

            JSONObject json = new JSONObject();
            JSONArray objects = new JSONArray();
            for (org.the3deer.android.engine.api.dto.Object3D obj : scene.getObjects()) {
                JSONObject o = new JSONObject();
                o.put("id", obj.getId());
                o.put("name", obj.getName());
                if (obj.getPosition() != null) o.put("position", new JSONArray(obj.getPosition()));
                if (obj.getRotation() != null) o.put("rotation", new JSONArray(obj.getRotation()));
                if (obj.getScale() != null) o.put("scale", new JSONArray(obj.getScale()));
                o.put("visible", obj.isVisible());
                o.put("color", obj.getColor());
                objects.put(o);
            }
            json.put("objects", objects);
            if (scene.getSelectedObject() != null) {
                json.put("selectedObject", scene.getSelectedObject());
            }
            return json.toString();
        } catch (Exception e) {
            Log.e(TAG, "Failed to generate scene summary", e);
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }

    @Override
    public String exportToGltf() {
        return model != null && model.getGltf() != null ? model.getGltf() : "";
    }

    private void processCommand(JSONObject cmd) throws Exception {
        final String action = cmd.optString("action");
        final String target = cmd.optString("target", cmd.optString("node", cmd.optString("id")));

        switch (action.toLowerCase()) {
            case "rotate":
                rotate(target, cmd.optString("axis", "y"), (float) cmd.optDouble("degrees", 0));
                break;
            case "scale":
                scale(target, (float) cmd.optDouble("factor", 1.0));
                break;
            case "color":
                color(target, cmd.optString("value", "#ffffff"));
                break;
            case "visibility":
                visibility(target, cmd.optBoolean("visible", true));
                break;
            case "delete":
                delete(target);
                break;
            case "update":
                processDelta(cmd);
                break;
            case "animate":
                animate(cmd.optString("name", ""));
                break;
            case "create":
                create(model.getActiveScene(),
                       cmd.optString("type", "cube"), 
                       cmd.optString("id", "obj_" + System.currentTimeMillis()),
                       cmd.optString("color", "#ffffff"),
                       cmd.opt("location"),
                       cmd.optJSONArray("scale"),
                       cmd.optJSONArray("rotation"),
                       cmd.optString("text", ""));
                break;
            case "create_custom":
                createCustom(model.getActiveScene(),
                             cmd.optString("id", "custom_" + System.currentTimeMillis()),
                             cmd.getJSONArray("vertices"),
                             cmd.optString("color", "#ffffff"),
                             cmd.opt("location"));
                break;
            case "create_gltf":
                loadGltf(cmd.getString("data"));
                break;
            case "translate":
            case "move":
                translate(target, cmd.opt("location"), cmd.optJSONArray("translation"));
                break;
        }
    }

    private void translate(String targetId, Object location, JSONArray translation) {
        List<Object3D> targets = getTargetObjects(targetId);
        for (Object3D obj : targets) {
            if (translation != null && translation.length() >= 3) {
                obj.setPosition((float) translation.optDouble(0), (float) translation.optDouble(1), (float) translation.optDouble(2));
            } else if (location != null) {
                applyLocation(obj, location);
            }
        }
    }

    private void rotate(String target, String axis, float degrees) {
        List<Object3D> objects = getTargetObjects(target);
        for (Object3D obj : objects) {
            float[] rot = obj.getRotation().clone();
            if ("x".equalsIgnoreCase(axis)) rot[0] += degrees;
            else if ("y".equalsIgnoreCase(axis)) rot[1] += degrees;
            else if ("z".equalsIgnoreCase(axis)) rot[2] += degrees;
            obj.setRotation(rot);
        }
    }

    private void scale(String target, float factor) {
        List<Object3D> objects = getTargetObjects(target);
        for (Object3D obj : objects) {
            float[] s = obj.getScale().clone();
            obj.setScale(s[0] * factor, s[1] * factor, s[2] * factor);
        }
    }

    private void color(String target, String hexColor) {
        List<Object3D> objects = getTargetObjects(target);
        for (Object3D obj : objects) {
            applyColor(obj, hexColor);
        }
    }

    private void visibility(String target, boolean visible) {
        List<Object3D> objects = getTargetObjects(target);
        for (Object3D obj : objects) {
            obj.setVisible(visible);
        }
    }

    private void delete(String target) {
        Scene scene = model.getActiveScene();
        if (scene == null) return;
        List<Object3D> objects = getTargetObjects(target);
        for (Object3D obj : objects) {
            obj.setVisible(false);
        }
    }

    private void animate(String name) {
        Scene scene = model.getActiveScene();
        if (scene == null || scene.getAnimations() == null) return;
        for (Animation anim : scene.getAnimations()) {
            if (anim.getName().equalsIgnoreCase(name) || name.isEmpty()) {
                scene.setActiveAnimation(anim);
                break;
            }
        }
    }

    private void create(Scene scene, String type, String id, String color, Object location, Object scale, Object rotation, String text) {
        if (scene == null) {
            Log.e(TAG, "Cannot create object: No scene provided");
            return;
        }

        Log.i(TAG, "Adding object to scene: " + scene.getName() + " (objects before: " + scene.getObjects().size() + ")");
        Object3D obj = primitiveManager.build(type);
        if (obj == null) {
            if ("text".equalsIgnoreCase(type) && fontManager.getFactoryObject() != null) {
                final int c = Color.parseColor(color == null ? "#ffffff" : color);
                final float[] colorArray = new float[]{ Color.red(c) / 255.0f, Color.green(c) / 255.0f, Color.blue(c) / 255.0f, 1.0f };
                obj = Text.build(text == null || text.isEmpty() ? " " : text, colorArray, fontManager.getFactoryObject());
                obj.setVisible(true);
            } else {
                return;
            }
        }

        obj.setId(id).setName(id);
        applyColor(obj, color);
        applyLocation(obj, location);
        applyScale(obj, scale);
        applyRotation(obj, rotation);
        scene.addObject(obj);
        Log.i(TAG, "Object " + id + " added to scene. Total objects: " + scene.getObjects().size());
    }

    private void createCustom(Scene scene, String id, org.json.JSONArray verticesJson, String color, Object location) {
        if (scene == null) return;

        float[] rawVertices = new float[verticesJson.length()];
        for (int i = 0; i < verticesJson.length(); i++) {
            rawVertices[i] = (float) verticesJson.optDouble(i);
        }

        int vertexCount = rawVertices.length / 3;
        float[] triangulated;
        if (vertexCount > 3) {
            int triangleCount = vertexCount - 2;
            triangulated = new float[triangleCount * 3 * 3];
            int idx = 0;
            for (int i = 1; i < vertexCount - 1; i++) {
                triangulated[idx++] = rawVertices[0]; triangulated[idx++] = rawVertices[1]; triangulated[idx++] = rawVertices[2];
                triangulated[idx++] = rawVertices[i * 3]; triangulated[idx++] = rawVertices[i * 3 + 1]; triangulated[idx++] = rawVertices[i * 3 + 2];
                triangulated[idx++] = rawVertices[(i + 1) * 3]; triangulated[idx++] = rawVertices[(i + 1) * 3 + 1]; triangulated[idx++] = rawVertices[(i + 1) * 3 + 2];
            }
        } else {
            triangulated = rawVertices;
        }

        FloatBuffer vertexBuffer = IOUtils.createFloatBuffer(triangulated.length).put(triangulated);
        Object3D obj = new Object3D(vertexBuffer);
        obj.setDrawMode(android.opengl.GLES20.GL_TRIANGLES);
        obj.setId(id).setName(id);
        applyColor(obj, color);
        applyLocation(obj, location);
        scene.addObject(obj);
    }

    private void applyLocation(Object3D obj, Object location) {
        if (location instanceof List) {
            List<Number> list = (List<Number>) location;
            if (list.size() >= 3) {
                obj.setPosition(list.get(0).floatValue(), list.get(1).floatValue(), list.get(2).floatValue());
            }
        } else if (location instanceof JSONArray) {
            JSONArray arr = (JSONArray) location;
            if (arr.length() >= 3) {
                obj.setPosition((float) arr.optDouble(0), (float) arr.optDouble(1), (float) arr.optDouble(2));
            }
        } else if (location instanceof JSONObject) {
            JSONObject json = (JSONObject) location;
            obj.setPosition((float) json.optDouble("x", 0), (float) json.optDouble("y", 0), (float) json.optDouble("z", 0));
        } else if (location instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) location;
            float x = ((Number)map.getOrDefault("x", 0f)).floatValue();
            float y = ((Number)map.getOrDefault("y", 0f)).floatValue();
            float z = ((Number)map.getOrDefault("z", 0f)).floatValue();
            obj.setPosition(x, y, z);
        } else if (location instanceof String) {
            try {
                String str = ((String) location).replace("[", "").replace("]", "").trim();
                String[] parts = str.split(",");
                if (parts.length >= 3) {
                    obj.setPosition(Float.parseFloat(parts[0].trim()), Float.parseFloat(parts[1].trim()), Float.parseFloat(parts[2].trim()));
                }
            } catch (Exception ignored) {}
        }
    }

    private void applyScale(Object3D obj, Object scale) {
        if (scale instanceof List) {
            List<Number> list = (List<Number>) scale;
            if (list.size() >= 3) {
                obj.setScale(list.get(0).floatValue(), list.get(1).floatValue(), list.get(2).floatValue());
            }
        } else if (scale instanceof JSONArray) {
            JSONArray arr = (JSONArray) scale;
            if (arr.length() >= 3) {
                obj.setScale((float) arr.optDouble(0), (float) arr.optDouble(1), (float) arr.optDouble(2));
            }
        } else if (scale instanceof JSONObject) {
            JSONObject json = (JSONObject) scale;
            obj.setScale((float) json.optDouble("x", 1), (float) json.optDouble("y", 1), (float) json.optDouble("z", 1));
        } else if (scale instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) scale;
            float x = ((Number)map.getOrDefault("x", 1f)).floatValue();
            float y = ((Number)map.getOrDefault("y", 1f)).floatValue();
            float z = ((Number)map.getOrDefault("z", 1f)).floatValue();
            obj.setScale(x, y, z);
        }
    }

    private void applyRotation(Object3D obj, Object rotation) {
        if (rotation instanceof List) {
            List<Number> list = (List<Number>) rotation;
            if (list.size() >= 3) {
                obj.setRotation(new float[]{list.get(0).floatValue(), list.get(1).floatValue(), list.get(2).floatValue()});
            }
        } else if (rotation instanceof JSONArray) {
            JSONArray arr = (JSONArray) rotation;
            if (arr.length() >= 3) {
                obj.setRotation(new float[]{(float) arr.optDouble(0), (float) arr.optDouble(1), (float) arr.optDouble(2)});
            }
        } else if (rotation instanceof JSONObject) {
            JSONObject json = (JSONObject) rotation;
            obj.setRotation(new float[]{(float) json.optDouble("x", 0), (float) json.optDouble("y", 0), (float) json.optDouble("z", 0)});
        } else if (rotation instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) rotation;
            float x = ((Number)map.getOrDefault("x", 0f)).floatValue();
            float y = ((Number)map.getOrDefault("y", 0f)).floatValue();
            float z = ((Number)map.getOrDefault("z", 0f)).floatValue();
            obj.setRotation(new float[]{x, y, z});
        }
    }

    private void applyColor(Object3D obj, String hexColor) {
        try {
            int c = Color.parseColor(hexColor == null ? "#ffffff" : hexColor);
            float[] colorArray = new float[]{ Color.red(c) / 255.0f, Color.green(c) / 255.0f, Color.blue(c) / 255.0f, 1.0f };
            if (obj.getMaterial() == null) obj.setMaterial(new Material());
            obj.getMaterial().setColor(colorArray);
        } catch (Exception ignored) {}
    }

    private List<Object3D> getTargetObjects(String target) {
        Scene scene = model.getActiveScene();
        if (scene == null) return Collections.emptyList();
        if ("selected".equalsIgnoreCase(target) && scene.getSelectedObject() != null) return Collections.singletonList(scene.getSelectedObject());
        if ("all".equalsIgnoreCase(target)) return scene.getObjects();
        List<Object3D> found = new ArrayList<>();
        for (Object3D obj : scene.getObjects()) {
            if (obj.getId().equalsIgnoreCase(target) || (obj.getName() != null && obj.getName().equalsIgnoreCase(target))) found.add(obj);
        }
        return found;
    }
}
