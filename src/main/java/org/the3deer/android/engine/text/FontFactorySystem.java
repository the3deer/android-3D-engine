package org.the3deer.android.engine.text;

import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.Rect;
import android.graphics.Typeface;

import org.the3deer.android.engine.model.Screen;
import org.the3deer.util.bean.Bean;
import org.the3deer.util.bean.BeanInit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

/**
 * Professional Vector Font Factory using Android System Fonts.
 * Extracts high-precision vector outlines from Typeface.DEFAULT (Roboto).
 */
@Bean(name = "system")
public class FontFactorySystem implements FontFactory {

    @Inject
    private Screen screen;

    private final float charWidth = 48;
    private final float charHeight = 48;

    private final Map<Character, float[]> glyphCache = new HashMap<>();

    @Override
    public void setScreen(final Screen screen) {
        this.screen = screen;
    }

    @BeanInit
    public void setUp() {
        final float fontSize = 40;
        // Pre-cache ASCII range for immediate availability
        for (char c = 32; c <= 126; c++) {
            cacheChar(c, fontSize);
        }
    }

    private void cacheChar(char c, float fontSize) {
        if (Character.isWhitespace(c)) {
            glyphCache.put(c, new float[0]);
            return;
        }

        final Paint paint = new Paint();
        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextSize(fontSize);
        paint.setAntiAlias(true);

        final String text = String.valueOf(c);
        final Path path = new Path();
        paint.getTextPath(text, 0, 1, 0, 0, path);

        final Rect bounds = new Rect();
        paint.getTextBounds(text, 0, 1, bounds);
        
        // Calculate offsets to center the glyph in the 48x48 box
        final float offsetX = -bounds.left + (charWidth - bounds.width()) / 2f;
        final float offsetY = -bounds.top + (charHeight - bounds.height()) / 2f;

        final List<Float> vertices = new ArrayList<>();
        final PathMeasure pm = new PathMeasure(path, false);
        final float[] pos = new float[2];
        float lastX = 0, lastY = 0;

        do {
            final float length = pm.getLength();
            boolean first = true;
            
            // Precision step in pixels (1.0f = high quality)
            final float step = 1.0f; 
            
            for (float distance = 0; distance <= length; distance += step) {
                pm.getPosTan(distance, pos, null);
                
                final float x = pos[0] + offsetX;
                final float y = -pos[1] + offsetY; // Invert Y for OpenGL

                if (first && !vertices.isEmpty()) {
                    // Pen-Up Jump: Handshake protocol using Z = -1
                    vertices.add(lastX); vertices.add(lastY); vertices.add(-1.0f);
                    vertices.add(x); vertices.add(y); vertices.add(-1.0f);
                }

                vertices.add(x); vertices.add(y); vertices.add(0.0f);
                lastX = x;
                lastY = y;
                first = false;
            }
            
            // Close the loop if needed
            if (pm.isClosed()) {
                pm.getPosTan(0, pos, null);
                final float x = pos[0] + offsetX;
                final float y = -pos[1] + offsetY;
                vertices.add(x); vertices.add(y); vertices.add(0.0f);
                lastX = x; lastY = y;
            }
            
        } while (pm.nextContour());

        final float[] result = new float[vertices.size()];
        for (int i = 0; i < vertices.size(); i++) {
            result[i] = vertices.get(i);
        }
        glyphCache.put(c, result);
    }

    @Override
    public float getCharWidth() {
        return charWidth;
    }

    @Override
    public float getCharHeight() {
        return charHeight;
    }

    @Override
    public float[] getChar(final char c) {
        if (!glyphCache.containsKey(c)) {
             cacheChar(c, 40);
        }
        return glyphCache.get(c);
    }

    @Override
    public float[] getSymbol(final String s) {
        if (s == null || s.isEmpty()) return new float[0];
        return getChar(s.charAt(0));
    }
}
