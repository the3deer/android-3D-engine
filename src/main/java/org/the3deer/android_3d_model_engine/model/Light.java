package org.the3deer.android_3d_model_engine.model;

public class Light {

    private float[] location;
    private boolean enabled = true;

    public Light(float [] location){
        this.location = location;
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
}
