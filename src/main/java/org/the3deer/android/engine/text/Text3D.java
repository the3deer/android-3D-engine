package org.the3deer.android.engine.text;

import android.opengl.GLES20;

import org.the3deer.android.engine.model.Constants;
import org.the3deer.android.engine.model.Object3D;
import org.the3deer.util.io.IOUtils;
import org.the3deer.util.math.Math3DUtils;

import java.nio.FloatBuffer;
import java.util.logging.Logger;

/**
 * Utility to build 3D text using extruded beams for vector fonts.
 */
public class Text3D {

    private static final Logger logger = Logger.getLogger(Text3D.class.getSimpleName());

    /**
     * Builds a solid 3D object representing the provided text.
     * 
     * @param text the text to render
     * @param thickness the thickness of the beams
     * @param fontFactory the font factory to use
     * @return a new Object3D representing the 3D text
     */
    public static Object3D build(final String text, final float thickness, final FontFactory fontFactory) {
        return build(text, Constants.COLOR_WHITE, thickness, fontFactory);
    }

    /**
     * Builds a solid 3D object representing the provided text with a specific color.
     */
    public static Object3D build(final String text, final float[] color, final float thickness, final FontFactory fontFactory) {
        if (text == null || text.isEmpty()) return new Object3D();

        final String[] lines = text.split("\\r?\\n");
        final int rows = lines.length;
        int maxCols = 0;
        int totalVerticesCount = 0;
        for (final String line : lines) {
            maxCols = Math.max(maxCols, line.length());
            for (int col = 0; col < line.length(); col++) {
                final float[] data = fontFactory.getChar(line.charAt(col));
                if (data != null && data.length >= 6) {
                    // Each segment (pair of points) is 36 vertices
                    totalVerticesCount += ((data.length / 3) - 1) * 36;
                }
            }
        }

        final FloatBuffer vertexBuffer = IOUtils.createFloatBuffer(totalVerticesCount * 3);
        final FloatBuffer normalBuffer = IOUtils.createFloatBuffer(totalVerticesCount * 3);
        final FloatBuffer colorBuffer = IOUtils.createFloatBuffer(totalVerticesCount * 4);

        // Centering logic (identical to 2D Text)
        final float totalWidth = maxCols * fontFactory.getCharWidth();
        final float totalHeight = rows * fontFactory.getCharHeight();
        final float centeringShiftX = totalWidth / 2.0f;
        final float centeringShiftY = totalHeight / 2.0f;

        int currentVertexCount = 0;
        for (int row = 0; row < rows; row++) {
            final String line = lines[row];
            for (int col = 0; col < line.length(); col++) {
                final float offsetX = col * fontFactory.getCharWidth() - centeringShiftX;
                final float offsetY = (rows - 1) * fontFactory.getCharHeight() - row * fontFactory.getCharHeight() - centeringShiftY;

                final char c = line.charAt(col);
                if (c == '\n') break;

                final float[] data = fontFactory.getChar(c);
                if (data == null || data.length < 6) continue;

                currentVertexCount = buildGlyph3D(vertexBuffer, normalBuffer, colorBuffer, currentVertexCount, 
                                                data, offsetX, offsetY, thickness, color);
            }
        }

        final Object3D object = new Object3D(vertexBuffer);
        object.setVertexNormalsArrayBuffer(normalBuffer);
        object.setColorsBuffer(colorBuffer);
        object.setDrawMode(GLES20.GL_TRIANGLES);
        
        object.setId("text3d_" + System.currentTimeMillis());
        object.setName(text);

        return object;
    }

    private static int buildGlyph3D(FloatBuffer vb, FloatBuffer nb, FloatBuffer cb, int vCount, 
                                  float[] data, float offsetX, float offsetY, float thickness, float[] color) {
        
        for (int i = 0; i < data.length - 3; i += 3) {
            float x1 = data[i] + offsetX;
            float y1 = data[i+1] + offsetY;
            float z1 = data[i+2];

            float x2 = data[i+3] + offsetX;
            float y2 = data[i+4] + offsetY;
            float z2 = data[i+5];

            // Detect handshake jump (pen-up): Marked with negative Z in the factory
            if (z1 < 0 || z2 < 0) continue;

            vCount = addBeam(vb, nb, cb, vCount, x1, y1, z1, x2, y2, z2, thickness, color);
        }
        return vCount;
    }

    private static int addBeam(FloatBuffer vb, FloatBuffer nb, FloatBuffer cb, int vCount,
                             float x1, float y1, float z1, float x2, float y2, float z2, 
                             float t, float[] color) {
        
        float[] p1 = {x1, y1, z1};
        float[] p2 = {x2, y2, z2};
        float[] dir = Math3DUtils.substract(p2, p1);
        float len = Math3DUtils.length(dir);
        if (len < 0.0001f) return vCount;
        
        Math3DUtils.normalizeVector(dir);

        // Orthonormal basis for the cross-section
        float[] up = {0, 1, 0};
        if (Math.abs(Math3DUtils.dotProduct(dir, up)) > 0.9f) {
            up = new float[]{1, 0, 0};
        }
        float[] side = Math3DUtils.crossProduct(dir, up);
        Math3DUtils.normalizeVector(side);
        up = Math3DUtils.crossProduct(side, dir);
        Math3DUtils.normalizeVector(up);

        // Scale basis by thickness
        float halfT = t / 2.0f;
        float[] vS = Math3DUtils.multiply(side, halfT);
        float[] vU = Math3DUtils.multiply(up, halfT);

        // 8 Corners of the box
        float[][] corners = new float[8][3];
        corners[0] = Math3DUtils.add(Math3DUtils.add(p1, vS), vU); // P1 +S +U
        corners[1] = Math3DUtils.add(Math3DUtils.substract(p1, vS), vU); // P1 -S +U
        corners[2] = Math3DUtils.substract(Math3DUtils.substract(p1, vS), vU); // P1 -S -U
        corners[3] = Math3DUtils.substract(Math3DUtils.add(p1, vS), vU); // P1 +S -U

        corners[4] = Math3DUtils.add(Math3DUtils.add(p2, vS), vU);
        corners[5] = Math3DUtils.add(Math3DUtils.substract(p2, vS), vU);
        corners[6] = Math3DUtils.substract(Math3DUtils.substract(p2, vS), vU);
        corners[7] = Math3DUtils.substract(Math3DUtils.add(p2, vS), vU);

        // 6 Faces, 2 triangles per face = 12 triangles = 36 vertices
        int[][] faceIndices = {
            {0, 1, 2, 0, 2, 3}, // Start Cap
            {4, 6, 5, 4, 7, 6}, // End Cap
            {0, 4, 5, 0, 5, 1}, // Top
            {3, 2, 6, 3, 6, 7}, // Bottom
            {1, 5, 6, 1, 6, 2}, // Left
            {0, 3, 7, 0, 7, 4}  // Right
        };

        float[][] faceNormals = {
            Math3DUtils.multiply(dir, -1f),
            dir,
            up,
            Math3DUtils.multiply(up, -1f),
            Math3DUtils.multiply(side, -1f),
            side
        };

        for (int f = 0; f < 6; f++) {
            float[] n = faceNormals[f];
            for (int vi = 0; vi < 6; vi++) {
                float[] p = corners[faceIndices[f][vi]];
                vb.put(p[0]).put(p[1]).put(p[2]);
                nb.put(n[0]).put(n[1]).put(n[2]);
                cb.put(color[0]).put(color[1]).put(color[2]).put(color[3]);
                vCount++;
            }
        }

        return vCount;
    }
}
