package org.the3deer.android_3d_model_engine.gui;

import android.util.Log;

import org.the3deer.android_3d_model_engine.model.Constants;
import org.the3deer.util.io.IOUtils;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

public class CheckList extends Widget {

    // bytes reserved per glyph
    private final static short GLYPH_BYTES = 18;
    // pixel size factor for glyphs
    private final static short GLYPH_SIZE = 1;

    private final static float GLYPH_WIDTH = 0.5f;
    private final static float GLYPH_HEIGHT = 0.7f;

    public enum Style {

        H1(96), BODY_1(16);

        final int size;

        Style(int size) {
            this.size = size;
        }
    }

    public static class ItemSelectedEvent extends EventObject {

        private final int selected;
        private final Object item;

        ItemSelectedEvent(Object source, Object item, int idx) {
            super(source);
            this.item = item;
            this.selected = idx;
        }

        public int getSelected() {
            return selected;
        }

        public Object getItem() {
            return item;
        }
    }

    public static class Builder {

        private List<Style> styles = new ArrayList<>();
        private List<Boolean> states = new ArrayList<>();
        private List<Object> items = new ArrayList<>();

        public Builder add(String text, Style style, boolean selected) {
            items.add(text);
            styles.add(style);
            states.add(selected);
            return this;
        }

        public void add(String text) {
            this.add(text, CheckList.Style.BODY_1, false);
        }

        public CheckList build(Widget parent) {
            if (items.isEmpty()) throw new IllegalArgumentException();
            return new CheckList(parent, items, styles, states);
        }

    }

    private final List<Object> items;
    private final List<Style> styles;
    private final List<Boolean> states;

    private final int totalGlyphs;
    private final int rows;
    private final int cols;

    private CheckList(Widget parent, List<Object> items, List<Style> styles, List<Boolean> states) {
        super(parent);
        this.items = items;
        this.states = states;
        this.styles = styles;
        // count total chars
        int max = 0;
        int count = 0;
        for (Object item_ : items) {
            if (item_ instanceof String) {
                String item = (String) item_;
                if (item.length() > max) max = item.length();
                count += item.length();
            }
        }
        this.cols = max + 1; // space reserved for checkbox
        this.rows = this.items.size();
        this.totalGlyphs = count;
        final int total = totalGlyphs + items.size() + 1; // reserve 1 for border
        setVertexBuffer(IOUtils.createFloatBuffer(total * GLYPH_BYTES * 3));
        setColorsBuffer(IOUtils.createFloatBuffer(total * GLYPH_BYTES * 4));
        build();
    }

    @Override
    public boolean onEvent(EventObject event) {
        super.onEvent(event);
        if (event instanceof ClickEvent) {
            ClickEvent clickEvent = (ClickEvent) event;
            if (clickEvent.getWidget() != this) return true;

            // FIXME: unproject this
            float y = clickEvent.getY();
            y -= getLocationY();
            y /= getScaleY();
            y /= GLYPH_SIZE;
            int idx = items.size() - 1 - (int) y;

            set(idx, !states.get(idx));
        }
        return true;
    }

    public void set(int idx, boolean state) {
        states.set(idx, state);
        buildCheckBox(idx);
        propagate(new ItemSelectedEvent(this, items.get(idx), idx));
    }

    private void buildCheckBox(int idx) {

        int mark = GLYPH_BYTES * 3 * totalGlyphs + GLYPH_BYTES * 3 * idx;
        int mark2 = GLYPH_BYTES * 4 * totalGlyphs + GLYPH_BYTES * 4 * idx;

        float offsetX = GLYPH_SIZE * cols - GLYPH_SIZE;
        float offsetY = (rows * GLYPH_SIZE) - ((idx + 1) * GLYPH_SIZE);

        getVertexBuffer().position(mark);
        getColorsBuffer().position(mark2);
        if (states.get(idx)) {
            Glyph.build(getVertexBuffer(), mark, getColorsBuffer(), Glyph.CHECKBOX_ON, Constants.COLOR_WHITE, offsetX, offsetY, 0);
        } else {
            Glyph.build(getVertexBuffer(), mark, getColorsBuffer(), Glyph.CHECKBOX_OFF, Constants.COLOR_WHITE, offsetX, offsetY, 0);
        }
        setVertexBuffer(getVertexBuffer());
    }

    /*@Override
    public void toggleVisible() {
        Log.i("CheckList", "Toggling menu...");

        // float[] finalPosition = calculatePosition(this, 4);
        float[] finalPosition = getLocation().clone();
        Log.i("CheckList", "Final position: " + Arrays.toString(finalPosition));

        if (isVisible()) {

            Log.i("CheckList", "Hiding menu...");

            JointTransform start = new JointTransform(new float[16]);
            start.setScale(new float[]{0.1f, 0.1f, 0.1f});
            start.setLocation(finalPosition);

            JointTransform end = new JointTransform(new float[16]);
            end.setScale(new float[]{0, 0, 0});
            if (getParent() != null)
                end.setLocation(getParent().getLocation());
            end.setVisible(false);

            animate(start, end, 250);
        } else {
            Log.i("CheckList", "Showing menu...");

            JointTransform start = new JointTransform(new float[16]);
            start.setScale(0f, 0f, 0f);
            if (getParent() != null)
                start.setLocation(getParent().getLocation());

            JointTransform end = new JointTransform(new float[16]);
            end.setLocation(calculatePosition(POSITION_MIDDLE, getCurrentDimensions(), 0.1f, getRatio(), getMargin()));
            end.setScale(new float[]{0.1f, 0.1f, 0.1f});

            animate(start, end, 250);
        }
    }*/

