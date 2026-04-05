package org.the3deer.android.engine.shader;

/**
 * This class represents a program, that is a couple of vertex and fragment shader (.glsl) resources
 */
public class Program {
    public static final int BASIC = 1;
    public static final int STATIC = 2;
    public static final int ANIMATED = 3;
    public static final int SKYBOX = 10;
    public static final int SUN = 11;
    public static final int SHADOW_MAP = 100;
    public static final int SHADOW = 101;

    final int id;
    final int openGLVersion;
    final String vertexShaderCode;
    final String fragmentShaderCode;

    public Program(int id, int openGLVersion, String vertexShaderCode, String fragmentShaderCode) {
        this.id = id;
        this.openGLVersion = openGLVersion;
        this.vertexShaderCode = vertexShaderCode;
        this.fragmentShaderCode = fragmentShaderCode;
    }

    public int getId() {
        return id;
    }

    public int getOpenGLVersion() {
        return openGLVersion;
    }

    public String getVertexShaderCode() {
        return vertexShaderCode;
    }

    public String getFragmentShaderCode() {
        return fragmentShaderCode;
    }
}
