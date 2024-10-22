package org.the3deer.util.event;

import java.util.EventObject;

public interface EventManager {

    /**
     * Process the event
     * @param event
     * @return true if the event was handle, false otherwise
     */
    boolean propagate(EventObject event);
}
