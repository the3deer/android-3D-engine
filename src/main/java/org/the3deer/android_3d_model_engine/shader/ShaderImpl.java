package org.the3deer.android_3d_model_engine.shader;

import android.opengl.GLES20;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;

import org.the3deer.android_3d_model_engine.model.AnimatedModel;
import org.the3deer.android_3d_model_engine.model.Constants;
import org.the3deer.android_3d_model_engine.model.Element;
import org.the3deer.android_3d_model_engine.model.Material;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.model.Texture;
import org.the3deer.util.android.GLUtil;
import org.the3deer.util.io.IOUtils;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
class ShaderImpl implements Shader {

    private static final String TAG = Shader.class.getSimpleName();

    private static final int COORDS_PER_VERTEX = 3;
    private static final int TEXTURE_COORDS_PER_VERTEX = 2;
    private static final int COLOR_COORDS_PER_VERTEX = 4;

    private final static float[] DEFAULT_COLOR = Constants.COLOR_WHITE;
    private final static float[] NO_COLOR_MASK = Constants.COLOR_WHITE;

    // specification
    private final String id;
    private final Set<String> features;

    // opengl program
    private final int mProgram;

    // animation data
    // put 0 to draw progressively, -1 to draw at once
    private long counter = -1;
    private double shift = -1d;

    // does the device support drawElements for GL_INT or not?
    private boolean drawUsingInt = true;
    private int textureCounter = 0;

    /**
     * Join transform names cache (optimization)
     */
    private final SparseArray<String> cache1 = new SparseArray<>();
    /**
     * Runtime flags
     */
    private static Map<Object, Object> flags = new HashMap<>();

    private boolean texturesEnabled = true;
    private boolean lightingEnabled = true;
    private boolean animationEnabled = true;

    private boolean autoUseProgram = true;

    /**
     * Load the shaders into GPU. Requires having a GL Context.
     * This can be called in the onDrawFrame() Thread.
     *
     * @param id
     * @param vertexShaderCode
     * @param fragmentShaderCode
     * @return the compiled Shader
     */
    static ShaderImpl getInstance(String id, String vertexShaderCode, String fragmentShaderCode) {
        Set<String> shaderFeatures = new HashSet<>();
        testShaderFeature(shaderFeatures, vertexShaderCode, "u_MMatrix");
        testShaderFeature(shaderFeatures, vertexShaderCode, "a_Position");
        testShaderFeature(shaderFeatures, vertexShaderCode, "a_Normal");
        testShaderFeature(shaderFeatures, vertexShaderCode, "a_Color");
        testShaderFeature(shaderFeatures, vertexShaderCode, "a_Tangent");
        testShaderFeature(shaderFeatures, vertexShaderCode, "a_TexCoordinate");
        testShaderFeature(shaderFeatures, vertexShaderCode, "u_LightPos");
        testShaderFeature(shaderFeatures, vertexShaderCode, "in_jointIndices");
        testShaderFeature(shaderFeatures, vertexShaderCode, "in_weights");
        testShaderFeature(shaderFeatures, fragmentShaderCode, "u_LightPos");
        testShaderFeature(shaderFeatures, fragmentShaderCode, "u_TextureCube");
        testShaderFeature(shaderFeatures, fragmentShaderCode, "u_AlphaCutoff");
        testShaderFeature(shaderFeatures, fragmentShaderCode, "u_AlphaMode");
        testShaderFeature(shaderFeatures, fragmentShaderCode, "u_TextureTransformed");
        testShaderFeature(shaderFeatures, fragmentShaderCode, "u_TransmissionTexture");
        return new ShaderImpl(id, vertexShaderCode, fragmentShaderCode, shaderFeatures);
    }

    private static void testShaderFeature(Set<String> outputFeatures, String shaderCode, String feature) {
        if (shaderCode.contains(feature)) {
            outputFeatures.add(feature);
        }
    }

    /**
     * Load the shaders into GPU. Requires having a GL Context.
     * This can be called in the onDrawFrame() Thread.
     *
     * @param id
     * @param vertexShaderCode
     * @param fragmentShaderCode
     * @param features
     */
    private ShaderImpl(String id, String vertexShaderCode, String fragmentShaderCode, Set<String> features) {

        this.id = id;
        this.features = features;
        Log.i("GLES20Renderer", "Compiling 3D Drawer... " + id);

        // load shaders
        int vertexShader = GLUtil.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = GLUtil.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        // compile program
        mProgram = GLUtil.createAndLinkProgram(vertexShader, fragmentShader, features.toArray(new String[0]));

        flags.clear();
        Log.v("GLES20Renderer", "Compiled 3D Drawer (" + id + ") with id " + mProgram);
    }

