package org.the3deer.android.engine.text;

import org.the3deer.android.engine.model.Screen;
import org.the3deer.util.bean.Bean;
import org.the3deer.util.bean.BeanInit;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.inject.Inject;

/**
 * Mono-size glyph implementation based on IBM 8x8 font.
 * 
 * height:  8 points
 * width:   8 points
 * max vertices: 75
 */
@Bean(name = "ibm8x8")
public class FontFactory1 implements FontFactory {

    private static final Logger logger = Logger.getLogger(FontFactory1.class.getSimpleName());

    @Inject
    private Screen screen;

    private float screen_width;
    private float screen_height;
    private float screen_ratio;

    private float charWidth;
    private float charHeight;
    private float g_x0;

    private float g_x1;
    private float g_x2;
    private float g_x3;
    private float g_x4;
    private float g_x5;
    private float g_x6;
    private float g_x7;
    private float g_y0;
    private float g_y1;
    private float g_y2;
    private float g_y3;
    private float g_y4;
    private float g_y5;
    private float g_y6;
    private float g_y7;
    private int g_xpoints = 8;
    private int g_ypoints = 8;

    private final Map<Character, float[]> chars = new HashMap<>();

    private final Map<String, float[]> symbols = new HashMap<>();

    @Override
    public void setScreen(Screen screen) {
        this.screen = screen;
        setUp();
    }

    @BeanInit
    public void setUp() {
        this.screen_width = screen.width;
        this.screen_height = screen.height;
        this.screen_ratio = screen_width / screen_height;

        final int rows;
        final int columns;
        if (screen_width > screen_height) {  // horizontal layout
            rows = 40;
            columns = 80;
        } else if (screen_height > screen_width) {  // vertical layout
            rows = 80;
            columns = 40;
        } else { // squared device?
            rows = 60;
            columns = 60;
        }

        charWidth = screen_width / columns;
        charHeight = screen_height / rows;

        charWidth = charHeight = 48;

        final float gXS = charWidth / (g_xpoints - 1);
        g_x0 = 0;
        g_x1 = gXS;
        g_x2 = gXS * 2;
        g_x3 = gXS * 3;
        g_x4 = gXS * 4;
        g_x5 = gXS * 5;
        g_x6 = gXS * 6;
        g_x7 = gXS * 7;
        final float gYS = charHeight / (g_ypoints - 1);
        g_y0 = 0;
        g_y1 = gYS;
        g_y2 = gYS * 2;
        g_y3 = gYS * 3;
        g_y4 = gYS * 4;
        g_y5 = gYS * 5;
        g_y6 = gYS * 6;
        g_y7 = gYS * 7;

        init();
    }

    @Override
    public float getCharWidth() {
        return charWidth;
    }

    @Override
    public float getCharHeight() {
        return charHeight;
    }

    public float getCharXS() {
        return g_x3;
    }

    public float getCharYS() {
        return g_y1;
    }

    @Override
    public float[] getChar(char c) {
        return this.chars.get(c);
    }

    public float[] getSymbol(String s) {
        return this.symbols.get(s);
    }

    private void init() {
        chars.put('0', _0());
        chars.put('1', _1());
        chars.put('2', _2());
        chars.put('3', _3());
        chars.put('4', _4());
        chars.put('a', _a());
        chars.put('c', _c());
        chars.put('e', _e());
        chars.put('f', _f());
        chars.put('g', _g());
        chars.put('h', _h());
        chars.put('i', _i());
        chars.put('l', _l());
        chars.put('m', _m());
        chars.put('n', _n());
        chars.put('o', _o());
        chars.put('p', _p());
        chars.put('r', _r());
        chars.put('s', _s());
        chars.put('t', _t());
        chars.put('u', _u());
        chars.put('w', _w());
        chars.put('x', _x());
        chars.put('F', _F());
        chars.put('P', _P());
        chars.put('S', _S());
        chars.put('X', _X());
        chars.put('.', _dot());
        chars.put('[', _squareBracketLeft());
        chars.put(']', _squareBracketRight());
        chars.put('(', _curlyBracketLeft());
        chars.put(')', _curlyBracketRight());
        symbols.put("burger", _burger());
    }

