package org.the3deer.android_3d_model_engine.renderer;

import android.util.Log;

import org.the3deer.android_3d_model_engine.model.Light;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.view.Renderer;
import org.the3deer.util.event.EventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EventObject;
import java.util.List;

import javax.inject.Inject;

public class DefaultRenderer implements Renderer, EventListener {

    private final static String TAG = DefaultRenderer.class.getSimpleName();

    private boolean enabled = true;

    @Inject
    private List<Drawer> drawers;

    @Inject
    private Light light;

    @Inject
    private List<EventListener> listeners = new ArrayList<>();

    /**
     * Construct a new renderer for the specified surface view
     */
    public DefaultRenderer() {
    }

    public List<? extends Object3DData> getObjects() {
        return Collections.emptyList();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public DefaultRenderer addListener(EventListener listener) {
        this.listeners.add(listener);
        return this;
    }

    @Override
    public void onSurfaceChanged(int width, int height) {

        // call decorators
        for (int i = 0; i < drawers.size(); i++) {
            try {
                drawers.get(i).onSurfaceChanged(width, height);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
                setEnabled(false);
            }
        }
    }

    @Override
    public void onDrawFrame() {

        // check
        if (!enabled) return;

        // debugger
        for (int i = 0; i < drawers.size(); i++) {
            if (!drawers.get(i).isEnabled()) {
                continue;
            }
            try {
                drawers.get(i).onDrawFrame(null);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
                drawers.get(i).setEnabled(false);
            }
        }
    }

    @Override
    public boolean onEvent(EventObject event) {
        return false;
    }
}