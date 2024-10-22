package org.the3deer.android_3d_model_engine.gui;

import android.util.Log;

import org.the3deer.android_3d_model_engine.model.Dimensions;
import org.the3deer.util.io.IOUtils;

import java.nio.Buffer;
import java.nio.FloatBuffer;

/**
 * 8 bit font
 * <p>
 * Based on 5 x 7 monospaced pixel font
 * <p>
 * All Glyphs are drawn using a line strip
 */
public final class Text extends Widget {

    private final int rows;
    private final int columns;
    private float padding;
    private String currentText = null;

    private Text(Widget parent, int columns, int rows) {
        this(parent, columns, rows, 0);
    }

    private Text(Widget parent, int columns, int rows, float padding) {
        super(parent);
        this.columns = columns;
        this.rows = rows;
        this.padding = padding;
        init();
    }

    @Override
    public void init() {
        setVertexBuffer(IOUtils.createFloatBuffer(columns * rows * 12 * 3));
        setColorsBuffer(IOUtils.createFloatBuffer(columns * rows * 12 * 4));

        // dimensions after because there isn't any vertex yet
        setDimensions(new Dimensions(0,
                columns * (0.5f + padding * 2), rows * (0.7f + padding * 2), 0, 0, 0));
        Log.d("Text", "Created text: " + getDimensions());
    }

    public static Text allocate(Widget parent, int columns, int rows) {
        return allocate(parent, columns, rows, 0);
    }

    public static Text allocate(Widget parent, int columns, int rows, float padding) {
        return new Text(parent, columns, rows, padding);
    }

    public void update(String text) {
        if (text == null || text.equals(this.currentText)) return;

        final String[] lines = text.split("\\r?\\n");

        final FloatBuffer vertexBuffer = getVertexBuffer();
        final Buffer colorBuffer = getColorsBuffer();

        int idx = 0;
        for (int row=0; row<this.rows && row<lines.length; row++){
            for (int column=0; column<this.columns && column < lines[row].length(); column++){
                float offsetX = column * (0.5f + padding * 2) + padding;
                float offsetY = (this.rows -1 ) * (0.7f + padding * 2) - row * (0.7f + padding * 2) + padding;

                final char letter = lines[row].charAt(column);
                if (letter == '\n') {
                    break;
                }

                final float[] data = Glyph.getChar(letter);
                if (data == null) continue;

                idx = Glyph.build(vertexBuffer, idx, colorBuffer, offsetX, offsetY, data, getColor());
            }
        }

        int idxColor = idx / 3 * 4;

        IOUtils.fill(vertexBuffer, idx, vertexBuffer.capacity(), 0);
        IOUtils.fill(colorBuffer, idxColor, colorBuffer.capacity(), 0);

        this.currentText = text;
    }

    public void update_old(String text) {
        if (text == null || text.equals(this.currentText)) return;

        final String[] lines = text.split("\\r?\\n");

        final FloatBuffer vertexBuffer = getVertexBuffer();
        final Buffer colorBuffer = getColorsBuffer();

        int idx = 0;
        for (int row = 0; row < this.rows && row < lines.length; row++) {
            for (int column = 0; column < this.columns && column < lines[row].length(); column++) {
                float offsetX = column * (0.5f + padding * 2) + padding;
                float offsetY = (this.rows - 1) * (0.7f + padding * 2) - row * (0.7f + padding * 2) + padding;

                final char letter = lines[row].charAt(column);
                if (letter == '\n') {
                    break;
                }

                final float[] data = Glyph.getChar(letter);
                if (data == null) continue;

                idx = Glyph.build(vertexBuffer, idx, colorBuffer, offsetX, offsetY, data, getColor());
            }
        }

        int idxColor = idx / 3 * 4;

        IOUtils.fill(vertexBuffer, idx, vertexBuffer.capacity(), 0);
        IOUtils.fill(colorBuffer, idxColor, colorBuffer.capacity(), 0);

        this.currentText = text;
    }

}
