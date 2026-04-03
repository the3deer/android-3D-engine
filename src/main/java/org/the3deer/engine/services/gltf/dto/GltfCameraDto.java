package org.the3deer.engine.services.gltf.dto;

public class GltfCameraDto {
    public String name;
    public String type; // "perspective" or "orthographic"
    
    // Perspective
    public Float aspectRatio;
    public Float yfov;
    public Float zfar;
    public Float znear;

    // Orthographic
    public Float xmag;
    public Float ymag;
}
