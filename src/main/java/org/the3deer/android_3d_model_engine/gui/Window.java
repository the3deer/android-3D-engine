package org.the3deer.android_3d_model_engine.gui;

import android.util.Log;

import org.the3deer.android_3d_model_engine.model.Constants;
import org.the3deer.android_3d_model_engine.model.Dimensions;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.util.io.IOUtils;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.EventObject;

public class Window extends Widget {

    public static class WindowClosed extends Event {
        public WindowClosed(Object source) {
            super(source);
        }
    }

    private float contentHeight;

    public Window(Widget parent) {
        super(parent, FontFactory.getInstance().getCharWidth() * 10,
                FontFactory.getInstance().getCharHeight());
        /*super(parent, content.getContentDimensions().getWidth(),
                content.getContentDimensions().getHeight()+FontFactory.getInstance().getCharHeight());*/
        //this.init();
    }

    public Object3DData setRelativeLocation(int relativeLocation) {
        super.setRelativeLocation(relativeLocation);
        return this;
    }

    public void refresh() {
        float totalHeight = 0;
        float maxWidth = 0;
        for (Widget widget : widgets) {
            if (widget.isParentBound()) continue;
            float childHeight = widget.getCurrentDimensions().getHeight();
            float childWidth = widget.getCurrentDimensions().getWidth();
            totalHeight += childHeight;
            if (maxWidth < childWidth) {
                maxWidth = childWidth;
            }
        }
        this.height = totalHeight;
        this.width = maxWidth;
        this.build();
    }

    private void build() {
        try {
            // allocate buffers
            final int total = 5;
            final FloatBuffer vertexBuffer = IOUtils.createFloatBuffer(total * Glyph.VSIZE);
            final FloatBuffer colorBuffer = IOUtils.createFloatBuffer(total * Glyph.CSIZE);

            // window
            final int voffset = build(vertexBuffer, 0, colorBuffer);

            setVertexBuffer(vertexBuffer);
            setColorsBuffer(colorBuffer);

            IOUtils.fill(vertexBuffer, voffset, vertexBuffer.capacity(), 0);
            IOUtils.fill(colorBuffer, voffset / 3 * 4, colorBuffer.capacity(), 0);

        } catch (Exception e) {
            Log.e("Window", e.getMessage(), e);
        }
    }

    private int build(FloatBuffer vertexBuffer, int idx, Buffer colorBuffer) {

        int idxColor = (idx / 3) * 4;

        // ------------------ border

        float padding = getPadding();

        // transparent link
        vertexBuffer.put(idx++, width).put(idx++, height).put(idx++, 0);
        for (int i = 0; i < 4; i++) ((FloatBuffer)colorBuffer).put(idxColor++, 0f);

        // border
        vertexBuffer.put(idx++, width).put(idx++, height).put(idx++, 0);
        vertexBuffer.put(idx++, 0).put(idx++, height).put(idx++, 0);
        vertexBuffer.put(idx++, 0).put(idx++, 0).put(idx++, 0);
        vertexBuffer.put(idx++, width).put(idx++, 0).put(idx++, 0);
        vertexBuffer.put(idx++, width).put(idx++, height).put(idx++, 0);
        for (int i = 0; i < 4 * 5; i++) ((FloatBuffer)colorBuffer).put(idxColor++, 1f);

        // transparent link
        vertexBuffer.put(idx++, width).put(idx++, height).put(idx++, 0);
        for (int i = 0; i < 4; i++) ((FloatBuffer)colorBuffer).put(idxColor++, 0f);

        // ------------------- separator

        // transparent link
        float charWidth = FontFactory.getInstance().getCharWidth();
        float charHeight = FontFactory.getInstance().getCharHeight();

        this.contentHeight = height - charHeight;
        vertexBuffer.put(idx++, width).put(idx++, contentHeight).put(idx++, 0);
        for (int i = 0; i < 4; i++) ((FloatBuffer)colorBuffer).put(idxColor++, 0f);

        // separator
        vertexBuffer.put(idx++, width).put(idx++, contentHeight).put(idx++, 0);
        for (int i = 0; i < 4; i++) ((FloatBuffer)colorBuffer).put(idxColor++, 1f);
        vertexBuffer.put(idx++, 0).put(idx++, contentHeight).put(idx++, 0);
        for (int i = 0; i < 4; i++) ((FloatBuffer)colorBuffer).put(idxColor++, 1f);

        // transparent link
        vertexBuffer.put(idx++, 0).put(idx++, contentHeight).put(idx++, 0);
        for (int i = 0; i < 4; i++) ((FloatBuffer)colorBuffer).put(idxColor++, 0f);

        /// ------------------- close button

        float[] data = FontFactory.getInstance().getChar('X'); //Glyph.getChar('X');
        float offsetX = width - FontFactory.getInstance().getCharWidth();
        float offsetY = height - FontFactory.getInstance().getCharHeight();

        idx = Glyph.build(vertexBuffer, idx, colorBuffer, offsetX, offsetY, data, Constants.COLOR_WHITE);
        //for (int i = 0; i < 4; i++) colorBuffer.put(coffset++, 0f);

        // transparent link
        /*vertexBuffer.put(idx++, 0).put(idx++, height - GLYPH_HEIGHT - padding * 2).put(idx++, 0);
        for (int i = 0; i < 4; i++) colorBuffer.put(idxColor++, 0f);*/

        IOUtils.fill(vertexBuffer, idx, vertexBuffer.capacity(), 0);
        IOUtils.fill(colorBuffer, (idx / 3) * 4, colorBuffer.capacity(), 0);

        return idx;
    }

