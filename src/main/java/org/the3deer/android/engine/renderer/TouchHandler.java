package org.the3deer.android.engine.renderer;

import org.the3deer.android.engine.event.MotionEvent;

public interface TouchHandler {

    boolean onSurfaceTouchEvent(MotionEvent event);
}
