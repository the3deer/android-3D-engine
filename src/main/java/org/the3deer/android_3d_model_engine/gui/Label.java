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
public class Label extends Widget {

    private final FontFactory fontFactory;
    private final int rows;
    private final int columns;
    private String currentText = null;


    public Label(String text) {
        this(FontFactory.getInstance(), text);
    }
    public Label(FontFactory fontFactory, String text) {
        super();
        this.fontFactory = fontFactory;
        this.columns = text.length();
        this.rows = 1;
        init();

        setText(text);
        this.currentText = text;
    }
    public Label(FontFactory fontFactory, int columns, int rows) {
        super();
        this.fontFactory = fontFactory;
        this.columns = columns;
        this.rows = rows;
        init();
    }


    @Override
    public void init() {
        setVertexBuffer(IOUtils.createFloatBuffer(columns * rows * 12 * 3));
        setColorsBuffer(IOUtils.createFloatBuffer(columns * rows * 12 * 4));

        IOUtils.fill(vertexBuffer, 0, vertexBuffer.capacity(), new float[]{0,0, GUIConstants.UI_TEXT_Z_HIDDEN});
        IOUtils.fill(colorsBuffer, 0, colorsBuffer.capacity(), new float[]{1,0,0,0});

        // dimensions initialized because there isn't any vertex yet
        setDimensions(new Dimensions(0,
                columns * fontFactory.getCharWidth(), rows * fontFactory.getCharHeight(), 0, 0, 0));
        Log.d("Label", "Created text: " + getDimensions());
    }

    public static Label forSymbol(FontFactory fontFactory) {
        return new Label(fontFactory, 1, 1);
    }

    public void setSymbol(String symbol){
        if (symbol == null || symbol.equals(this.currentText)) return;

        float[] data = fontFactory.getSymbol(symbol);

        Log.v("Label","About to paint symbol");
        int idx = Glyph.build(getVertexBuffer(), 0, getColorsBuffer(), 0, 0, data, getColor());
        IOUtils.fill(getVertexBuffer(), idx, getVertexBuffer().capacity(), 0);
        IOUtils.fill(getColorsBuffer(), idx/3*4, getColorsBuffer().capacity(), 0);

        this.currentText = symbol;
    }

    public Widget setText(String text) {
        if (text == null || text.equals(this.currentText)) return this;

        final String[] lines = text.split("\\r?\\n");

        final FloatBuffer vertexBuffer = getVertexBuffer();
        final Buffer colorBuffer = getColorsBuffer();

        int idx = 0;
        for (int row = 0; row < this.rows && row < lines.length; row++) {
            for (int column = 0; column < this.columns && column < lines[row].length(); column++) {
                float offsetX = column * fontFactory.getCharWidth();
                float offsetY = (this.rows - 1) * fontFactory.getCharHeight() - row * fontFactory.getCharHeight();

                final char letter = lines[row].charAt(column);
                if (letter == '\n') {
                    break;
                }

                final float[] data = fontFactory.getChar(letter);
                if (data == null) continue;

                idx = Glyph.build(vertexBuffer, idx, colorBuffer, offsetX, offsetY, data, getColor());
            }
        }

        int idxColor = (idx / 3) * 4;

        IOUtils.fill(vertexBuffer, idx, vertexBuffer.capacity(), 0);
        IOUtils.fill(colorBuffer, idxColor, colorBuffer.capacity(), 0);

        this.currentText = text;

        return this;
    }
}
