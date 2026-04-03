package org.the3deer.engine.services.collada.entities;

import java.util.ArrayList;
import java.util.List;

public class Node {
    private String id;
    // scope identifier - used by collada to reference channels in animations
    private String sid;
    private String name;
    private float[] transform; // Always a 4x4 matrix
    private Node parent;
    private List<Node> children = new ArrayList<>();
    private String instanceGeometryId; // The ID of the geometry this node instances e.g. "Cube_000-mesh"
    private String bindMaterialId; // The ID of the geometry this node instances e.g. "Cube_000-mesh"
    private String instanceControllerId; // The ID of the controller this node instances
    private String skinId;

    // Constructors, Getters & Setters
    public Node(String id, String name) {
        this.id = id;
        this.name = name;
    }
    
    // ... we will add getters and setters for all fields ...
    
    public String getId() { return id; }

    public String getName() {
        return name;
    }

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public void setTransform(float[] transform) { this.transform = transform; }
    public float[] getTransform() { return transform; }
    public void setParent(Node parent) { this.parent = parent; }
    public Node getParent() { return parent; }
    public void addChild(Node child) { this.children.add(child); }
    public List<Node> getChildren() { return children; }
    public void setInstanceGeometryId(String id) { this.instanceGeometryId = id; }
    public String getInstanceGeometryId() { return instanceGeometryId; }

    public String getBindMaterialId() {
        return bindMaterialId;
    }

    public void setBindMaterialId(String bindMaterialId) {
        this.bindMaterialId = bindMaterialId;
    }

    public String getInstanceControllerId() {
        return instanceControllerId;
    }

    public void setInstanceControllerId(String instanceControllerId) {
        this.instanceControllerId = instanceControllerId;
    }

    public String getSkinId() {
        return skinId;
    }

    public void setSkinId(String skinId) {
        this.skinId = skinId;
    }

    public boolean isAncestorOf(Node otherNode) {
        if (otherNode == null) return false;
        Node current = otherNode.getParent();
        while (current != null) {
            if (current.equals(this)) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }
}
