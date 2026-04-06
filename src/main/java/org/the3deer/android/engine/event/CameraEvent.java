package org.the3deer.android.engine.event;

import org.the3deer.android.engine.model.Camera;

/**
 * Triggers on any camera update
 */
public class CameraEvent extends Event {

    public enum Code {
        HANDLER_UPDATED,
        CAMERA_UPDATED,
        UNKNOWN
    }

    /**
     * Constructs a prototypical Event.
     *
     * @param source the object on which the Event initially occurred
     * @throws IllegalArgumentException if source is null
     */
    public CameraEvent(Object source) {
        this(source, null, Code.UNKNOWN);
    }

    public CameraEvent(Object source, Camera camera, Code code) {
        super(source, code);
        setData("camera", camera);
    }


    public Camera getCamera() {
        return getData("camera", Camera.class);
    }

    public void setCamera(Camera camera) {
        setData("camera", camera    );
    }
}