    private float[] _0() {
        return new float[]{
                g_x0, g_y2, 0,
                g_x1, g_y1, 0,
                g_x5, g_y1, 0,
                g_x6, g_y2, 0,
                g_x6, g_y6, 0,
                g_x5, g_y7, 0,
                g_x1, g_y7, 0,
                g_x0, g_y6, 0,
                g_x0, g_y2, 0,
                g_x6, g_y6, 0
        };
    }

    private float[] _1() {
        return new float[]{
                g_x0, g_y1, 0,
                g_x5, g_y1, 0,
                g_x3, g_y1, 0,
                g_x3, g_y7, 0,
                g_x1, g_y5, 0
        };
    }

    private float[] _2() {
        return new float[]{
                g_x1, g_y6, 0f,
                g_x2, g_y7, 0f,
                g_x4, g_y7, 0f,
                g_x5, g_y6, 0f,
                g_x5, g_y5, 0f,
                g_x0, g_y1, 0f,
                g_x6, g_y1, 0f
        };
    }

    private float[] _3() {
        return new float[]{
                g_x1, g_y6, 0f,
                g_x2, g_y7, 0f,
                g_x5, g_y7, 0f,
                g_x6, g_y6, 0f,
                g_x6, g_y5, 0f,
                g_x5, g_y4, 0f,
                g_x2, g_y4, 0f,
                g_x5, g_y4, 0f,
                g_x6, g_y3, 0f,
                g_x6, g_y2, 0f,
                g_x5, g_y1, 0f,
                g_x2, g_y1, 0f,
                g_x1, g_y2, 0f
        };
    }

    private float[] _4() {
        return new float[]{
                g_x5, g_y1, 0f,
                g_x5, g_y7, 0f,
                g_x0, g_y3, 0f,
                g_x6, g_y3, 0f,
                g_x5, g_y3, 0f,
                g_x5, g_y1, 0f,
                g_x6, g_y1, 0f,
                g_x4, g_y1, 0f
        };
    }

    private float[] _a() {
        return new float[]{
                g_x1, g_y5, 0,
                g_x3, g_y5, 0,
                g_x4, g_y4, 0,
                g_x4, g_y1, 0,
                g_x2, g_y1, 0,
                g_x1, g_y2, 0,
                g_x2, g_y3, 0,
                g_x4, g_y3, 0
        };
    }

    private float[] _c() {
        return new float[]{
                g_x5, g_y4, 0,
                g_x4, g_y5, 0,
                g_x1, g_y5, 0,
                g_x0, g_y4, 0,
                g_x0, g_y2, 0,
                g_x1, g_y1, 0,
                g_x4, g_y1, 0,
                g_x5, g_y2, 0
        };
    }

    private float[] _e() {
        return new float[]{
                g_x4, g_y1, 0f,
                g_x2, g_y1, 0f,
                g_x1, g_y2, 0f,
                g_x1, g_y4, 0f,
                g_x2, g_y5, 0f,
                g_x4, g_y5, 0f,
                g_x5, g_y4, 0f,
                g_x5, g_y3, 0f,
                g_x1, g_y3, 0f
        };
    }

    private float[] _f() {
        return new float[]{
                g_x2, g_y1, 0f,
                g_x2, g_y4, 0f,
                g_x1, g_y4, 0f,
                g_x3, g_y4, 0f,
                g_x2, g_y4, 0f,
                g_x2, g_y6, 0f,
                g_x3, g_y7, 0f,
                g_x4, g_y7, 0f,
                g_x5, g_y6, 0f
        };
    }

