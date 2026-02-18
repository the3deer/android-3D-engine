package org.the3deer.android_3d_model_engine.services.collada;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import java.io.InputStream;
import java.net.URI;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.the3deer.android_3d_model_engine.animation.Animation;
import org.the3deer.android_3d_model_engine.model.AnimatedModel;

import org.the3deer.android_3d_model_engine.model.Element;
import org.the3deer.android_3d_model_engine.model.Material;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.model.Scene;
import org.the3deer.android_3d_model_engine.model.Skin;
import org.the3deer.android_3d_model_engine.model.Texture;
import org.the3deer.android_3d_model_engine.scene.SceneImpl;
import org.the3deer.android_3d_model_engine.services.collada.entities.Controller;
import org.the3deer.android_3d_model_engine.services.collada.entities.EffectData;
import org.the3deer.android_3d_model_engine.services.collada.entities.Geometry;
import org.the3deer.android_3d_model_engine.services.collada.entities.MaterialData;
import org.the3deer.android_3d_model_engine.services.collada.entities.Mesh;
import org.the3deer.android_3d_model_engine.services.collada.entities.Node; // Import the new Node class
import org.the3deer.util.android.ContentUtils;
import org.the3deer.util.io.IOUtils;

public class ColladaLoader {

    private static final String TAG = ColladaLoader.class.getSimpleName();

    private String authoringTool;

    public ColladaLoader() {
    }

    public Scene load(URI uri) throws Exception {
        Scene scene = new SceneImpl();
        try (InputStream stream = ContentUtils.getInputStream(uri)) {
            // 1. PARSE THE FILE
            ColladaParser parser = new ColladaParser();
            parser.parse(stream);

            // get metadata
            authoringTool = parser.getAuthoringTool();

            // 2. GET ALL PARSED LIBRARIES
            Map<String, Geometry> geometries = parser.getGeometryLibrary();
            Map<String, Controller> controllers = parser.getControllerLibrary();
            Map<String, MaterialData> materialsLibrary = parser.getMaterialLibrary();
            Map<String, EffectData> effectLibrary = parser.getEffectLibrary();
            Map<String, String> imagesLibrary = parser.getImagesLibrary();
            List<Animation> animations = parser.getAnimationLibrary();

            // Load the actual texture file data for all materials that have one.
            final Map<String, Material> materials = buildMaterials(materialsLibrary, effectLibrary, imagesLibrary);
            loadTextureDatas(uri, materials);

            // 3. BUILD TEMPLATE MODELS
            // Create one "template" object for each geometry, which can be static or animated.
            // These templates will be cloned as we build the scene graph.
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

            // This map will cache our newly created model.Nodes so we can link them later.
            Map<Node, org.the3deer.android_3d_model_engine.model.Node> nodeMap = new HashMap<>();

            // First, build the hierarchy of the engine's Node objects for animation.
            List<org.the3deer.android_3d_model_engine.model.Node> rootModelNodes = buildNodeHierarchy(parser.getRootNodes(), nodeMap);

            // LINK THE SKINS TO THE SKELETONS
            Log.i(TAG, "Linking skins to skeleton nodes...");
            for (Node parserNode : parser.getNodeLibrary().values()) { // Iterate through ALL parser nodes

                // guess root joint from controller
                if (parserNode.getInstanceControllerId() != null && parserNode.getSkinId() == null){
                    Controller controller = controllers.get(parserNode.getInstanceControllerId());
                    if (controller != null) {
                        parserNode.setSkinId(controller.getSkin().getJointNames().get(0));
                    }
                }

                // Find nodes that instance a controller, like our "Cube" node.
                if (parserNode.getInstanceControllerId() != null && parserNode.getSkinId() != null) {

                    String controllerId = parserNode.getInstanceControllerId(); // e.g., "Armature_Cube-skin"
                    String skeletonRootId = parserNode.getSkinId(); // e.g., "Torso"

                    // Get the AnimatedModel template that contains the skin data
                    Object3DData template = controllerTemplates.get(controllerId);
                    if (template instanceof AnimatedModel) {
                        AnimatedModel animatedTemplate = (AnimatedModel) template;

                        // Find the engine Node that represents the root of the skeleton (e.g., the "Torso" node)
                        org.the3deer.android_3d_model_engine.model.Node skeletonRootNode = findNodeInMap(skeletonRootId, nodeMap);

                        if (skeletonRootNode != null) {
                            animatedTemplate.getSkin().setRootJoint(skeletonRootNode);
                            // Attach the actual Skin object to the skeleton's root Node.
                            skeletonRootNode.setSkin(animatedTemplate.getSkin());
                            Log.i(TAG, "LINK ESTABLISHED: Skin from controller '" + controllerId + "' attached to skeleton root node '" + skeletonRootId + "'");
                        } else {
                            Log.e(TAG, "LINK FAILED: Could not find skeleton root node '" + skeletonRootId + "' in node map.");
                        }
                    }
                }
            }


            // Then, build the flat list of renderable Object3DData instances.
            List<Object3DData> finalModels = new ArrayList<>();
            List<Node> rootParserNodes = parser.getRootNodes(); // <--- Your caret was here.

            if (!rootParserNodes.isEmpty()) {
                // Iterate through each root node and build its part of the scene
                for (Node rootNode : rootParserNodes) {
                    // The parent matrix is null for a root node
                    buildScene(rootNode, null, geometryTemplates, controllerTemplates, finalModels, nodeMap);
                }
            } else {
                Log.e(TAG, "No root nodes found in visual scene. Falling back to flat list.");
                finalModels.addAll(geometryTemplates.values()); // Fallback
            }

            // 5. POPULATE AND RETURN THE SCENE
            scene.setObjects(finalModels);
            //scene.setMaterials(new ArrayList<>(materials.values()));
            scene.setAnimations(animations);
            scene.setRootNodes(rootModelNodes); // <-- Attach the hierarchy to the scene

            return scene;
        }
    }