    @Override
    public void setAutoUseProgram(boolean autoUseProgram) {
        this.autoUseProgram = autoUseProgram;
    }

    @Override
    public void useProgram() {
        // Add program to OpenGL environment
        GLES20.glUseProgram(mProgram);
        if (GLUtil.checkGlError("glUseProgram")) {
            //return;
        }
    }

    @Override
    public int getProgram() {
        return mProgram;
    }

    public void setTexturesEnabled(boolean texturesEnabled) {
        this.texturesEnabled = texturesEnabled;
    }

    public void setLightingEnabled(boolean lightingEnabled) {
        this.lightingEnabled = lightingEnabled;
    }

    public void setAnimationEnabled(boolean animationEnabled) {
        this.animationEnabled = animationEnabled;
    }

    @Override
    public void draw(Object3DData obj, float[] pMatrix, float[] vMatrix, float[] lightPosInWorldSpace, float[] colorMask, float[] cameraPos, int drawMode, int drawSize) {
        if (obj.getElements() != null && obj.getElements().size() > 1) {
            for (int i = 0; i < obj.getElements().size(); i++) {
                if (obj.getElements().get(i).getMaterial() == null) {
                    draw2(obj, obj.getElements().get(i), pMatrix, vMatrix, lightPosInWorldSpace, colorMask, cameraPos, drawMode, drawSize);
                }
            }
            for (int i = 0; i < obj.getElements().size(); i++) {
                if (obj.getElements().get(i).getMaterial() != null && obj.getElements().get(i).getMaterial().getAlphaMode() == Material.AlphaMode.OPAQUE)
                    draw2(obj, obj.getElements().get(i), pMatrix, vMatrix, lightPosInWorldSpace, colorMask, cameraPos, drawMode, drawSize);
            }
            for (int i = 0; i < obj.getElements().size(); i++) {
                if (obj.getElements().get(i).getMaterial() != null && obj.getElements().get(i).getMaterial().getAlphaMode() != Material.AlphaMode.OPAQUE) {
                    draw2(obj, obj.getElements().get(i), pMatrix, vMatrix, lightPosInWorldSpace, colorMask, cameraPos, drawMode, drawSize);
                }
            }

        } else {
            draw2(obj, null, pMatrix, vMatrix, lightPosInWorldSpace, colorMask, cameraPos, drawMode, drawSize);
        }
    }

