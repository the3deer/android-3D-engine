package org.the3deer.android_3d_model_engine.scene;

import android.app.Activity;
import android.app.AlertDialog;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.the3deer.android_3d_model_engine.animation.Animation;
import org.the3deer.android_3d_model_engine.animation.Animator;
import org.the3deer.android_3d_model_engine.collision.CollisionEvent;
import org.the3deer.android_3d_model_engine.controller.TouchEvent;
import org.the3deer.android_3d_model_engine.event.SelectedObjectEvent;
import org.the3deer.android_3d_model_engine.model.AnimatedModel;
import org.the3deer.android_3d_model_engine.model.Camera;
import org.the3deer.android_3d_model_engine.model.Constants;
import org.the3deer.android_3d_model_engine.model.Dimensions;
import org.the3deer.android_3d_model_engine.model.Light;
import org.the3deer.android_3d_model_engine.model.Material;
import org.the3deer.android_3d_model_engine.model.Node;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.android_3d_model_engine.model.Scene;
import org.the3deer.android_3d_model_engine.model.Skin;
import org.the3deer.android_3d_model_engine.model.Transform;
import org.the3deer.android_3d_model_engine.objects.Point;
import org.the3deer.android_3d_model_engine.view.RenderListener;
import org.the3deer.util.android.ContentUtils;
import org.the3deer.util.event.EventListener;
import org.the3deer.util.event.EventManager;
import org.the3deer.util.io.IOUtils;
import org.the3deer.util.math.Math3DUtils;
import org.the3deer.util.math.Quaternion;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class loads a 3D scene as an example of what can be done with the app
 *
 * @author andresoviedo
 */
public class SceneImpl implements EventListener, RenderListener, org.the3deer.android_3d_model_engine.model.Scene {

    public static final String TAG = SceneImpl.class.getSimpleName();

    private String name = "Scene_" + System.identityHashCode(this);
    /**
     * Parent component
     */
    protected Activity parent;
    /**
     * Model uri
     */
    private URI uri;
    /**
     * 0 = obj, 1 = stl, 2 = dae
     */
    private int type;

    /**
     * List of root hierarchies
     */
    private List<Node> rootNodes = new ArrayList<>();
    /**
     * Skin data for every root node
     */
    private List<Skin> skinData = new ArrayList<>();
    /**
     * List of 3D models
     */
    private List<Object3DData> objects = new ArrayList<>();
    /**
     * List of cameras
     */
    private List<Camera> cameras;
    /**
     * List of Animations
     */
    private List<Animation> animations;
    /**
     * Current animation
     */
    private Animation currentAnimation;
    /**
     * Default engine camera
     */
    private Camera defaultCamera;
    /**
     * Point of view camera
     */
    private Camera camera;
    /**
     * Blender uses different coordinate system.
     * This is a patch to turn camera and SkyBox 90 degree on X axis
     */
    private boolean isFixCoordinateSystem = false;
    /**
     * Enable or disable blending (transparency)
     */
    private boolean isBlendingEnabled = true;
    /**
     * Force transparency
     */
    private boolean isBlendingForced = false;
    /**
     * state machine for drawing modes
     */
    private int drawMode = 0;
    /**
     * Whether to draw objects as wireframes
     */
    private boolean drawWireframe = false;
    /**
     * Whether to draw using points
     */
    private boolean drawPoints = false;
    /**
     * Whether to draw bounding boxes around objects
     */
    private boolean drawBoundingBox = false;
    /**
     * Whether to draw face normals. Normally used to debug models
     */
    private boolean drawNormals = false;
    /**
     * Whether to draw using textures
     */
    private boolean drawTextures = true;
    /**
     * Whether to draw using colors or use default white color
     */
    private boolean drawColors = true;
    private ArrayList<Material> materials;

    /**
     * Light toggle feature: we have 3 states: no light, light, light + rotation
     */
    public enum LightProfile {
        Rotating, PointOfView, Off
    }

    private LightProfile lightProfile = LightProfile.Rotating;

