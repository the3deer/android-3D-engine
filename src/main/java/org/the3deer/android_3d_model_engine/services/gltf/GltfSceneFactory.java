// File: GltfSceneFactory.java
package org.the3deer.android_3d_model_engine.services.gltf;

import android.util.Log;

import org.the3deer.android_3d_model_engine.animation.Animation;
import org.the3deer.android_3d_model_engine.animation.JointTransform;
import org.the3deer.android_3d_model_engine.model.AnimatedModel;
import org.the3deer.android_3d_model_engine.model.Camera;
import org.the3deer.android_3d_model_engine.model.Node;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.model.Scene;
import org.the3deer.android_3d_model_engine.scene.SceneImpl;
import org.the3deer.android_3d_model_engine.services.gltf.dto.GltfSceneData;
import org.the3deer.android_3d_model_engine.services.gltf.dto.GltfSceneDto;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class GltfSceneFactory {

    private static final String TAG = GltfSceneFactory.class.getSimpleName();

    /**
     * Assembles a list of renderable Scene objects from the raw data loaded from a glTF file.
     *
     * @param sceneData The raw data container from the GltfLoader.
     * @return A list of fully assembled scenes.
     */
    public List<Scene> createScenes(GltfSceneData sceneData) {
        List<Scene> finalScenes = new ArrayList<>();
        final var dto = sceneData.dto;
        final var allNodes = sceneData.nodes;
        final var allMeshes = sceneData.meshes;

        // CASE 1: The glTF file defines scenes.
        if (dto.scenes != null && !dto.scenes.isEmpty()) {
            Log.i(TAG, "Found " + dto.scenes.size() + " scene(s) defined in the file.");
            for (GltfSceneDto sceneDto : dto.scenes) {
                Scene scene = new SceneImpl();
                scene.setName(sceneDto.name);

                // Add the root nodes for THIS specific scene
                List<Node> rootNodes = new ArrayList<>();
                if (sceneDto.nodes != null) {
                    for (Integer nodeIndex : sceneDto.nodes) {
                        if (nodeIndex < allNodes.size()) {
                            rootNodes.add(allNodes.get(nodeIndex));
                        }
                    }
                }
                scene.setRootNodes(rootNodes);
                scene.setCamera(new Camera()); // Give each scene a default camera

                // Traverse the node hierarchy and collect all drawable meshes
                List<Object3DData> sceneObjects = collectMeshes(rootNodes);
                scene.setObjects(sceneObjects);

                // --- START ANIMATION LINKING ---
                if (sceneData.animations != null && !sceneData.animations.isEmpty()) {

                    // Collect all nodes that are part of this scene's hierarchy
                    List<Node> sceneNodes = collectNodes(rootNodes);

                    // Create a Set of scene node IDs for quick lookups
                    Set<String> sceneNodeIds = new HashSet<>();
                    for (Node node : sceneNodes) {
                        sceneNodeIds.add(node.getId());
                    }

                    // Filter animations to only include those that target nodes within this scene
                    List<Animation> sceneAnimations = new ArrayList<>();
                    for (Animation anim : sceneData.animations) {
                        // Check if this animation affects any node in our current scene
                        boolean belongsToScene = false;
                        if (anim.getKeyFrames() != null && anim.getKeyFrames().length > 0) {
                            // We only need to check the first keyframe's pose
                            Map<String, JointTransform> firstPose = anim.getKeyFrames()[0].getPose();
                            if (firstPose != null) {
                                for (String jointId : firstPose.keySet()) {
                                    if (sceneNodeIds.contains(jointId)) {
                                        belongsToScene = true;
                                        Log.d(TAG, "Found animation belonging to the scene.");
                                        break; // Found a match, no need to check further for this animation
                                    }
                                }
                            }
                        }

                        if (belongsToScene) {
                            sceneAnimations.add(anim);
                        }
                    }

                    if (!sceneAnimations.isEmpty()) {
                        scene.setAnimations(sceneAnimations);
                    }
                }
                // --- END ANIMATION LINKING ---

                finalScenes.add(scene);

                scene.onLoadComplete();
            }
        }
        // CASE 2: No scenes are defined. Create a default scene with all root nodes.
        else {
            Log.w(TAG, "Gltf file has no scenes defined. Creating a default scene.");
            Scene defaultScene = new SceneImpl();
            defaultScene.setName("Default Scene");

            // Find all nodes that are not children of any other node.
            List<Node> rootNodes = new ArrayList<>();
            for(Node node : allNodes){
                if(node.getParent() == null){
                    rootNodes.add(node);
                }
            }
            defaultScene.setRootNodes(rootNodes);
            List<Object3DData> sceneObjects = collectMeshes(rootNodes);
            defaultScene.setObjects(sceneObjects);
            defaultScene.setCamera(new Camera());
            finalScenes.add(defaultScene);

            defaultScene.onLoadComplete();
        }
        return finalScenes;
    }

    /**
     * Recursively traverses a list of nodes and their children to collect
     * all associated drawable meshes (Object3DData).
     *
     * @param rootNodes The starting nodes of the hierarchy.
     * @return A flat list of all meshes found in the hierarchy.
     */
    private List<Object3DData> collectMeshes(List<Node> rootNodes) {
        List<Object3DData> collectedMeshes = new ArrayList<>();
        if (rootNodes == null) {
            return collectedMeshes;
        }

        Stack<Node> stack = new Stack<>();
        stack.addAll(rootNodes);

        while (!stack.isEmpty()) {
            Node currentNode = stack.pop();

            if (currentNode.getMesh() != null) {
                collectedMeshes.add(currentNode.getMesh());
            }

            if (currentNode.getChildren() != null) {
                for (Node childNode : currentNode.getChildren()) {
                    stack.push(childNode);
                }
            }
        }
        return collectedMeshes;
    }

    // In GltfSceneFactory.java, after collectMeshes()

    /**
     * Recursively traverses a list of nodes and their children to collect
     * all nodes in the hierarchy.
     *
     * @param rootNodes The starting nodes of the hierarchy.
     * @return A flat list of all nodes found in the hierarchy.
     */
    private List<Node> collectNodes(List<Node> rootNodes) {
        List<Node> collectedNodes = new ArrayList<>();
        if (rootNodes == null) {
            return collectedNodes;
        }

        Stack<Node> stack = new Stack<>();
        stack.addAll(rootNodes);

        while (!stack.isEmpty()) {
            Node currentNode = stack.pop();
            collectedNodes.add(currentNode);

            if (currentNode.getChildren() != null) {
                for (Node childNode : currentNode.getChildren()) {
                    stack.push(childNode);
                }
            }
        }
        return collectedNodes;
    }

}
