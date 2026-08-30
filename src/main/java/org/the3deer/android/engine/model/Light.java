package org.the3deer.android.engine.model;

import org.the3deer.util.event.EventListener;

import java.util.EventObject;

public class Light implements EventListener {

    private float[] location;
    private boolean enabled = true;

    public Light(float [] location){
        this.location = location;
        //logger.finest("location: "+ Arrays.toString(this.location));
    }

    public int toggle(){
        this.enabled = !this.enabled;
        return this.enabled? 1 : 0;
    }

    public boolean isEnabled(){
        return enabled;
    }

    public float[] getLocation() {
        return location;
    }

    public void setLocation(float[] location) {
        this.location = location;
    }

    @Override
    public boolean onEvent(EventObject event) {
        return false;
    }
}
