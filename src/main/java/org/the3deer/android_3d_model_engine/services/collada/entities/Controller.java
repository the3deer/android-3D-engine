// in /.../entities/Controller.java
package org.the3deer.android_3d_model_engine.services.collada.entities;

import java.util.List;

public class Controller {

    private final String id;
    private final Skin skin;

    public Controller(String id, Skin skin) {
        this.id = id;
        this.skin = skin;
    }

    public Skin getSkin() {
        return skin;
    }

    public String getId() {
        return id;
    }

}
