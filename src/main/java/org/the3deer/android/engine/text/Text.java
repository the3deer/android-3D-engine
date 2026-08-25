package org.the3deer.android.engine.text;

import org.the3deer.android.engine.model.Constants;
import org.the3deer.android.engine.model.Object3D;
import org.the3deer.util.io.IOUtils;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.logging.Logger;

/**
 * Utility to build text vertex and color buffers and 3D objects.
 */
public class Text {

    private static final Logger logger = Logger.getLogger(Text.class.getSimpleName());

    public static final float UI_TEXT_Z_HIDDEN = -0.001f;

    // floats needed for vertices
    public static final int VSIZE = 25 * 3;
    // floats needed for colors
    public static final int CSIZE = 25 * 4;

    public static final int MENU = 1000;
    public static final int MENU_NEW = 1001;
    public static final int CHECKBOX_OFF = 2000;
    public static final int CHECKBOX_ON = 2001;
    public static final int GLYPH_LESS_THAN_CODE = 3000;
    public static final int GLYPH_GREATER_THAN_CODE = 3001;

    /**
     * Builds a 3D object representing the provided text using the specified font factory.
     *
     * @param text the text to render
     * @param fontFactory the font factory to use
     * @return a new Object3D representing the text
     */
    public static Object3D build(final String text, final FontFactory fontFactory) {
        return build(text, Constants.COLOR_WHITE, fontFactory);
    }

    /**
     * Builds a 3D object representing the provided text using the specified color and font factory.
     *
     * @param text the text to render
     * @param color the color to apply to the text
     * @param fontFactory the font factory to use
     * @return a new Object3D representing the text
     */
    public static Object3D build(final String text, final float[] color, final FontFactory fontFactory) {
        if (text == null || text.isEmpty()) return new Object3D();

        final String[] lines = text.split("\\r?\\n");
        final int rows = lines.length;
        int tempMaxColumns = 0;
        int totalVerticesCount = 0;
        for (final String line : lines) {
            final int lineLength = line.length();
            if (lineLength > tempMaxColumns) tempMaxColumns = lineLength;
            for (int col = 0; col < lineLength; col++) {
                final float[] data = fontFactory.getChar(line.charAt(col));
                if (data != null && data.length > 0) {
                    totalVerticesCount += (data.length / 3) + 2;
                }
            }
        }
        final int maxColumns = tempMaxColumns;

        final FloatBuffer vertexBuffer = IOUtils.createFloatBuffer(totalVerticesCount * 3);
        final FloatBuffer colorBuffer = IOUtils.createFloatBuffer(totalVerticesCount * 4);

        // Centering calculation: Start at -halfWidth and -halfHeight
        final float totalWidth = maxColumns * fontFactory.getCharWidth();
        final float totalHeight = rows * fontFactory.getCharHeight();
        final float centeringShiftX = totalWidth / 2.0f;
        final float centeringShiftY = totalHeight / 2.0f;

        int currentIdx = 0;
        for (int row = 0; row < rows; row++) {
            final String line = lines[row];
            for (int col = 0; col < line.length(); col++) {
                final float offsetX = col * fontFactory.getCharWidth() - centeringShiftX;
                final float offsetY = (rows - 1) * fontFactory.getCharHeight() - row * fontFactory.getCharHeight() - centeringShiftY;

                final char c = line.charAt(col);
                if (c == '\n') break;

                final float[] data = fontFactory.getChar(c);
                if (data == null || data.length == 0) continue;

                // Always use the smart 'build' to ensure inter-character bridges are transparent
                currentIdx = Text.build(vertexBuffer, currentIdx, colorBuffer, offsetX, offsetY, data, color);
            }
        }

        // Clean up remaining space in buffers
        IOUtils.fill(vertexBuffer, currentIdx, vertexBuffer.capacity(), 0);
        IOUtils.fill(colorBuffer, (currentIdx / 3) * 4, colorBuffer.capacity(), 0);

        final Object3D object = new Object3D(vertexBuffer);
        object.setColorsBuffer(colorBuffer);
        object.setDrawMode(Object3D.GL_LINE_STRIP);
        
        // Finalize Object metadata
        object.setId("text_" + System.currentTimeMillis());
        object.setName(text);

        return object;
    }

