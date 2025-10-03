package org.the3deer.android_3d_model_engine.model;

import android.util.Log;

import org.the3deer.util.event.EventListener;

import java.util.Arrays;
import java.util.EventObject;

import javax.inject.Inject;

public class Light implements EventListener {

    @Inject
    private Camera camera;

    private float[] location;
    private boolean enabled = true;

    public Light(float [] location){
        this.location = location;
        //Log.v("Light", "location: "+ Arrays.toString(this.location));
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
        if (event instanceof Camera.CameraUpdatedEvent){
            if (event.getSource() == camera) {
                //this.location = ((Camera) event.getSource()).getPos();
                //Log.v("Light", "new location: " + Arrays.toString(this.location));
            }
        }
        return false;
    }
}
