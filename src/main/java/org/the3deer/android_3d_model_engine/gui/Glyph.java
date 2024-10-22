package org.the3deer.android_3d_model_engine.gui;

import android.util.Log;

import org.the3deer.android_3d_model_engine.model.Constants;
import org.the3deer.util.io.IOUtils;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Mono-size glyph
 * <p>
 * height:  7 points
 * width:   5 points
 * max vertices: 75
 */
public class Glyph extends Widget {

    public static final float SIZE_H = 0.5f;
    public static final float SIZE_V = 0.7f;
    // floats needed for vertices
    static final int VSIZE = 25 * 3;
    // floats needed for colors
    static final int CSIZE = 25 * 4;

    public static final int MENU = 1000;
    public static final int MENU_NEW = 1001;
    public static final int CHECKBOX_OFF = 2000;
    public static final int CHECKBOX_ON = 2001;
    static final int GLYPH_LESS_THAN_CODE = 3000;
    static final int GLYPH_GREATER_THAN_CODE = 3001;
    final static float[] LETTER_X = new float[]{
            0.0f, 0.6f, 0f,
            0.0f, 0.5f, 0f,
            0.4f, 0.1f, 0f,
            0.4f, 0.0f, 0f,
            0.4f, 0.1f, 0f,
            0.2f, 0.3f, 0f,
            0.4f, 0.5f, 0f,
            0.4f, 0.6f, 0f,
            0.4f, 0.5f, 0f,
            0.0f, 0.1f, 0f,
            0.0f, 0.0f, 0f
    };
    private final static Map<Character, float[]> LETTERS = new HashMap<>();

    private final static float[] GLYPH_BOX = new float[]{
            0.0f, 0.0f, 0.0f,
            0.0f, 0.6f, 0.0f,
            0.4f, 0.6f, 0.0f,
            0.4f, 0.0f, 0.0f,
            0.0f, 0.0f, 0.0f
    };

    private final static float[] GLYPH_LESS_THAN = new float[]{
            0.3f, 0.0f, 0.0f,
            0.1f, 0.3f, 0.0f,
            0.3f, 0.6f, 0.0f
    };