    /**
     * Animate model (dae only) or not
     */
    private boolean doAnimation = true;
    /**
     * Animate model (dae only) or not
     */
    private boolean isSmooth = false;
    /**
     * show bind pose only
     */
    private boolean showBindPose = false;
    /**
     * Draw skeleton or not
     */
    private boolean drawSkeleton = false;
    /**
     * Toggle collision detection
     */
    private boolean isCollision = false;
    /**
     * Toggle 3d
     */
    private boolean isStereoscopic = false;
    /**
     * Toggle 3d anaglyph (red, blue glasses)
     */
    private boolean isAnaglyph = false;
    /**
     * Toggle 3d VR glasses
     */
    private boolean isVRGlasses = false;
    /**
     * Object selected by the user
     */
    private Object3DData selectedObject = null;
    /**
     * Light bulb 3d data
     */
    private final Object3DData lightBulb = Point.build(Constants.LIGHT_BULB_LOCATION).setId("light");
    /**
     * Animator
     */
    private Animator animator = new Animator();
    /**
     * Did the user touched the model for the first time?
     */
    private boolean userHasInteracted;
    /**
     * time when model loading has started (for stats)
     */
    private long startTime;

    /**
     * A cache to save original model dimensions before rescaling them to fit in screen
     * This enables rescaling several times
     */
    private Map<Object3DData, Dimensions> originalDimensions = new HashMap<>();

    private Map<Object3DData, Transform> originalTransforms = new HashMap<>();

    // This matrix will hold the global scale for the entire scene.
    private final float[] worldMatrix = Math3DUtils.IDENTITY_MATRIX.clone();

    private EventManager eventManager;

    private boolean enabled = true;

