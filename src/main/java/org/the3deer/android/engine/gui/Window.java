package org.the3deer.android.engine.gui;

import org.the3deer.android.engine.model.Constants;
import org.the3deer.android.engine.model.Dimensions;
import org.the3deer.android.engine.model.Object3D;
import org.the3deer.util.io.IOUtils;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.EventObject;
import java.util.logging.Level;

public class Window extends Widget {

    public static class WindowClosed extends Event {
        public WindowClosed(Object source) {
            super(source);
        }
    }

    private float contentHeight;

    private FontFactory fontFactory;

    public Window(FontFactory fontFactory, Widget parent) {
        super(parent, fontFactory.getCharWidth() * 10,
                fontFactory.getCharHeight());
        this.fontFactory = fontFactory;
        /*super(parent, content.getContentDimensions().getWidth(),
                content.getContentDimensions().getHeight()+FontFactory.getInstance().getCharHeight());*/
        //this.init();
    }

    public Object3D setRelativeLocation(int relativeLocation) {
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
            setVertexColorsArrayBuffer(colorBuffer);

            IOUtils.fill(vertexBuffer, voffset, vertexBuffer.capacity(), 0);
            IOUtils.fill(colorBuffer, voffset / 3 * 4, colorBuffer.capacity(), 0);

        } catch (Exception e) {
            logger.log(Level.SEVERE,  e.getMessage(), e);
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
        float charWidth = fontFactory.getCharWidth();
        float charHeight = fontFactory.getCharHeight();

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

        float[] data = fontFactory.getChar('X'); //Glyph.getChar('X');
        float offsetX = width - fontFactory.getCharWidth();
        float offsetY = height - fontFactory.getCharHeight();

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
        this.height = currentDimensions.getHeight() + fontFactory.getCharHeight();

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
            x /= fontFactory.getCharWidth();
            int idxX = (int) x;
            int last = (int) (width / fontFactory.getCharWidth()) - 1;

            float y = clickEvent.getY();
            y -= getLocationY();
            y /= getScaleY();
            y /= fontFactory.getCharHeight();
            int idxY = (int) (height / fontFactory.getCharHeight()) - 1 - (int) y;

            logger.finest( "x:" + idxX + ", y:" + idxY + ", last:" + last);
            if (idxY == 0 && idxX == last) {
                logger.finest("window closing...");
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

            // logger.finest(can handle: " + (idxY == 0 && idxX == last));
            if (idxY == 0 && idxX == last) {
                return true;
            }
        } else if (event instanceof MoveEvent) {
            MoveEvent moveEvent = (MoveEvent) event;
            float y = moveEvent.getY();
            y -= getLocationY();
            y /= getScaleY();
            y /= fontFactory.getCharHeight();
            int idxY = (int) (height / fontFactory.getCharHeight()) - 1 - (int) y;
            // logger.finest("can handle: y:" + idxY);
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
