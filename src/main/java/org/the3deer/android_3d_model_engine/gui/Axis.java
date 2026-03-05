package org.the3deer.android_3d_model_engine.gui;

import org.the3deer.android_3d_model_engine.model.Camera;

import java.util.EventObject;

public class Axis extends Widget {


    public Axis(){
        super(org.the3deer.android_3d_model_engine.objects.Axis.build());
        setId("gui_axis");
        setVisible(true);
        /*setRelativeScale(new float[]{0.1f, 0.1f, 0.1f});

        setRelativeLocation(Widget.POSITION_TOP_LEFT);*/
        /*setLocation(new float[]{-1,0,0});
        setScale(new float[]{0.5f, 0.5f, 0.5f});*/
    }

    @Override
    public boolean onEvent(EventObject event) {
        if (event instanceof Camera.CameraUpdatedEvent){

            // check
            final Camera camera = ((Camera.CameraUpdatedEvent) event).getCamera();
            if (camera == null) return false;

            // update
            setOrientation(camera.getOrientation());

        }
        return super.onEvent(event);
    }
}
