package org.the3deer.engine.android.shader;

import android.content.Context;

import androidx.annotation.NonNull;

import org.the3deer.engine.android.R;
import org.the3deer.engine.android.shader.v2.ShaderImplV2;
import org.the3deer.engine.android.shader.v3.ShaderImplV3;
import org.the3deer.engine.model.AnimatedModel;
import org.the3deer.engine.model.Constants;
import org.the3deer.engine.model.Object3D;
import org.the3deer.util.bean.BeanInit;
import org.the3deer.util.io.IOUtils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

/**
 * <p>
 * The ShaderFactory Encapsulates version selection (V2 vs V3) and shader resource management.
 * </p>
 * <p>
 * This component may only be invoked in the GLThread
 * </p>
 * <p>
 * Copyright 2013-2026 the3deer.org
 * </p>
 * <p>
 *
 * @author andresoviedo
 * @author Gemini AI
 */
public class ShaderFactory {

    private static final Logger logger = Logger.getLogger(ShaderFactory.class.getSimpleName());

    @Inject
    private Context context;

    private final Map<Integer, String> shadersCode = new HashMap<>();
    private final Map<String, Shader> shadersCache = new HashMap<>();

    @BeanInit
    public void setUp() {
        if (context == null) throw new IllegalStateException("Context is null");

        logger.info("Processing "+R.raw.class.getFields().length+" shaders...");
        Field[] fields = R.raw.class.getFields();
        for (Field field : fields) {
            try {
                int resId = field.getInt(field);
                String code = new String(IOUtils.read(context.getResources().openRawResource(resId)));
                shadersCode.put(resId, code);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to load shader resource: " + field.getName());
            }
        }
    }

    /**
     * Main entry point for clients to get the appropriate shader.
     * Clients don't need to know which GLSL files are being used.
     */
    public Shader getShader(Object3D obj) {
        // Determine the shader type based on model features
        boolean isAnimated = obj instanceof AnimatedModel && ((AnimatedModel) obj).getSkin() != null;
        boolean isPoints = obj.getDrawMode() == android.opengl.GLES20.GL_POINTS;

        // Map to the correct resource IDs based on version and type
        int vertResId, fragResId;
        String typeId;

        if (Constants.ANIMATIONS_ENABLED && isAnimated) {
            typeId = "shader_v"+Constants.DEFAULT_SHADER_VERSION+"_animated";
            vertResId = (Constants.DEFAULT_SHADER_VERSION == 3) ? R.raw.shader_v3_animated_vert : R.raw.shader_v2_animated_vert;
            fragResId = (Constants.DEFAULT_SHADER_VERSION == 3) ? R.raw.shader_v3_animated_frag : R.raw.shader_v2_animated_frag;
        } else if (Constants.LIGHTING_ENABLED && !isPoints) {
            typeId = "shader_v"+Constants.DEFAULT_SHADER_VERSION+"_static";
            vertResId = (Constants.DEFAULT_SHADER_VERSION == 3) ? R.raw.shader_v3_static_vert : R.raw.shader_v2_static_vert;
            fragResId = (Constants.DEFAULT_SHADER_VERSION == 3) ? R.raw.shader_v3_static_frag : R.raw.shader_v2_static_frag;
        } else {
            typeId = "shader_v"+Constants.DEFAULT_SHADER_VERSION+"_basic";
            vertResId = (Constants.DEFAULT_SHADER_VERSION == 3) ? R.raw.shader_v3_basic_vert : R.raw.shader_v2_basic_vert;
            fragResId = (Constants.DEFAULT_SHADER_VERSION == 3) ? R.raw.shader_v3_basic_frag : R.raw.shader_v2_basic_frag;
        }

        // Check cache
        String cacheKey = typeId;
        Shader shader = shadersCache.get(cacheKey);
        if (shader != null) return shader;

        String vertCode = shadersCode.get(vertResId);
        String fragCode = shadersCode.get(fragResId);

        // check
        if (vertCode == null || fragCode == null) {
           logger.finest("Engine is still loading. Cannot load shader: " + cacheKey);
            return null;
        }

        // Instantiate the right implementation
        logger.info("Instantiating Shader: " + cacheKey);

        if (Constants.DEFAULT_SHADER_VERSION == 3) {
            shader = ShaderImplV3.getInstance(cacheKey, vertCode, fragCode);
        } else {
            shader = ShaderImplV2.getInstance(cacheKey, vertCode, fragCode);
        }

        shadersCache.put(cacheKey, shader);

        // debug
        logger.info("Shader loaded: " + cacheKey);

        return shader;
    }

    public Shader getShader(ShaderResource shaderResource) {
        return getShader(shaderResource.id, shaderResource.vertexShaderResourceId, shaderResource.fragmentShaderResourceId);
    }

    /**
     * Return the shader loaded in GPU.
     * @param resIdVertexShader the shader resource id
     * @return
     */
    private Shader getShader(String id, int resIdVertexShader, int resIdFragmentShader){
        //final String shaderName = shadersCode.get(resIdVertexShader);
        final Shader shader = shadersCache.get(id);
        if (shader == null){
            logger.config("Loading shader... "+ id);
            final Shader impl = loadShader(id, resIdVertexShader, resIdFragmentShader);
            shadersCache.put(id, impl);
            logger.config("Loaded "+ id);
            return impl;
        }
        return shader;
    }

    @NonNull
    private Shader loadShader(String shaderId, int vertexShaderResourceId, int fragmentShaderResourceId) {
        if (Constants.DEFAULT_SHADER_VERSION == 3){
            return ShaderImplV3.getInstance(shaderId, shadersCode.get(vertexShaderResourceId), shadersCode.get(fragmentShaderResourceId));
        } else if (Constants.DEFAULT_SHADER_VERSION == 2) {
            return ShaderImplV2.getInstance(shaderId, shadersCode.get(vertexShaderResourceId), shadersCode.get(fragmentShaderResourceId));
        } else {
            throw new IllegalArgumentException("Unsupported shader version: "+Constants.DEFAULT_SHADER_VERSION);
        }
    }

    public void reset() {

        logger.info("Resetting shaders... size: "+shadersCache.size());

        for (Shader shader : shadersCache.values())
            shader.reset();

        shadersCache.clear();
    }

    @Deprecated
    public Map<String, Shader> getShaders() {
        return shadersCache;
    }
}