    public static int build(FloatBuffer vertexBuffer, int idx, Buffer colorBuffer,
                             int code, float[] color, float offsetX, float offsetY, float offsetZ,
                             FontFactory fontFactory) {

        int coffset = (idx / 3) * 4;

        switch (code) {
            case CHECKBOX_OFF:
                IOUtils.fill(vertexBuffer, idx, Text.VSIZE, 0);
                IOUtils.fill(colorBuffer, coffset, Text.CSIZE, 0);
                return Text.build(vertexBuffer, idx, colorBuffer, fontFactory.getChar('x'), color, offsetX, offsetY, offsetZ);
            case CHECKBOX_ON:
                IOUtils.fill(vertexBuffer, idx, Text.VSIZE, 0);
                IOUtils.fill(colorBuffer, coffset, Text.CSIZE, 0);
                return Text.build(vertexBuffer, idx, colorBuffer, fontFactory.getChar('X'), color, offsetX, offsetY, offsetZ);
            case GLYPH_LESS_THAN_CODE:
                return Text.build(vertexBuffer, idx, colorBuffer, fontFactory.getSymbol("<"), color, offsetX, offsetY, offsetZ);
            case GLYPH_GREATER_THAN_CODE:
                return Text.build(vertexBuffer, idx, colorBuffer, fontFactory.getSymbol(">"), color, offsetX, offsetY, offsetZ);
            case MENU:
                int startIdx = idx;
                int newVerts = Text.line(vertexBuffer, idx, colorBuffer, Constants.COLOR_WHITE,
                        0f + offsetX, 0.5f + offsetY,
                        0.4f + offsetX, 0.5f + offsetY);
                idx += newVerts * 3;
                coffset += newVerts * 4;
                newVerts = Text.line(vertexBuffer, idx, colorBuffer, Constants.COLOR_WHITE,
                        0f + offsetX, 0.3f + offsetY,
                        0.4f + offsetX, 0.3f + offsetY);
                idx += newVerts * 3;
                coffset += newVerts * 4;
                newVerts = Text.line(vertexBuffer, idx, colorBuffer, Constants.COLOR_WHITE,
                        0f + offsetX, 0.1f + offsetY,
                        0.4f + offsetX, 0.1f + offsetY);
                idx += newVerts * 3;
                coffset += newVerts * 4;
                // IOUtils.fill(vertexBuffer, idx, TextBuilder.VSIZE - (idx-startIdx), 0);
                // IOUtils.fill(colorBuffer, coffset, TextBuilder.CSIZE - (coffset-(startIdx/3*4)), 0);
                return idx;
            default:
                return idx;
        }
    }

    public static int build2(FloatBuffer vertexBuffer, int idx, Buffer colorBuffer, float offsetX, float offsetY, float[] data, float[] color) {
        if (data == null || data.length == 0) return idx;

        int idxColor = (idx / 3) * 4;

        for (int i = 0; i < data.length; i += 3) {
            vertexBuffer.put(idx++, data[i] + offsetX);
            vertexBuffer.put(idx++, data[i + 1] + offsetY);
            vertexBuffer.put(idx++, data[i + 2]);

            ((FloatBuffer) colorBuffer).put(idxColor++, color[0]);
            ((FloatBuffer) colorBuffer).put(idxColor++, color[1]);
            ((FloatBuffer) colorBuffer).put(idxColor++, color[2]);
            ((FloatBuffer) colorBuffer).put(idxColor++, color[3]);
        }
        return idx;
    }

