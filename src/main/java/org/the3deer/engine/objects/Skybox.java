package org.the3deer.engine.objects;

import android.net.Uri;
import android.opengl.GLES20;

import org.the3deer.engine.android.util.ContentUtils;
import org.the3deer.engine.model.CubeMap;
import org.the3deer.engine.model.Material;
import org.the3deer.engine.model.Object3D;
import org.the3deer.engine.model.Texture;
import org.the3deer.util.io.IOUtils;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Skyboxes downloaded from:
 * <p>
 * https://learnopengl.com/Advanced-OpenGL/Cubemaps
 * https://github.com/mobialia/jmini3d
 */
public class Skybox {

    private static final Logger logger = Logger.getLogger(Skybox.class.getSimpleName());

    private final static float VERTEX_DATA[] = {
            // positions
            -1.0f, 1.0f, -1.0f,
            -1.0f, -1.0f, -1.0f,
            1.0f, -1.0f, -1.0f,
            1.0f, -1.0f, -1.0f,
            1.0f, 1.0f, -1.0f,
            -1.0f, 1.0f, -1.0f,

            -1.0f, -1.0f, 1.0f,
            -1.0f, -1.0f, -1.0f,
            -1.0f, 1.0f, -1.0f,
            -1.0f, 1.0f, -1.0f,
            -1.0f, 1.0f, 1.0f,
            -1.0f, -1.0f, 1.0f,

            1.0f, -1.0f, -1.0f,
            1.0f, -1.0f, 1.0f,
            1.0f, 1.0f, 1.0f,
            1.0f, 1.0f, 1.0f,
            1.0f, 1.0f, -1.0f,
            1.0f, -1.0f, -1.0f,

            -1.0f, -1.0f, 1.0f,
            -1.0f, 1.0f, 1.0f,
            1.0f, 1.0f, 1.0f,
            1.0f, 1.0f, 1.0f,
            1.0f, -1.0f, 1.0f,
            -1.0f, -1.0f, 1.0f,

            -1.0f, 1.0f, -1.0f,
            1.0f, 1.0f, -1.0f,
            1.0f, 1.0f, 1.0f,
            1.0f, 1.0f, 1.0f,
            -1.0f, 1.0f, 1.0f,
            -1.0f, 1.0f, -1.0f,

            -1.0f, -1.0f, -1.0f,
            -1.0f, -1.0f, 1.0f,
            1.0f, -1.0f, -1.0f,
            1.0f, -1.0f, -1.0f,
            -1.0f, -1.0f, 1.0f,
            1.0f, -1.0f, 1.0f
    };

    public final Uri[] images;

    private CubeMap cubeMap = null;

    public Skybox(Uri[] images) throws IOException {
        if (images == null || images.length != 6)
            throw new IllegalArgumentException("skybox must contain exactly 6 faces");
        this.images = images;
        this.cubeMap = getCubeMap();
    }

    public CubeMap getCubeMap() throws IOException {
        if (cubeMap != null) {
            return cubeMap;
        }

        cubeMap = new CubeMap(
                IOUtils.read(ContentUtils.getInputStream(images[0])),
                IOUtils.read(ContentUtils.getInputStream(images[1])),
                IOUtils.read(ContentUtils.getInputStream(images[2])),
                IOUtils.read(ContentUtils.getInputStream(images[3])),
                IOUtils.read(ContentUtils.getInputStream(images[4])),
                IOUtils.read(ContentUtils.getInputStream(images[5])));

        return cubeMap;
    }

    /**
     * skybox downloaded from https://github.com/mobialia/jmini3d
     *
     * @return
     */
    public static Skybox getSkybox2() {
        try {
            return new Skybox(new Uri[]{
                    Uri.parse("android://org.the3deer.android.viewer/res/drawable/posx.png"),
                    Uri.parse("android://org.the3deer.android.viewer/res/drawable/negx.png"),
                    Uri.parse("android://org.the3deer.android.viewer/res/drawable/posy.png"),
                    Uri.parse("android://org.the3deer.android.viewer/res/drawable/negy.png"),
                    Uri.parse("android://org.the3deer.android.viewer/res/drawable/posz.png"),
                    Uri.parse("android://org.the3deer.android.viewer/res/drawable/negz.png")});
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * skybox downloaded from https://learnopengl.com/Advanced-OpenGL/Cubemaps
     *
     * @return
     */
    public static Skybox getSkybox1() {
        try {
            return new Skybox(new Uri[]{
                    Uri.parse("android://org.the3deer.android.viewer/res/drawable/right.png"),
                    Uri.parse("android://org.the3deer.android.viewer/res/drawable/left.png"),
                    Uri.parse("android://org.the3deer.android.viewer/res/drawable/top.png"),
                    Uri.parse("android://org.the3deer.android.viewer/res/drawable/bottom.png"),
                    Uri.parse("android://org.the3deer.android.viewer/res/drawable/front.png"),
                    Uri.parse("android://org.the3deer.android.viewer/res/drawable/back.png")});
        } catch (IOException e) {
            logger.log(Level.SEVERE,  "Exception: " + e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public static Object3D build(Skybox skybox) throws IOException {

        Object3D ret = new Object3D(IOUtils.createFloatBuffer(VERTEX_DATA.length).put(VERTEX_DATA)).setId("skybox");
        ret.setDrawMode(GLES20.GL_TRIANGLES);
        ret.setMaterial(new Material().setColorTexture(new Texture().setCubeMap(skybox.getCubeMap())));


        logger.info("Skybox : " + ret.getDimensions());

        return ret;
    }
}