    public void draw2(Object3DData obj, Element element, float[] pMatrix, float[] vMatrix, float[] lightPosInWorldSpace, float[] colorMask, float[] cameraPos, int drawMode, int drawSize) {

        // log event once
        /*if (!flags.contains(obj.getId())) {
            //Log.d("GLES20Renderer", "Rendering with shader: " + id + "vert... obj: " + obj);
            flags.put(obj.getId(), this.id);
        }*/

        if (this.autoUseProgram) {
            useProgram();
        }

        // reset texture counter
        textureCounter=0;

        //setFeatureFlag("u_Debug",false);

        // mvp matrix for position + lighting + animation
        if (supportsMMatrix()) {
            setUniformMatrix4(obj.getModelMatrix(), "u_MMatrix");
        }
        setUniformMatrix4(vMatrix, "u_VMatrix");
        setUniformMatrix4(pMatrix, "u_PMatrix");

        // pass in vertex buffer
        int mPositionHandle = setVBO("a_Position", obj.getVertexBuffer(), COORDS_PER_VERTEX, GLES20.GL_FLOAT);

        // pass in normals buffer for lighting
        int mNormalHandle = -1;
        if (supportsNormals() && obj.getNormalsBuffer() != null) {
            mNormalHandle = setVBO("a_Normal", obj.getNormalsBuffer(), COORDS_PER_VERTEX, GLES20.GL_FLOAT);
        }

        // pass in normals map for lighting
        int mNormalMapHandle = -1;
        if (supportsTangent()) {
            boolean toggle = obj.getNormalsBuffer() != null && obj.getTangentBuffer() != null;
            mNormalMapHandle = setVBO("a_Tangent", obj.getTangentBuffer(), COORDS_PER_VERTEX, GLES20.GL_FLOAT);
            setFeatureFlag("u_NormalTextured", toggle);
        }

        // pass in color or colors array
        if (obj.getColor() != null) {
            setUniform4(obj.getColor(), "vColor");
        } else {
            setUniform4(DEFAULT_COLOR, "vColor");
        }

        // colors
        int mColorHandle = -1;
        if (supportsColors()) {
            setFeatureFlag("u_Coloured", obj.getColorsBuffer() != null);
            if (obj.getColorsBuffer() != null) {
                mColorHandle = setVBO("a_Color", obj.getColorsBuffer(), COLOR_COORDS_PER_VERTEX, -1);
            }
        }

        // pass in color mask - i.e. stereoscopic
        setUniform4(colorMask != null ? colorMask : NO_COLOR_MASK, "vColorMask");

        // alpha settings
        if (supportsBlending()) {
            setUniform1(obj.getMaterial().getAlphaCutoff(), "u_AlphaCutoff");
            setUniformInt(obj.getMaterial().getAlphaMode().ordinal(), "u_AlphaMode");
        }

        // pass in texture UV buffer
        int mTextureHandle = -1;
        if (supportsTextures() && obj.getTextureBuffer() != null) {

            mTextureHandle = setVBO("a_TexCoordinate", obj.getTextureBuffer(), TEXTURE_COORDS_PER_VERTEX, GLES20.GL_FLOAT);
            setFeatureFlag("u_Textured", texturesEnabled);

            if (obj.getMaterial().getColorTexture() != null) {
                loadTexture(obj.getMaterial().getColorTexture());
                setTexture(obj.getMaterial().getColorTexture(), "u_Texture", 0);
                setFeatureFlag("u_Textured", true);
            }

            if (obj.getMaterial().getNormalTexture() != null) {
                loadTexture(obj.getMaterial().getNormalTexture());
                setTexture(obj.getMaterial().getNormalTexture(), "u_NormalTexture", 1);
                setFeatureFlag("u_NormalTextured", true);
            }

            boolean enableEmissive = obj.getMaterial().getEmissiveTexture() != null && obj.getMaterial().getEmissiveFactor() != null;
            setFeatureFlag("u_EmissiveTextured", enableEmissive);
            if (enableEmissive) {
                loadTexture(obj.getMaterial().getEmissiveTexture());
                setTexture(obj.getMaterial().getEmissiveTexture(), "u_EmissiveTexture", 2);
                setUniform3(obj.getMaterial().getEmissiveFactor(), "u_EmissiveFactor");
            }
        }

        // pass in the SkyBox texture
        if (obj.getMaterial().getColorTexture() != null && supportsTextureCube()) {
            setTextureCube(obj.getMaterial().getColorTexture().getId(), 3);
        }

        // pass in light position for lighting
        if (supportsLighting() && lightPosInWorldSpace != null && cameraPos != null) {
            boolean toggle = lightingEnabled && obj.getNormalsBuffer() != null;
            setFeatureFlag("u_Lighted", toggle);
            setUniform3(lightPosInWorldSpace, "u_LightPos");
            setUniform3(cameraPos, "u_cameraPos");
        }

        // pass in joint transformation for animated model
        int in_weightsHandle = -1;
        int in_jointIndicesHandle = -1;
        if (supportsJoints()) {
            final boolean animationOK = obj instanceof AnimatedModel
                    && ((AnimatedModel) obj).getJointMatrices() != null
                    && ((AnimatedModel) obj).getVertexWeights() != null
                    && ((AnimatedModel) obj).getJointIds() != null;
            boolean toggle = this.animationEnabled && animationOK;
            if (toggle) {
                in_weightsHandle = setVBO("in_weights", ((AnimatedModel) obj).getVertexWeights(), ((AnimatedModel) obj).getWeightsComponents(), -1);
                in_jointIndicesHandle = setVBO("in_jointIndices", ((AnimatedModel) obj).getJointIds(), ((AnimatedModel) obj).getJointComponents(), -1);
                setUniformMatrix4(((AnimatedModel) obj).getBindShapeMatrix(), "u_BindShapeMatrix");
                setJointTransforms((AnimatedModel) obj);
            }
            setFeatureFlag("u_Animated", toggle);
        }

        // draw mesh
        drawShape(obj, element, drawMode, drawSize);

        // Disable vertex handlers
        disableVBO(mPositionHandle);
        disableVBO(mColorHandle);
        disableVBO(mNormalHandle);
        disableVBO(mNormalMapHandle);
        disableVBO(mTextureHandle);
        disableVBO(in_weightsHandle);
        disableVBO(in_jointIndicesHandle);
    }