    public SceneImpl() {
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @NonNull
    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getId() {
        return this.name;
    }

/*public Scene(Activity main, Camera camera) {
        this(main, camera, null, -1);
    }

    public Scene(Activity main, Camera camera, URI uri, int type) {
        this.parent = main;
        this.camera = camera;
        this.uri = uri;
        this.type = type;

        float light_distance = Constants.UNIT;
        lightBulb.setLocation(new float[]{light_distance / 2, light_distance, 0});
        lightBulb.setColor(Constants.COLOR_WHITE);
    }*/

    @Override
    public void setDefaultCamera(Camera defaultCamera) {

        // check
        if (defaultCamera == null) throw new IllegalArgumentException("defaultCamera can't be null");

        // add default camera if not
        if (this.defaultCamera != null && cameras.contains(this.defaultCamera)) {
            cameras.remove(defaultCamera);
        }

        this.defaultCamera = defaultCamera;

        // add default camera if not
        if (this.cameras != null && !this.cameras.contains(defaultCamera)) {
            this.cameras.add(0, defaultCamera);
        }
    }

    @Override
    public void setCameras(List<Camera> cameras) {

        // check - ensure we have a camera available
        if (cameras != null && !cameras.isEmpty()) {

            // update camera
            this.cameras = cameras;

            // add default camera if not
            if (this.defaultCamera != null && !this.cameras.contains(defaultCamera)) {
                this.cameras.add(0, defaultCamera);
            }

        } else {
            Log.w(TAG, "setCameras with null or empty argument");
        }
    }

    /**
     * Blender:  object created in blender are oriented towards the z-axis (up-vector)
     * Therefore, this function is to fix orientation towards y-axis (natural up-vector)
     */
    public void fixCoordinateSystem() {
        final List<Object3DData> objects = getObjects();
        for (int i = 0; i < objects.size(); i++) {
            final Object3DData objData = objects.get(i);
            if (objData.getAuthoringTool() != null && objData.getAuthoringTool().toLowerCase().contains("blender")) {
                Matrix.rotateM(this.worldMatrix, 0, -90, 1, 0, 0);
                Log.d(TAG, "Fixed coordinate system to -90 degrees on y axis. worldMatrix: " + Arrays.toString(this.worldMatrix));
                break;
                /*Quaternion quaternion = Quaternion.getQuaternion(new float[]{1, 0, 0}, (float) (-Math.PI / 2f));
                quaternion.normalize();
                objData.setOrientation(quaternion);
                Log.i(TAG, "Fixed coordinate system to -90 degrees on x axis. object: " + objData.getId());
                this.isFixCoordinateSystem = true;*/
                //break;
            }
        }
    }

    //...
    public void addRootNode(Node node) {
        this.rootNodes.add(node);
    }

    public void setRootNodes(List<Node> rootNodes) {
        this.rootNodes = rootNodes;
    }

    public List<Node> getRootNodes() {
        return rootNodes;
    }

    public void addSkeleton(Skin skin) {

        // check
        if (this.skinData.contains(skin)) return;

        this.skinData.add(skin);
    }

    public List<Skin> getSkeletons() {
        return skinData;
    }

    public void setSkeletonData(List<Skin> skinData) {
        this.skinData = skinData;
    }

    public boolean isFixCoordinateSystem() {
        return this.isFixCoordinateSystem;
    }

    @Override
    public void setAnimations(List<Animation> animations) {
        this.animations = animations;

        // default animation
        if (animations != null && !animations.isEmpty()) {
            setCurrentAnimation(animations.get(0));
        }
    }

    @Override
    public final Camera getCamera() {
        if (camera == null){
            return defaultCamera;
        }
        return camera;
    }

    public Animator getAnimator() {
        return animator;
    }

    public float[] getWorldMatrix() {
        return worldMatrix;
    }


    @Override
    public void setMaterials(ArrayList<Material> materials) {
        this.materials = materials;
    }

    private void makeToastText(final String text, final int toastDuration) {
        if (parent == null) return;
        parent.runOnUiThread(() -> Toast.makeText(parent.getApplicationContext(), text, toastDuration).show());
    }

    public void setLightProfile(LightProfile lightProfile) {
        this.lightProfile = lightProfile;
    }

    /**
     * Hook for animating the objects before the rendering
     */
    public final void onPrepareFrame() {

        //animateLight();

        // initial camera animation. animate if user didn't touch the screen
        // animateCamera();

        if (objects.isEmpty()) return;

        /*if (doAnimation) {
            for (int i = 0; i < objects.size(); i++) {
                Object3DData obj = objects.get(i);
                if (obj instanceof AnimatedModel) {
                    animator.update(((AnimatedModel) obj).getRootJoint(), ((AnimatedModel) obj).getCurrentAnimation(), obj, isShowBindPose());
                }
            }
        }*/
    }

    private void animateLight() {
        if (lightProfile != LightProfile.Rotating) return;

        // animate light - Do a complete rotation every 60 seconds.
        long time = SystemClock.uptimeMillis() % 60000L;
        float angleInDegrees = (360.0f / 60000.0f) * ((int) time);
        lightBulb.setRotation(new float[]{0, angleInDegrees, 0});
    }

    /*private void animateCamera() {
        // smooth camera transition
        camera.animate();

        if (!userHasInteracted) {
            camera.translateCamera(0.0005f, 0f);
        }
    }*/

    @Override
    public final synchronized void addObject(Object3DData obj) {
        Log.d(TAG, "Adding object to scene: " + getName() + ", obj: " + obj);
        objects.add(obj);
        //requestRender();

        // rescale objects so they fit in the viewport
        // FIXME: this does not be reviewed
        //rescale(this.getObjects(), DEFAULT_MAX_MODEL_SIZE, new float[3]);
    }

    @Override
    public final synchronized void setObjects(List<Object3DData> objs) {
        Log.d(TAG, "Setting scene objects: " + objs);
        this.objects = objs;
    }


    @Override
    public final synchronized void addObjects(List<Object3DData> objs) {
        Log.d(TAG, "Adding objects to scene. objs: " + objs);
        objects.addAll(objs);
        //requestRender();

        // rescale objects so they fit in the viewport
        // FIXME: this does not be reviewed
        //rescale(this.getObjects(), DEFAULT_MAX_MODEL_SIZE, new float[3]);
    }

    /*public final synchronized void addGUIObject(Object3DData obj) {

        // log event
        Log.i(TAG, "Adding GUI object to scene... " + obj);

        // add object
        guiObjects.add(obj);

        // requestRender();
    }*/

    @Override
    public final synchronized List<Object3DData> getObjects() {
        return objects;
    }

    public void addAnimation(Animation animation) {
        if (this.animations == null) {
            this.animations = new ArrayList<>();
        }
        this.animations.add(animation);
        if (getCurrentAnimation() == null) {
            setCurrentAnimation(animation);
        }
    }

    public List<Animation> getAnimations() {
        return animations;
    }

    public Animation getCurrentAnimation() {
        return currentAnimation;
    }

    public void setCurrentAnimation(Animation currentAnimation) {
        this.currentAnimation = currentAnimation;
    }

    /*public final synchronized List<Object3DData> getGUIObjects() {
        return guiObjects;
    }*/

    public final void toggleWireframe() {

        // info: to enable normals, just change module to 5
        final int module = 4;
        this.drawMode = (this.drawMode + 1) % module;
        this.drawNormals = false;
        this.drawPoints = false;
        this.drawSkeleton = false;
        this.drawWireframe = false;

        // toggle state machine
        switch (drawMode) {
            case 0:
                makeToastText("Faces", Toast.LENGTH_SHORT);
                break;
            case 1:
                this.drawWireframe = true;
                makeToastText("Wireframe", Toast.LENGTH_SHORT);
                break;
            case 2:
                this.drawPoints = true;
                makeToastText("Points", Toast.LENGTH_SHORT);
                break;
            case 3:
                this.drawSkeleton = true;
                makeToastText("Skeleton", Toast.LENGTH_SHORT);
                break;
            case 4:
                this.drawWireframe = true;
                this.drawNormals = true;
                makeToastText("Normals", Toast.LENGTH_SHORT);
                break;
        }
        //requestRender();
    }

    public final boolean isDrawWireframe() {
        return this.drawWireframe;
    }

    public final boolean isDrawPoints() {
        return this.drawPoints;
    }

    public final void toggleBoundingBox() {
        this.drawBoundingBox = !drawBoundingBox;
        //requestRender();
    }

    public final boolean isDrawBoundingBox() {
        return drawBoundingBox;
    }

    public final boolean isDrawNormals() {
        return drawNormals;
    }

    public final void toggleTextures() {
        if (drawTextures && drawColors) {
            this.drawTextures = false;
            this.drawColors = true;
            makeToastText("Texture off", Toast.LENGTH_SHORT);
        } else if (drawColors) {
            this.drawTextures = false;
            this.drawColors = false;
            makeToastText("Colors off", Toast.LENGTH_SHORT);
        } else {
            this.drawTextures = true;
            this.drawColors = true;
            makeToastText("Textures on", Toast.LENGTH_SHORT);
        }
    }

    public final void toggleLighting() {
        switch (lightProfile) {
            case Rotating:
                lightProfile = LightProfile.PointOfView;
                makeToastText("Light Rotating", Toast.LENGTH_SHORT);
                break;
            case PointOfView:
                lightProfile = LightProfile.Off;
                makeToastText("Light POV", Toast.LENGTH_SHORT);
                break;
            case Off:
                lightProfile = LightProfile.Rotating;
                makeToastText("Light Off", Toast.LENGTH_SHORT);
                break;
        }
    }

    public final void toggleAnimation() {
        //showAnimationsDialog();
        if (!this.doAnimation) {
            this.doAnimation = true;
            this.showBindPose = false;
            makeToastText("Animation on", Toast.LENGTH_SHORT);
        } else {
            this.doAnimation = false;
            this.showBindPose = true;
            makeToastText("Bind pose", Toast.LENGTH_SHORT);
        }
    }

    public final void toggleSmooth() {
        for (int i = 0; i < getObjects().size(); i++) {
            if (getObjects().get(i).getMeshData() == null) continue;
            if (!this.isSmooth) {
                getObjects().get(i).getMeshData().smooth();
            } else {
                getObjects().get(i).getMeshData().unSmooth();
            }
            getObjects().get(i).getMeshData().refreshNormalsBuffer();
        }
        this.isSmooth = !this.isSmooth;
    }

    public final void showSettingsDialog() {

        final AnimatedModel animatedModel;
        if (objects.get(0) instanceof AnimatedModel) {
            animatedModel = (AnimatedModel) objects.get(0);
        } else return;

        AlertDialog.Builder builderSingle = new AlertDialog.Builder(this.parent);
        builderSingle.setTitle("Scene");
        String[] items = new String[]{"Geometries...", "Joints..."};
        builderSingle.setItems(items, (dialog, which) -> {
            switch (which) {
                case 0:
                    showGeometriesDialog(animatedModel);
                    break;
                case 1:
                    showJointsDialog(animatedModel);
                    break;
            }
        });
        builderSingle.setPositiveButton("Close", (dialog, which) -> {
            dialog.dismiss();
        });

        builderSingle.show();
    }

    private void showGeometriesDialog(final AnimatedModel animatedModel) {
        final AlertDialog.Builder builderSingle = new AlertDialog.Builder(this.parent);
        builderSingle.setTitle("Geometries");
        final String[] items = new String[animatedModel.getElements().size()];
        boolean[] selected = new boolean[animatedModel.getElements().size()];
        for (int i = 0; i < items.length; i++) {
            String jointId = "Element #" + i;
            items[i] = jointId;
            selected[i] = true;
        }

        builderSingle.setMultiChoiceItems(items, selected, (dialog, which, isChecked) -> {
            animatedModel.getElements().remove(which);
        });

        builderSingle.setPositiveButton("Close", (dialog, which) -> {
            dialog.dismiss();
        });
        builderSingle.show();
    }

    private void showJointsDialog(final AnimatedModel animatedModel) {
        final AlertDialog.Builder builderSingle = new AlertDialog.Builder(this.parent);
        builderSingle.setTitle("Joints");
        final String[] items = new String[animatedModel.getElements().size()];
        boolean[] selected = new boolean[animatedModel.getElements().size()];
        for (int i = 0; i < items.length; i++) {
            String jointId = "Joint #" + i;
            items[i] = jointId;
            selected[i] = true;
        }

        builderSingle.setMultiChoiceItems(items, selected, (dialog, which, isChecked) -> {
            animatedModel.getElements().remove(which);
        });

        builderSingle.setPositiveButton("Close", (dialog, which) -> {
            dialog.dismiss();
        });
        builderSingle.show();
    }

    public final boolean isDoAnimation() {
        return doAnimation;
    }

    public final boolean isShowBindPose() {
        return showBindPose;
    }

    public final void toggleCollision() {
        this.isCollision = !isCollision;
        makeToastText("Collisions: " + isCollision, Toast.LENGTH_SHORT);
    }

    public final void toggleStereoscopic() {
        if (!this.isStereoscopic) {
            this.isStereoscopic = true;
            this.isAnaglyph = true;
            this.isVRGlasses = false;
            makeToastText("Stereoscopic Anaplygh", Toast.LENGTH_SHORT);
        } else if (this.isAnaglyph) {
            this.isAnaglyph = false;
            this.isVRGlasses = true;
            // move object automatically cause with VR glasses we still have no way of moving object
            this.userHasInteracted = false;
            makeToastText("Stereoscopic VR Glasses", Toast.LENGTH_SHORT);
        } else {
            this.isStereoscopic = false;
            this.isAnaglyph = false;
            this.isVRGlasses = false;
            makeToastText("Stereoscopic disabled", Toast.LENGTH_SHORT);
        }
        // recalculate camera
        this.camera.setChanged(true);
    }

    public final boolean isVRGlasses() {
        return isVRGlasses;
    }

    public final boolean isDrawTextures() {
        return drawTextures;
    }

    public final boolean isDrawColors() {
        return drawColors;
    }

    public final boolean isDrawLighting() {
        return lightProfile != LightProfile.Off;
    }

    public final boolean isDrawSkeleton() {
        return drawSkeleton;
    }

    public final boolean isCollision() {
        return isCollision;
    }

    public final boolean isStereoscopic() {
        return isStereoscopic;
    }

    public final boolean isAnaglyph() {
        return isAnaglyph;
    }

    public final void toggleBlending() {
        if (this.isBlendingEnabled && !this.isBlendingForced) {
            makeToastText("X-Ray enabled", Toast.LENGTH_SHORT);
            this.isBlendingEnabled = true;
            this.isBlendingForced = true;
        } else if (this.isBlendingForced) {
            makeToastText("Blending disabled", Toast.LENGTH_SHORT);
            this.isBlendingEnabled = false;
            this.isBlendingForced = false;
        } else {
            makeToastText("X-Ray disabled", Toast.LENGTH_SHORT);
            this.isBlendingEnabled = true;
            this.isBlendingForced = false;
        }
    }

    public final boolean isBlendingEnabled() {
        return isBlendingEnabled;
    }

    public final boolean isBlendingForced() {
        return isBlendingForced;
    }


    public synchronized void onLoadComplete() {

        if (objects == null || objects.isEmpty()) {
            Log.w(TAG, "No objects were loaded");
        } else {
            Log.i(TAG, "onLoadComplete: " + getName() + ", Objects: " + objects.size());
        }


        // get complete list of objects loaded
        final List<Object3DData> objs = getObjects();

        for (int i = 0; i < objs.size(); i++) {
            for (int m = 0; m < objs.size(); m++) {
                loadTextureDatas(objs.get(m).getMaterial());
            }
        }

        // show object errors
        List<String> allErrors = new ArrayList<>();
        for (Object3DData data : objs) {
            allErrors.addAll(data.getErrors());
        }
        if (!allErrors.isEmpty()) {
            makeToastText(allErrors.toString(), Toast.LENGTH_LONG);
        }

        // notify user
        final String elapsed = (SystemClock.uptimeMillis() - startTime) / 1000 + " secs";
        makeToastText("Load complete (" + elapsed + ")", Toast.LENGTH_SHORT);

        // rescale all object so they fit in the screen
        //rescale(this.getObjects(), DEFAULT_MAX_MODEL_SIZE, new float[3]);
        //if (this.getObjects().stream().filter(obj->obj.isPinned()).count() == 1){
        final List<Object3DData> list = new ArrayList<>();
        for (int i = 0; i < getObjects().size(); i++) {
            if (getObjects().get(i).isPinned()) continue;
            list.add(getObjects().get(i));
        }
        /*if (list.size() == 1) {
            for (int i = 0; i < list.size(); i++) {
                list.get(i).setCentered(true);
            }
        }*/

        // Ensure all objects have a parent node to unify the rendering pipeline.
        if (getRootNodes() == null || getRootNodes().isEmpty()) {
            Log.i(TAG, "Scene has no root nodes. Creating default nodes for all objects.");
            List<Node> rootNodes = new ArrayList<>();
            for (Object3DData obj : getObjects()) {
                // Create a new node and assign the object to it.
                Node node = new Node();
                node.setMesh(obj); // Link the visible object to this node.
                node.setMatrix(obj.getModelMatrix());
                obj.setParentNode(node);
                obj.setParentBound(true);
                rootNodes.add(node);
            }
            setRootNodes(rootNodes);
        }

        // 1. UPDATE THE STATIC SCENE GRAPH
        // This sets the base pose for everything, including skeletons.
        if (getRootNodes() != null && !getRootNodes().isEmpty()) {
            for (int i = 0; i < getRootNodes().size(); i++) {
                // This method should recursively update all children
                getRootNodes().get(i).updateBindWorldTransform(Math3DUtils.IDENTITY_MATRIX);
            }
        }

        // fix coordinate system
        if (Constants.FIX_COORDINATE_SYSTEM) {
            fixCoordinateSystem();
        }

        // rescale objects so they all fit in the viewport
        if (Constants.FIX_SCALE) {
            rescale(list, Constants.DEFAULT_MODEL_SIZE, new float[3]);
        }
    }

    private void loadTextureDatas(Material mat) {
        if (mat == null) return;
        if (mat.getColorTexture() != null && mat.getColorTexture().getFile() != null) {
            String textureFile = mat.getColorTexture().getFile();
            Log.i(TAG, "Loading texture file: " + textureFile);
            try (InputStream stream = ContentUtils.getInputStream(textureFile)) {
                mat.getColorTexture().setData(IOUtils.read(stream));
                Log.i(TAG, "Texture successfully loaded: " + textureFile);
            } catch (Exception ex) {
                Log.e(TAG, "Error loading texture file '" + textureFile + "': " + ex.getMessage(), ex);
                makeToastText("Error loading texture file '" + textureFile + "': " + ex.getMessage(), Toast.LENGTH_LONG);
            }

        }
    }

    @Override
    public Object3DData getSelectedObject() {
        return selectedObject;
    }

    @Override
    public void setSelectedObject(Object3DData selectedObject) {
        this.selectedObject = selectedObject;
        /*if (eventManager != null) {
            eventManager.propagate(new SelectedObjectEvent(this, selectedObject));
        }*/
    }

/*    public void loadTexture(Object3DData obj, Uri uri) throws IOException {
        if (obj == null && objects.size() != 1) {
            makeToastText("Unavailable", Toast.LENGTH_SHORT);
            return;
        }
        obj = obj != null ? obj : objects.get(0);

        // load new texture
        obj.setTextureData(IOUtils.read(ContentUtils.getInputStream(uri)));

        this.drawTextures = true;
    }*/

    public final boolean isRotatingLight() {
        return lightProfile == LightProfile.Rotating;
    }

    @Override
    public boolean onEvent(EventObject event) {
        Object3DData selectedObject = getSelectedObject();
        if (event instanceof TouchEvent) {
            userHasInteracted = true;
            float[] right = camera.getRight();
            float[] up = camera.getUp();
            float[] pos = camera.getPos().clone();
            Math3DUtils.normalizeVector(pos);
            TouchEvent touch = (TouchEvent) event;
            if (touch.getAction() == TouchEvent.Action.ROTATE && selectedObject != null) {

                float angle = touch.getAngle();
                float factor = 1f; //1/360f * touch.getLength();


                // Log.v(TAG, "Q: Quaternion angle: " + Math.toDegrees(angle) + " ,dx:" + touch.getdX() + ", dy:" + -touch.getdY());
                Quaternion q0 = Quaternion.getQuaternion(pos, angle * factor);
                //q0.normalize();

                Quaternion multiply = Quaternion.multiply(selectedObject.getOrientation(), q0);
                selectedObject.setOrientation(multiply);

                return true;
            } else if (touch.getAction() == TouchEvent.Action.MOVE && selectedObject != null) {

                float angle = (float) (Math.atan2(-touch.getdY(), touch.getdX()));
                Log.v(TAG, "Rotating (axis:var): " + Math.toDegrees(angle) + " ,dx:" + touch.getdX() + ", dy:" + -touch.getdY());

                float[] rightd = Math3DUtils.multiply(right, touch.getdY());
                float[] upd = Math3DUtils.multiply(up, touch.getdX());
                float[] rot = Math3DUtils.add(rightd, upd);
                if (Math3DUtils.length(rot) > 0) {
                    rot = Math3DUtils.normalize2(rot);
                } else {
                    rot = new float[]{1, 0, 0};
                }

                float angle1 = touch.getLength() / 360;
                Quaternion q1 = Quaternion.getQuaternion(rot, angle1);
                //q1.normalize();

                Quaternion multiply = Quaternion.multiply(selectedObject.getOrientation(), q1);
                //multiply.normalize();

                selectedObject.setOrientation(multiply);

                return true;
            }
        } else if (event instanceof CollisionEvent) {
            this.userHasInteracted = true;
            Object3DData objectToSelect = ((CollisionEvent) event).getObject();
            float[] point = ((CollisionEvent) event).getPoint();
            if (isCollision() && point != null) {
                Log.v(TAG, "Adding collision point " + Arrays.toString(point));
                Object3DData point3D = Point.build(point).setColor(new float[]{1.0f, 0f, 0f, 1f});
                addObject(point3D);
            }
            if (selectedObject == objectToSelect) {
                Log.v(TAG, "Unselected object " + objectToSelect);
                setSelectedObject(null);
            } else {
                Log.i(TAG, "Selected object " + objectToSelect.getId());
                Log.d(TAG, "Selected object " + objectToSelect);
                setSelectedObject(objectToSelect);
            }
            return true;
        }
        return false;
    }

    private void rescale(List<Object3DData> datas, float newScale, float[] newPosition) {

        //if (true) return;

        // check we have objects to scale, otherwise, there should be an issue with LoaderTask
        if (datas == null || datas.isEmpty()) {
            return;
        }

        Log.d(TAG, "Calculating world matrix... objects: " + datas.size());
        // calculate the global max length
        final Object3DData firstObject = datas.get(0);
        final Dimensions currentDimensions;
        if (this.originalDimensions.containsKey(firstObject)) {
            currentDimensions = this.originalDimensions.get(firstObject);
        } else {
            currentDimensions = firstObject.getCurrentDimensions();
            this.originalDimensions.put(firstObject, currentDimensions);
        }
        Log.v(TAG, "Model[0] dimension: " + firstObject.getDimensions());
        Log.v(TAG, "Model[0] current dimension: " + currentDimensions);

        final float[] corner01 = currentDimensions.getCornerLeftTopNearVector();

        final float[] corner02 = currentDimensions.getCornerRightBottomFar();
        final float[] center01 = currentDimensions.getCenter();

        float maxLeft = corner01[0];
        float maxTop = corner01[1];
        float maxNear = corner01[2];
        float maxRight = corner02[0];
        float maxBottom = corner02[1];
        float maxFar = corner02[2];
        float maxCenterX = center01[0];
        float maxCenterY = center01[1];
        float maxCenterZ = center01[2];

        for (int i = 1; i < datas.size(); i++) {

            final Object3DData obj = datas.get(i);

            final Dimensions original;
            if (this.originalDimensions.containsKey(obj)) {
                original = this.originalDimensions.get(obj);
                Log.v(TAG, "Found dimension: " + original.toString());
            } else {
                original = obj.getCurrentDimensions();
                this.originalDimensions.put(obj, original);
            }


            Log.v(TAG, "Model[" + i + "] '" + obj.getId() + "' dimension: " + obj.getDimensions());
            Log.v(TAG, "Model[" + i + "] '" + obj.getId() + "' current dimension: " + original.toString());
            final float[] corner1 = original.getCornerLeftTopNearVector();
            final float[] corner2 = original.getCornerRightBottomFar();
            final float[] center = original.getCenter();
            float maxLeft2 = corner1[0];
            float maxTop2 = corner1[1];
            float maxNear2 = corner1[2];
            float maxRight2 = corner2[0];
            float maxBottom2 = corner2[1];
            float maxFar2 = corner2[2];
            float centerX = center[0];
            float centerY = center[1];
            float centerZ = center[2];

            if (maxRight2 > maxRight) maxRight = maxRight2;
            if (maxLeft2 < maxLeft) maxLeft = maxLeft2;
            if (maxTop2 > maxTop) maxTop = maxTop2;
            if (maxBottom2 < maxBottom) maxBottom = maxBottom2;
            if (maxNear2 > maxNear) maxNear = maxNear2;
            if (maxFar2 < maxFar) maxFar = maxFar2;
            if (maxCenterX < centerX) maxCenterX = centerX;
            if (maxCenterY < centerY) maxCenterY = centerY;
            if (maxCenterZ < centerZ) maxCenterZ = centerZ;
        }
        float lengthX = maxRight - maxLeft;
        float lengthY = maxTop - maxBottom;
        float lengthZ = maxNear - maxFar;

        float maxLength = lengthX;
        if (lengthY > maxLength) maxLength = lengthY;
        if (lengthZ > maxLength) maxLength = lengthZ;

        if (maxLength == 0.0f) {
            Log.w(TAG, "Max length is 0, cannot rescale.");
            return;
        }
        Log.v(TAG, "Max length: " + maxLength);

        float maxLocation = 0;
        if (datas.size() > 1) {
            maxLocation = maxCenterX;
            if (maxCenterY > maxLocation) maxLocation = maxCenterY;
            if (maxCenterZ > maxLocation) maxLocation = maxCenterZ;
        }
        //Log.v(TAG, "Max location: " + maxLocation);

        // calculate the scale factor
        float scaleFactor = newScale / (maxLength + maxLocation);
        Log.v(TAG, "scale factor: " + scaleFactor);

        // calculate the global center
        float centerX = (maxRight + maxLeft) / 2;
        float centerY = (maxTop + maxBottom) / 2;
        float centerZ = (maxNear + maxFar) / 2;
        //Log.v(TAG, "Total center: " + centerX + "," + centerY + "," + centerZ);

        // calculate the new location
        float translationX = -centerX + newPosition[0];
        float translationY = -centerY + newPosition[1];
        float translationZ = -centerZ + newPosition[2];
        final float[] globalDifference = new float[]{translationX, translationY, translationZ};
        Log.v(TAG, "Translation delta: " + Arrays.toString(globalDifference));


        if (scaleFactor < 0.5f || scaleFactor > 1.5f) {
            Matrix.translateM(this.worldMatrix, 0, globalDifference[0] * scaleFactor, globalDifference[1] * scaleFactor, globalDifference[2] * scaleFactor);
            Matrix.scaleM(this.worldMatrix, 0, scaleFactor, scaleFactor, scaleFactor);
        } else {
            Matrix.translateM(this.worldMatrix, 0, globalDifference[0] * scaleFactor, globalDifference[1] * scaleFactor, globalDifference[2] * scaleFactor);
        }
        Log.d(TAG, "World matrix: " + Arrays.toString(this.worldMatrix));
    }


    public void onOrientationChanged(int orientation) {
        /*if (orientation == 0 || orientation == 180) {
                    Log.v("ModelActivity","onOrientationChanged: portrait: "+orientation);
                } else if (orientation == 90 || orientation == 270) {
                    Log.v("ModelActivity","onOrientationChanged: landscape: "+orientation);
                } else {
                    Log.v("ModelActivity","onOrientationChanged: orientation: "+orientation);
                }*/
        Log.v(TAG, "onOrientationChanged: orientation: " + orientation);
        camera.setDeviceOrientation(orientation);
    }

    @Override
    public void setCamera(Camera camera) {

        // check
        if (camera == null) throw new IllegalArgumentException("active camera cannot be null");

        // update
        this.camera = camera;
    }

    @Override
    public List<Camera> getCameras() {
        if (this.cameras == null){
            return Collections.singletonList(defaultCamera);
        }
        return this.cameras;
    }

    public void setBlendingEnabled(boolean enabled) {
        this.isBlendingEnabled = enabled;
    }

    public void setBlendingForced(boolean forced) {
        isBlendingForced = forced;
    }

    @Override
    public void merge(Scene other) {
        if (other == null) return;

        // merge objects
        if (other.getObjects() != null) {
            addObjects(other.getObjects());
        }

        // merge skeletons
        if (other.getSkeletons() != null) {
            for (Skin skin : other.getSkeletons()) {
                addSkeleton(skin);
            }
        }

        // merge root nodes
        if (other.getRootNodes() != null) {
            for (Node node : other.getRootNodes()) {
                addRootNode(node);
            }
        }

        // merge animations
        if (other.getAnimations() != null) {
            for (Animation animation : other.getAnimations()) {
                addAnimation(animation);
            }
        }
    }
}