    public int pack(Widget content) {

        int idx = 0;

        final FloatBuffer vertexBuffer = getVertexBuffer();
        final Buffer colorBuffer = getColorsBuffer();

        final Dimensions currentDimensions = content.getCurrentDimensions();
        this.width = currentDimensions.getWidth();
        this.height = currentDimensions.getHeight() + FontFactory.getInstance().getCharHeight();

        return build(vertexBuffer, idx, colorBuffer);
    }

    @Override
    public boolean onEvent(EventObject event) {
        if (super.onEvent(event)) {
            return true;
        } else if (event instanceof ClickEvent) {
            ClickEvent clickEvent = (ClickEvent) event;
            if (clickEvent.getWidget() != this) return true;

            float x = clickEvent.getX();
            x -= getLocationX();
            x /= getScaleX();
            x /= FontFactory.getInstance().getCharWidth();
            int idxX = (int) x;
            int last = (int) (width / FontFactory.getInstance().getCharWidth()) - 1;

            float y = clickEvent.getY();
            y -= getLocationY();
            y /= getScaleY();
            y /= FontFactory.getInstance().getCharHeight();
            int idxY = (int) (height / FontFactory.getInstance().getCharHeight()) - 1 - (int) y;

            Log.v("Window", "x:" + idxX + ", y:" + idxY + ", last:" + last);
            if (idxY == 0 && idxX == last) {
                Log.v("Window", "window closing...");
                propagate(new WindowClosed(this));
                return true;
            }
        } else if (event instanceof MoveEvent) {
            MoveEvent moveEvent = (MoveEvent) event;
            float[] newPosition = moveEvent.getWidget().getLocation().clone();
            newPosition[0] += moveEvent.getDx();
            newPosition[1] += moveEvent.getDy();
            moveEvent.getWidget().setLocation(newPosition);
            moveEvent.getWidget().setUserLocation(newPosition);
            return true;
        }
        return false;
    }

    @Override
    protected boolean canHandle(EventObject event) {
        if (event instanceof ClickEvent) {
            ClickEvent clickEvent = (ClickEvent) event;
            float x = clickEvent.getX();
            x -= getLocationX();
            x /= getScaleX();
            x /= Glyph.SIZE_H;
            int idxX = (int) x;
            int last = (int) (width / Glyph.SIZE_H) - 1;

            float y = clickEvent.getY();
            y -= getLocationY();
            y /= getScaleY();
            y /= Glyph.SIZE_V;
            int idxY = (int) (height / Glyph.SIZE_V) - 1 - (int) y;

            // Log.v("Window", "can handle: x:" + idxX + ", y:" + idxY + ", last:" + last + ", can handle: " + (idxY == 0 && idxX == last));
            if (idxY == 0 && idxX == last) {
                return true;
            }
        } else if (event instanceof MoveEvent) {
            MoveEvent moveEvent = (MoveEvent) event;
            float y = moveEvent.getY();
            y -= getLocationY();
            y /= getScaleY();
            y /= FontFactory.getInstance().getCharHeight();
            int idxY = (int) (height / FontFactory.getInstance().getCharHeight()) - 1 - (int) y;
            // Log.v("Window", "can handle: y:" + idxY);
            if (idxY == 0) {
                return true;
            }
        }
        return false;
    }

    /*public float[] getViewport() {
        float[] port = new float[3];
        port[0] = super.getLocationX() + getPadding();
        port[1] = super.getLocationY() + getPadding();
        port[2] = 0f;
        return port;
    }*/
}
