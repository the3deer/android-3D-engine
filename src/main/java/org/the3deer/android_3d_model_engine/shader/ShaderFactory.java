package org.the3deer.android_3d_model_engine.shader;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import org.the3deer.android_3d_model_engine.R;
import org.the3deer.android_3d_model_engine.model.AnimatedModel;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.util.io.IOUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Copyright 2013-2020 the3deer.org
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE
 * OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
public class ShaderFactory {

    /**
     * singleton
     */
    private static ShaderFactory instance = null;
    /**
     * shader code loaded from raw resources
     * resources are cached on activity thread
     */
    private Map<Integer, String> shadersIds = new HashMap<>();
    /**
     * list of opengl drawers
     */
    private Map<ShaderResource, ShaderImpl> drawers = new HashMap<>();

    /**
     * Read all shader data from /raw folder (context required for IO).
     * @param context the application context
     * @throws IllegalAccessException if there is any issue accessing the R class
     * @throws IOException if there is any issue reading the file
     */
    public ShaderFactory(Context context) {

        Log.i("ShaderFactory", "Discovering shaders...");
        Field[] fields = R.raw.class.getFields();
        for (Field field : fields) {
            String shaderId = field.getName();
            try {
                Log.v("ShaderFactory", "Loading shader... " + shaderId);
                int shaderResId = field.getInt(field);
                byte[] shaderBytes = IOUtils.read(context.getResources().openRawResource(shaderResId));
                String shaderCode = new String(shaderBytes);
                shadersIds.put(shaderResId, shaderCode);
            } catch (Exception e) {
                Log.e("ShaderFactory", "Issue loading shader... " + shaderId);
            }
        }
        Log.i("ShaderFactory", "Shaders loaded: " + shadersIds.size());
        instance = this;
    }

    public static ShaderFactory getInstance() {
        return instance;
    }

    public void reset(){
        drawers.clear();
    }

    /**
     * Load (requires having a GL Context) or get the shaders loaded in GPU.
     *
     * @param obj
     * @param usingSkyBox
     * @param usingTextures
     * @param usingLights
     * @param usingAnimation
     * @param isShadow
     * @param isUsingShadows
     * @return
     */
    public Shader getShader(Object3DData obj, boolean usingSkyBox,
                            boolean usingTextures, boolean usingLights,
                            boolean usingAnimation, boolean isShadow, boolean isUsingShadows) {

        // double check features
        final boolean animationOK = obj instanceof AnimatedModel
                && ((AnimatedModel) obj).getAnimation() != null
                && (((AnimatedModel) obj).getAnimation()).isInitialized();
        final boolean isAnimated = usingAnimation && (obj == null || animationOK);
        final boolean isLighted = usingLights && obj != null && obj.getNormalsBuffer() != null;
        final boolean isTextured = usingTextures && obj != null && obj.getTextureBuffer() != null;

        // match shaders
        final ShaderResource shader;
        if (usingSkyBox){
            shader = ShaderResource.SKYBOX;
        } else {
            shader = getShader(isTextured, isLighted, isAnimated, isShadow, isUsingShadows);
        }

        // get cached shaders
        ShaderImpl renderer = drawers.get(shader);
        if (renderer != null) {
            renderer.setTexturesEnabled(isTextured);
            renderer.setLightingEnabled(isLighted);
            renderer.setAnimationEnabled(isAnimated);
            return renderer;
        }

        // build drawer
        String vertexShaderCode;

        // experimental: inject glPointSize
        vertexShaderCode = shadersIds.get(shader.vertexShaderResourceId).replace("void main(){", "void main(){\n\tgl_PointSize = 5.0;");

        // use opengl constant to dynamically set up array size in shaders. That should be >=120
        vertexShaderCode = vertexShaderCode.replace("const int MAX_JOINTS = 60;", "const int MAX_JOINTS = gl_MaxVertexUniformVectors > 60 ? 60 : gl_MaxVertexUniformVectors;");

        // create drawer
        /*Log.v("RendererFactory", "\n---------- Vertex shader ----------\n");
        Log.v("RendererFactory", vertexShaderCode);
        Log.v("RendererFactory", "---------- Fragment shader ----------\n");
        Log.v("RendererFactory", fragmentShaderCode);
        Log.v("RendererFactory", "-------------------------------------\n");*/
        renderer = ShaderImpl.getInstance(shader.id, vertexShaderCode, shadersIds.get(shader.fragmentShaderResourceId));
        renderer.setTexturesEnabled(isTextured);
        renderer.setLightingEnabled(isLighted);
        renderer.setAnimationEnabled(isAnimated);

        // cache drawer
        drawers.put(shader, renderer);

        // return drawer
        return renderer;
    }

    @NonNull
    private ShaderResource getShader(boolean isTextured, boolean isLighted, boolean isAnimated, boolean isShadow, boolean isUsingShadows) {

        final ShaderResource ret;
        if (isShadow) {
            return ShaderResource.SHADOW;
        } else if (isUsingShadows){
            return ShaderResource.SHADOWED;
        } else if (isAnimated || isTextured || isLighted){
            ret = ShaderResource.ANIMATED;
        } else {
            ret = ShaderResource.BASIC;
        }
        return ret;
    }

    public Shader  getBoundingBoxDrawer() {
        return getShader(null, false, false, false, false, false, false);
    }

    public Shader  getFaceNormalsDrawer() {
        return getShader(null, false, false, false, false, false, false);
    }

    public Shader  getBasicShader() {
        return getShader(null, false, false, false, false, false, false);
    }

    public Shader  getSkyBoxDrawer() {
        return getShader(null, true, false, false, false, false, false);
    }

    public Shader  getShadowRenderer(){
        return getShader(null, false, false, false, true, true, false);
    }

    public Shader  getShadowRenderer2(Object3DData obj){
        return getShader(obj, false, true, true, true, false, true);
    }

}
