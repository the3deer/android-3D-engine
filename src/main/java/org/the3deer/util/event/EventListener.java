package org.the3deer.util.event;

import java.util.EventObject;

public interface EventListener extends java.util.EventListener {

    /**
     * Process the event notification on the System
     *
     * @param event the event
     * @return <code>false</code> if the event must be propagated, <code>true</code> to stop it
     */
    boolean onEvent(EventObject event);
}