    private float[] _g() {
        return new float[]{
                g_x1, g_y0, 0f,
                g_x4, g_y0, 0f,
                g_x5, g_y1, 0f,
                g_x5, g_y5, 0f,
                g_x2, g_y5, 0f,
                g_x1, g_y4, 0f,
                g_x1, g_y3, 0f,
                g_x3, g_y2, 0f,
                g_x5, g_y2, 0f
        };
    }

    private float[] _h() {
        return new float[]{
                g_x1, g_y7, 0,
                g_x1, g_y1, 0,
                g_x1, g_y3, 0,
                g_x2, g_y5, 0,
                g_x3, g_y5, 0,
                g_x4, g_y3, 0,
                g_x4, g_y1, 0
        };
    }

    private float[] _i() {
        return new float[]{
                g_x1, g_y1, 0,
                g_x3, g_y1, 0,
                g_x2, g_y1, 0,
                g_x2, g_y5, 0,
                g_x1, g_y5, 0,
                g_x1, g_y5, 0,
                g_x2, g_y6, 0,
                g_x2, g_y6, 0,
                g_x2, g_y7, 0f
        };
    }

    private float[] _l() {
        return new float[]{
                g_x1, g_y1, 0,
                g_x3, g_y1, 0,
                g_x2, g_y1, 0,
                g_x2, g_y7, 0,
                g_x1, g_y7, 0f
        };
    }

    private float[] _m() {
        return new float[]{
                g_x1, g_y1, 0,
                g_x1, g_y5, 0,
                g_x2, g_y5, 0,
                g_x3, g_y4, 0,
                g_x3, g_y1, 0,
                g_x3, g_y5, 0,
                g_x4, g_y5, 0,
                g_x5, g_y4, 0,
                g_x5, g_y1, 0
        };
    }

    private float[] _n() {
        return new float[]{
                g_x1, g_y5, 0,
                g_x1, g_y1, 0,
                g_x1, g_y4, 0,
                g_x2, g_y5, 0,
                g_x4, g_y5, 0,
                g_x5, g_y4, 0,
                g_x5, g_y1, 0f
        };
    }

    private float[] _o() {
        return new float[]{
                g_x1, g_y1, 0,
                g_x3, g_y1, 0,
                g_x4, g_y2, 0,
                g_x4, g_y4, 0,
                g_x3, g_y5, 0,
                g_x1, g_y5, 0,
                g_x0, g_y4, 0,
                g_x0, g_y2, 0,
                g_x1, g_y1, 0
        };
    }

    private float[] _p() {
        return new float[]{
                g_x2, g_y0, 0,
                g_x2, g_y4, 0,
                g_x3, g_y5, 0,
                g_x5, g_y5, 0,
                g_x6, g_y4, 0,
                g_x6, g_y3, 0,
                g_x5, g_y2, 0,
                g_x2, g_y2, 0
        };
    }

    private float[] _r() {
        return new float[]{
                g_x1, g_y5, 0,
                g_x1, g_y1, 0,
                g_x1, g_y3, 0,
                g_x3, g_y5, 0,
                g_x4, g_y5, 0,
                g_x5, g_y3, 0
        };
    }

    private float[] _s() {
        return new float[]{
                g_x6, g_y5, 0,
                g_x3, g_y5, 0,
                g_x2, g_y4, 0,
                g_x3, g_y3, 0,
                g_x5, g_y3, 0,
                g_x6, g_y2, 0,
                g_x5, g_y1, 0,
                g_x2, g_y1, 0,
        };
    }

    private float[] _t() {
        return new float[]{
                g_x2, g_y7, 0,
                g_x2, g_y5, 0,
                g_x5, g_y5, 0,
                g_x1, g_y5, 0,
                g_x2, g_y5, 0,
                g_x2, g_y2, 0,
                g_x3, g_y1, 0,
                g_x4, g_y1, 0,
                g_x5, g_y2, 0
        };
    }

