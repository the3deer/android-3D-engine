package org.the3deer.android_3d_model_engine.shader;

import android.content.Context;
import android.opengl.GLES30;
import android.os.Bundle;
import android.util.SparseArray;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceGroup;

import org.the3deer.android_3d_model_engine.model.AnimatedModel;
import org.the3deer.android_3d_model_engine.model.Constants;
import org.the3deer.android_3d_model_engine.model.Element;
import org.the3deer.android_3d_model_engine.model.Material;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.model.Skin;
import org.the3deer.android_3d_model_engine.model.Texture;
import org.the3deer.android_3d_model_engine.preferences.PreferenceAdapter;
import org.the3deer.android_3d_model_engine.renderer.GpuAsset;
import org.the3deer.android_3d_model_engine.renderer.GpuManager;
import org.the3deer.util.android.GLUtil;
import org.the3deer.util.math.Math3DUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * OpenGL ES 3.x High-Performance Shader Implementation
 * Uses VAOs and VBOs for maximum efficiency.
 * 
 * @author Gemini AI
 */
public class ShaderImplV3 implements Shader, PreferenceAdapter {

    private static final String TAG = Shader.class.getSimpleName();

    private final String id;
    private final int mProgram;
    private final Set<String> features;

    // Feature toggles
    private final boolean supportsMMatrix;
    private final boolean supportsLighting;
    private final boolean supportsAnimation;
    private final boolean supportsTextures;
    private final boolean supportBlending;
    private final boolean supportsColors;

    private boolean autoUseProgram = true;
    private final SparseArray<String> jointCache = new SparseArray<>();
    private final List<Texture> textures = new ArrayList<>();

    private GpuManager gpuManager = new GpuManager();

    static ShaderImplV3 getInstance(String id, String vertexShaderCode, String fragmentShaderCode) {
        return new ShaderImplV3(id, vertexShaderCode, fragmentShaderCode);
    }

    private ShaderImplV3(String id, String vertexShaderCode, String fragmentShaderCode) {
        this.id = id;
        this.features = new HashSet<>();

        this.supportsMMatrix = vertexShaderCode.contains("u_MMatrix");
        this.supportsLighting = fragmentShaderCode.contains("u_LightPos");
        this.supportsAnimation = vertexShaderCode.contains("jointTransforms");
        this.supportsTextures = vertexShaderCode.contains("a_TexCoordinate");
        this.supportBlending = fragmentShaderCode.contains("u_AlphaMode");
        this.supportsColors = vertexShaderCode.contains("a_Color");

        int vertexShader = GLUtil.loadShader(GLES30.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = GLUtil.loadShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderCode);
        mProgram = GLUtil.createAndLinkProgram(vertexShader, fragmentShader, null);
    }

    @Override
    public void draw(Object3DData obj, float[] pMatrix, float[] vMatrix, float[] lightPos, float[] colorMask, float[] cameraPos, int drawMode, int drawSize) {

        // init or get
        GpuAsset gpuAsset = gpuManager.getAsset(obj);
        if (gpuAsset == null) return;

        // do draw
        draw(gpuAsset, obj, pMatrix, vMatrix, lightPos, colorMask, cameraPos);
    }