    public static int build(FloatBuffer vertexBuffer, int idx, Buffer colorBuffer, float offsetX, float offsetY, float[] data, float[] color) {

        if (data == null || data.length == 0) return idx;

        int idxColor = (idx / 3) * 4;

        // 1. LEAD: Start with a transparent point at the beginning of the glyph
        vertexBuffer.put(idx++, data[0] + offsetX);
        vertexBuffer.put(idx++, data[1] + offsetY);
        vertexBuffer.put(idx++, data[2] + UI_TEXT_Z_HIDDEN);

        ((FloatBuffer)colorBuffer).put(idxColor++, color[0]);
        ((FloatBuffer)colorBuffer).put(idxColor++, color[1]); 
        ((FloatBuffer)colorBuffer).put(idxColor++, color[2]);
        ((FloatBuffer)colorBuffer).put(idxColor++, 0f);

        // 2. BODY: Build the actual character vectors
        for (int i = 0; i < data.length; i += 3) {
            float z = data[i + 2];
            float alpha = (z < 0) ? 0f : color[3];

            vertexBuffer.put(idx++, data[i] + offsetX);
            vertexBuffer.put(idx++, data[i + 1] + offsetY);
            vertexBuffer.put(idx++, (z < 0) ? UI_TEXT_Z_HIDDEN : z);

            ((FloatBuffer)colorBuffer).put(idxColor++, color[0]);
            ((FloatBuffer)colorBuffer).put(idxColor++, color[1]);
            ((FloatBuffer)colorBuffer).put(idxColor++, color[2]);
            ((FloatBuffer)colorBuffer).put(idxColor++, alpha);
        }

        // 3. TRAIL: End with a transparent point at the end of the glyph
        vertexBuffer.put(idx++, data[data.length - 3] + offsetX);
        vertexBuffer.put(idx++, data[data.length - 2] + offsetY);
        vertexBuffer.put(idx++, data[data.length - 1] + UI_TEXT_Z_HIDDEN);

        ((FloatBuffer)colorBuffer).put(idxColor++, color[0]);
        ((FloatBuffer)colorBuffer).put(idxColor++, color[1]);
        ((FloatBuffer)colorBuffer).put(idxColor++, color[2]);
        ((FloatBuffer)colorBuffer).put(idxColor++, 0f);

        return idx;
    }


    public static int build(FloatBuffer vertexBuffer, int idx, Buffer colorBuffer,
                             float[] glyph,
                             float[] color,
                             float offsetX, float offsetY, float offsetZ) {

        int coffset = (idx / 3) * 4;

        // transparent link
        vertexBuffer.put(idx++, glyph[0] + offsetX).put(idx++, glyph[1] + offsetY).put(idx++, glyph[2] + offsetZ + UI_TEXT_Z_HIDDEN);
        ((FloatBuffer)colorBuffer).put(coffset++, 1f).put(coffset++, 0f).put(coffset++, 0f).put(coffset++, 0f);

        // glyph
        for (int i = 0; i < glyph.length; i += 3) {
            vertexBuffer.put(idx++, glyph[i] + offsetX)
                    .put(idx++, glyph[i + 1] + offsetY)
                    .put(idx++, glyph[i + 2] + offsetZ);
            ((FloatBuffer)colorBuffer).put(coffset++, color[0]).put(coffset++, color[1]).put(coffset++, color[2]).put(coffset++, color[3]);
        }

        // transparent link
        vertexBuffer.put(idx++, glyph[glyph.length - 3] + offsetX)
                .put(idx++, glyph[glyph.length - 2] + offsetY)
                .put(idx++, glyph[glyph.length - 1] + offsetZ + UI_TEXT_Z_HIDDEN);
        ((FloatBuffer)colorBuffer).put(coffset++, 1).put(coffset++, 0f).put(coffset++, 0f).put(coffset++, 0f);

        return glyph.length + 2;
    }

    public static int line(FloatBuffer vertexBuffer, int vp, Buffer colorBuffer, float[] color,
                            float x1, float y1,
                            float x2, float y2) {
        int voffset = vp;
        int cp = (voffset / 3) * 4;
        logger.info("voffset: " + vp + ", coffset:" + cp);
        vertexBuffer.put(voffset++, x1).put(voffset++, y1).put(voffset++, -1f);
        vertexBuffer.put(voffset++, x1).put(voffset++, y1).put(voffset++, 0f);
        vertexBuffer.put(voffset++, x2).put(voffset++, y2).put(voffset++, 0f);
        vertexBuffer.put(voffset++, x2).put(voffset++, y2).put(voffset, -1f);

        int coffset = cp;
        ((FloatBuffer)colorBuffer).put(coffset++, 1).put(coffset++, Constants.COLOR_NULL[1]).put(coffset++, Constants.COLOR_NULL[2]).put(coffset++, Constants.COLOR_NULL[3]);
        ((FloatBuffer)colorBuffer).put(coffset++, color[0]).put(coffset++, color[1]).put(coffset++, color[2]).put(coffset++, color[3]);
        ((FloatBuffer)colorBuffer).put(coffset++, color[0]).put(coffset++, color[1]).put(coffset++, color[2]).put(coffset++, color[3]);
        ((FloatBuffer)colorBuffer).put(coffset++, 1).put(coffset++, Constants.COLOR_NULL[1]).put(coffset++, Constants.COLOR_NULL[2]).put(coffset, Constants.COLOR_NULL[3]);

        return 4;  // 4 vertices
    }
}
