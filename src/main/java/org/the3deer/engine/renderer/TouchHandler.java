package org.the3deer.engine.renderer;

import org.the3deer.engine.event.MotionEvent;

public interface TouchHandler {

    boolean onSurfaceTouchEvent(MotionEvent event);
}
