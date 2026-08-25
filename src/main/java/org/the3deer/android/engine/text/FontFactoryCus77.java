package org.the3deer.android.engine.text;

import org.the3deer.android.engine.model.Screen;
import org.the3deer.util.bean.Bean;
import org.the3deer.util.bean.BeanInit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.inject.Inject;

/**
 * Professional Vector Font Factory using a human-readable 2-digit grid encoding.
 * 
 * ### Grid Layout:
 * (0,0) ... (6,0)  <- TOP (Cap height)
 * (0,2) ... (6,2)  <- X-Height
 * (0,6) ... (6,6)  <- BASELINE
 * (0,9) ... (6,9)  <- DESCENDER
 * 
 * String format: "X1Y1 X2Y2 ..." (each digit is a grid point 0-9).
 * Spaces indicate a "pen up" (jump) to the next coordinate.
 */
@Bean(name = "custom")
public class FontFactoryCus77 implements FontFactory {

    private static final Logger logger = Logger.getLogger(FontFactoryCus77.class.getSimpleName());

    @Inject
    private Screen screen;

    private final float charWidth = 48;
    private final float charHeight = 48;
    private final float scale = 1.8f; 

    private final Map<Character, String> glyphData = new HashMap<>();
    private final Map<Character, float[]> glyphCache = new HashMap<>();

    public void setScreen(final Screen screen) {
        this.screen = screen;
    }

    @BeanInit
    public void setUp() {
        initGlyphData();
        cacheGlyphs();
        
        // Debugging '4' as requested
        logNumericGlyph('4');
    }

    private void logNumericGlyph(final char c) {
        final String hData = glyphData.get(c);
        if (hData != null) {
            logger.info("Glyph '" + c + "' numeric string: " + hData);
        }
    }

    private void initGlyphData() {
        // Uppercase
        glyphData.put('A', "3016 3056 2444");
        glyphData.put('B', "1016 1040 4051 5142 4212 1243 4354 5445 4516"); 
        glyphData.put('C', "5140 2011 0204 1526 4655");
        glyphData.put('D', "1016 1041 4153 5345 4516");
        glyphData.put('E', "5010 1016 1656 1343");
        glyphData.put('F', "5010 1016 1343");
        glyphData.put('G', "5140 2011 0204 1526 4655 5553 33");
        glyphData.put('H', "1016 5056 1353");
        glyphData.put('I', "3036");
        glyphData.put('J', "5055 5536 3615");
        glyphData.put('K', "1016 1350 1356");
        glyphData.put('L', "1016 1656");
        glyphData.put('M', "1610 1033 3350 5056");
        glyphData.put('N', "1610 1056 5650");
        glyphData.put('O', "3020 1102 0415 2636 4655 6462 5140 30");
        glyphData.put('P', "1610 1040 4051 5142 4212");
        glyphData.put('Q', "3020 1102 0415 2636 4655 6462 5140 30 3456");
        glyphData.put('R', "1610 1040 4051 5142 4212 2356");
        glyphData.put('S', "5140 2011 0213 3353 6455 4626 15");
        glyphData.put('T', "1050 3036");
        glyphData.put('U', "1015 2646 5550");
        glyphData.put('V', "1036 3650");
        glyphData.put('W', "1016 1633 3356 5650");
        glyphData.put('X', "1056 5016");
        glyphData.put('Y', "1033 5033 3336");
        glyphData.put('Z', "1050 5016 1656");

        // Lowercase
        glyphData.put('a', "4323 2314 1415 1526 2646 4643 4346");
        glyphData.put('b', "1016 1646 4655 5543 4313");
        glyphData.put('c', "4323 2314 1415 1526 2646");
        glyphData.put('d', "4046 4626 2615 1514 1423 2343");
        glyphData.put('e', "1444 4443 4323 2314 1415 1526 2646");
        glyphData.put('f', "4030 3021 2126 1333");
        glyphData.put('g', "4323 2314 1415 1526 2646 4648 4839 3919");
        glyphData.put('h', "1016 1333 3343 4446");
        glyphData.put('i', "3232 3436");
        glyphData.put('j', "4242 4448 4839 3929");
        glyphData.put('k', "1016 1433 3346");
        glyphData.put('l', "3036");
        glyphData.put('m', "1316 1323 2326 2333 3336");
        glyphData.put('n', "1316 1333 3343 4446");
        glyphData.put('o', "2314 1415 1526 2645 4544 4423");
        glyphData.put('p', "1318 1343 4354 5445 4515");
        glyphData.put('q', "4348 4323 2314 1415 1526 2646");
        glyphData.put('r', "1316 1333 3343");
        glyphData.put('s', "4323 2334 3445 4526");
        glyphData.put('t', "2126 1333");
        glyphData.put('u', "1315 1536 3645 4543");
        glyphData.put('v', "1336 3653");
        glyphData.put('w', "1316 1634 3456 5653");
        glyphData.put('x', "1346 4316");
        glyphData.put('y', "1336 5336 3628 2818");
        glyphData.put('z', "1343 4316 1646");

        // Digits
        glyphData.put('0', "3020 1102 0415 2636 4655 6462 5140 30");
        glyphData.put('1', "2130 3036");
        glyphData.put('2', "1120 3040 5142 1656");
        glyphData.put('3', "1120 3141 3345 5616");
        glyphData.put('4', "4640 4013 1353");
        glyphData.put('5', "5010 1013 1343 4355 5546 4616");
        glyphData.put('6', "4021 2115 1536 3655 5554 5443 4313");
        glyphData.put('7', "1050 5026");
        glyphData.put('8', "2112 1242 4251 5140 4020 2011 1115 1526 2645 4554 5443 4323 2312"); 
        glyphData.put('9', "2313 1311 1120 2040 4051 5146 4626");

        // Symbols
        glyphData.put(' ', "");
        glyphData.put('.', "3636");
        glyphData.put(',', "3637");
        glyphData.put(':', "3232 3434");
        glyphData.put(';', "3232 3435");
        glyphData.put('+', "3234 2343");
        glyphData.put('-', "2343");
        glyphData.put('_', "1656");
        glyphData.put('(', "4031 3135 3546");
        glyphData.put(')', "2031 3135 3526");
        glyphData.put('?', "2130 3041 4142 4233 3335 3636");
        glyphData.put('!', "3034 3636");
        glyphData.put('\'', "3031");
        glyphData.put('"', "2021 4041");
    }