    private final static float[] GLYPH_GREATER_THAN = new float[]{
            0.1f, 0.0f, 0.0f,
            0.3f, 0.3f, 0.0f,
            0.1f, 0.6f, 0.0f
    };
    final static float[] SYMBOL_SQUARE_BRACKET_LEFT = new float[]{
            0.2f, 0.0f, 0f,
            0.0f, 0.0f, 0f,
            0.0f, 0.6f, 0f,
            0.2f, 0.6f, 0f,
    };
    final static float[] SYMBOL_SQUARE_BRACKET_RIGHT = new float[]{
            0.2f, 0.0f, 0f,
            0.4f, 0.0f, 0f,
            0.4f, 0.6f, 0f,
            0.2f, 0.6f, 0f,
    };
    final static float[] SYMBOL_MINUS = new float[]{
            0.0f, 0.3f, 0f,
            0.5f, 0.3f, 0f,
    };
    final static float[] SYMBOL_POINT = new float[]{
            0.1f, 0.1f, 0f,
            0.1f, 0.2f, 0f,
            0.2f, 0.2f, 0f,
            0.2f, 0.1f, 0f,
            0.1f, 0.1f, 0f,
    };
    final static float[] SYMBOL_COMMA = new float[]{
            0.1f, 0.0f, 0f,
            0.2f, 0.1f, 0f,
            0.2f, 0.2f, 0f,
    };
    final static float[] SYMBOL_COLON = new float[]{
            0.1f, 0.0f, 0f,
            0.1f, 0.2f, 0f,
            0.1f, 0.2f, 0f,
            0.1f, 0.3f, 0f,
            0.1f, 0.3f, 0f,
            0.1f, 0.5f, 0f
    };
    final static float[] _0 = new float[]{
            0f, 0.2f, 0f,
            0f, 0.1f, 0f,
            0.1f, 0f, 0f,
            0.3f, 0f, 0f,
            0.4f, 0.1f, 0f,
            0.4f, 0.5f, 0f,
            0.3f, 0.6f, 0f,
            0.1f, 0.6f, 0f,
            0f, 0.5f, 0f,
            0f, 0.2f, 0f,
            0.4f, 0.5f, 0f
    };
    final static float[] _1 = new float[]{
            0.1f, 0f, 0f,
            0.3f, 0f, 0f,
            0.2f, 0f, 0f,
            0.2f, 0.6f, 0f,
            0.1f, 0.5f, 0f
    };
    final static float[] LETTER_l = new float[]{
            0.1f, 0f, 0f,
            0.3f, 0f, 0f,
            0.2f, 0f, 0f,
            0.2f, 0.6f, 0f,
            0.1f, 0.6f, 0f
    };
    final static float[] LETTER_i = new float[]{
            0.1f, 0f, 0f,
            0.3f, 0f, 0f,
            0.2f, 0f, 0f,
            0.2f, 0.3f, 0f,
            0.1f, 0.3f, 0f,
            0.1f, 0.3f, 0f,
            0.2f, 0.4f, 0f,
            0.2f, 0.4f, 0f,
            0.2f, 0.5f, 0f,
    };
    final static float[] LETTER_j = new float[]{
            0.1f, 0.1f, 0f,
            0.2f, 0.0f, 0f,
            0.3f, 0.1f, 0f,
            0.3f, 0.4f, 0f,
            0.3f, 0.4f, 0f,
            0.3f, 0.5f, 0f,
            0.3f, 0.5f, 0f,
            0.3f, 0.6f, 0f,
    };
    final static float[] LETTER_k = new float[]{
            0.0f, 0.0f, 0f,
            0.0f, 0.6f, 0f,
            0.0f, 0.2f, 0f,
            0.1f, 0.2f, 0f,
            0.3f, 0.4f, 0f,
            0.1f, 0.2f, 0f,
            0.3f, 0.0f, 0f,
    };
    final static float[] LETTER_m = new float[]{
            0.0f, 0.0f, 0f,
            0.0f, 0.4f, 0f,
            0.1f, 0.4f, 0f,
            0.2f, 0.3f, 0f,
            0.2f, 0.0f, 0f,
            0.2f, 0.4f, 0f,
            0.3f, 0.4f, 0f,
            0.4f, 0.3f, 0f,
            0.4f, 0.0f, 0f
    };
    final static float[] _2 = new float[]{
            0f, 0.5f, 0f,
            0.1f, 0.6f, 0f,
            0.3f, 0.6f, 0f,
            0.4f, 0.5f, 0f,
            0.4f, 0.4f, 0f,
            0f, 0f, 0f,
            0.4f, 0f, 0f,
    };
    final static float[] _3 = new float[]{
            0.0f, 0.6f, 0f,
            0.4f, 0.6f, 0f,
            0.2f, 0.4f, 0f,
            0.4f, 0.2f, 0f,
            0.4f, 0.1f, 0f,
            0.3f, 0.0f, 0f,
            0.1f, 0.0f, 0f,
            0.0f, 0.1f, 0f
    };
    final static float[] _4 = new float[]{
            0.3f, 0.0f, 0f,
            0.3f, 0.6f, 0f,
            0.0f, 0.3f, 0f,
            0.0f, 0.2f, 0f,
            0.4f, 0.2f, 0f,
    };
    final static float[] _5 = new float[]{
            0.4f, 0.6f, 0f,
            0.0f, 0.6f, 0f,
            0.0f, 0.4f, 0f,
            0.3f, 0.4f, 0f,
            0.4f, 0.3f, 0f,
            0.4f, 0.1f, 0f,
            0.3f, 0.0f, 0f,
            0.0f, 0.0f, 0f,
    };
    final static float[] _6 = new float[]{
            0.3f, 0.6f, 0f,
            0.2f, 0.6f, 0f,
            0.0f, 0.4f, 0f,
            0.0f, 0.1f, 0f,
            0.1f, 0.0f, 0f,
            0.3f, 0.0f, 0f,
            0.4f, 0.1f, 0f,
            0.4f, 0.2f, 0f,
            0.3f, 0.3f, 0f,
            0.0f, 0.3f, 0f,
    };
    final static float[] _7 = new float[]{
            0.0f, 0.6f, 0f,
            0.4f, 0.6f, 0f,
            0.4f, 0.5f, 0f,
            0.1f, 0.2f, 0f,
            0.1f, 0.0f, 0f,
    };
    final static float[] _8 = new float[]{
            0.1f, 0.3f, 0f,
            0.0f, 0.2f, 0f,
            0.0f, 0.1f, 0f,
            0.1f, 0.0f, 0f,
            0.3f, 0.0f, 0f,
            0.4f, 0.1f, 0f,
            0.4f, 0.2f, 0f,
            0.3f, 0.3f, 0f,
            0.1f, 0.3f, 0f,
            0.0f, 0.4f, 0f,
            0.0f, 0.5f, 0f,
            0.1f, 0.6f, 0f,
            0.3f, 0.6f, 0f,
            0.4f, 0.5f, 0f,
            0.4f, 0.4f, 0f,
            0.3f, 0.3f, 0f,
    };
    final static float[] _9 = new float[]{
            0.1f, 0.0f, 0f,
            0.2f, 0.0f, 0f,
            0.4f, 0.3f, 0f,
            0.4f, 0.5f, 0f,
            0.3f, 0.6f, 0f,
            0.1f, 0.6f, 0f,
            0.0f, 0.5f, 0f,
            0.0f, 0.4f, 0f,
            0.1f, 0.3f, 0f,
            0.4f, 0.3f, 0f,
    };
    final static float[] LETTER_c = new float[]{
            0.4f, 0.3f, 0f,
            0.3f, 0.4f, 0f,
            0.1f, 0.4f, 0f,
            0.0f, 0.3f, 0f,
            0.0f, 0.1f, 0f,
            0.1f, 0.0f, 0f,
            0.3f, 0.0f, 0f,
            0.4f, 0.1f, 0f,
    };
    final static float[] LETTER_d = new float[]{
            0.4f, 0.6f, 0f,
            0.4f, 0.0f, 0f,
            0.4f, 0.2f, 0f,
            0.2f, 0.4f, 0f,
            0.1f, 0.4f, 0f,
            0.0f, 0.3f, 0f,
            0.0f, 0.1f, 0f,
            0.1f, 0.0f, 0f,
            0.2f, 0.0f, 0f,
            0.4f, 0.2f, 0f,
    };
    final static float[] f = new float[]{
            0.1f, 0.0f, 0f,
            0.1f, 0.3f, 0f,
            0.0f, 0.3f, 0f,
            0.2f, 0.3f, 0f,
            0.1f, 0.3f, 0f,
            0.1f, 0.5f, 0f,
            0.2f, 0.6f, 0f,
            0.3f, 0.6f, 0f,
            0.4f, 0.5f, 0f,
    };
    final static float[] p = new float[]{
            0.0f, 0.0f, 0f,
            0.0f, 0.4f, 0f,
            0.3f, 0.4f, 0f,
            0.4f, 0.3f, 0f,
            0.3f, 0.2f, 0f,
            0.0f, 0.2f, 0f,
    };
    final static float[] LETTER_q = new float[]{
            0.4f, 0.0f, 0f,
            0.4f, 0.4f, 0f,
            0.1f, 0.4f, 0f,
            0.0f, 0.3f, 0f,
            0.1f, 0.2f, 0f,
            0.4f, 0.2f, 0f,
    };
    final static float[] s = new float[]{
            0.4f, 0.4f, 0f,
            0.1f, 0.4f, 0f,
            0.0f, 0.3f, 0f,
            0.1f, 0.2f, 0f,
            0.3f, 0.2f, 0f,
            0.4f, 0.1f, 0f,
            0.3f, 0.0f, 0f,
            0.0f, 0.0f, 0f,
    };
    final static float[] LETTER_o = new float[]{
            0.1f, 0.0f, 0f,
            0.3f, 0.0f, 0f,
            0.4f, 0.1f, 0f,
            0.4f, 0.3f, 0f,
            0.3f, 0.4f, 0f,
            0.1f, 0.4f, 0f,
            0.0f, 0.3f, 0f,
            0.0f, 0.1f, 0f,
            0.1f, 0.0f, 0f,
    };
    final static float[] LETTER_e = new float[]{
            0.4f, 0.0f, 0f,
            0.1f, 0.0f, 0f,
            0.0f, 0.1f, 0f,
            0.0f, 0.3f, 0f,
            0.1f, 0.4f, 0f,
            0.3f, 0.4f, 0f,
            0.4f, 0.3f, 0f,
            0.4f, 0.2f, 0f,
            0.0f, 0.2f, 0f
    };
    final static float[] LETTER_g = new float[]{
            0.1f, 0.0f, 0f,
            0.3f, 0.0f, 0f,
            0.4f, 0.1f, 0f,
            0.4f, 0.4f, 0f,
            0.1f, 0.4f, 0f,
            0.0f, 0.3f, 0f,
            0.1f, 0.2f, 0f,
            0.4f, 0.2f, 0f,
    };
    final static float[] LETTER_h = new float[]{
            0.0f, 0.6f, 0f,
            0.0f, 0.0f, 0f,
            0.0f, 0.2f, 0f,
            0.2f, 0.4f, 0f,
            0.3f, 0.4f, 0f,
            0.4f, 0.3f, 0f,
            0.4f, 0.0f, 0f,
    };
    final static float[] LETTER_n = new float[]{
            0.0f, 0.4f, 0f,
            0.0f, 0.0f, 0f,
            0.0f, 0.2f, 0f,
            0.2f, 0.4f, 0f,
            0.3f, 0.4f, 0f,
            0.4f, 0.3f, 0f,
            0.4f, 0.0f, 0f,
    };
    final static float[] LETTER_t = new float[]{
            0.1f, 0.6f, 0f,
            0.1f, 0.4f, 0f,
            0.2f, 0.4f, 0f,
            0.0f, 0.4f, 0f,
            0.1f, 0.4f, 0f,
            0.1f, 0.1f, 0f,
            0.2f, 0.0f, 0f,
            0.3f, 0.0f, 0f,
            0.4f, 0.1f, 0f,
    };
    final static float[] LETTER_x = new float[]{
            0.0f, 0.4f, 0f,
            0.4f, 0.0f, 0f,
            0.2f, 0.2f, 0f,
            0.4f, 0.4f, 0f,
            0.0f, 0.0f, 0f,
            0.0f, 0.0f, 0f,
    };
    final static float[] LETTER_y = new float[]{
            0.0f, 0.4f, 0f,
            0.0f, 0.3f, 0f,
            0.1f, 0.2f, 0f,
            0.4f, 0.2f, 0f,
            0.4f, 0.4f, 0f,
            0.4f, 0.1f, 0f,
            0.3f, 0.0f, 0f,
            0.1f, 0.0f, 0f,
    };
    final static float[] LETTER_z = new float[]{
            0.0f, 0.4f, 0f,
            0.4f, 0.4f, 0f,
            0.0f, 0.0f, 0f,
            0.4f, 0.0f, 0f
    };
    final static float[] LETTER_A = new float[]{
            0.0f, 0.0f, 0f,
            0.0f, 0.5f, 0f,
            0.1f, 0.6f, 0f,
            0.3f, 0.6f, 0f,
            0.4f, 0.5f, 0f,
            0.4f, 0.0f, 0f,
            0.4f, 0.2f, 0f,
            0.0f, 0.2f, 0f
    };
    final static float[] LETTER_B = new float[]{
            0.0f, 0.0f, 0f,
            0.0f, 0.6f, 0f,
            0.3f, 0.6f, 0f,
            0.4f, 0.5f, 0f,
            0.4f, 0.4f, 0f,
            0.3f, 0.3f, 0f,
            0.0f, 0.3f, 0f,
            0.3f, 0.3f, 0f,
            0.4f, 0.2f, 0f,
            0.4f, 0.1f, 0f,
            0.3f, 0.0f, 0f,
            0.0f, 0.0f, 0f,

    };
    final static float[] LETTER_C = new float[]{
            0.4f, 0.5f, 0f,
            0.3f, 0.6f, 0f,
            0.1f, 0.6f, 0f,
            0.0f, 0.5f, 0f,
            0.0f, 0.1f, 0f,
            0.1f, 0.0f, 0f,
            0.3f, 0.0f, 0f,
            0.4f, 0.1f, 0f,

    };
    final static float[] LETTER_L = new float[]{
            0.0f, 0.6f, 0f,
            0.0f, 0.0f, 0f,
            0.4f, 0.0f, 0f,
    };
    final static float[] LETTER_M = new float[]{
            0.0f, 0.0f, 0f,
            0.0f, 0.6f, 0f,
            0.2f, 0.3f, 0f,
            0.5f, 0.6f, 0f,
            0.5f, 0.0f, 0f,
    };
    final static float[] LETTER_P = new float[]{
            0.0f, 0.0f, 0f,
            0.0f, 0.6f, 0f,
            0.3f, 0.6f, 0f,
            0.4f, 0.5f, 0f,
            0.4f, 0.3f, 0f,
            0.3f, 0.3f, 0f,
            0.0f, 0.3f, 0f,
    };
    final static float[] LETTER_T = new float[]{
            0.2f, 0.0f, 0f,
            0.2f, 0.6f, 0f,
            0.0f, 0.6f, 0f,
            0.4f, 0.6f, 0f,
    };
    final static float[] LETTER_V = new float[]{
            0.0f, 0.6f, 0f,
            0.2f, 0.0f, 0f,
            0.4f, 0.6f, 0f
    };
    final static float[] LETTER_w = new float[]{
            0.0f, 0.4f, 0f,
            0.0f, 0.1f, 0f,
            0.1f, 0.0f, 0f,
            0.2f, 0.1f, 0f,
            0.2f, 0.2f, 0f,
            0.2f, 0.1f, 0f,
            0.3f, 0.0f, 0f,
            0.4f, 0.1f, 0f,
            0.4f, 0.4f, 0f,
    };
    final static float[] LETTER_u = new float[]{
            0.0f, 0.4f, 0f,
            0.0f, 0.1f, 0f,
            0.1f, 0.0f, 0f,
            0.2f, 0.0f, 0f,
            0.4f, 0.2f, 0f,
            0.4f, 0.4f, 0f,
            0.4f, 0.0f, 0f,
    };
    final static float[] LETTER_v = new float[]{
            0.0f, 0.4f, 0f,
            0.2f, 0.0f, 0f,
            0.4f, 0.4f, 0f,
    };
    final static float[] LETTER_r = new float[]{
            0.0f, 0.4f, 0f,
            0.0f, 0.0f, 0f,
            0.0f, 0.2f, 0f,
            0.2f, 0.4f, 0f,
            0.3f, 0.4f, 0f,
            0.4f, 0.3f, 0f,
    };
    final static float[] LETTER_a = new float[]{
            0.1f, 0.4f, 0f,
            0.3f, 0.4f, 0f,
            0.4f, 0.3f, 0f,
            0.4f, 0.0f, 0f,
            0.1f, 0.0f, 0f,
            0.0f, 0.1f, 0f,
            0.1f, 0.2f, 0f,
            0.4f, 0.2f, 0f,
    };
    final static float[] LETTER_b = new float[]{
            0.0f, 0.0f, 0f,
            0.0f, 0.7f, 0f,
            0.0f, 0.3f, 0f,
            0.1f, 0.3f, 0f,
            0.2f, 0.4f, 0f,
            0.3f, 0.4f, 0f,
            0.4f, 0.3f, 0f,
            0.4f, 0.1f, 0f,
            0.3f, 0.0f, 0f,
            0.2f, 0.0f, 0f,
            0.1f, 0.1f, 0f,
            0.0f, 0.1f, 0f
    };

