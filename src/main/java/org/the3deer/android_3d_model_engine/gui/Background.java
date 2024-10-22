package org.the3deer.android_3d_model_engine.gui;

import android.opengl.GLES20;

import org.the3deer.android_3d_model_engine.model.Dimensions;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.util.io.IOUtils;

import java.nio.FloatBuffer;
import java.util.EventObject;

public class Background extends Widget {

    private static final float EFFECT_3D = 0.015f;

    public Background(Widget parent) {
        super(parent);
        setId(parent.getId() + "_bg");

        setParentBound(true);
        setScale(parent.getScale());
        setLocation(parent.getLocation());
        setVisible(parent.isVisible());
        setSolid(false);

        // parent.addListener(this);

        // vertex buffer
        int size = 12;
        final FloatBuffer vertexBuffer = IOUtils.createNativeByteBuffer(size * 3 * 4).asFloatBuffer();
        setVertexBuffer(vertexBuffer);

        // color buffer
        final FloatBuffer colorsBuffer = IOUtils.createNativeByteBuffer(size * 4 * 4).asFloatBuffer();
        setColorsBuffer(colorsBuffer);

        int idxColor = 0;
        colorsBuffer.put(idxColor++, 0.5f).put(idxColor++, 0.5f).put(idxColor++, 0.5f).put(idxColor++, GUIConstants.UI_BACKGROUND_ALPHA);
        colorsBuffer.put(idxColor++, 0.5f).put(idxColor++, 0.5f).put(idxColor++, 0.5f).put(idxColor++, GUIConstants.UI_BACKGROUND_ALPHA);
        colorsBuffer.put(idxColor++, 0.5f).put(idxColor++, 0.5f).put(idxColor++, 0.5f).put(idxColor++, GUIConstants.UI_BACKGROUND_ALPHA);
        colorsBuffer.put(idxColor++, 0.5f).put(idxColor++, 0.5f).put(idxColor++, 0.5f).put(idxColor++, GUIConstants.UI_BACKGROUND_ALPHA);
        colorsBuffer.put(idxColor++, 0.5f).put(idxColor++, 0.5f).put(idxColor++, 0.5f).put(idxColor++, GUIConstants.UI_BACKGROUND_ALPHA);
        colorsBuffer.put(idxColor++, 0.5f).put(idxColor++, 0.5f).put(idxColor++, 0.5f).put(idxColor++, GUIConstants.UI_BACKGROUND_ALPHA);

        colorsBuffer.put(idxColor++, 0.25f).put(idxColor++, 0.25f).put(idxColor++, 0.25f).put(idxColor++, GUIConstants.UI_BACKGROUND_ALPHA);
        colorsBuffer.put(idxColor++, 0.25f).put(idxColor++, 0.25f).put(idxColor++, 0.25f).put(idxColor++, GUIConstants.UI_BACKGROUND_ALPHA);
        colorsBuffer.put(idxColor++, 0.25f).put(idxColor++, 0.25f).put(idxColor++, 0.25f).put(idxColor++, GUIConstants.UI_BACKGROUND_ALPHA);
        colorsBuffer.put(idxColor++, 0.25f).put(idxColor++, 0.25f).put(idxColor++, 0.25f).put(idxColor++, GUIConstants.UI_BACKGROUND_ALPHA);
        colorsBuffer.put(idxColor++, 0.25f).put(idxColor++, 0.25f).put(idxColor++, 0.25f).put(idxColor++, GUIConstants.UI_BACKGROUND_ALPHA);
        colorsBuffer.put(idxColor++, 0.25f).put(idxColor++, 0.25f).put(idxColor++, 0.25f).put(idxColor++, GUIConstants.UI_BACKGROUND_ALPHA);

        setDrawMode(GLES20.GL_TRIANGLES);

        // build
        //refresh();
    }

    @Override
    public float[] getLocation() {
        return parent.getLocation();
    }

    public static Background build(Widget parent) {
        return new Background(parent);
    }

    @Override
    public void refresh() {
        setLocation(parent.getLocation());
        setScale(parent.getScale());
        setRotation(parent.getRotation());
        this.refreshImpl();
        super.refresh();
        //this.setRotation2(source.getRotation2(), source.getRotation2Location());
        //super.setVisible(parent.isVisible());
    }

    @Override
    public boolean onEvent(EventObject event) {
        super.onEvent(event);
        if (event instanceof ChangeEvent) {
            Object3DData source = (Object3DData) event.getSource();
            if (source == parent) {
                refresh();
            }
        } else if (event instanceof ChildAdded || event instanceof ChildRemoved) {
            if (((Widget)event.getSource()).getParent() == getParent()) {
                refresh();
                this.refresh();
            }
        }
        return false;
    }


    private void refreshImpl() {

        if (this.getDimensions() == parent.getDimensions()) {
            return;
        }

        //Log.v("Background", "Refreshing...");

        final Dimensions dimensions = parent.getDimensions();
        float[] min = dimensions.getMin();
        float[] max = dimensions.getMax();

        int idx = 0;
        float minZ = min[2];

        vertexBuffer.put(idx++, min[0]).put(idx++, max[1]).put(idx++, minZ);
        vertexBuffer.put(idx++, max[0]).put(idx++, max[1]).put(idx++, minZ);
        vertexBuffer.put(idx++, min[0]).put(idx++, min[1]).put(idx++, minZ);
        vertexBuffer.put(idx++, min[0]).put(idx++, min[1]).put(idx++, minZ);
        vertexBuffer.put(idx++, max[0]).put(idx++, min[1]).put(idx++, minZ);
        vertexBuffer.put(idx++, max[0]).put(idx++, max[1]).put(idx++, minZ);

        vertexBuffer.put(idx++, min[0] + EFFECT_3D).put(idx++, max[1] - EFFECT_3D).put(idx++, minZ);
        vertexBuffer.put(idx++, max[0] + EFFECT_3D).put(idx++, max[1] - EFFECT_3D).put(idx++, minZ);
        vertexBuffer.put(idx++, min[0] + EFFECT_3D).put(idx++, min[1] - EFFECT_3D).put(idx++, minZ);
        vertexBuffer.put(idx++, min[0] + EFFECT_3D).put(idx++, min[1] - EFFECT_3D).put(idx++, minZ);
        vertexBuffer.put(idx++, max[0] + EFFECT_3D).put(idx++, min[1] - EFFECT_3D).put(idx++, minZ);
        vertexBuffer.put(idx++, max[0] + EFFECT_3D).put(idx++, max[1] - EFFECT_3D).put(idx++, minZ);

        setVertexBuffer(vertexBuffer);

        this.setDimensions(parent.getDimensions());

        // 3D
        /*if (min[2] != max[2]) {

            vertexBuffer.put(max[0]).put(min[1]).put(max[2]);
            vertexBuffer.put(max[0]).put(max[1]).put(max[2]);

            vertexBuffer.put(min[0]).put(min[1]).put(max[2]);
            vertexBuffer.put(min[0]).put(max[1]).put(max[2]);

            vertexBuffer.put(min[0]).put(min[1]).put(min[2]);
            vertexBuffer.put(min[0]).put(max[1]).put(min[2]);
        }*/
    }
}
