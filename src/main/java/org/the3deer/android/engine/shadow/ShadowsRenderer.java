package org.the3deer.android.engine.shadow;

import android.opengl.GLES20;

import org.the3deer.android.engine.model.Camera;
import org.the3deer.android.engine.model.Constants;
import org.the3deer.android.engine.model.Material;
import org.the3deer.android.engine.model.Object3D;
import org.the3deer.android.engine.model.Scene;
import org.the3deer.android.engine.model.Texture;
import org.the3deer.android.engine.objects.Plane;
import org.the3deer.android.engine.objects.Plane2;
import org.the3deer.android.engine.renderer.Renderer;
import org.the3deer.android.engine.shader.Program;
import org.the3deer.android.engine.shader.Shader;
import org.the3deer.android.engine.shader.ShaderManager;
import org.the3deer.android.engine.util.GLUtil;
import org.the3deer.android.util.Matrix;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class ShadowsRenderer {

    private static final Logger logger = Logger.getLogger(ShadowsRenderer.class.getSimpleName());

    /**
     * The vertex and fragment shader to render depth map
     */
    private Shader mDepthMapProgram;
    private Shader mActiveRenderer;

    private int mActiveProgram;

    private final float[] mMVPMatrix = new float[16];
    private final float[] mMVMatrix = new float[16];
    private final float[] mNormalMatrix = new float[16];
    private final float[] mProjectionMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];
    private final float[] mModelMatrix = new float[16];

    private final float[] mCubeRotation = new float[16];

    /**
     * MVP matrix used at rendering shadow map for stationary objects
     */
    private final float[] mLightMvpMatrix_staticShapes = new float[16];

    /**
     * MVP matrix used at rendering shadow map for the big cube in the center
     */
    private final float[] mLightMvpMatrix_dynamicShapes = new float[16];

    /**
     * View matrix of light source
     */
    private final float[] mLightViewMatrix = new float[16];

    /**
     * Position of light source in eye space
     */
    private final float[] mLightPosInEyeSpace = new float[16];

    /**
     * Light source position in model space
     */
    private final float[] mLightPosModel = new float []
            {-5.0f, 9.0f, 0.0f, 1.0f};

    private float[] mActualLightPosition = new float[4];

    /**
     * Current X,Y axis rotation of center cube
     */
    private float mRotationX;
    private float mRotationY;

    /**
     * Current shadow map sizes
     */
    private int mShadowMapWidth;
    private int mShadowMapHeight;

    private boolean mHasDepthTextureExtension = false;

    int[] fboId;
    int[] depthTextureId;
    int[] renderTextureId;

    // Uniform locations for scene render program
    private int scene_normalMatrixUniform;
    private int scene_schadowLightViewMatrixUniform;
    private int scene_schadowLightProjectionMatrixUniform;
    private int scene_textureUniform;
    private int scene_mapStepXUniform;
    private int scene_mapStepYUniform;

    /**
     * Shadow map size:
     * 	- displayWidth * SHADOW_MAP_RATIO
     * 	- displayHeight * SHADOW_MAP_RATIO
     */
    private float mShadowMapRatio = 1;

    // point of view light
    private Camera camera = new Camera("default", Constants.DEFAULT_CAMERA_POSITION);

    final Object3D plane = Plane2.build();
    final Object3D debugPlane = Plane.buildTextured();
    final Texture debugTexture = new Texture();

    {
        plane.setColor(Constants.COLOR_GRAY.clone());
        plane.setLocation(new float[]{0f, -Constants.DEFAULT_MODEL_SIZE/2, 0f});
        plane.setPinned(true);

        debugPlane.setId("debugShadowMap");
        debugPlane.setScale(5f, 1f, 5f);
        debugPlane.setLocation(0f, 5f, 0f);
        debugPlane.setRotation(new float[]{90f, 0f, 0f});
        debugPlane.setPinned(true);
        //debugPlane.getMaterial().setColorTexture(debugTexture);
        debugPlane.setMaterial(new Material().setColorTexture(debugTexture));
    }

    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
		// Test OES_depth_texture extension
		String extensions = GLES20.glGetString(GLES20.GL_EXTENSIONS);
        if (extensions != null && extensions.contains("OES_depth_texture")) {
            mHasDepthTextureExtension = true;
        }

        //Set the background frame color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        //Enable depth testing
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);

		GLES20.glEnable(GLES20.GL_CULL_FACE);
    }

    /**
     * Sets up the framebuffer and renderbuffer to render to texture
     */
    public void generateShadowFBO(Renderer.Config config){

        if (fboId != null) {
            return;
        }

        mShadowMapWidth = Math.round(config.viewPortWidth * this.mShadowMapRatio);
        mShadowMapHeight = Math.round(config.viewPortHeigth * this.mShadowMapRatio);

        fboId = new int[1];
        depthTextureId = new int[1];
        renderTextureId = new int[1];

        // create a framebuffer object
        GLES20.glGenFramebuffers(1, fboId, 0);
        GLUtil.checkGlError("glGenFramebuffers");

        // create render buffer and bind 16-bit depth buffer
        GLES20.glGenRenderbuffers(1, depthTextureId, 0);
        GLUtil.checkGlError("glGenRenderbuffers");

        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, depthTextureId[0]);
        GLUtil.checkGlError("glBindRenderbuffer");

        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, mShadowMapWidth, mShadowMapHeight);
        GLUtil.checkGlError("glRenderbufferStorage");

        // Try to use a texture depth component
        GLES20.glGenTextures(1, renderTextureId, 0);
        GLUtil.checkGlError("glGenTextures");

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, renderTextureId[0]);
        GLUtil.checkGlError("glBindTexture");

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

        // Remove artifact on the edges of the shadowmap
        GLES20.glTexParameteri( GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE );
        GLES20.glTexParameteri( GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE );

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId[0]);
        GLUtil.checkGlError("glBindFramebuffer");

        if (!mHasDepthTextureExtension) {
            GLES20.glTexImage2D( GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mShadowMapWidth, mShadowMapHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

            // specify texture as color attachment
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, renderTextureId[0], 0);

            // attach the texture to FBO depth attachment point
            GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, depthTextureId[0]);
        }
        else {
            // Use a depth texture
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_DEPTH_COMPONENT, mShadowMapWidth, mShadowMapHeight, 0, GLES20.GL_DEPTH_COMPONENT, GLES20.GL_UNSIGNED_INT, null);

            // Attach the depth texture to FBO depth attachment point
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_TEXTURE_2D, renderTextureId[0], 0);
        }

        // check FBO status
        int FBOstatus = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
        GLUtil.checkGlError("glCheckFramebufferStatus");

        if(FBOstatus != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            logger.log(Level.SEVERE, "GL_FRAMEBUFFER_COMPLETE failed, CANNOT use FBO. code: "+FBOstatus);
            throw new IllegalStateException("GL_FRAMEBUFFER_COMPLETE failed, CANNOT use FBO. code: "+FBOstatus);
        }
    }

    public void onDrawFrame(ShaderManager shaderFactory, Renderer.Config config, float[] mProjectionMatrix, float[] mViewMatrix, float[] mLightProjectionMatrix, float[] mLightViewMatrix, float[] mActualLightPosition, Scene scene) {

        GLES20.glViewport(0, 0, config.viewPortWidth, config.viewPortHeigth);

        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);

        // sync
        System.arraycopy(mLightViewMatrix, 0, this.mLightViewMatrix, 0, 16);

     	renderScene(shaderFactory, config, scene, mProjectionMatrix, mViewMatrix, mLightProjectionMatrix, mActualLightPosition);

        // Print openGL errors to console
        int debugInfo = GLES20.glGetError();

        if (debugInfo != GLES20.GL_NO_ERROR) {
            String msg = "OpenGL error: " + debugInfo;
            logger.warning(msg);
        }

    }

    public void onPrepareFrame(ShaderManager shaderFactory, Renderer.Config config, float[] mProjectionMatrix, float[] mViewMatrix, float[] mLightProjectionMatrix, float[] mLightViewMatrix, float[] mActualLightPosition, Scene scene) {

        GLES20.glViewport(0, 0, config.viewPortWidth, config.viewPortHeigth);

        generateShadowFBO(config);

        mDepthMapProgram = shaderFactory.getShader(Program.SHADOW_MAP);
        mDepthMapProgram.setAutoUseProgram(false);

        Matrix.setIdentityM(mModelMatrix, 0);

        // sync
        System.arraycopy(mLightViewMatrix, 0, this.mLightViewMatrix, 0, 16);

        // Cull front faces for shadow generation to avoid self shadowing
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_FRONT);

        renderShadowMap(mLightProjectionMatrix, scene);
    }


    private void renderShadowMap(float[] mLightProjectionMatrix, Scene scene) {

        if (fboId == null) {
            return; // this is an activity restart
        }

        // bind the generated framebuffer
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId[0]);

        GLES20.glViewport(0, 0, mShadowMapWidth, mShadowMapHeight);

        // Clear color and buffers
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        GLES20.glClear( GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);

        // Disable blending to ensure depth values are not mixed
        GLES20.glDisable(GLES20.GL_BLEND);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // Render all stationary shapes on scene
        List<Object3D> objects = scene.getObjects();
        for (int i = 0; i < objects.size(); i++) {
            Object3D objData = objects.get(i);
            if (!objData.isVisible()) {
                continue;
            }

            mDepthMapProgram.useProgram();
            this.mDepthMapProgram.draw(objData, mLightProjectionMatrix, mLightViewMatrix,
                    null, null, null, objData.getDrawMode(), objData.getDrawSize());
        }
    }

    private void renderScene(ShaderManager shaderFactory, Renderer.Config config, Scene scene, float[] mProjectionMatrix, float[] mViewMatrix, float[] mLightProjectionMatrix, float[] mActualLightPosition) {

        // bind default framebuffer
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);

        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT);

        GLES20.glViewport(0, 0, config.viewPortWidth, config.viewPortHeigth);

        // Re-enable blending for the main scene pass
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // 1. Update ground plane to be proportional to scene
        if (scene.getDimensions() != null) {
            final org.the3deer.android.engine.model.Dimensions dim = scene.getDimensions();
            float size = Math.max(dim.getWidth(), dim.getDepth()) * 2.5f; // 5x padding
            if (size <= 0) size = Constants.DEFAULT_MODEL_SIZE * 5.0f;
            
            plane.setScale(size, 1.0f, size);
            // Place it at the bottom of the bounding box
            plane.setLocation(dim.getCenter()[0], dim.getMin()[1], dim.getCenter()[2]);
        }

        float[] tempResultMatrix = new float[16];

        // Cull front faces for shadow generation to avoid self shadowing
        GLES20.glEnable(GLES20.GL_CULL_FACE);

        // 2. Draw plane where the shadow is projected
        drawObject(shaderFactory, mProjectionMatrix, mViewMatrix, mLightProjectionMatrix, mActualLightPosition, plane, tempResultMatrix);

        GLES20.glDisable(GLES20.GL_CULL_FACE);

        // draw debug plane
        if (renderTextureId != null && renderTextureId[0] > 0) {
            debugTexture.setId(renderTextureId[0]);
            //drawDebugPlane(shaderFactory, mProjectionMatrix, mViewMatrix, debugPlane, tempResultMatrix);
        }

        for (int i=0; i<scene.getObjects().size(); i++) {
            final Object3D data = scene.getObjects().get(i);
            drawObject(shaderFactory, mProjectionMatrix, mViewMatrix, mLightProjectionMatrix, mActualLightPosition, data, tempResultMatrix);
        }
    }

    private void drawObject(ShaderManager shaderFactory, float[] mProjectionMatrix, float[] mViewMatrix, float[] mLightProjectionMatrix, float[] mActualLightPosition, Object3D data, float[] tempResultMatrix) {

        final float[] mModelMatrix = data.getModelMatrix();

        //calculate MV matrix
        Matrix.multiplyMM(tempResultMatrix, 0, mViewMatrix, 0, mModelMatrix, 0);
        System.arraycopy(tempResultMatrix, 0, mMVMatrix, 0, 16);

        mActiveRenderer = shaderFactory.getShader(Program.SHADOW);
        mActiveRenderer.setAutoUseProgram(false);
        mActiveRenderer.useProgram();

        mActiveProgram = mActiveRenderer.getProgram();

        scene_normalMatrixUniform = GLES20.glGetUniformLocation(mActiveProgram, "u_NormalMatrix");
        scene_schadowLightViewMatrixUniform = GLES20.glGetUniformLocation(mActiveProgram, "u_LVMatrix");
        scene_schadowLightProjectionMatrixUniform = GLES20.glGetUniformLocation(mActiveProgram, "u_LPMatrix");
        scene_textureUniform = GLES20.glGetUniformLocation(mActiveProgram, "uShadowTexture");
        scene_mapStepXUniform = GLES20.glGetUniformLocation(mActiveProgram, "u_ShadowTexelSizeX");
        scene_mapStepYUniform = GLES20.glGetUniformLocation(mActiveProgram, "u_ShadowTexelSizeY");

        //calculate Normal Matrix as uniform (invert transpose MV)
        Matrix.invertM(tempResultMatrix, 0, mMVMatrix, 0);
        Matrix.transposeM(mNormalMatrix, 0, tempResultMatrix, 0);

        //pass in Normal Matrix as uniform
        GLES20.glUniformMatrix4fv(scene_normalMatrixUniform, 1, false, mNormalMatrix, 0);

        //MVP matrix that was used during depth map render
        GLES20.glUniformMatrix4fv(scene_schadowLightViewMatrixUniform, 1, false, mLightViewMatrix, 0);
        GLES20.glUniformMatrix4fv(scene_schadowLightProjectionMatrixUniform, 1, false, mLightProjectionMatrix, 0);

        // Pass dynamic texel size for PCF
        GLES20.glUniform1f(scene_mapStepXUniform, 1.0f / mShadowMapWidth);
        GLES20.glUniform1f(scene_mapStepYUniform, 1.0f / mShadowMapHeight);

        //pass in texture where depth map is stored
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + 10);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, renderTextureId[0]);
        GLES20.glUniform1i(scene_textureUniform, 10);

        mActiveRenderer.draw(data, mProjectionMatrix, mViewMatrix, mActualLightPosition, null, camera.getPos(), data.getDrawMode(), data.getDrawSize());
    }

    private void drawDebugPlane(ShaderManager shaderFactory, float[] mProjectionMatrix, float[] mViewMatrix, Object3D data, float[] tempResultMatrix) {
        Shader debugShader = shaderFactory.getShader(Program.DEBUG_SHADOW);
        debugShader.setLightingEnabled(false);
        debugShader.draw(data, mProjectionMatrix, mViewMatrix, null, null, camera.getPos(), data.getDrawMode(), data.getDrawSize());
        debugShader.setLightingEnabled(true);
    }

    public int getPositionHandler() {
        int handler = GLES20.glGetAttribLocation(mActiveRenderer.getProgram(), "a_Position");
        return handler;
    }

    public int getNormalHandler() {
        int handler = GLES20.glGetAttribLocation(mActiveRenderer.getProgram(), "a_Normal");
        return handler;
    }

    public int getColorHandler() {
        int handler = GLES20.glGetAttribLocation(mActiveRenderer.getProgram(), "a_Color");
        return handler;
    }

    public int getProgram() {
        return mActiveRenderer.getProgram();
    }

    public void reset() {
        fboId = null;
        depthTextureId = null;
        renderTextureId = null;
    }
}