    static {

        LETTERS.put('-', Glyph.SYMBOL_MINUS);
        LETTERS.put('.', Glyph.SYMBOL_POINT);
        LETTERS.put(',', Glyph.SYMBOL_COMMA);
        LETTERS.put(':', Glyph.SYMBOL_COLON);
        LETTERS.put('[', Glyph.SYMBOL_SQUARE_BRACKET_LEFT);
        LETTERS.put(']', Glyph.SYMBOL_SQUARE_BRACKET_RIGHT);

        LETTERS.put('0', Glyph._0);
        LETTERS.put('1', Glyph._1);
        LETTERS.put('2', Glyph._2);
        LETTERS.put('3', Glyph._3);
        LETTERS.put('4', Glyph._4);
        LETTERS.put('5', Glyph._5);
        LETTERS.put('6', Glyph._6);
        LETTERS.put('7', Glyph._7);
        LETTERS.put('8', Glyph._8);
        LETTERS.put('9', Glyph._9);

        LETTERS.put('a', Glyph.LETTER_a);
        LETTERS.put('b', Glyph.LETTER_b);
        LETTERS.put('c', Glyph.LETTER_c);
        LETTERS.put('d', Glyph.LETTER_d);
        LETTERS.put('e', Glyph.LETTER_e);
        LETTERS.put('f', Glyph.f);
        LETTERS.put('g', Glyph.LETTER_g);
        LETTERS.put('h', Glyph.LETTER_h);
        LETTERS.put('i', Glyph.LETTER_i);
        LETTERS.put('j', Glyph.LETTER_j);
        LETTERS.put('k', Glyph.LETTER_k);
        LETTERS.put('m', Glyph.LETTER_m);
        LETTERS.put('n', Glyph.LETTER_n);
        LETTERS.put('l', Glyph.LETTER_l);
        LETTERS.put('p', Glyph.p);
        LETTERS.put('q', Glyph.LETTER_q);
        LETTERS.put('r', Glyph.LETTER_r);
        LETTERS.put('s', Glyph.s);
        LETTERS.put('o', Glyph.LETTER_o);
        LETTERS.put('t', Glyph.LETTER_t);
        LETTERS.put('u', Glyph.LETTER_u);
        LETTERS.put('v', Glyph.LETTER_v);
        LETTERS.put('w', Glyph.LETTER_w);
        LETTERS.put('x', Glyph.LETTER_x);
        LETTERS.put('y', Glyph.LETTER_y);
        LETTERS.put('z', Glyph.LETTER_z);

        LETTERS.put('A', Glyph.LETTER_A);
        LETTERS.put('B', Glyph.LETTER_B);
        LETTERS.put('C', Glyph.LETTER_C);
        LETTERS.put('L', Glyph.LETTER_L);
        LETTERS.put('M', Glyph.LETTER_M);
        LETTERS.put('P', Glyph.LETTER_P);
        LETTERS.put('T', Glyph.LETTER_T);
        LETTERS.put('V', Glyph.LETTER_V);
        LETTERS.put('X', Glyph.LETTER_X);
    }


