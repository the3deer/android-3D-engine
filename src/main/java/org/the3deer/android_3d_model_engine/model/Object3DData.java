package org.the3deer.android_3d_model_engine.model;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import org.the3deer.android_3d_model_engine.collision.Octree;
import org.the3deer.android_3d_model_engine.services.collada.entities.MeshData;
import org.the3deer.android_3d_model_engine.shader.ShaderFactory;
import org.the3deer.util.android.AndroidUtils;
import org.the3deer.util.event.EventListener;
import org.the3deer.util.io.IOUtils;
import org.the3deer.util.math.Math3DUtils;
import org.the3deer.util.math.Quaternion;

import java.net.URI;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This is the basic 3D data necessary to build the 3D object
 * <p>
 * Transforms are applied as follows:
 * <p>
 * MATRIX = ROTATION X LOCATION X SCALE X ORIENTATION [X CENTER] X OBJECT
 *
 * @author andresoviedo
 */
public class Object3DData {

    public Object3DData(String id, FloatBuffer positions, FloatBuffer normals, FloatBuffer texCoords, FloatBuffer colors, Material material) {
        this.id = id;
        this.vertexArrayBuffer = positions;
        this.vertexNormalsArrayBuffer = normals;
        this.textureCoordsArrayBuffer = texCoords;
        this.vertexColorsArrayBuffer = colors;
        this.material = material;
    }

    private boolean isIndexed = false;

    public Object isIndexed() {
        return this.isIndexed;
    }

    public void setIndexed(boolean indexed) {
        this.isIndexed = indexed;
        this.drawUsingArrays = !indexed;
    }

    public static class ChangeEvent extends EventObject {
        public ChangeEvent(Object3DData source) {
            super(source);
        }

        @Override
        public Object3DData getSource() {
            return (Object3DData) super.getSource();
        }
    }

    /**
     * The node where this object is attached to (when there is a scene node hierarchy)
     */
    protected Node parentNode;
    /**
     * CAD Tool used to generate model
     */
    private String authoringTool;
    /**
     * Parent object if hierarchy of objects
     */
    protected Object3DData parent = null;
    /**
     * Bind transformation to parent
     */
    protected boolean isParentBound;
    /**
     * model resource locator
     */
    private URI uri;
    /**
     * model id
     */
    private String id;
    /**
     * model friendly name or joint name
     */
    private String name;
    /**
     * Whether to draw object using indices or not
     */
    private boolean drawUsingArrays = false;
    /**
     * Whether the object is to be drawn (and children)
     */
    private boolean isVisible = true;

    /**
     * Whether the object is to be rendered (not children)
     */
    private boolean render = true;

    /**
     * can be hit by collision colliders
     */
    private boolean isSolid = true;
    /**
     * responds to click
     */
    private boolean isClickable = false;
    /**
     * responds to movement
     */
    private boolean isMovable = false;

    /**
     * The minimum thing we can draw in space is a vertex (or point).
     * This drawing mode uses the vertexBuffer
     */
    private int drawMode = GLES20.GL_POINTS;

    // mesh vertex data
    private MeshData meshData = null;

    // Model data
    protected FloatBuffer vertexArrayBuffer = null;
    protected FloatBuffer vertexNormalsArrayBuffer = null;
    private FloatBuffer tangentBuffer = null;
    protected Buffer vertexColorsArrayBuffer = null;
    protected FloatBuffer textureCoordsArrayBuffer = null;
    protected List<Element> elements;
    /**
     * Object materials
     */
    private Materials materials;

    // simple object variables for drawing using arrays
    private Material material = new Material("default", "default");
    protected Buffer indexBuffer = null;

    // Processed arrays
    private List<int[]> drawModeList = null;

    // derived data
    private BoundingBox boundingBox;

    // Transformation data (ordered from top to bottom)
    protected Quaternion orientation = new Quaternion(0, 0, 0, 1);
    protected float[] scale = new float[]{1, 1, 1};
    protected float[] location = new float[]{0f, 0f, 0f};
    protected float[] rotation = new float[]{0f, 0f, 0f};

    // Transformation data (relative to parent)
    protected float[] relativeScale;

    // extra transforms
    // FIXME: can we remove this?
    private float[] rotation1 = null;
    private float[] rotation2 = null;
    private float[] rotation2Location = null;

    /**
     * This is the local rotation matrix
     */
    private final float[] orientationMatrix = new float[16];
    /**
     * This is the local transformation (M=TSR)
     */
    protected final float[] modelMatrix = new float[16];
    /**
     * Normal matrix
     */
    private final float[] normalMatrix = new float[16];
    private final float[] normalMatrixTemp = new float[16];
    /**
     * This is the local transformation (M=SR), except translation
     */
    private final float[] modelMatrix2 = new float[16];
    /**
     * This is the global transformation when we have node hierarchy (ie. {@code <visual_scene><node><transform></transform></node></visual_scene>}
     */
    private float[] worldTransform = Math3DUtils.IDENTITY_MATRIX.clone();
    /**
     * This is the final model transformation
     */
    private float[] finalModelMatrix = new float[16];

