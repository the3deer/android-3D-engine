package org.the3deer.android.engine.gui;

import org.the3deer.android.engine.model.Constants;
import org.the3deer.android.engine.text.FontFactory;
import org.the3deer.util.io.IOUtils;

import java.nio.FloatBuffer;
import java.util.logging.Level;

/**
 * A widget that represents a single symbolic glyph (e.g. Checkbox, Menu).
 */
public class SymbolWidget extends Widget {

    public static final int MENU = 1000;
    public static final int MENU_NEW = 1001;
    public static final int CHECKBOX_OFF = 2000;
    public static final int CHECKBOX_ON = 2001;
    public static final int GLYPH_LESS_THAN_CODE = 3000;
    public static final int GLYPH_GREATER_THAN_CODE = 3001;

    private final int code;
    private final FontFactory fontFactory;

    public SymbolWidget(Widget parent, int code, FontFactory fontFactory) {
        super(parent);
        this.code = code;
        this.fontFactory = fontFactory;
        this.init();
    }

    public static SymbolWidget build(Widget parent, int code, FontFactory fontFactory) {
        return new SymbolWidget(parent, code, fontFactory);
    }

    @Override
    public void init() {
        try {
            final FloatBuffer vertexBuffer = IOUtils.createFloatBuffer(org.the3deer.android.engine.text.Text.VSIZE);
            final FloatBuffer colorBuffer = IOUtils.createFloatBuffer(org.the3deer.android.engine.text.Text.CSIZE);

            org.the3deer.android.engine.text.Text.build(vertexBuffer, 0, colorBuffer, this.code, Constants.COLOR_WHITE, 0f, 0f, 0f, fontFactory);

            setVertexBuffer(vertexBuffer);
            setVertexColorsArrayBuffer(colorBuffer);
        } catch (Exception e) {
            logger.log(Level.SEVERE,  e.getMessage(), e);
        }
    }
}
