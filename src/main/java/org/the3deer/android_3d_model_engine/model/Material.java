package org.the3deer.android_3d_model_engine.model;

import androidx.annotation.NonNull;

import java.util.Arrays;

public class Material {

    public enum AlphaMode {
        /**
         * Opaque mode
         */
        OPAQUE,

        /**
         * Masking mode
         */
        MASK,

        /**
         * Blend mode
         */
        BLEND
    }

    // material name
    private String name;

    // colour info
    private float[] ambient;
    private float[] diffuse;
    private float[] specular;
    private float shininess;
    private float alpha = 1.0f;
    private float alphaCutoff = 0f;
    private AlphaMode alphaMode = AlphaMode.BLEND;

    // final color
    private float[] color;

    // texture info
    private Texture colorTexture;
    private Texture normalTexture;
    private Texture emissiveTexture;
    private float[] emissiveFactor;

    // volume
    private Texture transmissionTexture;
    private float thicknessFactor;
    private float attenuationDistance;
    private float[] attenuationColor;

    public Material() {
    }

    public Material(String nm) {
        name = nm;
    }

    // --------- set/get methods for colour info --------------

    public void setAlpha(float val) {
        alpha = val;
    }

    public float getAlpha() {
        return alpha;
    }

    public float getAlphaCutoff() {
        return alphaCutoff;
    }

    public void setAlphaCutoff(float alphaCutoff) {
        this.alphaCutoff = alphaCutoff;
    }

    public AlphaMode getAlphaMode() {
        return alphaMode;
    }

    public void setAlhaMode(AlphaMode alphaMode) {
        this.alphaMode = alphaMode;
    }

    public void setShininess(float val) {
        shininess = val;
    }

    public float getShininess() {
        return shininess;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float[] getAmbient() {
        return ambient;
    }

    public void setAmbient(float[] ambient) {
        this.ambient = ambient;
    }

    public float[] getDiffuse() {
        return diffuse;
    }

    public void setDiffuse(float[] diffuse) {
        this.diffuse = diffuse;
    }

    public float[] getSpecular() {
        return specular;
    }

    public void setSpecular(float[] specular) {
        this.specular = specular;
    }

    public void setColorTexture(Texture colorTexture) {
        this.colorTexture = colorTexture;
    }

    public Texture getColorTexture() {
        return this.colorTexture;
    }

    public void setTransmissionTexture(Texture transmissionTexture) {
        this.transmissionTexture = transmissionTexture;
    }

    public float getAttenuationDistance() {
        return attenuationDistance;
    }

    public void setAttenuationDistance(float attenuationDistance) {
        this.attenuationDistance = attenuationDistance;
    }

    public float[] getAttenuationColor() {
        return attenuationColor;
    }

    public void setAttenuationColor(float[] attenuationColor) {
        this.attenuationColor = attenuationColor;
    }

    public float getThicknessFactor() {
        return thicknessFactor;
    }

    public void setThicknessFactor(float thicknessFactor) {
        this.thicknessFactor = thicknessFactor;
    }

    public Texture getTransmissionTexture() {
        return this.transmissionTexture;
    }

    public Texture getNormalTexture() {
        return normalTexture;
    }

    public void setNormalTexture(Texture normalTexture) {
        this.normalTexture = normalTexture;
    }

    public Texture getEmissiveTexture() {
        return emissiveTexture;
    }

    public void setEmissiveTexture(Texture emissiveTexture) {
        this.emissiveTexture = emissiveTexture;
    }

    public float[] getEmissiveFactor() {
        return emissiveFactor;
    }

    public void setEmissiveFactor(float[] emissiveFactor) {
        this.emissiveFactor = emissiveFactor;
    }

    /**
     * @return the material color or white if not set (default)
     */
    public float[] getColor() {
        if (this.color == null) {
            this.color = Constants.COLOR_WHITE.clone();
        }
        if (this.diffuse != null) {
            this.color[0] = this.diffuse[0];
            this.color[1] = this.diffuse[1];
            this.color[2] = this.diffuse[2];
            if (this.diffuse.length >= 4)
                this.color[3] = this.diffuse[3];
        }
        if (this.ambient != null) {
            this.color[0] += this.ambient[0];
            this.color[1] += this.ambient[1];
            this.color[2] += this.ambient[2];
            if (this.ambient.length >= 4)
                this.color[3] += this.ambient[3];
        }
        this.color[3] *= this.alpha;
        return color;
    }

    @Override
    public String toString() {
        return "Material{" +
                "name='" + name + '\'' +
                ", ambient=" + Arrays.toString(ambient) +
                ", diffuse=" + Arrays.toString(diffuse) +
                ", specular=" + Arrays.toString(specular) +
                ", shininess=" + shininess +
                ", alpha=" + alpha +
                ", alphaCutoff=" + alphaCutoff +
                ", colorTexture='" + colorTexture + '\'' +
                '}';
    }

    @NonNull
    @Override
    protected Material clone() {
        final Material ret = new Material();
        ret.setColorTexture(this.getColorTexture());
        ret.setEmissiveTexture(this.getEmissiveTexture());
        ;
        ret.setNormalTexture(this.getNormalTexture());
        return ret;
    }
}