    {
        //
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.setIdentityM(modelMatrix2, 0);
        Matrix.setIdentityM(finalModelMatrix, 0);
        Matrix.setIdentityM(orientationMatrix, 0);
    }

    // whether the object has changed
    private boolean changed;

    // defines whether this object matrix can be changed
    private boolean readOnly;

    // fixed place
    private boolean pinned;

    /**
     * Defines whether the transformations are relative to it's bounding box center
     * Impacts the {@link #modelMatrix} calculation
     */
    private boolean centered;

    // Async Loader
    // current dimensions
    private Dimensions dimensions = null;

    // collision detection
    private Octree octree = null;

    // errors detected
    private List<String> errors = new ArrayList<>();

    // event listeners
    private final Set<EventListener> listeners = new HashSet<>();

    // skin property
    private boolean isSkinned = false;


    public Object3DData() {
    }

    public void setAuthoringTool(String authoringTool) {
        this.authoringTool = authoringTool;
    }

    public String getAuthoringTool() {
        return authoringTool;
    }

    public void setMeshData(MeshData meshData) {
        this.meshData = meshData;
    }

    public MeshData getMeshData() {
        return meshData;
    }

    public Object3DData(FloatBuffer vertexArrayBuffer) {
        this.vertexArrayBuffer = vertexArrayBuffer;
        this.setDrawUsingArrays(true);
        updateDimensions();
    }

    public Object3DData(FloatBuffer vertexArrayBuffer, Buffer drawOrder) {
        this.vertexArrayBuffer = vertexArrayBuffer;
        this.indexBuffer = drawOrder;
        this.setDrawUsingArrays(false);
        updateDimensions();
    }

    public Object3DData(FloatBuffer vertexArrayBuffer, FloatBuffer textureCoordsArrayBuffer, byte[] texData) {
        this.vertexArrayBuffer = vertexArrayBuffer;
        this.textureCoordsArrayBuffer = textureCoordsArrayBuffer;
        this.getMaterial().setColorTexture(new Texture().setData(texData));
        this.setDrawUsingArrays(true);
        updateDimensions();
    }

    public Object3DData(FloatBuffer vertexArrayBuffer, FloatBuffer vertexColorsArrayBuffer,
                        FloatBuffer textureCoordsArrayBuffer, byte[] texData) {
        this.vertexArrayBuffer = vertexArrayBuffer;
        this.vertexColorsArrayBuffer = vertexColorsArrayBuffer;
        this.textureCoordsArrayBuffer = textureCoordsArrayBuffer;
        this.getMaterial().setColorTexture(new Texture().setData(texData));
        this.setDrawUsingArrays(true);
        updateDimensions();
    }

    public Object3DData(FloatBuffer verts, FloatBuffer normals,
                        Materials materials) {
        super();
        this.vertexArrayBuffer = verts;
        this.vertexNormalsArrayBuffer = normals;
        this.materials = materials;
        this.setDrawUsingArrays(false);
        this.updateDimensions();
    }

    public Node getParentNode() {
        return parentNode;
    }

    public Object3DData setParentNode(Node parentNode) {
        this.parentNode = parentNode;
        return this;
    }

    public Object3DData getParent() {
        return parent;
    }

    public Object3DData setParent(Object3DData parent) {
        this.parent = parent;
        return this;
    }

    public Object3DData setParentBound(boolean bind) {
        this.isParentBound = bind;
        return this;
    }

    public boolean isParentBound() {
        return this.isParentBound;
    }


    public Object3DData setId(String id) {
        this.id = id;
        return this;
    }

    public String getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setSkined(boolean skinned) {
        this.isSkinned = skinned;
    }

