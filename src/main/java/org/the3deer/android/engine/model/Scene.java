package org.the3deer.android.engine.model;

import org.the3deer.android.engine.animation.Animation;
import org.the3deer.android.util.Matrix;
import org.the3deer.util.math.Math3DUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class Scene {

    private final Logger logger = Logger.getLogger(this.getClass().getName());

    private String id;
    private String name;

    private final float[] worldMatrix = new float[16];
    {
        Matrix.setIdentityM(worldMatrix, 0);
    }

    // Core Hierarchy
    private final List<Node> rootNodes = new ArrayList<>();
    private final List<Object3D> objects = new ArrayList<>();
    private final List<Skin> skins = new ArrayList<>();

    // Object Management
    private Object3D selectedObject;

    // Texture Management
    private final List<Texture> textures = new ArrayList<>();

    // Camera Management
    private final List<Camera> cameras = new ArrayList<>();
    private Camera activeCamera;

    // Animation Management
    private List<Animation> animations = new ArrayList<>();
    private Animation activeAnimation;
    private Dimensions dimensions;
    private boolean isSmooth = false;
    private boolean isCollision = true;

    public Scene() {
        this("Scene_" + System.currentTimeMillis());
    }

    public Scene(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String aDefault) {
        this.id = aDefault;
    }

    public float[] getWorldMatrix() {
        return worldMatrix;
    }

    public List<Node> getRootNodes() {
        return rootNodes;
    }

    public List<Object3D> getObjects() {
        return objects;
    }

    public List<Skin> getSkins() {
        return skins;
    }

    public Object3D getSelectedObject() {
        return selectedObject;
    }

    public void setSelectedObject(Object3D selectedObject) {
        this.selectedObject = selectedObject;
    }

    public List<Texture> getTextures() {
        return textures;
    }

    public List<Camera> getCameras() {
        return cameras;
    }

    public Camera getActiveCamera() {
        return activeCamera;
    }


    public void addCamera(Camera camera) {
        this.cameras.add(camera);
    }

    public void setActiveCamera(Camera activeCamera) {
        this.activeCamera = activeCamera;
    }

    public List<Animation> getAnimations() {
        return animations;
    }

    public void setAnimations(List<Animation> animations) {
        this.animations = animations;
    }

    public Animation getActiveAnimation() {
        return activeAnimation;
    }

    public void setActiveAnimation(Animation activeAnimation) {
        this.activeAnimation = activeAnimation;
    }

    public Dimensions getDimensions() {
        if (dimensions == null) {
            Dimensions dimensions = new Dimensions();
            for (Object3D object : getObjects()) {
                if (object.isDecorator()) continue;
                final Dimensions objDimensions = object.getCurrentDimensions();
                if (objDimensions != null) {
                    float[] min = objDimensions.getMin();
                    float[] max = objDimensions.getMax();
                    dimensions.update(min[0], min[1], min[2]);
                    dimensions.update(max[0], max[1], max[2]);
                }
            }
            this.dimensions = (dimensions);

        }
        return dimensions;
    }

    public void setDimensions(Dimensions dimensions) {
        this.dimensions = dimensions;
    }

    public boolean isSmooth() {
        return isSmooth;
    }

    public void setSmooth(boolean smooth) {
        isSmooth = smooth;
    }

    public boolean isCollision() {
        return isCollision;
    }

    public void setCollision(boolean collision) {
        isCollision = collision;
    }

    public void update() {

        /*// center scene
        final float[] center = getDimensions().getCenter();
        Matrix.setIdentityM(worldMatrix, 0);
        Matrix.translateM(worldMatrix, 0, -center[0], -center[1], -center[2]);

        logger.info("- World transform translation: "+center[0]+", "+center[1]+", "+center[2]);

        for (Node node : rootNodes) {
            node.updateWorldTransform(worldMatrix);
        }*/

        for (Node node : rootNodes) {
            node.updateWorldTransform(Math3DUtils.IDENTITY_MATRIX);
        }

        if (!animations.isEmpty() && activeAnimation == null) {
            activeAnimation = animations.get(0);
            logger.info("- Animation active:: "+activeAnimation.getName());
        }
    }

    public void addObject(Object3D data) {
        logger.config("Adding object: " + data.getId());
        objects.add(data);
    }

    public void merge(Scene other) {
        // merge objects
        objects.addAll(other.objects);

        // merge skeletons
        skins.addAll(other.skins);

        // merge root nodes
        rootNodes.addAll(other.rootNodes);

        // merge animations
        animations.addAll(other.animations);
    }

    public long getMemoryUsage() {
        long memory = 0;
        for (Object3D object : objects) {
            memory += object.getMemoryUsage();
        }
        return memory;
    }
}