    private int code;

    private Glyph(Widget parent, int code) {
        super(parent);
        this.code = code;
        this.init();
    }

    public static float[] getChar(char c) {
        return LETTERS.get(c);
    }

    public static float[] getCharNew(char c) {
        float[] ld = LETTERS.get(c);
        return ld;
    }

    public int getCode() {
        return code;
    }

    public static Glyph build(Widget parent, int code) {
        return new Glyph(parent, code);
    }

    @Override
    public void init() {
        try {
            // buffers
            final FloatBuffer vertexBuffer = IOUtils.createFloatBuffer(VSIZE);
            final FloatBuffer colorBuffer = IOUtils.createFloatBuffer(CSIZE);

            // build
            build(vertexBuffer, 0, colorBuffer, this.code, Constants.COLOR_WHITE, 0f, 0f, 0f);

            // setup
            setVertexBuffer(vertexBuffer);
            setColorsBuffer(colorBuffer);
        } catch (Exception e) {
            Log.e("Glyph", e.getMessage(), e);
        }
    }

    public static void build(FloatBuffer vertexBuffer, int idx, Buffer colorBuffer,
                             int code, float[] color, float offsetX, float offsetY, float offsetZ) {

        /// new vertices added
        int coffset = (idx / 3) * 4;

        switch (code) {
            case CHECKBOX_OFF:
                IOUtils.fill(vertexBuffer, idx, VSIZE, 0);
                IOUtils.fill(colorBuffer, coffset, CSIZE, 0);
                build(vertexBuffer, idx, colorBuffer, LETTER_x, color, offsetX, offsetY, offsetZ);
                break;
            case CHECKBOX_ON:
                IOUtils.fill(vertexBuffer, idx, VSIZE, 0);
                IOUtils.fill(colorBuffer, coffset, CSIZE, 0);
                build(vertexBuffer, idx, colorBuffer, LETTER_X, color, offsetX, offsetY, offsetZ);
                break;
            case GLYPH_LESS_THAN_CODE:
                build(vertexBuffer, idx, colorBuffer, GLYPH_LESS_THAN, color, offsetX, offsetY, offsetZ);
                break;
            case GLYPH_GREATER_THAN_CODE:
                build(vertexBuffer, idx, colorBuffer, GLYPH_GREATER_THAN, color, offsetX, offsetY, offsetZ);
                break;
            case MENU_NEW:
                /*int newVerts2 = line(vertexBuffer, idx, colorBuffer, Constants.COLOR_WHITE,
                        0f, g_y5,
                        g_x4, g_y5);
                idx += newVerts2 * 3;
                coffset += newVerts2 * 4;
                newVerts2 = line(vertexBuffer, idx, colorBuffer, Constants.COLOR_RED,
                        0f, g_y3,
                        g_x4, g_y3);
                idx += newVerts2 * 3;
                coffset += newVerts2 * 4;
                newVerts2 = line(vertexBuffer, idx, colorBuffer, Constants.COLOR_GREEN,
                        0f, g_y1,
                        g_x4, g_y1);*//*
                idx += newVerts2 * 3;
                coffset += newVerts2 * 4;
                fillArray(vertexBuffer, idx, VSIZE, 0);
                fillArray(colorBuffer, coffset, CSIZE, 0);*/
                break;
            case MENU:
                int newVerts = line(vertexBuffer, idx, colorBuffer, Constants.COLOR_WHITE,
                        0f, 0.5f,
                        0.4f, 0.5f);
                idx += newVerts * 3;
                coffset += newVerts * 4;
                newVerts = line(vertexBuffer, idx, colorBuffer, Constants.COLOR_WHITE,
                        0f, 0.3f,
                        0.4f, 0.3f);
                idx += newVerts * 3;
                coffset += newVerts * 4;
                newVerts = line(vertexBuffer, idx, colorBuffer, Constants.COLOR_WHITE,
                        0f, 0.1f,
                        0.4f, 0.1f);
                idx += newVerts * 3;
                coffset += newVerts * 4;
                IOUtils.fill(vertexBuffer, idx, VSIZE, 0);
                IOUtils.fill(colorBuffer, coffset, CSIZE, 0);
                break;
            default:
                break;
        }
    }

