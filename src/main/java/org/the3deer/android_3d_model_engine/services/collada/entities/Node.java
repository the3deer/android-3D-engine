package org.the3deer.android_3d_model_engine.services.collada.entities;

import java.util.ArrayList;
import java.util.List;

public class Node {
    private String id;
    private float[] transform; // Always a 4x4 matrix
    private Node parent;
    private List<Node> children = new ArrayList<>();
    private String instanceGeometryId; // The ID of the geometry this node instances e.g. "Cube_000-mesh"
    private String instanceControllerId; // The ID of the controller this node instances

    // Constructors, Getters & Setters
    public Node(String id) {
        this.id = id;
    }
    
    // ... we will add getters and setters for all fields ...
    
    public String getId() { return id; }
    public void setTransform(float[] transform) { this.transform = transform; }
    public float[] getTransform() { return transform; }
    public void setParent(Node parent) { this.parent = parent; }
    public Node getParent() { return parent; }
    public void addChild(Node child) { this.children.add(child); }
    public List<Node> getChildren() { return children; }
    public void setInstanceGeometryId(String id) { this.instanceGeometryId = id; }
    public String getInstanceGeometryId() { return instanceGeometryId; }

    public String getInstanceControllerId() {
        return instanceControllerId;
    }

    public void setInstanceControllerId(String instanceControllerId) {
        this.instanceControllerId = instanceControllerId;
    }
}
