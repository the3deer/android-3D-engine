package org.the3deer.android_3d_model_engine.services.collada;

import android.opengl.GLES20;
import android.util.Log;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.the3deer.android_3d_model_engine.model.AnimatedModel;
import org.the3deer.android_3d_model_engine.model.Material;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.model.Scene;
import org.the3deer.android_3d_model_engine.model.Skin;
import org.the3deer.android_3d_model_engine.scene.SceneImpl;
import org.the3deer.android_3d_model_engine.services.collada.entities.Controller;
import org.the3deer.android_3d_model_engine.services.collada.entities.Geometry;
import org.the3deer.android_3d_model_engine.services.collada.entities.Node; // Import the new Node class
import org.the3deer.util.android.ContentUtils;
import org.the3deer.util.io.IOUtils;

public class ColladaLoader {

    private static final String TAG = ColladaLoader.class.getSimpleName();

    public ColladaLoader() {
    }

    public Scene load(URI uri) throws Exception {
        Scene scene = new SceneImpl();
        try (InputStream stream = ContentUtils.getInputStream(uri)) {
            // 1. PARSE THE FILE
            ColladaParser parser = new ColladaParser();
            parser.parse(stream);

            // 2. GET ALL PARSED LIBRARIES
            Map<String, Geometry> geometries = parser.getGeometryLibrary();
            Map<String, Controller> controllers = parser.getControllerLibrary();
            Map<String, Material> materials = parser.getMaterialLibrary();

            // Load the actual texture file data for all materials that have one.
            loadTextureDatas(uri, materials);

            // --- START OF NEW LOGIC ---

            // 3. BUILD TEMPLATE MODELS
            // Create one "template" object for each geometry, which can be static or animated.
            // These templates will be cloned as we build the scene graph.
            // In ColladaLoader.java, inside the load() method, at the start of "Step 3"
            // Map of templates, keyed by the GEOMETRY ID they are based on.
            Map<String, Object3DData> geometryTemplates = new HashMap<>();
            // Map of templates, keyed by the CONTROLLER ID that uses them.
            Map<String, Object3DData> controllerTemplates = new HashMap<>();


            // Build all the skinned model templates
            for (Controller controller : controllers.values()) {
                String meshId = controller.getSkin().getSource(); // Get the geometry ID, e.g., "Cube-mesh"
                Geometry geometry = geometries.get(meshId);
                if (geometry == null) continue;

                Log.d(TAG, "Building TEMPLATE AnimatedModel for controller: " + controller.getId());
                AnimatedModel model = buildAnimatedModel(geometry, controller, materials);

                // Store this template in BOTH maps, using the correct key for each.
                geometryTemplates.put(meshId, model);
                controllerTemplates.put(controller.getId(), model); // e.g., Key: "Armature_Cube-skin"
            }

            // Build static (non-skinned) model templates
            for (Geometry geometry : geometries.values()) {
                if (!geometryTemplates.containsKey(geometry.getId())) { // Check if it wasn't already made as a skinned model
                    Log.d(TAG, "Building TEMPLATE static Object3DData for geometry: " + geometry.getId());
                    Object3DData staticModel = buildStaticModel(geometry, materials);
                    geometryTemplates.put(geometry.getId(), staticModel); // Key: e.g., "Cube_094-mesh"
                }
            }

            // 4. BUILD THE SCENE GRAPH
            List<Object3DData> finalModels = new ArrayList<>();

            // Get the LIST of root nodes from the parser
            List<Node> rootNodes = parser.getRootNodes();

            if (!rootNodes.isEmpty()) {
                // Iterate through each root node and build its part of the scene
                for (Node rootNode : rootNodes) {
                    // The parent matrix is null for a root node
                    buildScene(rootNode, null, geometryTemplates, controllerTemplates, finalModels);
                }
            } else {
                Log.e(TAG, "No root nodes found in visual scene. Falling back to flat list.");
                finalModels.addAll(geometryTemplates.values()); // Fallback
            }

            // 5. POPULATE AND RETURN THE SCENE
            scene.setObjects(finalModels);
            scene.setMaterials(new ArrayList<>(materials.values()));
            // TODO: Set animations, cameras, lights etc. on the scene later
            return scene;
            // --- END OF NEW LOGIC ---
        }
    }