    static int build(FloatBuffer vertexBuffer, int idx, Buffer colorBuffer, float offsetX, float offsetY, float[] data, float[] color) {

        if (data == null) return idx;

        int idxColor = (idx / 3) * 4;

        vertexBuffer.put(idx++, data[0] + offsetX);
        vertexBuffer.put(idx++, data[1] + offsetY);
        vertexBuffer.put(idx++, data[2] + GUIConstants.UI_TEXT_Z_HIDDEN);

        ((FloatBuffer)colorBuffer).put(idxColor++, 0f);
        ((FloatBuffer)colorBuffer).put(idxColor++, 1f);
        ((FloatBuffer)colorBuffer).put(idxColor++, 0f);
        ((FloatBuffer)colorBuffer).put(idxColor++, 0f);

        boolean lastblind = false;
        int isBlind = 0;
        for (int i = 0; i < data.length; i += 3) {
            vertexBuffer.put(idx++, data[i] + offsetX);
            vertexBuffer.put(idx++, data[i + 1] + offsetY);

            if (isBlind == 0 && !lastblind
                    && i > 2 && i < data.length - 3 &&
                    (data[i - 3] == data[i] && data[i - 2] == data[i + 1] && data[i - 1] == data[i + 2])) {
                // same vertex - blind spot
                vertexBuffer.put(idx++, data[i + 2] + GUIConstants.UI_TEXT_Z_HIDDEN);
                isBlind = 2;
            } else {
                vertexBuffer.put(idx++, data[i + 2]);
            }

            if (isBlind > 0) {
                ((FloatBuffer)colorBuffer).put(idxColor++, 0f);
                ((FloatBuffer)colorBuffer).put(idxColor++, 1f);
                ((FloatBuffer)colorBuffer).put(idxColor++, 0f);
                ((FloatBuffer)colorBuffer).put(idxColor++, 0f);
                isBlind--;
                lastblind = true;
            } else {
                ((FloatBuffer)colorBuffer).put(idxColor++, color[0]);
                ((FloatBuffer)colorBuffer).put(idxColor++, color[1]);
                ((FloatBuffer)colorBuffer).put(idxColor++, color[2]);
                ((FloatBuffer)colorBuffer).put(idxColor++, color[3]);
                lastblind = false;
            }
        }
        vertexBuffer.put(idx++, data[data.length - 3] + offsetX);
        vertexBuffer.put(idx++, data[data.length - 2] + offsetY);
        vertexBuffer.put(idx++, data[data.length - 1] + GUIConstants.UI_TEXT_Z_HIDDEN);

        ((FloatBuffer)colorBuffer).put(idxColor++, 0f);
        ((FloatBuffer)colorBuffer).put(idxColor++, 1f);
        ((FloatBuffer)colorBuffer).put(idxColor++, 0f);
        ((FloatBuffer)colorBuffer).put(idxColor++, 0f);

        return idx;
    }


