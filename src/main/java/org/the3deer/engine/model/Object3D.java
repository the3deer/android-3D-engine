package org.the3deer.engine.model;

import android.opengl.GLES20;
import android.opengl.Matrix;

import org.the3deer.engine.android.shader.ShaderFactory;
import org.the3deer.engine.android.util.AndroidUtils;
import org.the3deer.engine.collision.Octree;
import org.the3deer.engine.services.collada.entities.MeshData;
import org.the3deer.util.event.EventListener;
import org.the3deer.util.io.IOUtils;
import org.the3deer.util.math.Math3DUtils;
import org.the3deer.util.math.Quaternion;

import java.net.URI;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is the basic 3D data necessary to build the 3D object
 * <p>
 * Transforms are applied as follows:
 * <p>
 * MATRIX = ROTATION X LOCATION X SCALE X ORIENTATION [X CENTER] X OBJECT
 *
 * @author andresoviedo
 */
public class Object3D {

    private static final Logger logger = Logger.getLogger(Object3D.class.getSimpleName());

    public Object3D(String id, FloatBuffer positions, FloatBuffer normals, FloatBuffer texCoords, FloatBuffer colors, Material material) {
        this.id = id;
        this.vertexArrayBuffer = positions;
        this.vertexNormalsArrayBuffer = normals;
        this.textureCoordsArrayBuffer = texCoords;
        this.vertexColorsArrayBuffer = colors;
        this.material = material;
        this.vertexCount = (positions != null) ? positions.capacity() / 3 : 0;
    }

    public boolean isIndexed() {
        return this.isIndexed;
    }

    public Object3D setIndexed(boolean indexed) {
        this.isIndexed = indexed;
        return this;
    }

    public static class ChangeEvent extends EventObject {
        public ChangeEvent(Object3D source) {
            super(source);
        }

