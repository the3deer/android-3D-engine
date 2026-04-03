// in /.../entities/Controller.java
package org.the3deer.engine.services.collada.entities;

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
