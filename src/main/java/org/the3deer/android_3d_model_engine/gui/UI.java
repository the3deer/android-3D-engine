package org.the3deer.android_3d_model_engine.gui;

import org.the3deer.android_3d_model_engine.model.Camera;
import org.the3deer.util.bean.BeanManaged;
import org.the3deer.util.event.EventListener;

import java.util.List;

import javax.inject.Inject;

public class UI extends Widget implements EventListener {

    @Inject
    private Camera camera;
    @Inject
    private List<BeanManaged> beans;

    //private

    public UI() {
        super();
        setVisible(true);
        setRender(false);
    }

    public void setUp(){

    }
}
