package org.the3deer.util.event;

import java.util.EventObject;

public interface EventListener extends java.util.EventListener {

    /**
     * Process the event notification on the System
     *
     * @param event the event
     * @return <code>true</code> if the event was handled (stop propagation)
     */
    boolean onEvent(EventObject event);
}