    private void cacheGlyphs() {
        for (final Map.Entry<Character, String> entry : glyphData.entrySet()) {
            glyphCache.put(entry.getKey(), decodeCustom(entry.getValue()));
        }
        if (!glyphCache.containsKey(' ')) glyphCache.put(' ', new float[0]);
    }

    private float[] decodeCustom(final String path) {
        if (path == null || path.isEmpty()) return new float[0];
        
        final List<Float> vertices = new ArrayList<>();
        float lastX = -1, lastY = -1;
        boolean jumpNext = false;

        for (int i = 0; i < path.length(); i++) {
            final char c = path.charAt(i);
            if (Character.isWhitespace(c)) {
                jumpNext = true;
                continue;
            }

            if (Character.isDigit(c) && i + 1 < path.length()) {
                final int ix = Character.getNumericValue(c);
                final int iy = Character.getNumericValue(path.charAt(++i));

                // Logic: hX = (gridPoint - 3) * 3.5, hY = gridPoint * 3.5 - 12.0
                final float hX = (ix - 3) * 3.5f;
                final float hY = (iy * 3.5f) - 12.0f;

                final float x = hX * scale + (charWidth / 2);
                final float y = -hY * scale + (charHeight / 2);

                if (jumpNext) {
                    if (!vertices.isEmpty()) {
                        // Lift Pen: Sentinel Z Handshake
                        vertices.add(lastX); vertices.add(lastY); vertices.add(-1.0f);
                        vertices.add(x); vertices.add(y); vertices.add(-1.0f);
                    }
                    jumpNext = false;
                }

                vertices.add(x); vertices.add(y); vertices.add(0f);
                lastX = x; lastY = y;
            }
        }

        final float[] result = new float[vertices.size()];
        for (int i = 0; i < vertices.size(); i++) result[i] = vertices.get(i);
        return result;
    }

    @Override
    public float[] getChar(final char c) {
        float[] glyph = glyphCache.get(c);
        if (glyph == null) {
            if (Character.isLowerCase(c)) glyph = glyphCache.get(Character.toUpperCase(c));
            else if (Character.isUpperCase(c)) glyph = glyphCache.get(Character.toLowerCase(c));
        }
        return glyph != null ? glyph : glyphCache.get(' ');
    }

    @Override
    public float[] getSymbol(final String s) {
        if (s == null || s.isEmpty()) return new float[0];
        return getChar(s.charAt(0));
    }

    public float getCharWidth() { return charWidth; }
    public float getCharHeight() { return charHeight; }
}