    private void loadTexture(Texture texture) {

        // check
        if (texture == null || texture.hasId()) return;

        // check
        if (texture.getData() == null && texture.getBitmap() == null) return;

        // load
        final int textureId;
        if (texture.getBitmap() != null) {
            textureId = GLUtil.loadTexture(texture.getBitmap());
            texture.setId(textureId);
        } else if (texture.getData() != null) {
            textureId = GLUtil.loadTexture(texture.getData());
            texture.setId(textureId);
        }
    }

    private int setVBO(final String shaderVariableName, final Buffer buffer, int componentsPerVertex, int glType) {

        // check
        if (buffer == null) return -1;

        int handler = GLES20.glGetAttribLocation(mProgram, shaderVariableName);
        GLUtil.checkGlError("glGetAttribLocation");

        GLES20.glEnableVertexAttribArray(handler);
        GLUtil.checkGlError("glEnableVertexAttribArray");

        // FIXME: type should be a preset
        if (glType == -1){
            if (buffer instanceof FloatBuffer){
                glType = GLES20.GL_FLOAT;
            } else if (buffer instanceof IntBuffer){
                glType = GLES20.GL_UNSIGNED_INT;
            } else if (buffer instanceof ShortBuffer){
                glType = GLES20.GL_UNSIGNED_SHORT;
            } else if (buffer instanceof ByteBuffer){
                glType = GLES20.GL_UNSIGNED_BYTE;
            }

        }

        buffer.position(0);
        GLES20.glVertexAttribPointer(handler, componentsPerVertex, glType, false, 0, buffer);
        GLUtil.checkGlError("glVertexAttribPointer");

        return handler;
    }

    private void setUniformInt(int value, String variableName) {
        int handle = GLES20.glGetUniformLocation(mProgram, variableName);
        GLUtil.checkGlError("glGetUniformLocation");
        GLES20.glUniform1i(handle, value);
        GLUtil.checkGlError("glUniform1f");
    }

    private void setUniform1(float value, String variableName) {
        int handle = GLES20.glGetUniformLocation(mProgram, variableName);
        GLUtil.checkGlError("glGetUniformLocation");
        GLES20.glUniform1f(handle, value);
        GLUtil.checkGlError("glUniform1f");
    }

    private void setUniform2(float[] uniform2f, String variableName) {
        int handle = GLES20.glGetUniformLocation(mProgram, variableName);
        GLUtil.checkGlError("glGetUniformLocation");

        GLES20.glUniform2fv(handle, 1, uniform2f, 0);
        GLUtil.checkGlError("glUniform2fv");
    }

    private void setUniform3(float[] uniform3f, String variableName) {
        int handle = GLES20.glGetUniformLocation(mProgram, variableName);
        GLUtil.checkGlError("glGetUniformLocation");

        GLES20.glUniform3fv(handle, 1, uniform3f, 0);
        GLUtil.checkGlError("glUniform3fv");
    }

    private void setUniform4(float[] uniform4f, String variableName) {
        int handle = GLES20.glGetUniformLocation(mProgram, variableName);
        GLUtil.checkGlError("glGetUniformLocation");

        GLES20.glUniform4fv(handle, 1, uniform4f, 0);
        GLUtil.checkGlError("glUniform4fv");
    }

    private void setUniformMatrix4(float[] matrix, String variableName) {
        int handle = GLES20.glGetUniformLocation(mProgram, variableName);
        GLUtil.checkGlError("glGetUniformLocation");

        // Pass in the light position in eye space.
        GLES20.glUniformMatrix4fv(handle, 1, false, matrix, 0);
        GLUtil.checkGlError("glUniformMatrix4fv");
    }

    private void disableVBO(int handle) {
        if (handle != -1) {
            GLES20.glDisableVertexAttribArray(handle);
            GLUtil.checkGlError("glDisableVertexAttribArray");
        }
    }

    private boolean supportsMMatrix() {
        return features.contains("u_MMatrix");
    }

    private boolean supportsTextureCube() {
        return features.contains("u_TextureCube");
    }

    private boolean supportsColors() {
        return features.contains("a_Color");
    }

    private boolean supportsNormals() {
        return features.contains("a_Normal");
    }