    private void build() {
        try {
            // allocate buffers
            final FloatBuffer vertexBuffer = getVertexBuffer();
            final Buffer colorBuffer = getColorsBuffer();

            vertexBuffer.position(0);
            colorBuffer.position(0);

            int idx = 0;
            float[] data;
            for (int row = 0; row < rows; row++) {
                final String text = (String) this.items.get(row);
                for (int column = 0; column < text.length(); column++, idx++) {
                    float offsetX = GLYPH_SIZE * column;
                    float offsetY = (rows * GLYPH_SIZE) - ((row + 1) * GLYPH_SIZE);

                    final char character = text.charAt(column);
                    data = Glyph.getChar(character);
                    if (data == null) continue;

                    vertexBuffer.put(data[0] + offsetX);
                    vertexBuffer.put(data[1] + offsetY);
                    vertexBuffer.put(data[2]);
                    for (int i = 0; i < data.length; i += 3) {
                        vertexBuffer.put(data[i] + offsetX);
                        vertexBuffer.put(data[i + 1] + offsetY);
                        vertexBuffer.put(data[i + 2]);
                    }
                    vertexBuffer.put(data[data.length - 3] + offsetX);
                    vertexBuffer.put(data[data.length - 2] + offsetY);
                    vertexBuffer.put(data[data.length - 1]);

                    ((FloatBuffer)colorBuffer).put(0f);
                    ((FloatBuffer)colorBuffer).put(0f);
                    ((FloatBuffer)colorBuffer).put(0f);
                    ((FloatBuffer)colorBuffer).put(0f);
                    for (int i = 0; i < data.length; i += 3) {
                        ((FloatBuffer)colorBuffer).put(1f);
                        ((FloatBuffer)colorBuffer).put(1f);
                        ((FloatBuffer)colorBuffer).put(1f);
                        ((FloatBuffer)colorBuffer).put(1f);
                    }
                    ((FloatBuffer)colorBuffer).put(0f);
                    ((FloatBuffer)colorBuffer).put(0f);
                    ((FloatBuffer)colorBuffer).put(0f);
                    ((FloatBuffer)colorBuffer).put(0f);
                }
            }

            float totalWidth = cols * GLYPH_SIZE + GLYPH_SIZE;
            float height = rows * GLYPH_SIZE;
            Log.v("CheckList", "Size. width:" + totalWidth + ", height:" + height + ", depth:" + totalWidth);

            // draw border
            vertexBuffer.put(0);
            vertexBuffer.put(0);
            vertexBuffer.put(0);
            for (int i = 0; i < 4; i++) ((FloatBuffer)colorBuffer).put(0f);

            // buildBorder(vertexBuffer, colorBuffer, totalWidth, rows * GLYPH_SIZE, 0f);

            vertexBuffer.put(0);
            vertexBuffer.put(0);
            vertexBuffer.put(0);
            for (int i = 0; i < 4; i++) ((FloatBuffer)colorBuffer).put(1f);

            buildCube(vertexBuffer, colorBuffer, 0, getLocation(), totalWidth, height, totalWidth);
            for (int i = 0; i < items.size(); i++) buildCheckBox(i);
            IOUtils.fill(vertexBuffer, vertexBuffer.position(), GLYPH_SIZE, 0);
            IOUtils.fill(colorBuffer, colorBuffer.position(), GLYPH_SIZE, 0);

            setVertexBuffer(vertexBuffer);
            setColorsBuffer(colorBuffer);
        } catch (Exception e) {
            Log.e("CheckList", e.getMessage(), e);
        }
    }

    private static void buildCube(FloatBuffer resultVertexBuffer, Buffer resultColorBuffer,
                                  int offset, float[] location, float width, float height, float depth) {

        resultVertexBuffer.put(location[0]).put(location[1]).put(location[2]);
        ((FloatBuffer)resultColorBuffer).put(1f).put(1f).put(1f).put(1f);

        resultVertexBuffer.put(location[0] + width).put(location[1]).put(location[2]);
        ((FloatBuffer)resultColorBuffer).put(1f).put(1f).put(1f).put(1f);

        resultVertexBuffer.put(location[0] + width).put(location[1]).put(location[2] - depth);
        ((FloatBuffer)resultColorBuffer).put(1f).put(1f).put(1f).put(1f);

        resultVertexBuffer.put(location[0]).put(location[1]).put(location[2] - depth);
        ((FloatBuffer)resultColorBuffer).put(1f).put(1f).put(1f).put(1f);
    }
}
