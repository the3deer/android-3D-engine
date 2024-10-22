package org.the3deer.android_3d_model_engine.model;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import org.the3deer.android_3d_model_engine.collision.Octree;
import org.the3deer.android_3d_model_engine.services.collada.entities.MeshData;
import org.the3deer.android_3d_model_engine.shader.ShaderFactory;
import org.the3deer.util.android.AndroidUtils;
import org.the3deer.util.event.EventListener;
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
    private boolean isParentBound;
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
    protected FloatBuffer vertexBuffer = null;
    private FloatBuffer normalsBuffer = null;
    private FloatBuffer tangentBuffer = null;
    protected Buffer colorsBuffer = null;
    private FloatBuffer textureBuffer = null;
    protected List<Element> elements;
    /**
     * Object materials
     */
    private Materials materials;

    // simple object variables for drawing using arrays
    private Material material = new Material("default");
    private Buffer indexBuffer = null;

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
    private final float[] modelMatrix = new float[16];
    /**
     * This is the local transformation (M=SR), except translation
     */
    private final float[] modelMatrix2 = new float[16];
    /**
     * This is the local transformation when we have node hierarchy (ie. {@code <visual_scene><node><transform></transform></node></visual_scene>}
     */
    private float[] bindTransform;
    /**
     * This is the final model transformation
     */
    private float[] newModelMatrix = new float[16];

    {
        //
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.setIdentityM(modelMatrix2, 0);
        Matrix.setIdentityM(newModelMatrix, 0);
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

    public Object3DData(FloatBuffer vertexBuffer) {
        this.vertexBuffer = vertexBuffer;
        this.setDrawUsingArrays(true);
        updateDimensions();
    }

    public Object3DData(FloatBuffer vertexBuffer, Buffer drawOrder) {
        this.vertexBuffer = vertexBuffer;
        this.indexBuffer = drawOrder;
        this.setDrawUsingArrays(false);
        updateDimensions();
    }

    public Object3DData(FloatBuffer vertexBuffer, FloatBuffer textureBuffer, byte[] texData) {
        this.vertexBuffer = vertexBuffer;
        this.textureBuffer = textureBuffer;
        this.getMaterial().setColorTexture(new Texture().setData(texData));
        this.setDrawUsingArrays(true);
        updateDimensions();
    }

    public Object3DData(FloatBuffer vertexBuffer, FloatBuffer colorsBuffer,
                        FloatBuffer textureBuffer, byte[] texData) {
        this.vertexBuffer = vertexBuffer;
        this.colorsBuffer = colorsBuffer;
        this.textureBuffer = textureBuffer;
        this.getMaterial().setColorTexture(new Texture().setData(texData));
        this.setDrawUsingArrays(true);
        updateDimensions();
    }

    public Object3DData(FloatBuffer verts, FloatBuffer normals,
                        Materials materials) {
        super();
        this.vertexBuffer = verts;
        this.normalsBuffer = normals;
        this.materials = materials;
        this.setDrawUsingArrays(false);
        this.updateDimensions();
    }

    public Object3DData getParent() {
        return parent;
    }

    public Object3DData setParent(Object3DData parent) {
        this.parent = parent;
        return this;
    }

    public void setParentBound(boolean bind) {
        this.isParentBound = bind;
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
        return this;
    }

    public Material getMaterial() {
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

    private void refreshDimensions() {
        final Dimensions dimensions = new Dimensions();

        if (vertexBuffer != null) {

            if (this.elements == null || this.elements.isEmpty()) {
                for (int i = 0; i < vertexBuffer.capacity() - 2; i += 3) {
                    dimensions.update(vertexBuffer.get(i), vertexBuffer.get(i + 1), vertexBuffer.get(i + 2));
                }
            } else {
                for (Element element : getElements()) {
                    final Buffer indexBuffer = element.getIndexBuffer();
                    for (int i = 0; i < indexBuffer.capacity(); i++) {
                        final int idx;
                        if (indexBuffer instanceof IntBuffer) {
                            idx = ((IntBuffer) indexBuffer).get(i);
                        } else if (indexBuffer instanceof ShortBuffer) {
                            idx = ((ShortBuffer) indexBuffer).get(i);
                        } else if (indexBuffer instanceof ByteBuffer) {
                            idx = ((ByteBuffer) indexBuffer).get(i);
                        } else {
                            throw new IllegalStateException("IndexBuffer is of unknown type");
                        }
                        if (idx < 0 || idx >= vertexBuffer.capacity()) {
                            Log.w("Object3DData", "Wrong index: " + idx);
                            continue;
                        }
                        dimensions.update(vertexBuffer.get(idx * 3), vertexBuffer.get(idx * 3 + 1), vertexBuffer.get(idx * 3 + 2));
                    }
                }
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

    public Object3DData setModelMatrix(float[] modelMatrix) {
        this.newModelMatrix = modelMatrix;
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
    public Object3DData setBindTransform(float[] matrix) {
        this.bindTransform = matrix;
        this.updateModelMatrix();
        //this.updateModelDimensions();
        return this;
    }

    /**
     * This is the bind shape transform found in skin (ie. {@code <library_controllers><skin><bind_shape_matrix>}
     */
    public void setBindShapeMatrix(float[] matrix) {
        if (matrix == null) return;

        float[] vertex = new float[]{0, 0, 0, 1};
        float[] shaped = new float[]{0, 0, 0, 1};
        for (int i = 0; i < this.vertexBuffer.capacity(); i += 3) {
            vertex[0] = this.vertexBuffer.get(i);
            vertex[1] = this.vertexBuffer.get(i + 1);
            vertex[2] = this.vertexBuffer.get(i + 2);
            Matrix.multiplyMV(shaped, 0, matrix, 0, vertex, 0);
            this.vertexBuffer.put(i, shaped[0]);
            this.vertexBuffer.put(i + 1, shaped[1]);
            this.vertexBuffer.put(i + 2, shaped[2]);
        }
        updateDimensions();
    }

    public float[] getBindTransform() {
        return bindTransform;
    }

    protected void updateModelMatrix() {

        if (isReadOnly()) return;

        if (this.isParentBound && this.parent != null) {
            // geometries not linked to any joint does not have bind transform
            System.arraycopy(this.parent.modelMatrix, 0, this.newModelMatrix, 0, 16);
        } else {
            Matrix.setIdentityM(modelMatrix, 0);
            Matrix.setIdentityM(modelMatrix2, 0);

            //float[] axisAngle = orientation.toAxisAngle();
            //Matrix.rotateM(modelMatrix, 0, axisAngle[3], axisAngle[0], axisAngle[1], axisAngle[2]);
            //Log.v("Object3DData","angle:"+Arrays.toString(axisAngle))

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

        /*

        if (getScale() != null) {
            Matrix.scaleM(modelMatrix, 0, getScaleX(), getScaleY(), getScaleZ());
        }

        float[] temp = new float[16];
        orientation.toRotationMatrix(temp);
        Matrix.multiplyMM(temp,0,orientationMatrix,0,modelMatrix,0);
        System.arraycopy(temp,0,modelMatrix,0, temp.length);*/

        /*Matrix.translateM(modelMatrix, 0, -getDimensions().getCenter()[0],
                -getDimensions().getCenter()[1], -getDimensions().getCenter()[2]);*/

            // apply rotation (euler)
        /*if (rotation1 != null) {
            //Matrix.rotateM(modelMatrix, 0, rotation1[0], 1f, 0f, 0f);
            Matrix.rotateM(modelMatrix, 0, rotation1[1], 0, 1f, 0f);
            //Matrix.rotateM(modelMatrix, 0, rotation1[2], 0, 0, 1f);
        }*/

        /*Matrix.translateM(modelMatrix, 0, getDimensions().getCenter()[0],
                getDimensions().getCenter()[1], getDimensions().getCenter()[2]);*/




        /*if (rotation2 != null && rotation2Location != null) {
            Matrix.translateM(modelMatrix, 0, rotation2Location[0], rotation2Location[1], rotation2Location[2]);
            Matrix.rotateM(modelMatrix, 0, getRotation2()[0], 1f, 0f, 0f);
            Matrix.rotateM(modelMatrix, 0, getRotation2()[1], 0, 1f, 0f);
            Matrix.rotateM(modelMatrix, 0, getRotation2()[2], 0, 0, 1f);
            Matrix.translateM(modelMatrix, 0, -rotation2Location[0], -rotation2Location[1], -rotation2Location[2]);
        }
        if (getRotation() != null) {
            Matrix.rotateM(modelMatrix, 0, getRotation()[0], 1f, 0f, 0f);
            Matrix.rotateM(modelMatrix, 0, getRotation()[1], 0, 1f, 0f);
            Matrix.rotateM(modelMatrix, 0, getRotationZ(), 0, 0, 1f);
        }*/

            // apply orientation (quaternion)


            if (this.bindTransform == null) {
                // geometries not linked to any joint does not have bind transform
                System.arraycopy(this.modelMatrix, 0, this.newModelMatrix, 0, 16);
            } else {
                Matrix.multiplyMM(newModelMatrix, 0, this.modelMatrix, 0, this.bindTransform, 0);
            }
        }

        propagate(new ChangeEvent(this));
    }

    public float[] getModelMatrix() {
        return newModelMatrix;
    }

    public Transform getTransform() {
        return new Transform(this.scale, this.rotation, this.location);
    }

    public Buffer getDrawOrder() {
        return indexBuffer;
    }

    public Object3DData setDrawOrder(Buffer drawBuffer) {
        this.indexBuffer = drawBuffer;
        return this;
    }


    // -------------------- Buffers ---------------------- //

    public FloatBuffer getVertexBuffer() {
        return vertexBuffer;
    }

    public Object3DData setVertexBuffer(FloatBuffer vertexBuffer) {
        this.vertexBuffer = vertexBuffer;
        updateDimensions();
        return this;
    }

    public FloatBuffer getNormalsBuffer() {
        return normalsBuffer;
    }

    public FloatBuffer getTangentBuffer() {
        return tangentBuffer;
    }

    public Object3DData setNormalsBuffer(FloatBuffer normalsBuffer) {
        this.normalsBuffer = normalsBuffer;
        return this;
    }

    public Object3DData setTangentBuffer(FloatBuffer buffer) {
        this.tangentBuffer = buffer;
        return this;
    }

    public FloatBuffer getTextureBuffer() {
        return textureBuffer;
    }

    public Object3DData setTextureBuffer(FloatBuffer textureBuffer) {
        this.textureBuffer = textureBuffer;
        return this;
    }

    public List<int[]> getDrawModeList() {
        return drawModeList;
    }

    public Object3DData setDrawModeList(List<int[]> drawModeList) {
        this.drawModeList = drawModeList;
        return this;
    }

    public Buffer getColorsBuffer() {
        return colorsBuffer;
    }

    public Object3DData setColorsBuffer(Buffer colorsBuffer) {
        if (colorsBuffer != null && colorsBuffer.capacity() % 4 != 0)
            throw new IllegalArgumentException("Color buffer not multiple of 4 floats");
        this.colorsBuffer = colorsBuffer;
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
        if (elements == null && getDrawOrder() != null) {
            Element element = new Element(getId(), getDrawOrder(), null);
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
        ret.bindTransform = this.bindTransform;
        ret.centered = this.centered;
        ret.orientation = this.orientation;

        //ret.setCurrentDimensions(this.getCurrentDimensions());
        ret.setVertexBuffer(this.getVertexBuffer());
        ret.setDrawOrder(this.getDrawOrder());
        ret.setNormalsBuffer(this.getNormalsBuffer());
        ret.setColorsBuffer(this.getColorsBuffer());
        ret.setTextureBuffer(this.getTextureBuffer());
        if (this.getElements() != null) {
            ret.setElements(new ArrayList<>());
            for (int i = 0; i < this.getElements().size(); i++) {
                ret.getElements().add(this.getElements().get(i).clone());
            }
        }
        ret.setMaterial(this.getMaterial());
        ret.setDrawMode(this.getDrawMode());
        ret.setDrawUsingArrays(this.isDrawUsingArrays());
        //ret.setDrawUsingArrays(this.isDrawUsingArrays());
    }

    public void dispose() {
        //Log.v("Object3DData","Disposing object... "+getId());
        this.listeners.clear();
        this.parent = null;

        this.vertexBuffer = null;
        this.colorsBuffer = null;
        this.indexBuffer = null;
    }

    @Override
    public String toString() {
        return "Object3DData{" +
                "id='" + id + "'" +
                ", name=" + getName() +
                ", isVisible=" + isVisible +
                ", color=" + (getColorsBuffer() != null ? getColorsBuffer().toString() : Arrays.toString(getMaterial().getColor())) +
                ", position=" + Arrays.toString(location) +
                ", scale=" + Arrays.toString(scale) +
                ", indexed=" + !isDrawUsingArrays() +
                ", vertices: " + (vertexBuffer != null ? vertexBuffer.capacity() / 3 : 0) +
                ", normals: " + (normalsBuffer != null ? normalsBuffer.capacity() / 3 : 0) +
                ", dimensions: " + this.dimensions +
                //", current dimensions: " + this.currentDimensions +
                ", material=" + getMaterial() +
                ", elements=" + this.elements +
                '}';
    }
}
