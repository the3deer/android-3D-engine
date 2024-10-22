package org.the3deer.android_3d_model_engine.renderer;

import org.the3deer.android_3d_model_engine.model.Projection;

import java.util.EventObject;

public class RenderEvent extends EventObject {

    private final Code code;

    private int width;
    private int height;
    private Projection projection;

    public enum Code {SURFACE_CREATED, SURFACE_CHANGED, PROJECTION_CHANGED}

    public RenderEvent(Object source, Code code) {
        super(source);
        this.code = code;
    }

    public RenderEvent(Object source, Code code, int width, int height) {
        super(source);
        this.code = code;
        this.width = width;
        this.height = height;
    }

    public Code getCode() {
        return code;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Projection getProjection() {
        return projection;
    }

    public void setProjection(Projection projection) {
        this.projection = projection;
    }

    @Override
    public String toString() {
        return "ViewEvent{" +
                "code=" + code +
                '}';
    }
}
