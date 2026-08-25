package org.the3deer.android.engine.text;

import org.the3deer.android.engine.model.Screen;
import org.the3deer.util.bean.Bean;
import org.the3deer.util.bean.BeanInit;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.inject.Inject;

/**
 * Data-driven Vector Font Factory (8x8 Grid)
 * Compact representation of the IBM 8x8 font.
 */
@Bean(name = "ibm8x8_optimized")
public class FontFactory2 implements FontFactory {

    private static final Logger logger = Logger.getLogger(FontFactory2.class.getSimpleName());

    @Inject
    private Screen screen;

    private float charWidth;
    private float charHeight;
    private float gXS;
    private float gYS;

    private final Map<Character, String> glyphData = new HashMap<>();
    private final Map<Character, float[]> glyphCache = new HashMap<>();

    public void setScreen(Screen screen) {
        this.screen = screen;
        setUp();
    }

    @BeanInit
    public void setUp() {
        this.charWidth = 48;
        this.charHeight = 48;
        this.gXS = charWidth / 7f;
        this.gYS = charHeight / 7f;

        initGlyphData();
        cacheGlyphs();
    }

    private void initGlyphData() {
        // Digits
        glyphData.put('0', "11165651111651");
        glyphData.put('1', "123151313615");
        glyphData.put('2', "1656541161");
        glyphData.put('3', "165634545111");
        glyphData.put('4', "16135356");
        glyphData.put('5', "561614545111");
        glyphData.put('6', "561611515414");
        glyphData.put('7', "165651");
        glyphData.put('8', "11165651111454");
        glyphData.put('9', "115156161454");

        // Uppercase
        glyphData.put('A', "111414143636365454545151512343");
        glyphData.put('B', "111616164655541414145443524111");
        glyphData.put('C', "51111656");
        glyphData.put('D', "111616164655524111");
        glyphData.put('E', "5616115111111444");
        glyphData.put('F', "111616165656561444");
        glyphData.put('G', "54561611515232");
        glyphData.put('H', "111616161454545456565651");
        glyphData.put('I', "26462636312141");
        glyphData.put('J', "1221415256");
        glyphData.put('K', "11161616144614141441");
        glyphData.put('L', "161151");
        glyphData.put('M', "1116345651");
        glyphData.put('N', "11165156");
        glyphData.put('O', "1116565111");
        glyphData.put('P', "1116161656555414");
        glyphData.put('Q', "111656511111113250");
        glyphData.put('R', "1116161656555414141451");
        glyphData.put('S', "115154141656");
        glyphData.put('T', "16563631");
        glyphData.put('U', "16115156");
        glyphData.put('V', "163156");
        glyphData.put('W', "16113134315156");
        glyphData.put('X', "1156341651");
        glyphData.put('Y', "1634563431");
        glyphData.put('Z', "16561151");

        // Lowercase
        glyphData.put('a', "133443411113333343");
        glyphData.put('b', "1611515313");
        glyphData.put('c', "53131151");
        glyphData.put('d', "5651111353");
        glyphData.put('e', "53131555535111");
        glyphData.put('f', "313636365656562444");
        glyphData.put('g', "5054141555");
        glyphData.put('h', "161111111344444441");
        glyphData.put('i', "313434343636");
        glyphData.put('j', "2030414444444646");
        glyphData.put('k', "16111111134413131341");
        glyphData.put('l', "3631");
        glyphData.put('m', "11143431345451");
        glyphData.put('n', "11143441");
        glyphData.put('o', "1114545111");
        glyphData.put('p', "1014545212");
        glyphData.put('q', "5054141252");
        glyphData.put('r', "111425");
        glyphData.put('s', "115152121454");
        glyphData.put('t', "252121211434");
        glyphData.put('u', "14115154");
        glyphData.put('v', "143154");
        glyphData.put('w', "1421324154");
        glyphData.put('x', "1154321451");
        glyphData.put('y', "1431543130");
        glyphData.put('z', "14541151");

        // Symbols
        glyphData.put('.', "3132");
        glyphData.put(',', "3120");
        glyphData.put('!', "31323436");
        glyphData.put('?', "15165654343231");
        glyphData.put('(', "46353241");
        glyphData.put(')', "26353221");
        glyphData.put('[', "41111646");
        glyphData.put(']', "21515626");
        glyphData.put('-', "2343");
        glyphData.put('+', "32342343");
        glyphData.put('=', "14541252");
        glyphData.put(' ', " "); // Empty glyph
    }

    private void cacheGlyphs() {
        for (Map.Entry<Character, String> entry : glyphData.entrySet()) {
            glyphCache.put(entry.getKey(), decodePath(entry.getValue()));
        }
    }

    private float[] decodePath(String path) {
        if (path == null || path.trim().isEmpty()) return new float[0];
        
        int numPoints = path.length() / 2;
        float[] vertices = new float[numPoints * 3];
        for (int i = 0; i < numPoints; i++) {
            int ix = Character.getNumericValue(path.charAt(i * 2));
            int iy = Character.getNumericValue(path.charAt(i * 2 + 1));
            vertices[i * 3] = ix * gXS;
            vertices[i * 3 + 1] = iy * gYS;
            vertices[i * 3 + 2] = 0;
        }
        return vertices;
    }

    public float[] getChar(char c) {
        float[] glyph = glyphCache.get(c);
        return glyph != null ? glyph : glyphCache.get(' ');
    }

    @Override
    public float[] getSymbol(String s) {
        if ("burger".equalsIgnoreCase(s)) {
            return decodePath("25656523236363212161");
        }
        return new float[0];
    }

    public float getCharWidth() { return charWidth; }
    public float getCharHeight() { return charHeight; }
}