    private static int build(FloatBuffer vertexBuffer, int idx, Buffer colorBuffer,
                             float[] glyph,
                             float[] color,
                             float offsetX, float offsetY, float offsetZ) {

        int coffset = (idx / 3) * 4;

        // transparent link
        vertexBuffer.put(idx++, glyph[0] + offsetX).put(idx++, glyph[1] + offsetY).put(idx++, glyph[2] + offsetZ + GUIConstants.UI_TEXT_Z_HIDDEN);
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
                .put(idx++, glyph[glyph.length - 1] + offsetZ + GUIConstants.UI_TEXT_Z_HIDDEN);
        ((FloatBuffer)colorBuffer).put(coffset++, 1).put(coffset++, 0f).put(coffset++, 0f).put(coffset++, 0f);

        return glyph.length + 2;
    }

    /**
     * Build a line between x and y (2 vertices + 2 duplicates for the transparent link)
     *
     * @param vertexBuffer
     * @param vp           vertex buffer pointer
     * @param colorBuffer
     * @param color           color buffer pointer
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     */
    private static int line(FloatBuffer vertexBuffer, int vp, Buffer colorBuffer, float[] color,
                            float x1, float y1,
                            float x2, float y2) {
        int voffset = vp;
        int cp = (voffset / 3) * 4;
        Log.i("Glyph", "voffset: " + vp + ", coffset:" + cp);
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