    // In ColladaLoader.java, add these new methods

    /**
     * Builds the final scene graph of model.Node objects from the parser's DTO nodes.
     *
     * @param rootParserNodes The list of root nodes from the parser.
     * @param nodeMap
     * @return A list of the root nodes of the final model hierarchy.
     */
    private List<org.the3deer.android_3d_model_engine.model.Node> buildNodeHierarchy(List<Node> rootParserNodes, Map<Node, org.the3deer.android_3d_model_engine.model.Node> nodeMap) {
        if (rootParserNodes == null || rootParserNodes.isEmpty()) {
            return new ArrayList<>();
        }

        // This map ensures we create each model.Node only once.
        // Key: Parser DTO Node, Value: Final model.Node
        List<org.the3deer.android_3d_model_engine.model.Node> rootModelNodes = new ArrayList<>();

        for (Node rootParserNode : rootParserNodes) {
            // Root nodes have a null parent.
            rootModelNodes.add(buildModelNode(rootParserNode, null, nodeMap));
        }

        return rootModelNodes;
    }

    /**
     * Recursively creates a single model.Node and all its children.
     * @param parserNode The current DTO node to process.
     * @param parentModelNode The parent model.Node to inherit from.
     * @param nodeMap A map to cache created nodes and avoid duplicates.
     * @return The created (or cached) model.Node.
     */
    private org.the3deer.android_3d_model_engine.model.Node buildModelNode(Node parserNode,
                                                                           org.the3deer.android_3d_model_engine.model.Node parentModelNode,
                                                                           Map<Node, org.the3deer.android_3d_model_engine.model.Node> nodeMap){
    // If we've already created this node, return the cached version.
        if (nodeMap.containsKey(parserNode)) {
            return nodeMap.get(parserNode);
        }

        // Create a new model Node using its ID from the COLLADA file.
        // Assuming your model.Node has a constructor that accepts an ID string.
        org.the3deer.android_3d_model_engine.model.Node modelNode = new org.the3deer.android_3d_model_engine.model.Node(parserNode.getId());
        modelNode.setName(parserNode.getName());
        modelNode.setParent(parentModelNode);
        modelNode.setMatrix(parserNode.getTransform());

        // Cache the new node *before* recursing to handle circular dependencies.
        nodeMap.put(parserNode, modelNode);

        // Recursively build all children and add them to the new model node.
        for (Node childParserNode : parserNode.getChildren()) {
            org.the3deer.android_3d_model_engine.model.Node childModelNode = buildModelNode(childParserNode, modelNode, nodeMap);
            modelNode.addChild(childModelNode);
        }

        return modelNode;
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
                            List<Object3DData> finalModels,
                            Map<Node, org.the3deer.android_3d_model_engine.model.Node> nodeMap) {

        // Calculate the world matrix for this node
        float[] worldMatrix = new float[16];
        if (parentMatrix != null) {
            // Inherit parent's transform
            Matrix.multiplyMM(worldMatrix, 0, parentMatrix, 0, parserNode.getTransform(), 0);
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

            // Find the corresponding model.Node from the map and set it.
            org.the3deer.android_3d_model_engine.model.Node parentModelNode = nodeMap.get(parserNode);
            newInstance.setParentNode(parentModelNode);

            // Find the engine Node that represents the root of the skeleton (e.g., the "Torso" node)
            final String skinId = parserNode.getSkinId();
            if (skinId != null) {
                org.the3deer.android_3d_model_engine.model.Node skeletonRootNode = findNodeInMap(skinId, nodeMap);
                if (skeletonRootNode != null) {
                    // Attach the actual Skin object to the skeleton's root Node.
                    newInstance.setParentNode(skeletonRootNode);
                    Log.i(TAG, "LINK ESTABLISHED: Skin from controller '" + skinId + "' attached to skeleton root node '" + parserNode.getId() + "'");
                } else {
                    Log.e(TAG, "LINK FAILED: Could not find skeleton root node '" + skinId + "' in node map.");
                }
            }

            finalModels.add(newInstance);
            Log.d(TAG, "Node '" + parserNode.getId() + "' resolved to template "+parserNode.getId());

            // This log is now even more useful
            Log.d(TAG, "Node '" + parserNode.getId() + "' resolved to template "+parserNode.getId());
        }

        // Recurse for all children
        for (Node childNode : parserNode.getChildren()) {
            buildScene(childNode, worldMatrix, geometryTemplates, controllerTemplates, finalModels, nodeMap);
        }
    }

