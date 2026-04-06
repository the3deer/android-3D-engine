package org.the3deer.android.engine.model;

import org.the3deer.android.engine.event.CameraEvent;
import org.the3deer.util.event.EventListener;

import java.util.EventObject;

import javax.inject.Inject;

public class Light implements EventListener {

    @Inject
    private Camera camera;

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
        if (event instanceof CameraEvent){
            if (event.getSource() == camera) {
                //this.location = ((Camera) event.getSource()).getPos();
                //logger.finest("new location: " + Arrays.toString(this.location));
            }
        }
        return false;
    }
}