    private boolean supportsTangent() {
        return features.contains("a_Tangent");
    }

    private boolean supportsLighting() {
        return features.contains("u_LightPos");
    }

    private boolean supportsTextures() {
        return features.contains("a_TexCoordinate");
    }

    private boolean supportsTransmissionTexture() {
        return features.contains("u_TransmissionTexture");
    }

    private boolean supportsTextureTransformed() {
        return features.contains("u_TextureTransformed");
    }

    private boolean supportsBlending() {
        return features.contains("u_AlphaCutoff") && features.contains("u_AlphaMode");
    }

    private void setFeatureFlag(String variableName, boolean enabled) {
        int handle = GLES20.glGetUniformLocation(mProgram, variableName);
        GLUtil.checkGlError("glGetUniformLocation");

        GLES20.glUniform1i(handle, enabled ? 1 : 0);
        GLUtil.checkGlError("glUniform1i");
    }

    private void setTexture(Texture texture, String variableName, int textureIndex) {

        // check
        if (texture == null || !texture.hasId()) return;

        int mTextureUniformHandle = GLES20.glGetUniformLocation(mProgram, variableName);
        GLUtil.checkGlError("glGetUniformLocation");

        // Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + textureIndex);
        GLUtil.checkGlError("glActiveTexture");

        // Bind to the texture in OpenGL
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture.getId());
        GLUtil.checkGlError("glBindTexture");

        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        GLES20.glUniform1i(mTextureUniformHandle, textureIndex);
        GLUtil.checkGlError("glUniform1i");
    }

    private void setTextureCube(int textureId, int textureIndex) {

        int mTextureUniformHandle = GLES20.glGetUniformLocation(mProgram, "u_TextureCube");
        GLUtil.checkGlError("glGetUniformLocation");

        // Set the active texture unit to texture unit 0.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + textureIndex);
        GLUtil.checkGlError("glActiveTexture");

        // Bind to the texture in OpenGL
        GLES20.glBindTexture(GLES20.GL_TEXTURE_CUBE_MAP, textureId);
        GLUtil.checkGlError("glBindTexture");

        // Tell the texture uniform sampler to use this texture in the shader by binding to texture unit 0.
        GLES20.glUniform1i(mTextureUniformHandle, textureIndex);
        GLUtil.checkGlError("glUniform1i");

    }

    private boolean supportsJoints() {
        return features.contains("in_jointIndices") && features.contains("in_weights");
    }

    private void setJointTransforms(AnimatedModel animatedModel) {
        float[][] jointTransformsArray = animatedModel.getJointMatrices();

        // TODO: optimize this (memory allocation)
        for (int i = 0; i < jointTransformsArray.length; i++) {
            float[] jointTransform = jointTransformsArray[i];
            String jointTransformHandleName = cache1.get(i);
            if (jointTransformHandleName == null) {
                jointTransformHandleName = "jointTransforms[" + i + "]";
                cache1.put(i, jointTransformHandleName);
            }
            setUniformMatrix4(jointTransform, jointTransformHandleName);
        }
    }

    private void drawShape(Object3DData obj, Element element, int drawMode, int drawSize) {


        int drawBufferType = -1;
        Buffer drawOrderBuffer;
        final FloatBuffer vertexBuffer;
        if (obj.isDrawUsingArrays()) {
            drawOrderBuffer = null;
            vertexBuffer = obj.getVertexBuffer();
        } else {
            vertexBuffer = obj.getVertexBuffer();
            drawOrderBuffer = obj.getDrawOrder();

            if (!drawUsingInt && drawOrderBuffer instanceof IntBuffer) {
                ShortBuffer indexShortBuffer = null;
                drawOrderBuffer.position(0);
                indexShortBuffer = IOUtils.createShortBuffer(drawOrderBuffer.capacity());
                for (int j = 0; j < indexShortBuffer.capacity(); j++) {
                    indexShortBuffer.put((short) ((IntBuffer) drawOrderBuffer).get(j));
                }
                drawOrderBuffer = indexShortBuffer;
                obj.setDrawOrder(drawOrderBuffer);
            }

            if (drawOrderBuffer instanceof IntBuffer) {
                drawBufferType = GLES20.GL_UNSIGNED_INT;
            } else if (drawOrderBuffer instanceof ShortBuffer) {
                drawBufferType = GLES20.GL_UNSIGNED_SHORT;
            } else if (drawOrderBuffer instanceof ByteBuffer) {
                drawBufferType = GLES20.GL_UNSIGNED_BYTE;
            }
        }
        vertexBuffer.position(0);

        List<int[]> drawModeList = obj.getDrawModeList();
        if (drawModeList != null) {
            if (obj.isDrawUsingArrays()) {
                drawPolygonsUsingArrays(drawMode, drawModeList);
            } else {
                drawPolygonsUsingIndex(drawOrderBuffer, drawBufferType, drawModeList);
            }
        } else {
            if (obj.isDrawUsingArrays()) {
                drawTrianglesUsingArrays(drawMode, drawSize, vertexBuffer.capacity() / COORDS_PER_VERTEX);
            } else {
                drawTrianglesUsingIndex(obj, element, drawMode, drawSize, drawOrderBuffer, drawBufferType);
            }
        }
    }

    private void drawTrianglesUsingArrays(int drawMode, int drawSize, int drawCount) {
        if (drawSize <= 0) {
            // if we want to animate, initialize counter=0 at variable declaration
            if (this.shift >= 0) {
                double rotation = ((SystemClock.uptimeMillis() % 10000) / 10000f) * (Math.PI * 2);

                if (this.shift == 0d) {
                    this.shift = rotation;
                }
                drawCount = (int) ((Math.sin(rotation - this.shift + Math.PI / 2 * 3) + 1) / 2f * drawCount);
            }
            GLES20.glDrawArrays(drawMode, 0, drawCount);
            GLUtil.checkGlError("glDrawArrays");
        } else {
            //Log.d(obj.getId(),"Drawing single triangles using arrays...");
            for (int i = 0; i < drawCount; i += drawSize) {
                GLES20.glDrawArrays(drawMode, i, drawSize);
                GLUtil.checkGlError("glDrawArrays");
            }
        }
    }

    private void drawTrianglesUsingIndex(Object3DData obj, Element el, int drawMode, int drawSize, Buffer drawOrderBuffer, int drawBufferType) {


        if (drawSize <= 0) {

            if (el != null) {
                drawObjectElement(obj, el, drawMode, drawBufferType);
            } else {

                // String mode = drawMode == GLES20.GL_POINTS ? "Points" : drawMode == GLES20.GL_LINES? "Lines": "Triangles?";
                // Log.v(obj.getId(),"Drawing all elements with mode '"+drawMode+"'...");


            /*for (int i = 0; i<obj.getOldElements().size(); i++) {
                drawOrderBuffer = obj.getOldElements().get(i);
                drawOrderBuffer.position(0);
                GLES20.glDrawElements(drawMode, drawOrderBuffer.capacity(), drawBufferType,
                        drawOrderBuffer);
                boolean error = GLUtil.checkGlError("glDrawElements");
                if (drawUsingInt && error) {
                    drawUsingInt = false;
                }
            }*/

                int size = obj.getElements().size();
            /*if (id != flags.get(obj.getElements())) {
                Log.i("GLES20Renderer", "Rendering elements... obj: " + obj.getId()
                        + ", total:" + size);
                flags.put(obj.getElements(), this.id);
            }*/

                // draw rest
                for (int i = 0; i < size; i++) {

                    // get next element
                    Element element = obj.getElements().get(i);

                    if (element.getMaterial() == null) {
                        drawObjectElement(obj, element, drawMode, drawBufferType);
                    }
                }

                // draw opaque elements
                for (int i = 0; i < size; i++) {

                    // get next element
                    Element element = obj.getElements().get(i);
                    if (element.getMaterial() != null && element.getMaterial().getAlphaMode() == Material.AlphaMode.OPAQUE) {
                        drawObjectElement(obj, element, drawMode, drawBufferType);
                    }
                }

                // draw opaque elements
                for (int i = 0; i < size; i++) {

                    // get next element
                    Element element = obj.getElements().get(i);
                    if (element.getMaterial() != null && element.getMaterial().getAlphaMode() == Material.AlphaMode.MASK) {
                        drawObjectElement(obj, element, drawMode, drawBufferType);
                    }
                }

                // draw translucent elements
                for (int i = 0; i < size; i++) {

                    // get next element
                    Element element = obj.getElements().get(i);

                    if (element.getMaterial() != null && element.getMaterial().getAlphaMode() == Material.AlphaMode.BLEND) {
                        drawObjectElement(obj, element, drawMode, drawBufferType);
                    }
                }
            }

        } else {
            //Log.d(obj.getId(),"Drawing single elements of size '"+drawSize+"'...");
            for (int i = 0; i < drawOrderBuffer.capacity(); i += drawSize) {
                drawOrderBuffer.position(i);
                GLES20.glDrawElements(drawMode, drawSize, drawBufferType, drawOrderBuffer);
                boolean error = GLUtil.checkGlError("glDrawElements");
                if (drawUsingInt && error) {
                    Log.e("ShaderImpl", "Exception drawing elements. Switching to ShortBuffer");
                    drawUsingInt = false;
                }
            }
        }
    }

    private void drawObjectElement(Object3DData obj, Element element, int drawMode, int drawBufferType) {

        Buffer drawOrderBuffer = element.getIndexBuffer();

        if (!drawUsingInt && drawOrderBuffer instanceof IntBuffer) {
            ShortBuffer indexShortBuffer = null;
            drawOrderBuffer.position(0);
            indexShortBuffer = IOUtils.createShortBuffer(drawOrderBuffer.capacity());
            for (int j = 0; j < indexShortBuffer.capacity(); j++) {
                indexShortBuffer.put((short) ((IntBuffer) drawOrderBuffer).get(j));
            }
            drawOrderBuffer = indexShortBuffer;
            element.setIndexBuffer(drawOrderBuffer);
        }

        if (drawOrderBuffer instanceof IntBuffer) {
            drawBufferType = GLES20.GL_UNSIGNED_INT;
        } else if (drawOrderBuffer instanceof ShortBuffer) {
            drawBufferType = GLES20.GL_UNSIGNED_SHORT;
        } else if (drawOrderBuffer instanceof ByteBuffer) {
            drawBufferType = GLES20.GL_UNSIGNED_BYTE;
        }

        // log event
        /*if (id != flags.get(element)) {
            Log.v("GLES20Renderer", "Rendering element " + i + "....  " + element);
            flags.put(element, id);
        }*/

        if (element.getMaterial() != null && element.getMaterial().getColor() != null) {
            setUniform4(element.getMaterial().getColor(), "vColor");
        } else {
            //setUniform4(DEFAULT_COLOR, "vColor");
        }

        /*// default is no textured
        if (supportsColors()) {
            setFeatureFlag("u_Coloured", obj.getColorsBuffer() != null);
        }*/

        // default is no textured
        if (supportsTextures()) {
            setFeatureFlag("u_Textured", obj.getTextureBuffer() != null
                    && element.getMaterial() != null && element.getMaterial().getColorTexture() != null
                    && element.getMaterial().getColorTexture().hasId()
                    && texturesEnabled);
        }

        // texture transform (Khronos)
        if (supportsTextureTransformed()) {
            setFeatureFlag("u_TextureTransformed", false);
            try {
                if (element.getMaterial().getColorTexture() != null &&
                        element.getMaterial().getColorTexture().getExtensions() != null &&
                        element.getMaterial().getColorTexture().getExtensions().containsKey("KHR_texture_transform")) {
                    Map<String, ?> extensions = (Map<String, ?>) element.getMaterial().getColorTexture().getExtensions().get("KHR_texture_transform");
                    List<Double> offset = (List<Double>) extensions.get("offset");
                    if (offset != null) {
                        setUniform2(new float[]{offset.get(0).floatValue(), offset.get(1).floatValue()}, "u_TextureOffset");
                    } else {
                        setUniform2(new float[]{0f, 0f}, "u_TextureOffset");
                    }
                    List<Double> scale = (List<Double>) extensions.get("scale");
                    if (scale != null) {
                        setUniform2(new float[]{scale.get(0).floatValue(), scale.get(1).floatValue()}, "u_TextureScale");
                    } else {
                        setUniform2(new float[]{1f, 1f}, "u_TextureScale");
                    }
                    Double rotation = (Double) extensions.get("rotation");
                    if (rotation != null) {
                        setUniform1(rotation.floatValue(), "u_TextureRotation");
                    } else {
                        setUniform1(0, "u_TextureRotation");
                    }
                    setFeatureFlag("u_TextureTransformed", true);
                }
            } catch (Exception e) {
                // ignore
            }
        }

        textureCounter = 4;
        if (element.getMaterial() != null) {

            // set alpha cutoff
            if (supportsBlending()) {
                setUniform1(element.getMaterial().getAlphaCutoff(), "u_AlphaCutoff");
                setUniformInt(element.getMaterial().getAlphaMode().ordinal(), "u_AlphaMode");
            }

            if (element.getMaterial().getColorTexture() != null) {
                loadTexture(element.getMaterial().getColorTexture());
                setTexture(element.getMaterial().getColorTexture(), "u_Texture", textureCounter++);
                setFeatureFlag("u_Textured", true);
            }

            // transmission map
            if (supportsTransmissionTexture()) {
                boolean toggle = element.getMaterial().getTransmissionTexture() != null;
                loadTexture(element.getMaterial().getTransmissionTexture());
                setTexture(element.getMaterial().getTransmissionTexture(), "u_TransmissionTexture", textureCounter++);
                setUniform1(element.getMaterial().getThicknessFactor(), "u_TransmissionFactor");
                setFeatureFlag("u_TransmissionTextured", toggle);
            }

            if (element.getMaterial().getNormalTexture() != null) {
                loadTexture(element.getMaterial().getNormalTexture());
                setTexture(element.getMaterial().getNormalTexture(), "u_NormalTexture", textureCounter++);
                setFeatureFlag("u_NormalTextured", true);
            }

            boolean enableEmissive = element.getMaterial().getEmissiveTexture() != null && element.getMaterial().getEmissiveFactor() != null;
            setFeatureFlag("u_EmissiveTextured", enableEmissive);
            if (enableEmissive) {
                loadTexture(element.getMaterial().getEmissiveTexture());
                setTexture(element.getMaterial().getEmissiveTexture(), "u_EmissiveTexture", textureCounter++);
                setUniform3(element.getMaterial().getEmissiveFactor(), "u_EmissiveFactor");
            }
        }

        // draw element
        drawOrderBuffer.position(0);
        GLES20.glDrawElements(drawMode, drawOrderBuffer.capacity(), drawBufferType,
                drawOrderBuffer);
        boolean error = GLUtil.checkGlError("glDrawElements");
        if (drawUsingInt && error) {
            Log.e("ShaderImpl", "Exception drawing elements. Switching to ShortBuffer");
            drawUsingInt = false;
        }

        // log event
        /*if (id != flags.get(element)) {
            Log.i("GLES20Renderer", "Rendering element " + i + " finished");
            flags.put(element, this.id);
        }*/
    }

    private void drawPolygonsUsingIndex(Buffer drawOrderBuffer, int drawBufferType, List<int[]> polygonsList) {
        // Log.d(obj.getId(),"Drawing single polygons using elements...");
        for (int i = 0; i < polygonsList.size(); i++) {
            int[] drawPart = polygonsList.get(i);
            int drawModePolygon = drawPart[0];
            int vertexPos = drawPart[1];
            int drawSizePolygon = drawPart[2];
            drawOrderBuffer.position(vertexPos);
            GLES20.glDrawElements(drawModePolygon, drawSizePolygon, drawBufferType, drawOrderBuffer);
            boolean error = GLUtil.checkGlError("glDrawElements");
            if (drawUsingInt && error) {
                Log.e("ShaderImpl", "Exception drawing elements. Switching to ShortBuffer");
                drawUsingInt = false;
            }
        }
    }

    private void drawPolygonsUsingArrays(int drawMode, List<int[]> polygonsList) {
        // Log.v(obj.getId(), "Drawing single polygons using arrays...");
        for (int j = 0; j < polygonsList.size(); j++) {
            int[] polygon = polygonsList.get(j);
            int drawModePolygon = polygon[0];
            int vertexPos = polygon[1];
            int drawSizePolygon = polygon[2];
            if (drawMode == GLES20.GL_LINE_LOOP && polygon[2] > 3) {
                // is this wireframe?
                // Log.v("GLES20Renderer","Drawing wireframe for '" + obj.getId() + "' (" + drawSizePolygon + ")...");
                for (int i = 0; i < polygon[2] - 2; i++) {
                    // Log.v("GLES20Renderer","Drawing wireframe triangle '" + i + "' for '" + obj.getId() + "'...");
                    GLES20.glDrawArrays(drawMode, polygon[1] + i, 3);
                    GLUtil.checkGlError("glDrawArrays");
                }
            } else {
                GLES20.glDrawArrays(drawMode, polygon[1], polygon[2]);
                GLUtil.checkGlError("glDrawArrays");
            }
        }
    }

    @Override
    public String toString() {
        return "GLES20Renderer{" +
                "id='" + id + '\'' +
                ", features=" + features +
                '}';
    }
}
