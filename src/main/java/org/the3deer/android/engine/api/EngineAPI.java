package org.the3deer.android.engine.api;

import java.util.Map;

/**
 * Agnostic contract for interacting with the 3D Engine.
 * Any AI assistant should use these methods to manipulate the scene.
 */
public interface EngineAPI {

    // --- VIEWING (Functional) ---
    
    void zoom(float factor);
    
    void rotateCamera(float x, float y, float z);
    
    void setWireframe(boolean enabled);

    // --- EDITION (CRUD) ---

    /**
     * Create a new object (cube, pyramid, or from glTF string).
     */
    void createObject(String type, String id, Map<String, Object> attributes);

    /**
     * Get properties of an object or the whole scene.
     */
    Map<String, Object> readObject(String id);

    /**
     * Update existing object properties (translation, rotation, scale, color, visibility).
     */
    void updateObject(String id, Map<String, Object> attributes);

    /**
     * Remove an object from the scene.
     */
    void deleteObject(String id);

    /**
     * Executes a natural language command by querying the internal AIAssistant.
     */
    void executeSceneCommand(String command);

    /**
     * Executes a technical command (glTF document or Delta JSON array).
     */
    void executeTechnicalCommand(String command);

    // --- SYSTEM ---

    /**
     * Returns a JSON-like summary of the scene for the AI to "see" the world.
     */
    String getSceneSummary();

    /**
     * Exports the current scene state to a glTF 2.0 string.
     */
    String exportToGltf();
}
