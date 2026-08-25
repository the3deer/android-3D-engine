package org.the3deer.android.engine.text;

import org.the3deer.android.engine.model.Screen;
import org.the3deer.util.bean.Bean;

import java.util.HashMap;
import java.util.Map;

/**
 * Mono-size glyph implementation based on IBM 7x5 font.
 * 
 * height:  7 points
 * width:   5 points
 */
@Bean(name = "ibm7x5")
public class FontFactoryIBM75 implements FontFactory {

    public static final float SIZE_H = 0.5f;
    public static final float SIZE_V = 0.7f;

    private final static Map<Character, float[]> LETTERS = new HashMap<>();
    
    static {
        final float[] _0 = new float[]{0f, 0.2f, 0f, 0f, 0.1f, 0f, 0.1f, 0f, 0.3f, 0f, 0.4f, 0.1f, 0f, 0.4f, 0.5f, 0f, 0.3f, 0.6f, 0f, 0.1f, 0.6f, 0f, 0f, 0.5f, 0f, 0f, 0.2f, 0f, 0.4f, 0.5f, 0f};
        final float[] _1 = new float[]{0.1f, 0f, 0f, 0.3f, 0f, 0f, 0.2f, 0f, 0f, 0.2f, 0.6f, 0f, 0.1f, 0.5f, 0f};
        final float[] _2 = new float[]{0f, 0.5f, 0f, 0.1f, 0.6f, 0f, 0.3f, 0.6f, 0f, 0.4f, 0.5f, 0f, 0.4f, 0.4f, 0f, 0f, 0f, 0f, 0.4f, 0f, 0f};
        final float[] _3 = new float[]{0.0f, 0.6f, 0f, 0.4f, 0.6f, 0f, 0.2f, 0.4f, 0f, 0.4f, 0.2f, 0f, 0.4f, 0.1f, 0f, 0.3f, 0.0f, 0f, 0.1f, 0.0f, 0f, 0.0f, 0.1f, 0f};
        final float[] _4 = new float[]{0.3f, 0.0f, 0f, 0.3f, 0.6f, 0f, 0.0f, 0.3f, 0f, 0.0f, 0.2f, 0f, 0.4f, 0.2f, 0f};
        final float[] _5 = new float[]{0.4f, 0.6f, 0f, 0.0f, 0.6f, 0f, 0.0f, 0.4f, 0f, 0.3f, 0.4f, 0f, 0.4f, 0.3f, 0f, 0.4f, 0.1f, 0f, 0.3f, 0.0f, 0f, 0.0f, 0.0f, 0f};
        final float[] _6 = new float[]{0.3f, 0.6f, 0f, 0.2f, 0.6f, 0f, 0.0f, 0.4f, 0f, 0.0f, 0.1f, 0f, 0.1f, 0.0f, 0f, 0.3f, 0.0f, 0f, 0.4f, 0.1f, 0f, 0.4f, 0.2f, 0f, 0.3f, 0.3f, 0f, 0.0f, 0.3f, 0f};
        final float[] _7 = new float[]{0.0f, 0.6f, 0f, 0.4f, 0.6f, 0f, 0.4f, 0.5f, 0f, 0.1f, 0.2f, 0f, 0.1f, 0.0f, 0f};
        final float[] _8 = new float[]{0.1f, 0.3f, 0f, 0.0f, 0.2f, 0f, 0.0f, 0.1f, 0f, 0.1f, 0.0f, 0f, 0.3f, 0.0f, 0f, 0.4f, 0.1f, 0f, 0.4f, 0.2f, 0f, 0.3f, 0.3f, 0f, 0.1f, 0.3f, 0f, 0.0f, 0.4f, 0f, 0.0f, 0.5f, 0f, 0.1f, 0.6f, 0f, 0.3f, 0.6f, 0f, 0.4f, 0.5f, 0f, 0.4f, 0.4f, 0f, 0.3f, 0.3f, 0f};
        final float[] _9 = new float[]{0.1f, 0.0f, 0f, 0.2f, 0.0f, 0f, 0.4f, 0.3f, 0f, 0.4f, 0.5f, 0f, 0.3f, 0.6f, 0f, 0.1f, 0.6f, 0f, 0.0f, 0.5f, 0f, 0.0f, 0.4f, 0f, 0.1f, 0.3f, 0f, 0.4f, 0.3f, 0f};

        final float[] LETTER_a = new float[]{0.1f, 0.4f, 0f, 0.3f, 0.4f, 0f, 0.4f, 0.3f, 0f, 0.4f, 0.0f, 0f, 0.1f, 0.0f, 0f, 0.0f, 0.1f, 0f, 0.1f, 0.2f, 0f, 0.4f, 0.2f, 0f};
        final float[] LETTER_b = new float[]{0.0f, 0.0f, 0f, 0.0f, 0.7f, 0f, 0.0f, 0.3f, 0f, 0.1f, 0.3f, 0f, 0.2f, 0.4f, 0f, 0.3f, 0.4f, 0f, 0.4f, 0.3f, 0f, 0.4f, 0.1f, 0f, 0.3f, 0.0f, 0f, 0.2f, 0.0f, 0f, 0.1f, 0.1f, 0f, 0.0f, 0.1f, 0f};
        final float[] LETTER_c = new float[]{0.4f, 0.3f, 0f, 0.3f, 0.4f, 0f, 0.1f, 0.4f, 0f, 0.0f, 0.3f, 0f, 0.0f, 0.1f, 0f, 0.1f, 0.0f, 0f, 0.3f, 0.0f, 0f, 0.4f, 0.1f, 0f};
        final float[] LETTER_d = new float[]{0.4f, 0.6f, 0f, 0.4f, 0.0f, 0f, 0.4f, 0.2f, 0f, 0.2f, 0.4f, 0f, 0.1f, 0.4f, 0f, 0.0f, 0.3f, 0f, 0.0f, 0.1f, 0f, 0.1f, 0.0f, 0f, 0.2f, 0.0f, 0f, 0.4f, 0.2f, 0f};
        final float[] LETTER_e = new float[]{0.4f, 0.0f, 0f, 0.1f, 0.0f, 0f, 0.0f, 0.1f, 0f, 0.0f, 0.3f, 0f, 0.1f, 0.4f, 0f, 0.3f, 0.4f, 0f, 0.4f, 0.3f, 0f, 0.4f, 0.2f, 0f, 0.0f, 0.2f, 0f};
        final float[] LETTER_f = new float[]{0.1f, 0.0f, 0f, 0.1f, 0.3f, 0f, 0.0f, 0.3f, 0f, 0.2f, 0.3f, 0f, 0.1f, 0.3f, 0f, 0.1f, 0.5f, 0f, 0.2f, 0.6f, 0f, 0.3f, 0.6f, 0f, 0.4f, 0.5f, 0f};
        final float[] LETTER_g = new float[]{0.1f, 0.0f, 0f, 0.3f, 0.0f, 0f, 0.4f, 0.1f, 0f, 0.4f, 0.4f, 0f, 0.1f, 0.4f, 0f, 0.0f, 0.3f, 0f, 0.1f, 0.2f, 0f, 0.4f, 0.2f, 0f};
        final float[] LETTER_h = new float[]{0.0f, 0.6f, 0f, 0.0f, 0.0f, 0f, 0.0f, 0.2f, 0f, 0.2f, 0.4f, 0f, 0.3f, 0.4f, 0f, 0.4f, 0.3f, 0f, 0.4f, 0.0f, 0f};
        final float[] LETTER_i = new float[]{0.1f, 0f, 0f, 0.3f, 0f, 0f, 0.2f, 0f, 0f, 0.2f, 0.3f, 0f, 0.1f, 0.3f, 0f, 0.1f, 0.3f, 0f, 0.2f, 0.4f, 0f, 0.2f, 0.4f, 0f, 0.2f, 0.5f, 0f};
        final float[] LETTER_j = new float[]{0.1f, 0.1f, 0f, 0.2f, 0.0f, 0f, 0.3f, 0.1f, 0f, 0.3f, 0.4f, 0f, 0.3f, 0.4f, 0f, 0.3f, 0.5f, 0f, 0.3f, 0.5f, 0f, 0.3f, 0.6f, 0f};
        final float[] LETTER_k = new float[]{0.0f, 0.0f, 0f, 0.0f, 0.6f, 0f, 0.0f, 0.2f, 0f, 0.1f, 0.2f, 0f, 0.3f, 0.4f, 0f, 0.1f, 0.2f, 0f, 0.3f, 0.0f, 0f};
        final float[] LETTER_l = new float[]{0.1f, 0f, 0f, 0.3f, 0f, 0f, 0.2f, 0f, 0f, 0.2f, 0.6f, 0f, 0.1f, 0.6f, 0f};
        final float[] LETTER_m = new float[]{0.0f, 0.0f, 0f, 0.0f, 0.4f, 0f, 0.1f, 0.4f, 0f, 0.2f, 0.3f, 0f, 0.2f, 0.0f, 0f, 0.2f, 0.4f, 0f, 0.3f, 0.4f, 0f, 0.4f, 0.3f, 0f, 0.4f, 0.0f, 0f};
        final float[] LETTER_n = new float[]{0.0f, 0.4f, 0f, 0.0f, 0.0f, 0f, 0.0f, 0.2f, 0f, 0.2f, 0.4f, 0f, 0.3f, 0.4f, 0f, 0.4f, 0.3f, 0f, 0.4f, 0.0f, 0f};
        final float[] LETTER_o = new float[]{0.1f, 0.0f, 0f, 0.3f, 0.0f, 0f, 0.4f, 0.1f, 0f, 0.4f, 0.3f, 0f, 0.3f, 0.4f, 0f, 0.1f, 0.4f, 0f, 0.0f, 0.3f, 0f, 0.0f, 0.1f, 0f, 0.1f, 0.0f, 0f};
        final float[] LETTER_p = new float[]{0.0f, 0.0f, 0f, 0.0f, 0.4f, 0f, 0.3f, 0.4f, 0f, 0.4f, 0.3f, 0f, 0.3f, 0.2f, 0f, 0.0f, 0.2f, 0f};
        final float[] LETTER_q = new float[]{0.4f, 0.0f, 0f, 0.4f, 0.4f, 0f, 0.1f, 0.4f, 0f, 0.0f, 0.3f, 0f, 0.1f, 0.2f, 0f, 0.4f, 0.2f, 0f};
        final float[] LETTER_r = new float[]{0.0f, 0.4f, 0f, 0.0f, 0.0f, 0f, 0.0f, 0.2f, 0f, 0.2f, 0.4f, 0f, 0.3f, 0.4f, 0f, 0.4f, 0.3f, 0f};
        final float[] LETTER_s = new float[]{0.4f, 0.4f, 0f, 0.1f, 0.4f, 0f, 0.0f, 0.3f, 0f, 0.1f, 0.2f, 0f, 0.3f, 0.2f, 0f, 0.4f, 0.1f, 0f, 0.3f, 0.0f, 0f, 0.0f, 0.0f, 0f};
        final float[] LETTER_t = new float[]{0.1f, 0.6f, 0f, 0.1f, 0.4f, 0f, 0.2f, 0.4f, 0f, 0.0f, 0.4f, 0f, 0.1f, 0.4f, 0f, 0.1f, 0.1f, 0f, 0.2f, 0.0f, 0f, 0.3f, 0.0f, 0f, 0.4f, 0.1f, 0f};
        final float[] LETTER_u = new float[]{0.0f, 0.4f, 0f, 0.0f, 0.1f, 0f, 0.1f, 0.0f, 0f, 0.2f, 0.0f, 0f, 0.4f, 0.2f, 0f, 0.4f, 0.4f, 0f, 0.4f, 0.0f, 0f};
        final float[] LETTER_v = new float[]{0.0f, 0.4f, 0f, 0.2f, 0.0f, 0f, 0.4f, 0.4f, 0f};
        final float[] LETTER_w = new float[]{0.0f, 0.4f, 0f, 0.0f, 0.1f, 0f, 0.1f, 0.0f, 0f, 0.2f, 0.1f, 0f, 0.2f, 0.2f, 0f, 0.2f, 0.1f, 0f, 0.3f, 0.0f, 0f, 0.4f, 0.1f, 0f, 0.4f, 0.4f, 0f};
        final float[] LETTER_x = new float[]{0.0f, 0.4f, 0f, 0.4f, 0.0f, 0f, 0.2f, 0.2f, 0f, 0.4f, 0.4f, 0f, 0.0f, 0.0f, 0f, 0.0f, 0.0f, 0f};
        final float[] LETTER_y = new float[]{0.0f, 0.4f, 0f, 0.0f, 0.3f, 0f, 0.1f, 0.2f, 0f, 0.4f, 0.2f, 0f, 0.4f, 0.4f, 0f, 0.4f, 0.1f, 0f, 0.3f, 0.0f, 0f, 0.1f, 0.0f, 0f};
        final float[] LETTER_z = new float[]{0.0f, 0.4f, 0f, 0.4f, 0.4f, 0f, 0.0f, 0.0f, 0f, 0.4f, 0.0f, 0f};

        final float[] LETTER_A = new float[]{0.0f, 0.0f, 0f, 0.0f, 0.5f, 0f, 0.1f, 0.6f, 0f, 0.3f, 0.6f, 0f, 0.4f, 0.5f, 0f, 0.4f, 0.0f, 0f, 0.4f, 0.2f, 0f, 0.0f, 0.2f, 0f};
        final float[] LETTER_B = new float[]{0.0f, 0.0f, 0f, 0.0f, 0.6f, 0f, 0.3f, 0.6f, 0f, 0.4f, 0.5f, 0f, 0.4f, 0.4f, 0f, 0.3f, 0.3f, 0f, 0.0f, 0.3f, 0f, 0.3f, 0.3f, 0f, 0.4f, 0.2f, 0f, 0.4f, 0.1f, 0f, 0.3f, 0.0f, 0f, 0.0f, 0.0f, 0f};
        final float[] LETTER_C = new float[]{0.4f, 0.5f, 0f, 0.3f, 0.6f, 0f, 0.1f, 0.6f, 0f, 0.0f, 0.5f, 0f, 0.0f, 0.1f, 0f, 0.1f, 0.0f, 0f, 0.3f, 0.0f, 0f, 0.4f, 0.1f, 0f};
        final float[] LETTER_L = new float[]{0.0f, 0.6f, 0f, 0.0f, 0.0f, 0f, 0.4f, 0.0f, 0f};
        final float[] LETTER_M = new float[]{0.0f, 0.0f, 0f, 0.0f, 0.6f, 0f, 0.2f, 0.3f, 0f, 0.5f, 0.6f, 0f, 0.5f, 0.0f, 0f};
        final float[] LETTER_P = new float[]{0.0f, 0.0f, 0f, 0.0f, 0.6f, 0f, 0.3f, 0.6f, 0f, 0.4f, 0.5f, 0f, 0.4f, 0.3f, 0f, 0.3f, 0.3f, 0f, 0.0f, 0.3f, 0f};
        final float[] LETTER_T = new float[]{0.2f, 0.0f, 0f, 0.2f, 0.6f, 0f, 0.0f, 0.6f, 0f, 0.4f, 0.6f, 0f};
        final float[] LETTER_V = new float[]{0.0f, 0.6f, 0f, 0.2f, 0.0f, 0f, 0.4f, 0.6f, 0f};
        final float[] LETTER_X = new float[]{0.0f, 0.6f, 0f, 0.0f, 0.5f, 0f, 0.4f, 0.1f, 0f, 0.4f, 0.0f, 0f, 0.4f, 0.1f, 0f, 0.2f, 0.3f, 0f, 0.4f, 0.5f, 0f, 0.4f, 0.6f, 0f, 0.4f, 0.5f, 0f, 0.0f, 0.1f, 0f, 0.0f, 0.0f, 0f};

        final float[] SYMBOL_MINUS = new float[]{0.0f, 0.3f, 0f, 0.5f, 0.3f, 0f};
        final float[] SYMBOL_POINT = new float[]{0.1f, 0.1f, 0f, 0.1f, 0.2f, 0f, 0.2f, 0.2f, 0f, 0.2f, 0.1f, 0f, 0.1f, 0.1f, 0f};
        final float[] SYMBOL_COMMA = new float[]{0.1f, 0.0f, 0f, 0.2f, 0.1f, 0f, 0.2f, 0.2f, 0f};
        final float[] SYMBOL_COLON = new float[]{0.1f, 0.0f, 0f, 0.1f, 0.2f, 0f, 0.1f, 0.2f, 0f, 0.1f, 0.3f, 0f, 0.1f, 0.3f, 0f, 0.1f, 0.5f, 0f};
        final float[] SYMBOL_SQUARE_BRACKET_LEFT = new float[]{0.2f, 0.0f, 0f, 0.0f, 0.0f, 0f, 0.0f, 0.6f, 0f, 0.2f, 0.6f, 0f};
        final float[] SYMBOL_SQUARE_BRACKET_RIGHT = new float[]{0.2f, 0.0f, 0f, 0.4f, 0.0f, 0f, 0.4f, 0.6f, 0f, 0.2f, 0.6f, 0f};

        LETTERS.put('-', SYMBOL_MINUS);
        LETTERS.put('.', SYMBOL_POINT);
        LETTERS.put(',', SYMBOL_COMMA);
        LETTERS.put(':', SYMBOL_COLON);
        LETTERS.put('[', SYMBOL_SQUARE_BRACKET_LEFT);
        LETTERS.put(']', SYMBOL_SQUARE_BRACKET_RIGHT);

        LETTERS.put('0', _0);
        LETTERS.put('1', _1);
        LETTERS.put('2', _2);
        LETTERS.put('3', _3);
        LETTERS.put('4', _4);
        LETTERS.put('5', _5);
        LETTERS.put('6', _6);
        LETTERS.put('7', _7);
        LETTERS.put('8', _8);
        LETTERS.put('9', _9);

        LETTERS.put('a', LETTER_a);
        LETTERS.put('b', LETTER_b);
        LETTERS.put('c', LETTER_c);
        LETTERS.put('d', LETTER_d);
        LETTERS.put('e', LETTER_e);
        LETTERS.put('f', LETTER_f);
        LETTERS.put('g', LETTER_g);
        LETTERS.put('h', LETTER_h);
        LETTERS.put('i', LETTER_i);
        LETTERS.put('j', LETTER_j);
        LETTERS.put('k', LETTER_k);
        LETTERS.put('m', LETTER_m);
        LETTERS.put('n', LETTER_n);
        LETTERS.put('l', LETTER_l);
        LETTERS.put('o', LETTER_o);
        LETTERS.put('p', LETTER_p);
        LETTERS.put('q', LETTER_q);
        LETTERS.put('r', LETTER_r);
        LETTERS.put('s', LETTER_s);
        LETTERS.put('t', LETTER_t);
        LETTERS.put('u', LETTER_u);
        LETTERS.put('v', LETTER_v);
        LETTERS.put('w', LETTER_w);
        LETTERS.put('x', LETTER_x);
        LETTERS.put('y', LETTER_y);
        LETTERS.put('z', LETTER_z);

        LETTERS.put('A', LETTER_A);
        LETTERS.put('B', LETTER_B);
        LETTERS.put('C', LETTER_C);
        LETTERS.put('L', LETTER_L);
        LETTERS.put('M', LETTER_M);
        LETTERS.put('P', LETTER_P);
        LETTERS.put('T', LETTER_T);
        LETTERS.put('V', LETTER_V);
        LETTERS.put('X', LETTER_X);
    }

    @Override
    public void setScreen(Screen screen) {}

    @Override
    public float getCharWidth() { return SIZE_H; }

    @Override
    public float getCharHeight() { return SIZE_V; }

    @Override
    public float[] getChar(char c) {
        return LETTERS.get(c);
    }

    @Override
    public float[] getSymbol(String s) {
        if (s == null) return null;
        if (s.length() == 1) return getChar(s.charAt(0));
        return null;
    }
}
