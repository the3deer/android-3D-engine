package org.the3deer.android.engine.renderer

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import org.the3deer.android.engine.model.Camera
import org.the3deer.android.engine.model.Model
import org.the3deer.android.engine.model.Node
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class SceneRenderer : GLSurfaceView.Renderer {

    private var program: Int = 0
    private var color = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f)
    private var activeModel: Model? = null
    
    // Use a default camera that matches the legacy engine's default
    private val defaultCamera = Camera("Default").apply {
        set(0f, 0f, 5f, 0f, 0f, 0f, 0f, 1f, 0f)
    }

    private val vertexShaderCode =
        "uniform mat4 uMVPMatrix;" +
        "attribute vec4 vPosition;" +
        "void main() {" +
        "  gl_Position = uMVPMatrix * vPosition;" +
        "}"

    private val fragmentShaderCode =
        "precision mediump float;" +
        "uniform vec4 vColor;" +
        "void main() {" +
        "  gl_FragColor = vColor;" +
        "}"

    private val mvpMatrix = FloatArray(16)

    fun updateModel(model: Model) {
        activeModel = model
    }

    fun updateColor(newColor: FloatArray) {
        color = newColor
    }

    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        
        // IMPORTANT: Enable depth testing so overlapping objects render correctly
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)

        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }
    }

    override fun onDrawFrame(unused: GL10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        
        val model = activeModel ?: return
        val scene = model.activeScene ?: return
        val camera = scene.activeCamera ?: defaultCamera

        scene.update()

        GLES20.glUseProgram(program)
        
        val mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        val colorHandle = GLES20.glGetUniformLocation(program, "vColor")
        val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")

        GLES20.glUniform4fv(colorHandle, 1, color, 0)

        for (rootNode in scene.rootNodes){
            renderNode(
                rootNode,
                camera.viewMatrix,
                camera.projectionMatrix,
                mvpMatrixHandle,
                positionHandle
            )
        }
    }

    private fun renderNode(node: Node, viewMatrix: FloatArray, projectionMatrix: FloatArray, mvpHandle: Int, posHandle: Int) {
        val mesh = node.mesh
        if (mesh != null) {
            val vPMatrix = FloatArray(16)
            Matrix.multiplyMM(vPMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
            Matrix.multiplyMM(mvpMatrix, 0, vPMatrix, 0, node.worldMatrix, 0)

            GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvpMatrix, 0)

            GLES20.glEnableVertexAttribArray(posHandle)
            mesh.vertexBuffer.rewind();
            GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT, false, 3 * 4, mesh.vertexBuffer)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, mesh.vertexCount)
            GLES20.glDisableVertexAttribArray(posHandle)
        }

        for (child in node.children) {
            renderNode(child, viewMatrix, projectionMatrix, mvpHandle, posHandle)
        }
    }

    override fun onSurfaceChanged(unused: GL10, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        
        // Update projection for both cameras
        defaultCamera.setProjection(width, height)
        activeModel?.activeScene?.cameras?.forEach { 
            it.setProjection(width, height) 
        }
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }
}