        @Override
        public Object3D getSource() {
            return (Object3D) super.getSource();
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
    protected Object3D parent = null;
    /**
     * Bind transformation to parent
     */
    protected boolean isParentBound;
    /**
     * model resource locator
     */
    private URI uri;
    /**
     * model id - default name
     */
    private String id = "object_"+System.identityHashCode(this);
    /**
     * model id - friendly name
     */
    private String name = "object_"+System.identityHashCode(this);;
    /**
     * Whether to draw object using indices or not
     */
    private boolean isIndexed = false;
    /**
     * Whether the object is to be drawn (and children)
     */
    private boolean isVisible = true;

    /**
     * Whether the object is to be rendered (not children)
     */
    private boolean render = true;

    /**
     * decorators does not get hit by rays
     */
    private boolean isDecorator = false;
    /**
     * responds to click
     */
    private boolean isClickable = false;
    /**
     * responds to movement
     */
    private boolean isMovable = false;

    private boolean enabled = true;

    /**
     * The minimum thing we can draw in space is a vertex (or point).
     * This drawing mode uses the vertexBuffer
     */
    private int drawMode = GLES20.GL_POINTS;

    // mesh vertex data
    private MeshData meshData = null;

    // Model data
    protected FloatBuffer vertexArrayBuffer = null;
    protected int vertexCount = 0;
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
    private Material material;
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


    public Object3D() {
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

    public Object3D(float[] vertexArray) {
        this(IOUtils.createFloatBuffer(vertexArray));
    }

    public Object3D(FloatBuffer vertexArrayBuffer) {
        this(vertexArrayBuffer, null);
    }

    public Object3D(FloatBuffer verticesBuffer, Buffer indicesBuffer) {
        this.vertexArrayBuffer = verticesBuffer;
        this.vertexCount = (verticesBuffer != null) ? verticesBuffer.capacity() / 3 : 0;
        this.indexBuffer = indicesBuffer;
        updateDimensions();
    }

    public Object3D(FloatBuffer vertexArrayBuffer, FloatBuffer textureCoordsArrayBuffer, byte[] texData) {
        this.vertexArrayBuffer = vertexArrayBuffer;
        this.vertexCount = (vertexArrayBuffer != null) ? vertexArrayBuffer.capacity() / 3 : 0;
        this.textureCoordsArrayBuffer = textureCoordsArrayBuffer;
        this.getMaterial().setColorTexture(new Texture().setData(texData));
        updateDimensions();
    }

    public Object3D(FloatBuffer vertexArrayBuffer, FloatBuffer vertexColorsArrayBuffer,
                    FloatBuffer textureCoordsArrayBuffer, byte[] texData) {
        this.vertexArrayBuffer = vertexArrayBuffer;
        this.vertexCount = (vertexArrayBuffer != null) ? vertexArrayBuffer.capacity() / 3 : 0;
        this.vertexColorsArrayBuffer = vertexColorsArrayBuffer;
        this.textureCoordsArrayBuffer = textureCoordsArrayBuffer;
        this.getMaterial().setColorTexture(new Texture().setData(texData));
        updateDimensions();
    }

    public Object3D(FloatBuffer verts, FloatBuffer normals,
                    Materials materials) {
        super();
        this.vertexArrayBuffer = verts;
        this.vertexCount = (verts != null) ? verts.capacity() / 3 : 0;
        this.vertexNormalsArrayBuffer = normals;
        this.materials = materials;
        this.updateDimensions();
    }

    public int getVertexCount() {
        return vertexCount;
    }

    public Node getParentNode() {
        return parentNode;
    }

    public Object3D setParentNode(Node parentNode) {
        this.parentNode = parentNode;
        return this;
    }

    public Object3D getParent() {
        return parent;
    }

    public Object3D setParent(Object3D parent) {
        this.parent = parent;
        return this;
    }

    public Object3D setParentBound(boolean bind) {
        this.isParentBound = bind;
        return this;
    }

    public boolean isParentBound() {
        return this.isParentBound;
    }


    public Object3D setId(String id) {
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

    public void setSkinned(boolean skinned) {
        this.isSkinned = skinned;
    }

    public boolean isSkinned() {
        return this.isSkinned;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public URI getUri() {
        return this.uri;
    }

    public Material getMaterial() {

        // material bound to node
        if (parentNode != null && parentNode.getMaterial() != null) return parentNode.getMaterial();

        return material;
    }

    public Object3D setMaterial(Material material) {
        if (material == null) throw new IllegalArgumentException("Material cannot be null");
        this.material = material;
        return this;
    }

    public Materials getMaterials() {
        return materials;
    }


    public Object3D setDecorator(boolean decorator) {
        isDecorator = decorator;
        return this;
    }

    public boolean isDecorator() {
        return isDecorator;
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
        isDecorator = true;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enable){
        this.enabled = enable;
    }
    // ---------------------------- dimensions ----------------------------- //

    public void setDimensions(Dimensions dimensions) {
        this.dimensions = dimensions;
        //logger.config("New fixed dimensions for " + getId() + ": " + this.dimensions);
    }

    public Dimensions getDimensions() {
        if (dimensions == null) {
            refreshDimensions();
            logger.finest("New dimensions for '" + getId() + "': " + this.dimensions);
        }
        return dimensions;
    }

    private void refreshDimensions() {
        final Dimensions dimensions = new Dimensions();

        // Do nothing if there are no vertices
        if (vertexArrayBuffer == null || vertexArrayBuffer.capacity() == 0) {
            this.dimensions = dimensions;
            return;
        }

        // Ensure buffer position is at the start for reading
        int originalPos = vertexArrayBuffer.position();
        vertexArrayBuffer.position(0);

        try {
            // Case 1: The object is drawn using glDrawArrays (non-indexed)
            if (!isIndexed()) {
                for (int i = 0; i < vertexArrayBuffer.capacity() / 3; i++) {
                    dimensions.update(vertexArrayBuffer.get(i * 3), vertexArrayBuffer.get(i * 3 + 1), vertexArrayBuffer.get(i * 3 + 2));
                }
            }
            // Case 2: The object is drawn using glDrawElements (indexed)
            else if (this.elements != null && !this.elements.isEmpty()) {
                Set<Integer> processedIndices = new HashSet<>();
                for (Element element : getElements()) {
                    final Buffer indexBuffer = element.getIndexBuffer();
                    if (indexBuffer != null) {
                        int originalIdxPos = indexBuffer.position();
                        indexBuffer.position(0);
                        for (int i = 0; i < indexBuffer.capacity(); i++) {
                            final int idx = IOUtils.getIntBufferValue(indexBuffer, i);

                            if (processedIndices.contains(idx)) {
                                continue; 
                            }

                            if (idx < 0 || idx >= vertexArrayBuffer.capacity() / 3) {
                                logger.warning("Wrong index '" + idx + "' while getting dimensions for '" + getId() + "'");
                                continue;
                            }

                            dimensions.update(vertexArrayBuffer.get(idx * 3), vertexArrayBuffer.get(idx * 3 + 1), vertexArrayBuffer.get(idx * 3 + 2));
                            processedIndices.add(idx);
                        }
                        indexBuffer.position(originalIdxPos);
                    }
                }
            } else if (indexBuffer != null) {
                int originalIdxPos = indexBuffer.position();
                indexBuffer.position(0);
                for (int i = 0; i < indexBuffer.capacity(); i++) {
                    final int idx = IOUtils.getIntBufferValue(indexBuffer, i);

                    if (idx < 0 || idx >= vertexArrayBuffer.capacity() / 3) {
                        logger.warning("Wrong index '" + idx + "' while getting dimensions for '" + getId() + "'");
                        continue;
                    }

                    dimensions.update(vertexArrayBuffer.get(idx * 3), vertexArrayBuffer.get(idx * 3 + 1), vertexArrayBuffer.get(idx * 3 + 2));
                }
                indexBuffer.position(originalIdxPos);
            } else {
                // No elements/indices defined, fallback to processing all vertices
                for (int i = 0; i < vertexArrayBuffer.capacity() / 3; i++) {
                    dimensions.update(vertexArrayBuffer.get(i * 3), vertexArrayBuffer.get(i * 3 + 1), vertexArrayBuffer.get(i * 3 + 2));
                }
            }
        } finally {
            // Restore original position
            vertexArrayBuffer.position(originalPos);
        }

        // --- ENHANCEMENT: Handle 0-size reports for non-empty buffers ---
        if (dimensions.getWidth() == 0 && dimensions.getHeight() == 0 && dimensions.getDepth() == 0 && vertexArrayBuffer.capacity() > 0) {
            logger.warning("Dimensions calculated as zero for non-empty buffer (" + getId() + "). Buffer capacity: " + vertexArrayBuffer.capacity());
            // If it's literally a single vertex, the math is correct but framing will be hard.
        }

        this.dimensions = dimensions;
    }


    public Object3D setRelativeScale(float[] relativeScale) {
        this.relativeScale = relativeScale;

        // default scale
        /*if (getParent() != null) {

            // recalculate based on parent
            // That is, -1+1 is 100% parent dimension
            Dimensions parentDim = getParent().getCurrentDimensions();
            float relScale = parentDim.getRelationTo(getCurrentDimensions());
            logger.finest("Relative scale for '" + getId() + "': " + relScale);
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
        //logger.finest("Relative scale. Parent dim (" + parentDim + ")");
        //logger.finest("Relative scale. Current dim (" + getDimensions() + ")");
        float realScale = getDimensions().getRelationTo(parentDim);
        //logger.finest("Real scale (" + getId() + "): " + realScale);
        //logger.finest("Current scale (" + getId() + "): " + Arrays.toString(getScale()));
        float[] newScale = Math3DUtils.divide(relativeScale, realScale);
        //logger.finest("New scale (" + getId() + "): " + Arrays.toString(newScale));
        setScale(newScale);
        //logger.finest("New dim (" + getId() + "): " + getCurrentDimensions());
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


    public Object3D hide() {
        return this.setVisible(false);
    }

    public Object3D show() {
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

    public Object3D setVisible(boolean isVisible) {
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

    public Object3D setReadOnly(boolean readOnly) {
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

        // check material
        if (material != null){
            return material.getColor();
        }

        return null;
    }

    public Object3D setColor(float[] color) {

        // set color only if valid data
        if (color != null) {

            // init material if null
            if (material == null){
                material = new Material();
            }

            // color variable when using single color
            this.getMaterial().setDiffuse(color);
            this.getMaterial().setAlpha(color[3]);
        }
        return this;
    }

    public int getDrawMode() {
        return drawMode;
    }

    public Object3D setDrawMode(int drawMode) {
        this.drawMode = drawMode;
        return this;
    }

    public int getDrawSize() {
        return 0;
    }

    /*public void setTextureData(byte[] textureData) {
        logger.info("New texture: "+textureData.length+" (bytes)");
        this.getMaterial().setTextureData(textureData);
        if (this.getElements() != null && this.getElements().size() == 1){
            // TODO: let user pick object and/or element to update texture
            // as for now, let's just update 1st element
            for (int i=0; i<1; i++) {
                if (getElements().get(i).getMaterial() == null) continue;
                if (getElements().get(i).getMaterial().getTextureData() == null) continue;
                this.getElements().get(i).getMaterial().setTextureData(textureData);
                logger.info("New texture for element ("+i+"): "+getElements().get(i).getMaterial());
            }
        }
    }*/

    public Object3D setMatrix(float[] modelMatrix) {
        System.arraycopy(modelMatrix, 0, this.modelMatrix, 0, 16);
        return this;
    }

    public Object3D setModelMatrix(float[] modelMatrix) {
        System.arraycopy(modelMatrix, 0, this.modelMatrix, 0, 16);
        return this;
    }

    public Object3D setLocation(float[] location) {
        return this.setLocation(location[0], location[1], location[2]);
    }

    public Object3D setLocation(float locx, float locy, float locz) {
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

    public Object3D setScale(float[] scale) {
        return this.setScale(scale[0], scale[1], scale[2]);
    }

    public Object3D setScale(float sx, float sy, float sz) {
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
    public Object3D setRotation(float[] rotation) {
        this.rotation = rotation;
        updateModelMatrix();
        return this;
    }

/*    public Object3D setRotation1(float[] rotation) {
        this.rotation1 = rotation;
        updateModelMatrix();
        return this;
    }*/

    public Object3D setRotation2(float[] rotation2, float[] rotation2Location) {
        this.rotation2 = rotation2;
        this.rotation2Location = rotation2Location;
        updateModelMatrix();
        return this;
    }

    public float[] getRotation2() {
        return rotation2;
    }

    // binding coming from skeleton
    public Object3D setWorldTransform(float[] matrix) {
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
        if (Matrix.invertM(inverted, 0, modelMatrix, 0)) {
            Matrix.transposeM(normalMatrix, 0, inverted, 0);
        }

        propagate(new ChangeEvent(this));
    }

    public float[] getModelMatrix() {

        // bounding box
        /*if (id != null && id.contains("_boundingBox_")) {
            if (parentNode != null && parentNode.getRoot() != null) {
                final Node rootNode = parentNode.getRoot();
                if (rootNode.getAnimatedWorldTransform() != null) {
                    return rootNode.getAnimatedWorldTransform();
                } else {
                    return rootNode.getBindWorldTransform();
                }
            }
        }*/

/*        if (isParentBound && parent != null) {
            return parent.getModelMatrix();
        }*/

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
                final float[] parentWorldTransform = parentNode.getWorldTransform();

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

    // In Object3D.java

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
            if (parentNode.getWorldTransform() != null) {
                return parentNode.getWorldTransform();
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

    public Object3D setIndexBuffer(Buffer drawBuffer) {
        this.indexBuffer = drawBuffer;
        return this;
    }


    // -------------------- Buffers ---------------------- //

    public FloatBuffer getVerts() {
        return vertexArrayBuffer;
    }

    public FloatBuffer getVertexBuffer() {
        return vertexArrayBuffer;
    }

    public Object3D setVertices(FloatBuffer vertexArray) {
        return setVertexBuffer(vertexArray);
    }

    public Object3D setVertexBuffer(FloatBuffer vertexArrayBuffer) {
        this.vertexArrayBuffer = vertexArrayBuffer;
        this.vertexCount = (vertexArrayBuffer != null) ? vertexArrayBuffer.capacity() / 3 : 0;
        updateDimensions();
        return this;
    }

    public FloatBuffer getNormalsBuffer() {
        return vertexNormalsArrayBuffer;
    }

    /**
     * @deprecated use instead {@link #getNormalsBuffer()}
     */
    @Deprecated
    public FloatBuffer getVertexNormalsArrayBuffer() {
        return getNormalsBuffer();
    }

    public FloatBuffer getTangentBuffer() {
        return tangentBuffer;
    }

    public Object3D setNormalsBuffer(FloatBuffer normals){
        this.vertexNormalsArrayBuffer = normals;
        return this;
    }

    /**
     * @deprecated use instead {@link #setNormalsBuffer(FloatBuffer)} ()}
     */
    @Deprecated
    public Object3D setVertexNormalsArrayBuffer(FloatBuffer vertexNormalsArrayBuffer) {
        return setNormalsBuffer(vertexNormalsArrayBuffer);
    }

    public Object3D setTangentBuffer(FloatBuffer buffer) {
        this.tangentBuffer = buffer;
        return this;
    }

    public FloatBuffer getTextureCoordsArrayBuffer() {
        return textureCoordsArrayBuffer;
    }

    public Object3D setTextureCoordsArrayBuffer(FloatBuffer textureCoordsArrayBuffer) {
        this.textureCoordsArrayBuffer = textureCoordsArrayBuffer;
        return this;
    }

    public List<int[]> getDrawModeList() {
        return drawModeList;
    }

    public Object3D setDrawModeList(List<int[]> drawModeList) {
        this.drawModeList = drawModeList;
        return this;
    }

    public Buffer getColorsBuffer() {
        return vertexColorsArrayBuffer;
    }

    public Object3D setColorsBuffer(Buffer colorsBuffer){
        if (colorsBuffer != null && colorsBuffer.capacity() % 4 != 0)
            throw new IllegalArgumentException("Color buffer not multiple of 4 floats");
        this.vertexColorsArrayBuffer = colorsBuffer;
        return this;
    }

    @Deprecated
    public Object3D setVertexColorsArrayBuffer(Buffer colorsBuffer) {
        return setColorsBuffer(colorsBuffer);
    }

    protected void updateDimensions() {
        refreshDimensions();
    }

    /**
     * @return the local bounding box, without any transformation applied, just the original dimensions.
     */
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

    public Object3D setElements(List<Element> elements) {
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

    public Object3D setOrientation(Quaternion orientation) {
        if (this.orientation == null) {
            this.orientation = orientation;
        } else {
            this.orientation.update(orientation);
        }
        updateModelMatrix();
        setChanged(true);
        return this;
    }

    // In Object3D.java
    protected int primaryJointIndex = -1;

    public int getPrimaryJointIndex() {
        return primaryJointIndex;
    }

    public void setPrimaryJointIndex(int primaryJointIndex) {
        this.primaryJointIndex = primaryJointIndex;
    }

    @Override
    public Object3D clone() {
        Object3D ret = new Object3D();
        copy(ret);
        return ret;
    }

    void copy(Object3D ret) {
        ret.setId(this.getId());
        ret.location = this.location;
        ret.scale = this.scale;
        ret.rotation = this.rotation;
        ret.centered = this.centered;
        ret.orientation = this.orientation;

        //ret.setCurrentDimensions(this.getCurrentDimensions());
        ret.setVertexBuffer(this.getVertexBuffer());
        ret.setIndexBuffer(this.getIndexBuffer());
        ret.setNormalsBuffer(this.getNormalsBuffer());
        ret.setColorsBuffer(this.getColorsBuffer());
        ret.setTextureCoordsArrayBuffer(this.getTextureCoordsArrayBuffer());
        if (this.getElements() != null) {
            ret.setElements(new ArrayList<>());
            for (int i = 0; i < this.getElements().size(); i++) {
                ret.getElements().add(this.getElements().get(i).clone());
            }
        }
        ret.material = this.material;
        ret.setDrawMode(this.getDrawMode());
        ret.setIndexed(this.isIndexed());
        ret.setParentNode(this.getParentNode());
        //ret.setDrawUsingArrays(this.isDrawUsingArrays());

        // metadata
        ret.authoringTool = this.authoringTool;
    }

    public long getMemoryUsage() {
        long memory = 0;
        if (vertexArrayBuffer != null) memory += (long) vertexArrayBuffer.capacity() * 4;
        if (vertexNormalsArrayBuffer != null) memory += (long) vertexNormalsArrayBuffer.capacity() * 4;
        if (vertexColorsArrayBuffer != null) memory += (long) vertexColorsArrayBuffer.capacity() * (vertexColorsArrayBuffer instanceof FloatBuffer ? 4 : 1);
        if (textureCoordsArrayBuffer != null) memory += (long) textureCoordsArrayBuffer.capacity() * 4;
        if (tangentBuffer != null) memory += (long) tangentBuffer.capacity() * 4;
        if (indexBuffer != null) {
            if (indexBuffer instanceof IntBuffer) memory += (long) indexBuffer.capacity() * 4;
            else if (indexBuffer instanceof ShortBuffer) memory += (long) indexBuffer.capacity() * 2;
            else if (indexBuffer instanceof ByteBuffer) memory += (long) indexBuffer.capacity();
        }
        if (elements != null) {
            for (Element element : elements) {
                final Buffer buffer = element.getIndexBuffer();
                if (buffer == null) continue;
                if (buffer instanceof IntBuffer) memory += (long) buffer.capacity() * 4;
                else if (buffer instanceof ShortBuffer) memory += (long) buffer.capacity() * 2;
                else if (buffer instanceof ByteBuffer) memory += (long) buffer.capacity();
            }
        }
        if (getMaterial() != null && getMaterial().getColorTexture() != null) {
            if (getMaterial().getColorTexture().getData() != null) {
                memory += getMaterial().getColorTexture().getData().length;
            }
        }
        return memory;
    }

    public void dispose() {
        //logger.finest("Disposing object... "+getId());
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

            logger.finest("Generating normals... " + getId());

            // init normal buffer
            vertexNormalsArrayBuffer = IOUtils.createFloatBuffer(getVertexBuffer().capacity());

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

            logger.finest("Generating normals finished. " + getId());
        }
    }

    private float[] getVertexBufferValue(int offset) {
        return new float[]{vertexArrayBuffer.get(offset), vertexArrayBuffer.get(offset + 1), vertexArrayBuffer.get(offset + 2)};
    }

    @Override
    public String toString() {
        return "Object3D{" +
                "id='" + id + "'" +
                ", name=" + getName() +
                ", isVisible=" + isVisible +
                ", position=" + Arrays.toString(location) +
                ", scale=" + Arrays.toString(scale) +
                ", indexed=" + isIndexed() +
                ", colors=" + (vertexColorsArrayBuffer != null ? vertexColorsArrayBuffer.capacity() / 4 : 0) +
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
            logger.config("--- MODEL DATA --- " + getId());
// Print first 30 floats (10 vertices)
            if (modelMatrix != null) {
                StringBuilder pos_sb = new StringBuilder("modelMatrix: ").append(Arrays.toString(modelMatrix));
                logger.config(pos_sb.toString());
            }

            if (vertexArrayBuffer != null) {
                StringBuilder pos_sb = new StringBuilder("Positions: ").append("(").append(vertexArrayBuffer.capacity()).append(") ");
                for (int i = 0; i < 16 && i < vertexArrayBuffer.capacity(); i++) {
                    pos_sb.append(vertexArrayBuffer.get(i)).append(" ");
                }
                logger.config(pos_sb.toString());
            }

// Print first 30 floats (10 normals)
// IMPORTANT: Add a null check for finalNormals
            if (vertexNormalsArrayBuffer != null && vertexNormalsArrayBuffer.capacity() >= 30) {
                StringBuilder norm_sb = new StringBuilder("Normals:   ").append("(").append(vertexNormalsArrayBuffer.capacity()).append(") ");
                ;
                for (int i = 0; i < 16 && i < vertexNormalsArrayBuffer.capacity(); i++) {
                    norm_sb.append(vertexNormalsArrayBuffer.get(i)).append(" ");
                }
                logger.config(norm_sb.toString());
            } else {
                logger.config("Normals: null or too short.");
            }

            // Print first 15 indices
            if (indexBuffer != null) {
                StringBuilder idx_sb = new StringBuilder("Indices:   ").append("(").append(indexBuffer.capacity()).append(") ");
                ;
                idx_sb.append("(").append(indexBuffer.getClass().getSimpleName()).append(")");

                for (int i = 0; i < 16 && i < indexBuffer.capacity(); i++) {
                    idx_sb.append(IOUtils.getIntBufferValue(indexBuffer, i)).append(" ");
                }
                logger.config(idx_sb.toString());
            }

            if (textureCoordsArrayBuffer != null) {
                StringBuilder norm_sb = new StringBuilder("Textures:   ").append("(").append(textureCoordsArrayBuffer.capacity()).append(") ");
                ;
                for (int i = 0; i < 16 && i < textureCoordsArrayBuffer.capacity(); i++) {
                    norm_sb.append(textureCoordsArrayBuffer.get(i)).append(" ");
                }
                logger.config(norm_sb.toString());
            } else {
                logger.config("Textures: null or too short.");
            }

            // --- END LOGGING ---
        } catch (Exception e) {
            logger.log(Level.SEVERE,  e.getMessage(), e);
        }
    }
}