    public boolean isSkined() {
        return this.isSkinned;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public URI getUri() {
        return this.uri;
    }

    public boolean isDrawUsingArrays() {
        return drawUsingArrays;
    }

    public Object3DData setDrawUsingArrays(boolean drawUsingArrays) {
        this.drawUsingArrays = drawUsingArrays;
        this.isIndexed = !drawUsingArrays;
        return this;
    }

    public Material getMaterial() {
        if (material == null) material = new Material("default", "default");
        return material;
    }

    public Object3DData setMaterial(Material material) {
        this.material = material;
        return this;
    }

    public Materials getMaterials() {
        return materials;
    }


    public Object3DData setSolid(boolean solid) {
        isSolid = solid;
        return this;
    }

    public boolean isSolid() {
        return isSolid;
    }


    public boolean isClickable() {
        return isClickable;
    }

    public void setClickable(boolean clickable) {
        isClickable = clickable;
    }

    public boolean isMovable() {
        return isMovable;
    }

    public void setMovable(boolean movable) {
        isMovable = movable;
        isSolid = true;
    }

    // ---------------------------- dimensions ----------------------------- //

    public void setDimensions(Dimensions dimensions) {
        this.dimensions = dimensions;
        //Log.d("Object3DData", "New fixed dimensions for " + getId() + ": " + this.dimensions);
    }

    public Dimensions getDimensions() {
        if (dimensions == null) {
            refreshDimensions();
            Log.v("Object3DData", "New dimensions for '" + getId() + "': " + this.dimensions);
        }
        return dimensions;
    }

    // REPLACEMENT for the refreshDimensions() method

    private void refreshDimensions() {
        final Dimensions dimensions = new Dimensions();

        // Do nothing if there are no vertices
        if (vertexArrayBuffer == null || vertexArrayBuffer.capacity() == 0) {
            this.dimensions = dimensions;
            return;
        }

        // Ensure buffer position is at the start
        vertexArrayBuffer.position(0);

        // Case 1: The object is drawn using glDrawArrays (non-indexed)
        // This is typically true when 'elements' is null or empty.
        if (isDrawUsingArrays()) {
            for (int i = 0; i < vertexArrayBuffer.capacity() / 3; i++) {
                dimensions.update(vertexArrayBuffer.get(i * 3), vertexArrayBuffer.get(i * 3 + 1), vertexArrayBuffer.get(i * 3 + 2));
            }
        }
        // Case 2: The object is drawn using glDrawElements (indexed)
        else if (this.elements != null && !this.elements.isEmpty()) {
            // We only need to process the indices. All elements share the same vertex buffer.
            // Using a Set to avoid updating dimensions for the same vertex multiple times.
            Set<Integer> processedIndices = new HashSet<>();
            for (Element element : getElements()) {
                final Buffer indexBuffer = element.getIndexBuffer();
                if (indexBuffer != null) {
                    indexBuffer.position(0);
                    for (int i = 0; i < indexBuffer.capacity(); i++) {
                        final int idx = IOUtils.getIntBufferValue(indexBuffer, i);

                        if (processedIndices.contains(idx)) {
                            continue; // Already processed this vertex, skip.
                        }

                        if (idx < 0 || idx >= vertexArrayBuffer.capacity() / 3) {
                            Log.w("Object3DData", "Wrong index '" + idx + "' while getting dimensions for '" + getId() + "'");
                            continue;
                        }

                        dimensions.update(vertexArrayBuffer.get(idx * 3), vertexArrayBuffer.get(idx * 3 + 1), vertexArrayBuffer.get(idx * 3 + 2));
                        processedIndices.add(idx);
                    }
                }
            }
        } else if (indexBuffer != null) {
            // If we have an index buffer but no elements, we can still use it to calculate dimensions
            indexBuffer.position(0);
            for (int i = 0; i < indexBuffer.capacity(); i++) {
                final int idx = IOUtils.getIntBufferValue(indexBuffer, i);

                if (idx < 0 || idx >= vertexArrayBuffer.capacity() / 3) {
                    Log.w("Object3DData", "Wrong index '" + idx + "' while getting dimensions for '" + getId() + "'");
                    continue;
                }

                dimensions.update(vertexArrayBuffer.get(idx * 3), vertexArrayBuffer.get(idx * 3 + 1), vertexArrayBuffer.get(idx * 3 + 2));
            }
        } else {
            // No elements defined, fallback to processing all vertices (similar to non-indexed case)
            for (int i = 0; i < vertexArrayBuffer.capacity() / 3; i++) {
                dimensions.update(vertexArrayBuffer.get(i * 3), vertexArrayBuffer.get(i * 3 + 1), vertexArrayBuffer.get(i * 3 + 2));
            }
        }

        this.dimensions = dimensions;
    }


    public Object3DData setRelativeScale(float[] relativeScale) {
        this.relativeScale = relativeScale;

        // default scale
        /*if (getParent() != null) {

            // recalculate based on parent
            // That is, -1+1 is 100% parent dimension
            Dimensions parentDim = getParent().getCurrentDimensions();
            float relScale = parentDim.getRelationTo(getCurrentDimensions());
            Log.v("Object3DData", "Relative scale for '" + getId() + "': " + relScale);
            float[] newScale = Math3DUtils.multiply(relativeScale, relScale);

            setScale(newScale);
        }*/

        refreshRelativeScale();

        return this;
    }

    public void refresh() {
        refreshRelativeScale();
        updateModelMatrix();
    }

    protected void refreshRelativeScale() {
        // checks
        if (getParent() == null) return;
        if (relativeScale == null) return;
        if (getParent().getCurrentDimensions().getLargest() == 0) return;

        // recalculate based on parent
        // That is, -1+1 is 100% parent dimension
        Dimensions parentDim = getParent().getCurrentDimensions();
        //Log.v("Widget", "Relative scale. Parent dim (" + parentDim + ")");
        //Log.v("Widget", "Relative scale. Current dim (" + getDimensions() + ")");
        float realScale = getDimensions().getRelationTo(parentDim);
        //Log.v("Widget", "Real scale (" + getId() + "): " + realScale);
        //Log.v("Widget", "Current scale (" + getId() + "): " + Arrays.toString(getScale()));
        float[] newScale = Math3DUtils.divide(relativeScale, realScale);
        //Log.v("Widget", "New scale (" + getId() + "): " + Arrays.toString(newScale));
        setScale(newScale);
        //Log.v("Widget", "New dim (" + getId() + "): " + getCurrentDimensions());
    }

    public Dimensions getCurrentDimensions() {
        return new Dimensions(getDimensions(), getModelMatrix());
    }

    /**
     * @return the current dimensions of the object including the current scaling,
     * but not translation
     */
    public Dimensions getCurrentDimensions2() {
        return new Dimensions(getDimensions(), modelMatrix2);
    }

    public void setOctree(Octree octree) {
        this.octree = octree;
    }

    public Octree getOctree() {
        return octree;
    }

    public void addListener(EventListener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(EventListener listener) {
        this.listeners.remove(listener);
    }

    /**
     * Fires the event to all the registered listeners
     *
     * @param event the event
     * @return true if the event was handled, false otherwise
     */
    protected boolean propagate(EventObject event) {
        return AndroidUtils.fireEvent(listeners, event);
    }

    public void render(ShaderFactory drawer, Camera camera, float[] lightPosInWorldSpace, float[] colorMask) {
    }


    public Object3DData hide() {
        return this.setVisible(false);
    }

    public Object3DData show() {
        return this.setVisible(true);
    }


    public void setRender(boolean render) {
        this.render = render;
    }

    public boolean isRender() {
        return this.render;
    }

    public boolean isVisible() {
        if (isParentBound && parent != null) {
            return parent.isVisible();
        }
        return isVisible;
    }

    public Object3DData setVisible(boolean isVisible) {
        this.isVisible = isVisible;
        propagate(new ChangeEvent(this));
        return this;
    }

    public void toggleVisible() {
        setVisible(!this.isVisible());
    }

    public boolean isChanged() {
        return changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public Object3DData setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        return this;
    }

    public boolean isCentered() {
        return centered;
    }

    public void setCentered(boolean centered) {
        this.centered = centered;
        updateModelMatrix();
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public float[] getColor() {
        return getMaterial().getColor();
    }

    public Object3DData setColor(float[] color) {

        // set color only if valid data
        if (color != null) {

            // color variable when using single color
            this.getMaterial().setDiffuse(color);
            this.getMaterial().setAlpha(color[3]);
        }
        return this;
    }

    public int getDrawMode() {
        return drawMode;
    }

    public Object3DData setDrawMode(int drawMode) {
        this.drawMode = drawMode;
        return this;
    }

    public int getDrawSize() {
        return 0;
    }

    /*public void setTextureData(byte[] textureData) {
        Log.i("Object3DData","New texture: "+textureData.length+" (bytes)");
        this.getMaterial().setTextureData(textureData);
        if (this.getElements() != null && this.getElements().size() == 1){
            // TODO: let user pick object and/or element to update texture
            // as for now, let's just update 1st element
            for (int i=0; i<1; i++) {
                if (getElements().get(i).getMaterial() == null) continue;
                if (getElements().get(i).getMaterial().getTextureData() == null) continue;
                this.getElements().get(i).getMaterial().setTextureData(textureData);
                Log.i("Object3DData","New texture for element ("+i+"): "+getElements().get(i).getMaterial());
            }
        }
    }*/

    public Object3DData setMatrix(float[] modelMatrix) {
        System.arraycopy(modelMatrix, 0, this.modelMatrix, 0, 16);
        return this;
    }

    public Object3DData setModelMatrix(float[] modelMatrix) {
        System.arraycopy(modelMatrix, 0, this.modelMatrix, 0, 16);
        return this;
    }

    public Object3DData setLocation(float[] location) {
        return this.setLocation(location[0], location[1], location[2]);
    }

    public Object3DData setLocation(float locx, float locy, float locz) {
        this.location[0] = locx;
        this.location[1] = locy;
        this.location[2] = locz;
        this.updateModelMatrix();
        //this.updateModelDimensions();
        this.changed = true;
        return this;
    }

    public float[] getLocation() {
        return location;
    }

    public float getLocationX() {
        return location != null ? location[0] : 0;
    }

    public float getLocationY() {
        return location != null ? location[1] : 0;
    }

    public float getLocationZ() {
        return location != null ? location[2] : 0;
    }

    public float[] getRotation() {
        return rotation;
    }

    public Object3DData setScale(float[] scale) {
        return this.setScale(scale[0], scale[1], scale[2]);
    }

    public Object3DData setScale(float sx, float sy, float sz) {
        this.scale[0] = sx;
        this.scale[1] = sy;
        this.scale[2] = sz;
        updateModelMatrix();
        //updateModelDimensions();
        this.changed = true;
        return this;
    }

    public float[] getScale() {
        return scale;
    }

    public float getScaleX() {
        return getScale()[0];
    }

    public float getScaleY() {
        return getScale()[1];
    }

    public float getScaleZ() {
        return getScale()[2];
    }

    /**
     * @param rotation euler rotation
     * @return this
     */
    public Object3DData setRotation(float[] rotation) {
        this.rotation = rotation;
        updateModelMatrix();
        return this;
    }

/*    public Object3DData setRotation1(float[] rotation) {
        this.rotation1 = rotation;
        updateModelMatrix();
        return this;
    }*/

    public Object3DData setRotation2(float[] rotation2, float[] rotation2Location) {
        this.rotation2 = rotation2;
        this.rotation2Location = rotation2Location;
        updateModelMatrix();
        return this;
    }

    public float[] getRotation2() {
        return rotation2;
    }

    // binding coming from skeleton
    public Object3DData setWorldTransform(float[] matrix) {
        this.worldTransform = matrix;
        //this.updateModelMatrix();
        return this;
    }

    public float[] getWorldTransform() {
        return worldTransform;
    }

    protected void updateModelMatrix() {

        if (isReadOnly()) return;

        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.setIdentityM(modelMatrix2, 0);

        if (getLocation() != null) {
            Matrix.translateM(modelMatrix, 0, getLocationX(), getLocationY(), getLocationZ());
        }

        if (getRotation() != null) {
            Matrix.rotateM(modelMatrix, 0, getRotation()[2], 0, 0, 1f);
            Matrix.rotateM(modelMatrix, 0, getRotation()[1], 0, 1f, 0f);
            Matrix.rotateM(modelMatrix, 0, getRotation()[0], 1f, 0f, 0f);
            Matrix.rotateM(modelMatrix2, 0, getRotation()[2], 0, 0, 1f);
            Matrix.rotateM(modelMatrix2, 0, getRotation()[1], 0, 1f, 0f);
            Matrix.rotateM(modelMatrix2, 0, getRotation()[0], 1f, 0f, 0f);
        }

        if (orientation != null) {
            Matrix.multiplyMM(modelMatrix, 0, modelMatrix, 0, orientation.getMatrix(), 0);
        }

        if (getScale() != null) {
            Matrix.scaleM(modelMatrix, 0, getScaleX(), getScaleY(), getScaleZ());
            Matrix.scaleM(modelMatrix2, 0, getScaleX(), getScaleY(), getScaleZ());
        }

        if (isCentered()) {
            float[] center = getDimensions().getCenter();
            Matrix.translateM(modelMatrix, 0, -center[0], -center[1], -center[2]);
            Matrix.translateM(modelMatrix2, 0, -center[0], -center[1], -center[2]);
        }

        /*if (this.worldTransform != null) {
            //System.arraycopy(this.worldTransform, 0, this.modelMatrix, 0, 16);
            Matrix.multiplyMM(modelMatrix, 0, modelMatrix, 0, this.worldTransform, 0);
        }*/

        // normal matrix calculation
        Matrix.setIdentityM(normalMatrix, 0);
        final float[] inverted = new float[16];
        Matrix.invertM(inverted, 0, modelMatrix, 0);
        Matrix.transposeM(normalMatrix, 0, inverted, 0);

        propagate(new ChangeEvent(this));
    }

    /**
     * Returns the raw, local model matrix for this object without any other calculations.
     * This is safe to call from anywhere and will not cause recursion.
     */
    public float[] getNodeMatrix() {
        if (parentNode == null) {
            return Math3DUtils.IDENTITY_MATRIX;
        }
        final Node rootNode = parentNode.getRoot();
        return rootNode.getBindWorldTransform();
    }

    public float[] getModelMatrix() {

        // bounding box
        if (id != null && id.contains("_boundingBox_")) {
            if (parentNode != null) {
                final Node rootNode = parentNode.getRoot();
                return rootNode.getBindWorldTransform();

            }
        }

        if (isParentBound && parent != null) {
            return parent.getModelMatrix();
        }

        if (parentNode != null) {
            // If this mesh is attached to a node in the scene graph...
            // ...get the node's current, final, animated world transform.

            if (parentNode.getAnimatedWorldTransform() != null) {

                // FIXME: this needs to be true for the door.dae
                //if (true) return parentNode.getAnimatedWorldTransform();

                // Get the parent's final animated world transform.
                // This will be an identity matrix for static scenes like the door,
                // or the true animated transform for skeletons.
                final float[] parentWorldTransform = parentNode.getAnimatedWorldTransform();

                // Calculate the final world matrix for THIS object by applying its local
                // transform on top of its parent's world transform.
                // Final = ParentWorld * Local
                Matrix.multiplyMM(finalModelMatrix, 0, parentWorldTransform, 0, this.modelMatrix, 0);

                return finalModelMatrix;
            } else {

                // Get the parent's final animated world transform.
                // This will be an identity matrix for static scenes like the door,
                // or the true animated transform for skeletons.
                final float[] parentWorldTransform = parentNode.getBindWorldTransform();

                //if (true) return parentWorldTransform;

                // Calculate the final world matrix for THIS object by applying its local
                // transform on top of its parent's world transform.
                // Final = ParentWorld * Local
                Matrix.multiplyMM(finalModelMatrix, 0, parentWorldTransform, 0, this.modelMatrix, 0);

                return finalModelMatrix;
            }

        }
        return modelMatrix;
    }

    // In Object3DData.java

    /**
     * Returns the true final world transform of this object, considering animation.
     * This method is intended to be called by children or special cases like a bounding box,
     * bypassing any specific logic in getModelMatrix() like returning an identity matrix for skinning.
     *
     * @return the final world-space transformation matrix.
     */
    public float[] getFinalWorldTransform() {
        // If this mesh is attached to a node in the scene graph, get the node's final transform.
        if (parentNode != null) {
            if (parentNode.getAnimatedWorldTransform() != null) {
                return parentNode.getAnimatedWorldTransform();
            }
            if (parentNode.getBindWorldTransform() != null) {
                return parentNode.getBindWorldTransform();
            }
        }
        // Otherwise, fall back to the object's static model matrix.
        return modelMatrix;
    }


    public float[] getNormalMatrix() {
        // 1. Get the FINAL model matrix that is being used for the vertices.
        // This correctly gets the matrix from the node hierarchy or skeleton.
        float[] finalModelMatrix = getModelMatrix();

        // 2. Perform the inverse-transpose calculation on THAT final matrix.
        // Note: It's important to handle cases where the matrix can't be inverted.
        if (Matrix.invertM(normalMatrixTemp, 0, finalModelMatrix, 0)) {
            Matrix.transposeM(normalMatrix, 0, normalMatrixTemp, 0);
        } else {
            // If the matrix is not invertible (e.g., scale is 0),
            // return the last known good normal matrix or an identity matrix
            // to prevent rendering artifacts or crashes.
            Matrix.setIdentityM(normalMatrix, 0);
        }
        return normalMatrix;
    }

    public Buffer getIndexBuffer() {
        return indexBuffer;
    }

    public Object3DData setIndexBuffer(Buffer drawBuffer) {
        this.indexBuffer = drawBuffer;
        return this;
    }


    // -------------------- Buffers ---------------------- //

    public FloatBuffer getVerts() {
        return vertexArrayBuffer;
    }

    public FloatBuffer getVertexArrayBuffer() {
        return vertexArrayBuffer;
    }

    public Object3DData setVertices(FloatBuffer vertexArray) {
        return setVertexArrayBuffer(vertexArray);
    }

    public Object3DData setVertexArrayBuffer(FloatBuffer vertexArrayBuffer) {
        this.vertexArrayBuffer = vertexArrayBuffer;
        updateDimensions();
        return this;
    }

    public FloatBuffer getVertexNormalsArrayBuffer() {
        return vertexNormalsArrayBuffer;
    }

    public FloatBuffer getTangentBuffer() {
        return tangentBuffer;
    }

    public Object3DData setVertexNormalsArrayBuffer(FloatBuffer vertexNormalsArrayBuffer) {
        this.vertexNormalsArrayBuffer = vertexNormalsArrayBuffer;
        return this;
    }

    public Object3DData setTangentBuffer(FloatBuffer buffer) {
        this.tangentBuffer = buffer;
        return this;
    }

    public FloatBuffer getTextureCoordsArrayBuffer() {
        return textureCoordsArrayBuffer;
    }

    public Object3DData setTextureCoordsArrayBuffer(FloatBuffer textureCoordsArrayBuffer) {
        this.textureCoordsArrayBuffer = textureCoordsArrayBuffer;
        return this;
    }

    public List<int[]> getDrawModeList() {
        return drawModeList;
    }

    public Object3DData setDrawModeList(List<int[]> drawModeList) {
        this.drawModeList = drawModeList;
        return this;
    }

    public Buffer getVertexColorsArrayBuffer() {
        return vertexColorsArrayBuffer;
    }

    public Object3DData setVertexColorsArrayBuffer(Buffer colorsBuffer) {
        if (colorsBuffer != null && colorsBuffer.capacity() % 4 != 0)
            throw new IllegalArgumentException("Color buffer not multiple of 4 floats");
        this.vertexColorsArrayBuffer = colorsBuffer;
        return this;
    }

    protected void updateDimensions() {
        this.dimensions = null;
    }

    public BoundingBox getBoundingBox() {
        return BoundingBox.create("bbox_" + getId(), getDimensions(), Math3DUtils.IDENTITY_MATRIX);
    }

    /**
     * @return the current bounding box
     */
    public BoundingBox getCurrentBoundingBox() {
        return BoundingBox.create("bbox_" + getId(), getDimensions(), getModelMatrix());
    }

    public void addError(String error) {
        errors.add(error);
    }

    public List<String> getErrors() {
        return errors;
    }

    public Object3DData setElements(List<Element> elements) {
        this.elements = elements;
        this.updateDimensions();
        return this;
    }

    /**
     * @return either list of elements (when using indices), otherwise <code>null</code>
     */
    public List<Element> getElements() {
        if (elements == null && getIndexBuffer() != null) {
            Element element = new Element(getId(), getIndexBuffer(), null);
            element.setMaterial(this.getMaterial());
            elements = Collections.singletonList(element);
        }
        return elements;
    }

    public float[] getRotation2Location() {
        return rotation2Location;
    }

    public Quaternion getOrientation() {
        return orientation;
    }

    public Object3DData setOrientation(Quaternion orientation) {
        if (this.orientation == null) {
            this.orientation = orientation;
        } else {
            this.orientation.update(orientation);
        }
        updateModelMatrix();
        setChanged(true);
        return this;
    }

    // In Object3DData.java
    protected int primaryJointIndex = -1;

    public int getPrimaryJointIndex() {
        return primaryJointIndex;
    }

    public void setPrimaryJointIndex(int primaryJointIndex) {
        this.primaryJointIndex = primaryJointIndex;
    }

    @Override
    public Object3DData clone() {
        Object3DData ret = new Object3DData();
        copy(ret);
        return ret;
    }

    void copy(Object3DData ret) {
        ret.setId(this.getId());
        ret.location = this.location;
        ret.scale = this.scale;
        ret.rotation = this.rotation;
        ret.centered = this.centered;
        ret.orientation = this.orientation;

        //ret.setCurrentDimensions(this.getCurrentDimensions());
        ret.setVertexArrayBuffer(this.getVertexArrayBuffer());
        ret.setIndexBuffer(this.getIndexBuffer());
        ret.setVertexNormalsArrayBuffer(this.getVertexNormalsArrayBuffer());
        ret.setVertexColorsArrayBuffer(this.getVertexColorsArrayBuffer());
        ret.setTextureCoordsArrayBuffer(this.getTextureCoordsArrayBuffer());
        if (this.getElements() != null) {
            ret.setElements(new ArrayList<>());
            for (int i = 0; i < this.getElements().size(); i++) {
                ret.getElements().add(this.getElements().get(i).clone());
            }
        }
        ret.setMaterial(this.getMaterial());
        ret.setDrawMode(this.getDrawMode());
        ret.setDrawUsingArrays(this.isDrawUsingArrays());
        ret.setParentNode(this.getParentNode());
        //ret.setDrawUsingArrays(this.isDrawUsingArrays());

        // metadata
        ret.authoringTool = this.authoringTool;
    }

    public void dispose() {
        //Log.v("Object3DData","Disposing object... "+getId());
        this.listeners.clear();
        this.parent = null;

        this.vertexArrayBuffer = null;
        this.vertexColorsArrayBuffer = null;
        this.indexBuffer = null;
    }

    // -- helper methods

    /**
     * Init normals from vertices. Only for triangulated polygons
     */
    public void initNormals() {

        // check
        if (drawMode != GLES20.GL_TRIANGLES) return;

        // loop indices
        if (vertexNormalsArrayBuffer == null && vertexArrayBuffer != null) {

            Log.v("Object3DData", "Generating normals... " + getId());

            // init normal buffer
            vertexNormalsArrayBuffer = IOUtils.createFloatBuffer(getVertexArrayBuffer().capacity());

            for (int i = 0; i < vertexArrayBuffer.capacity() - 9; i += 9) {

                final float[] v1 = getVertexBufferValue(i);
                final float[] v2 = getVertexBufferValue(i + 3);
                final float[] v3 = getVertexBufferValue(i + 6);

                // calculate normal
                final float[] calculatedNormal = Math3DUtils.calculateNormal(v1, v2, v3);

                // add normal
                vertexNormalsArrayBuffer.put(calculatedNormal);
                vertexNormalsArrayBuffer.put(calculatedNormal);
                vertexNormalsArrayBuffer.put(calculatedNormal);
            }

            Log.v("Object3DData", "Generating normals finished. " + getId());
        }
    }

    private float[] getVertexBufferValue(int offset) {
        return new float[]{vertexArrayBuffer.get(offset), vertexArrayBuffer.get(offset + 1), vertexArrayBuffer.get(offset + 2)};
    }

    @Override
    public String toString() {
        return "Object3DData{" +
                "id='" + id + "'" +
                ", name=" + getName() +
                ", isVisible=" + isVisible +
                ", color=" + (getVertexColorsArrayBuffer() != null ? getVertexColorsArrayBuffer().toString() : Arrays.toString(getMaterial().getColor())) +
                ", position=" + Arrays.toString(location) +
                ", scale=" + Arrays.toString(scale) +
                ", indexed=" + !isDrawUsingArrays() +
                ", vertices: " + (vertexArrayBuffer != null ? vertexArrayBuffer.capacity() / 3 : 0) +
                ", normals: " + (vertexNormalsArrayBuffer != null ? vertexNormalsArrayBuffer.capacity() / 3 : 0) +
                ", dimensions: " + this.dimensions +
                //", current dimensions: " + this.currentDimensions +
                ", material=" + getMaterial() +
                ", elements=" + this.elements +
                ", matrix=" + Arrays.toString(modelMatrix) +
                '}';
    }

    public void debug() {
        try {
            // --- EXPANDED LOGGING ---
            Log.d("MODEL_DEBUG", "--- MODEL DATA --- " + getId());
// Print first 30 floats (10 vertices)
            if (modelMatrix != null) {
                StringBuilder pos_sb = new StringBuilder("modelMatrix: ").append(Arrays.toString(modelMatrix));
                Log.d("MODEL_DEBUG", pos_sb.toString());
            }

            if (vertexArrayBuffer != null) {
                StringBuilder pos_sb = new StringBuilder("Positions: ").append("(").append(vertexArrayBuffer.capacity()).append(") ");
                for (int i = 0; i < 16 && i < vertexArrayBuffer.capacity(); i++) {
                    pos_sb.append(vertexArrayBuffer.get(i)).append(" ");
                }
                Log.d("MODEL_DEBUG", pos_sb.toString());
            }

// Print first 30 floats (10 normals)
// IMPORTANT: Add a null check for finalNormals
            if (vertexNormalsArrayBuffer != null && vertexNormalsArrayBuffer.capacity() >= 30) {
                StringBuilder norm_sb = new StringBuilder("Normals:   ").append("(").append(vertexNormalsArrayBuffer.capacity()).append(") ");
                ;
                for (int i = 0; i < 16 && i < vertexNormalsArrayBuffer.capacity(); i++) {
                    norm_sb.append(vertexNormalsArrayBuffer.get(i)).append(" ");
                }
                Log.d("MODEL_DEBUG", norm_sb.toString());
            } else {
                Log.d("MODEL_DEBUG", "Normals: null or too short.");
            }

            // Print first 15 indices
            if (indexBuffer != null) {
                StringBuilder idx_sb = new StringBuilder("Indices:   ").append("(").append(indexBuffer.capacity()).append(") ");
                ;
                idx_sb.append("(").append(indexBuffer.getClass().getSimpleName()).append(")");

                for (int i = 0; i < 16 && i < indexBuffer.capacity(); i++) {
                    idx_sb.append(IOUtils.getIntBufferValue(indexBuffer, i)).append(" ");
                }
                Log.d("MODEL_DEBUG", idx_sb.toString());
            }

            if (textureCoordsArrayBuffer != null) {
                StringBuilder norm_sb = new StringBuilder("Textures:   ").append("(").append(textureCoordsArrayBuffer.capacity()).append(") ");
                ;
                for (int i = 0; i < 16 && i < textureCoordsArrayBuffer.capacity(); i++) {
                    norm_sb.append(textureCoordsArrayBuffer.get(i)).append(" ");
                }
                Log.d("MODEL_DEBUG", norm_sb.toString());
            } else {
                Log.d("MODEL_DEBUG", "Textures: null or too short.");
            }

            // --- END LOGGING ---
        } catch (Exception e) {
            Log.e("MODEL_DEBUG", e.getMessage(), e);
        }
    }
}
