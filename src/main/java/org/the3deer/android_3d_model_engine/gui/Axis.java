package org.the3deer.android_3d_model_engine.gui;

import org.the3deer.android_3d_model_engine.model.Camera;
import org.the3deer.android_3d_model_engine.model.Scene;
import org.the3deer.util.bean.BeanInit;

import java.util.EventObject;

import javax.inject.Inject;

public class Axis extends Widget {

    @Inject
    private Scene scene;
    //private final float[] matrix = new float[16];
    //private final Quaternion orientation = new Quaternion(matrix);

    public Axis(){
        super(org.the3deer.android_3d_model_engine.objects.Axis.build());
        setId("gui_axis");
        setVisible(true);
        /*setRelativeScale(new float[]{0.1f, 0.1f, 0.1f});

        setRelativeLocation(Widget.POSITION_TOP_LEFT);*/
        /*setLocation(new float[]{-1,0,0});
        setScale(new float[]{0.5f, 0.5f, 0.5f});*/
    }

    @BeanInit
    public void setUp(){
        // this.sceneCamera = BeanFactory.getInstance().find(Camera.class, "scene_0");
        if (this.scene != null && this.scene.getCamera() != null) {
            this.scene.getCamera().addListener(this);
        }
    }

    public void dispose(){
        if (this.scene != null) {
            //this.camera.removeListener(this);
        }
    }

    @Override
    public boolean onEvent(EventObject event) {
        if (event instanceof Camera.CameraUpdatedEvent){
            if (this.scene != null && this.scene.getCamera() != null) {
                setOrientation(this.scene.getCamera().getOrientation());
            }
        }
        return super.onEvent(event);
    }
}