    /**
     * Recursively traverses the parser's Node tree and builds the final scene graph.
     * @param parserNode The current node from the parser's temporary tree.
     * @param parentMatrix The transformation matrix inherited from the parent node.
     * @param geometryTemplates A map of all available "template" models.
     * @param controllerTemplates A map of all available "template" models.
     * @param finalModels The master list where all final, transformed models are collected.
     */
    private void buildScene(Node parserNode, float[] parentMatrix,
                            Map<String, Object3DData> geometryTemplates,
                            Map<String, Object3DData> controllerTemplates,
                            List<Object3DData> finalModels) {
        // Calculate the world matrix for this node
        float[] worldMatrix = new float[16];
        if (parentMatrix != null) {
            // Inherit parent's transform
            android.opengl.Matrix.multiplyMM(worldMatrix, 0, parentMatrix, 0, parserNode.getTransform(), 0);
        } else {
            // This is a root node, its world matrix is its local matrix
            worldMatrix = parserNode.getTransform();
        }

        Object3DData template = null;

        // --- THIS IS THE FIX ---
        // Check if the node instances a controller
        if (parserNode.getInstanceControllerId() != null) {
            String controllerId = parserNode.getInstanceControllerId();
            template = controllerTemplates.get(controllerId);
            Log.d(TAG, "Node '" + parserNode.getId() + "' is instancing a CONTROLLER: " + controllerId);

            // Check if the node instances a geometry
        } else if (parserNode.getInstanceGeometryId() != null) {
            String geometryId = parserNode.getInstanceGeometryId();
            template = geometryTemplates.get(geometryId);
            Log.d(TAG, "Node '" + parserNode.getId() + "' is instancing a GEOMETRY: " + geometryId);
        }
        // --- END OF FIX ---

        if (template != null) {
            // The rest of the cloning logic remains exactly the same
            Object3DData newInstance = template.clone();
            newInstance.setId(parserNode.getId());
            newInstance.setMatrix(worldMatrix);
            finalModels.add(newInstance);

            // This log is now even more useful
            Log.d(TAG, "Node '" + parserNode.getId() + "' resolved to template "+parserNode.getId());
        }

        // Recurse for all children
        for (Node childNode : parserNode.getChildren()) {
            buildScene(childNode, worldMatrix, geometryTemplates, controllerTemplates, finalModels);
        }
    }


    private void loadTextureDatas(URI modelUri, Map<String, Material> materials) {
        if (materials == null) return;
        for (Material mat : materials.values()) {
            if (mat.getColorTexture() != null && mat.getColorTexture().getFile() != null) {
                String textureFile = mat.getColorTexture().getFile();
                try {
                    URI textureUri = modelUri.resolve(textureFile);
                    try (InputStream stream = ContentUtils.getInputStream(textureUri)) {
                        mat.getColorTexture().setData(IOUtils.read(stream));
                        Log.i(TAG, "Texture linked and data loaded for file: " + textureFile);
                    }
                } catch (Exception ex) {
                    Log.e(TAG, "Error loading texture file '" + textureFile + "': " + ex.getMessage(), ex);
                }
            }
        }
    }

    private AnimatedModel buildAnimatedModel(Geometry geometry, Controller controller, Map<String, Material> materials) {// --- THIS IS THE FIX ---
        // The method signature was wrong, it should now get jointNames from the skin object.
        AnimatedModel model = new AnimatedModel(
                geometry.getId(), // id
                geometry.getPositions(), // vertex attribute
                geometry.getNormals(), // vertex attribute
                geometry.getColors(), // vertex attribute
                geometry.getTexCoords(), // vertex attribute
                materials.get(geometry.getMaterialId()), // vertex color/texture
                new Skin(
                        IOUtils.createIntBuffer(controller.getSkin().getWeights().getJointIndices()), // skinning data
                        IOUtils.createFloatBuffer(controller.getSkin().getWeights().getWeights()),
                        controller.getSkin().getInverseBindMatrices(),
                        controller.getSkin().getJointNames()) // is this needed here ?
        );
        model.setIndexed(false);
        model.setDrawMode(GLES20.GL_TRIANGLES);
        return model;
    }

    private Object3DData buildStaticModel(Geometry geometry, Map<String, Material> materials) {
        Object3DData model = new Object3DData();
        model.setId(geometry.getId());
        model.setVertexArrayBuffer(geometry.getPositions());
        model.setVertexNormalsArrayBuffer(geometry.getNormals());
        model.setTextureCoordsArrayBuffer(geometry.getTexCoords());
        model.setVertexColorsArrayBuffer(geometry.getColors());

        Material material = materials.get(geometry.getMaterialId());
        if (material != null) {
            model.setMaterial(material);
        }
        model.setIndexed(false);
        model.setDrawMode(GLES20.GL_TRIANGLES);
        return model;
    }
}
