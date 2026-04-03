package org.the3deer.engine.services.collada.entities;

// Add this private static inner class to ColladaParser.java
public class EffectData {
    public String effectId;
    public String imageId;

    public float[] diffuseColor; // To hold the <diffuse><color>
    public float[] specularColor;
    public float[] ambientColor;
    public float[] emissionColor;

    public Float transparency;   // To hold the <transparency><float>
    public String specularTextureId;

    public EffectData(String effectId) {
        this.effectId = effectId;
    }
}