    public void draw(GpuAsset asset, Object3DData obj, float[] pMatrix, float[] vMatrix, float[] lightPos, float[] colorMask, float[] cameraPos) {
        if (this.autoUseProgram) {
            useProgram();
        }

        // 1. Set Global Matrices
        setUniformMatrix4(vMatrix, "u_VMatrix");
        setUniformMatrix4(pMatrix, "u_PMatrix");

        // 2. Set Object Matrix
        float[] modelMatrix = obj.getModelMatrix();
        if (obj instanceof AnimatedModel) {
            AnimatedModel animatedModel = (AnimatedModel) obj;
            if (animatedModel.getParentNode() != null && animatedModel.getParentNode().getSkin() != null) {
                // If skinned, world transform is in the bones, so model matrix is identity
                modelMatrix = Math3DUtils.IDENTITY_MATRIX;
            }
        }
        setUniformMatrix4(modelMatrix, "u_MMatrix");

        // 3. Set Normal Matrix (Inverse-Transpose of Model Matrix)
        float[] normalMatrix = obj.getNormalMatrix();
        if (obj instanceof AnimatedModel) {
            AnimatedModel animatedModel = (AnimatedModel) obj;
            if (animatedModel.getParentNode() != null && animatedModel.getParentNode().getSkin() != null) {
                normalMatrix = Math3DUtils.IDENTITY_MATRIX;
            }
        }
        setUniformMatrix4(normalMatrix, "u_NormalMatrix");

        // 4. Set Lighting & Camera
        if (supportsLighting) {
            setUniform3(lightPos, "u_LightPos");
            setUniform3(cameraPos, "u_cameraPos");
            setFeatureFlag("u_Lighted", lightPos != null);
        }

        // 5. Set Animation Data
        if (supportsAnimation && obj instanceof AnimatedModel) {
            AnimatedModel anim = (AnimatedModel) obj;
            Skin skin = anim.getSkin();
            boolean isAnimated = skin != null && skin.getJointTransforms() != null;
            setFeatureFlag("u_Animated", isAnimated);
            if (isAnimated) {
                setUniformMatrix4(skin.getBindShapeMatrix(), "u_BindShapeMatrix");
                setJointTransforms(anim);
            }
        }

        // 6. Set Default Color
        // Set Element-specific Color/Material
        final Material baseMaterial = obj.getMaterial() != null? obj.getMaterial() : Constants.DEFAULT_MATERIAL;
        float[] baseColor = baseMaterial.getColor() != null ? baseMaterial.getColor() : Constants.DEFAULT_MATERIAL.getColor();
        setUniform4(baseColor, "u_Color");

        // 7. Set Per-Vertex Color Flag
        if (supportsColors) {
            setFeatureFlag("u_Coloured", obj.getColorsBuffer() != null);
        }

        // 8. Bind VAO and Draw (Handle Multi-Elements)
        asset.bind();
        List<GpuAsset.GpuElement> gpuElements = asset.getGpuElements();
        if (gpuElements != null && !gpuElements.isEmpty()) {
            List<Element> elements = obj.getElements();
            for (int i = 0; i < gpuElements.size(); i++) {
                GpuAsset.GpuElement gpuElement = gpuElements.get(i);
                
                // Set Element-specific Color/Material
                Material material = (elements != null && i < elements.size()) ? elements.get(i).getMaterial() : null;
                if (material == null) {
                    material = obj.getMaterial();
                }
                
                float[] color = (material != null) ? material.getColor() : Constants.COLOR_WHITE;
                if (color == null) {
                    color = Constants.COLOR_WHITE;
                }
                setUniform4(color, "u_Color");

                // Set Alpha mode for blending
                if (supportBlending && material != null) {
                    setUniformInt(material.getAlphaMode().ordinal(), "u_AlphaMode");
                    setUniform1(material.getAlphaCutoff(), "u_AlphaCutoff");
                }

                // Bind Index Buffer and Draw
                GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, gpuElement.getEboId());
                GLES30.glDrawElements(asset.getDrawMode(), gpuElement.getCount(), gpuElement.getType(), 0);
            }
        } else {
            // Default object color fallback
            float[] color = obj.getMaterial() != null ? obj.getMaterial().getColor() : Constants.COLOR_WHITE;
            if (color == null) color = Constants.COLOR_WHITE;
            setUniform4(color, "u_Color");

            GLES30.glDrawArrays(asset.getDrawMode(), 0, asset.getVertexCount());
        }
        asset.unbind();
    }



    private void setJointTransforms(AnimatedModel animatedModel) {
        float[][] transforms = animatedModel.getSkin().getJointTransforms();
        for (int i = 0; i < transforms.length; i++) {
            String name = jointCache.get(i);
            if (name == null) {
                name = "jointTransforms[" + i + "]";
                jointCache.put(i, name);
            }
            setUniformMatrix4(transforms[i], name);
        }
    }

    private void setUniformMatrix4(float[] matrix, String name) {
        int handle = GLES30.glGetUniformLocation(mProgram, name);
        if (handle != -1 && matrix != null) GLES30.glUniformMatrix4fv(handle, 1, false, matrix, 0);
    }

    private void setUniform3(float[] vec, String name) {
        int handle = GLES30.glGetUniformLocation(mProgram, name);
        if (handle != -1 && vec != null) GLES30.glUniform3fv(handle, 1, vec, 0);
    }

    private void setUniform4(float[] vec, String name) {
        int handle = GLES30.glGetUniformLocation(mProgram, name);
        if (handle != -1 && vec != null) GLES30.glUniform4fv(handle, 1, vec, 0);
    }

    private void setUniform1(float value, String name) {
        int handle = GLES30.glGetUniformLocation(mProgram, name);
        if (handle != -1) GLES30.glUniform1f(handle, value);
    }

    private void setUniformInt(int value, String name) {
        int handle = GLES30.glGetUniformLocation(mProgram, name);
        if (handle != -1) GLES30.glUniform1i(handle, value);
    }

    private void setFeatureFlag(String name, boolean enabled) {
        int handle = GLES30.glGetUniformLocation(mProgram, name);
        if (handle != -1) GLES30.glUniform1i(handle, enabled ? 1 : 0);
    }

    @Override public void useProgram() { GLES30.glUseProgram(mProgram); }
    @Override public int getProgram() { return mProgram; }
    @Override public int getId() { return mProgram; }
    @Override public String getName() { return id; }
    @Override public void setAutoUseProgram(boolean auto) { this.autoUseProgram = auto; }
    @Override public void reset() {}
    @Override public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey, Context context, PreferenceGroup screen) {}
}
