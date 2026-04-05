package org.the3deer.android.engine.event;

import java.util.EventObject;

public class MotionEvent extends EventObject {
    /**
     * Constructs a prototypical Event.
     *
     * @param source the object on which the Event initially occurred
     * @throws IllegalArgumentException if source is null
     */
    public MotionEvent(Object source) {
        super(source);
    }
}
