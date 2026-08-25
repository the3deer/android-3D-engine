package org.the3deer.android.engine.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Object3D {
    private String id;
    private String name;
    private float[] position;
    private float[] rotation;
    private float[] scale;
    private boolean visible;
    private String color;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public float[] getPosition() { return position; }
    public void setPosition(float[] position) { this.position = position; }

    public float[] getRotation() { return rotation; }
    public void setRotation(float[] rotation) { this.rotation = rotation; }

    public float[] getScale() { return scale; }
    public void setScale(float[] scale) { this.scale = scale; }

    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}