    private Map<String, Material> buildMaterials(Map<String, MaterialData> materialLibrary,
                                                 Map<String, EffectData> effectLibrary,
                                                 Map<String, String> imageIdToFileNameMap){

        final Map<String, Material> ret = new HashMap<>();

        for (Map.Entry<String, MaterialData> entry : materialLibrary.entrySet()) {
            String materialId = entry.getKey();
            MaterialData materialData = entry.getValue();
            Material material = new Material(materialData.id, materialData.name);
            ret.put(materialId, material);

            // check
            final String effectId = materialData.effectId;
            if (effectId == null) continue;

            // Get the rich effect data we parsed earlier
            EffectData effectData = effectLibrary.get(effectId);
            if (effectData == null) {
                Log.w(TAG, "Material '" + materialId + "' references unknown effect '" + effectId + "'");
                continue;
            }

            // Set diffuse color if available
            if (effectData.diffuseColor != null) {
                material.setDiffuse(effectData.diffuseColor);
            }

            // Override alpha with transparency if it exists
            if (effectData.transparency != null) {
                material.setAlpha(effectData.transparency);
            }

            // Resolve the texture file name through the maps
            if (effectData.imageId != null) {
                String fileName = imageIdToFileNameMap.get(effectData.imageId);
                if (fileName != null) {
                    material.setColorTexture(new Texture().setFile(fileName));
                    Log.d(TAG, "Assembled material '" + materialId + "' with texture '" + fileName + "'");
                }
            }
        }
        return ret;
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

    /**
     * Searches the nodeMap for a final engine Node by its string ID.
     *
     * @param nodeId  The ID of the node to find (e.g., "Torso").
     * @param nodeMap The map containing all the created engine nodes.
     * @return The found model.Node, or null if it doesn't exist.
     */
    private org.the3deer.android_3d_model_engine.model.Node findNodeInMap(String nodeId, Map<Node, org.the3deer.android_3d_model_engine.model.Node> nodeMap) {
        if (nodeId == null || nodeMap == null) {
            return null;
        }

        // We iterate through the DTO->Engine Node map to find the one with the matching ID.
        for (org.the3deer.android_3d_model_engine.model.Node modelNode : nodeMap.values()) {
            if (nodeId.equals(modelNode.getId())) {
                return modelNode;
            }
        }

        // If no node with the given ID was found.
        return null;
    }


    private AnimatedModel buildAnimatedModel(Geometry geometry, Controller controller, Map<String, Material> materials) {// --- THIS IS THE FIX ---

        // Skinning data for the 710 unique vertices
        int[] sourceJointIndices = controller.getSkin().getWeights().getJointIndices();
        float[] sourceWeights = controller.getSkin().getWeights().getWeights();

        // Geometry index data (4260 indices)
        final IntBuffer vertexIndices = geometry.getIndices();
        int finalVertexCount = vertexIndices.capacity();

        // --- THIS IS THE UNROLLING LOGIC ---
        // Create final, correctly-sized buffers
        float[] finalJointIndicesAsFloats = new float[finalVertexCount * 4];
        float[] finalWeights = new float[finalVertexCount * 4];

        for (int i = 0; i < finalVertexCount; i++) {
            // Get the original vertex index (e.g., a number between 0 and 709)
            int originalVertexIndex = vertexIndices.get(i);

            // For this final vertex, copy the 4 joints and 4 weights from the source data
            for (int j = 0; j < 4; j++) {
                // Source index for the skinning data
                int sourceIndex = originalVertexIndex * 4 + j;
                // Destination index for the final buffer
                int destIndex = i * 4 + j;

                finalJointIndicesAsFloats[destIndex] = (float) sourceJointIndices[sourceIndex];
                finalWeights[destIndex] = sourceWeights[sourceIndex];
            }
        }
        // --- END OF UNROLLING LOGIC ---

        float[] bindShapeMatrixTransposed = null;
        if (controller.getSkin().getBindShapeMatrix() != null) {
            bindShapeMatrixTransposed = new float[16];
            Matrix.transposeM(bindShapeMatrixTransposed, 0, controller.getSkin().getBindShapeMatrix(), 0);
        }

        // The method signature was wrong, it should now get jointNames from the skin object.
        Skin skin = new Skin(
                bindShapeMatrixTransposed,
                IOUtils.createFloatBuffer(finalJointIndicesAsFloats), // skinning data
                IOUtils.createFloatBuffer(finalWeights),
                controller.getSkin().getInverseBindMatrices(),
                controller.getSkin().getJointNames());
        skin.setDoInverseBindTranspose(true);

        AnimatedModel model = new AnimatedModel(
                geometry.getId(), // id
                geometry.getPositions(), // vertex attribute
                geometry.getNormals(), // vertex attribute
                geometry.getColors(), // vertex attribute
                geometry.getTexCoords(), // vertex attribute
                materials.get(geometry.getMaterialId()), // vertex color/texture
                skin // is this needed here ?
        );
        model.setIndexBuffer(geometry.getIndices());
        model.setIndexed(false);
        model.setDrawMode(GLES20.GL_TRIANGLES);

        // metadata
        model.setAuthoringTool(this.authoringTool);

        // FIXME:
        model.setBindShapeMatrix(skin.getBindShapeMatrix());
        skin.setBindShapeMatrix(null);

        return model;
    }

    private Object3DData buildStaticModel(Geometry geometry, Map<String, Material> materials) {
        Object3DData model = new Object3DData();
        model.setId(geometry.getId());
        model.setVertexArrayBuffer(geometry.getPositions());
        model.setVertexNormalsArrayBuffer(geometry.getNormals());
        model.setTextureCoordsArrayBuffer(geometry.getTexCoords());
        model.setVertexColorsArrayBuffer(geometry.getColors());

        // build elements
        if (geometry.getMeshes().isEmpty()) {
            Material material = materials.get(geometry.getMaterialId());
            if (material != null) {
                model.setMaterial(material);
            }
            model.setIndexed(false);
        } else {
            Log.d(TAG, "Geometry '" + geometry.getId()
                    + "' has multiple meshes: "+geometry.getMeshes().size());
            List<Element> elements = new ArrayList<>();
            for (Mesh mesh : geometry.getMeshes()) {
                Element element = new Element();
                if (!materials.containsKey(mesh.getMaterialId())){
                Log.w(TAG, "Geometry '" + geometry.getId()
                        + "' references unknown material '" + mesh.getMaterialId() + "'");
                    continue;
                }
                element.setMaterial(materials.get(mesh.getMaterialId()));
                element.setIndexBuffer(IOUtils.createIntBuffer(mesh.getIndices()));
                elements.add(element);
            }
            model.setElements(elements);
            model.setIndexed(true);
        }

        model.setDrawMode(GLES20.GL_TRIANGLES);

        // metadata
        model.setAuthoringTool(this.authoringTool);

        return model;
    }
}
