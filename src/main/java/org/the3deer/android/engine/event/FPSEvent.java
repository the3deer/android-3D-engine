package org.the3deer.android.engine.event;

public class FPSEvent extends Event {

    private int fps;

    public FPSEvent(Object source) {
        super(source);
    }

    public void setFps(int fps) {
        this.fps = fps;
    }

    public int getFps() {
        return fps;
    }
}