    private float[] _u() {
        return new float[]{
                g_x1, g_y5, 0,
                g_x1, g_y2, 0,
                g_x2, g_y1, 0,
                g_x4, g_y1, 0,
                g_x5, g_y2, 0,
                g_x5, g_y5, 0,
                g_x5, g_y2, 0,
                g_x6, g_y1, 0
        };
    }

    private float[] _w() {
        return new float[]{
                0.0f, g_y5, 0,
                0.0f, g_y2, 0,
                g_x1, g_y1, 0,
                g_x3, g_y2, 0,
                g_x5, g_y1, 0,
                g_x6, g_y2, 0,
                g_x6, g_y5, 0
        };
    }

    private float[] _x() {
        return new float[]{
                g_x0, g_y5, 0,
                g_x6, g_y1, 0,
                g_x3, g_y3, 0,
                g_x6, g_y5, 0,
                g_x0, g_y1, 0,
                g_x0, g_y1, 0
        };
    }

    private float[] _F() {
        return new float[]{
                g_x1, g_y1, 0,
                g_x1, g_y7, 0,
                g_x6, g_y7, 0,
                g_x1, g_y7, 0,
                g_x1, g_y5, 0,
                g_x5, g_y5, 0
        };
    }

    private float[] _P() {
        return new float[]{
                g_x1, g_y1, 0,
                g_x1, g_y7, 0,
                g_x5, g_y7, 0,
                g_x6, g_y6, 0,
                g_x6, g_y5, 0,
                g_x5, g_y4, 0,
                g_x1, g_y4, 0
        };
    }

    private float[] _S() {
        return new float[]{
                g_x0, g_y2, 0,
                g_x1, g_y1, 0,
                g_x4, g_y1, 0,
                g_x5, g_y2, 0,
                g_x0, g_y6, 0,
                g_x1, g_y7, 0,
                g_x5, g_y7, 0,
                g_x6, g_y6, 0
        };
    }

    private float[] _X() {
        return new float[]{
                g_x0, g_y7, 0,
                g_x6, g_y1, 0,
                g_x3, g_y4, 0,
                g_x6, g_y7, 0,
                g_x0, g_y1, 0
        };
    }

    private float[] _dot() {
        return new float[]{
                g_x1, g_y1, 0,
                g_x2, g_y1, 0,
                g_x2, g_y2, 0,
                g_x1, g_y2, 0,
                g_x1, g_y1, 0
        };
    }

    private float[] _squareBracketLeft() {
        return new float[]{
                g_x4, g_y1, 0,
                g_x1, g_y1, 0,
                g_x1, g_y7, 0,
                g_x4, g_y7, 0
        };
    }

    private float[] _squareBracketRight() {
        return new float[]{
                g_x1, g_y1, 0,
                g_x4, g_y1, 0,
                g_x4, g_y7, 0f,
                g_x1, g_y7, 0
        };
    }

    private float[] _curlyBracketLeft() {
        return new float[]{
                g_x4, g_y7, 0,
                g_x3, g_y7, 0,
                g_x1, g_y5, 0,
                g_x1, g_y3, 0,
                g_x3, g_y1, 0,
                g_x4, g_y1, 0
        };
    }

    private float[] _curlyBracketRight() {
        return new float[]{
                g_x1, g_y7, 0,
                g_x2, g_y7, 0,
                g_x4, g_y5, 0,
                g_x4, g_y3, 0,
                g_x2, g_y1, 0,
                g_x1, g_y1, 0
        };
    }

    private float[] _burger() {
        return new float[]{
                g_x2, g_y5, 0,
                g_x6, g_y5, 0,
                g_x6, g_y5, 0,
                g_x2, g_y3, 0,
                g_x2, g_y3, 0,
                g_x6, g_y3, 0,
                g_x6, g_y3, 0,
                g_x2, g_y1, 0,
                g_x2, g_y1, 0,
                g_x6, g_y1, 0
        };
    }
}
