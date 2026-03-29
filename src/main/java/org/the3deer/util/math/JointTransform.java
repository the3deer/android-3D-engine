package org.the3deer.util.math;

import androidx.annotation.Nullable;

import java.util.Arrays;

/**
 * Represents the local bone-space transform of a joint at a certain keyframe
 * during an animation. This includes the position and rotation of the joint,
 * relative to the parent joint (for the root joint it's relative to the model's
 * origin, seeing as the root joint has no parent). The transform is stored as a
 * position vector and a quaternion (rotation) so that these values can be
 * easily interpolated, a functionality that this class also provides.
 * This position and rotation are relative to the parent bone!
 * <b>type</b>
 * <p>The transform is either composite (location, scale, euler rotation) or
 * absolute (matrix)</p>
 *
 * <b>quaternion<b/>
 * <p>The quaternion rotation is either calculated (from rotation)
 * or extracted (from matrix)
 * </p>
 *
 * <b>transform<b/>
 * <p>The final transform is the add-up of the scale position and rotation.
 * The rotation is either rotation (if composite) or quaternion (if absolute)
 * </p>
 *
 * @author andresoviedo
 */

public class JointTransform {

    // TODO: should matrix be final?
    private float[] matrix;
    private Float[] scale;
    private Quaternion qRotation;
    private Float[] rotation;
    private Float[] location;

    // Transformation = L x R x S
    private float[] transform;

    // visibility transformation
    private boolean visible;

    // FIXME: what's this?
    private Float[] rotation1;
    private Float[] rotation2;

    // FIXME: what's this?
    private Float[] rotation2Location;

    // cache
    private static final Float[] tempScale = new Float[3];
    private static final Float[] tempRotation = new Float[3];
    private static final Quaternion tempQRotation = new Quaternion();
    private static final Float[] tempLocation = new Float[3];
    private static final float[] tempMatrix = new float[16];

    // gltf - new parser
    public JointTransform() {
        refresh();
    }

    // ui
    public JointTransform(Float[] floats, Object o, Float[] floats1) {
        refresh();
    }

    // collada legacy
    public static JointTransform ofScale(Float[] scale) {
        return new JointTransform(scale, (Float[]) null, null);
    }

    // collada legacy
    public static JointTransform ofRotation(Float[] rotation) {
        return new JointTransform(null, rotation, null);
    }

    // collada legacy
    public static JointTransform ofLocation(Float[] location) {
        return new JointTransform(null, (Float[]) null, location);
    }

    // animator
    // TODO: is this really needed?
    static JointTransform ofNull() {
        return new JointTransform(new Float[3], new Float[3], new Float[3]);
    }

    // gltf - new parser
    public JointTransform(float[] matrix) {
        this.matrix = matrix;

        this.qRotation = Quaternion.fromMatrix(matrix);
        this.scale = Math3DUtils.scaleFromMatrix(matrix);
        if (matrix != null) {
            this.rotation = Quaternion.fromMatrix(matrix).normalize().toAnglesF(null);
        }
        this.location = new Float[]{matrix[12], matrix[13], matrix[14]};
        this.visible = true;

        // FIXME: what's this?
        this.rotation1 = new Float[]{0f, 0f, 0f};
        this.rotation2 = new Float[]{0f, 0f, 0f};
        this.rotation2Location = new Float[]{0f, 0f, 0f};

        refresh();
    }

    private JointTransform(Float[] scale, Float[] rotation, Float[] location) {
        this.matrix = null;
        this.qRotation = null;

        this.scale = scale;
        this.rotation = rotation;
        this.location = location;

        this.visible = true;
        //this.calculatedMatrix = new float[16];

        refresh();
    }

    // interpolation / animator - when QUATERNION is enabled
    private JointTransform(Float[] scale, Quaternion qRotation, Float[] location) {
        this.scale = scale;
        this.matrix = new float[0]; // dummy flag - not composite
        this.qRotation = qRotation;
        this.location = location;
        if (qRotation != null) {
            this.rotation = qRotation.toAnglesF(null);
        }

        this.visible = true;
        //this.calculatedMatrix = null;

        refresh();
    }

    /**
     * Set the final matrix for this transform.
     * Scale, Quaternion and translation are extracted from it.
     *
     * @param matrix
     */
    // collada - new
    public void setTransform(float[] matrix) {
        this.matrix = matrix;
        this.qRotation = Quaternion.fromMatrix(matrix);

        // extracted transforms
        this.scale = Math3DUtils.scaleFromMatrix(matrix);
        this.rotation = qRotation.toAnglesF();
        this.location = new Float[]{matrix[12], matrix[13], matrix[14]};

        refresh();
    }

    /**
     * @return the final matrix for this transform.
     */
    public float[] getMatrix() {
        return matrix;
    }

    /**
     * @return true if it does not have a matrix
     */
    public boolean isComposite() {
        return matrix == null;
    }

    public Float[] getScale() {
        return scale;
    }

    public void setScale(float[] scale) {
        this.setScale(scale[0], scale[1], scale[2]);
    }

    public void setRotation(Float[] rotation) {
        this.rotation = rotation;
    }

    public void setScale(float x, float y, float z) {
        if (this.scale == null) {
            this.scale = new Float[3];
        }
        this.scale[0] = x;
        this.scale[1] = y;
        this.scale[2] = z;
        refresh();
    }

    boolean isComplete() {
        return matrix != null || isComplete(getScale()) && isComplete(getRotation()) && isComplete(getLocation());
    }

    private static boolean isComplete(Float[] array) {
        if (array == null) return false;
        for (Float aFloat : array) {
            if (aFloat == null) return false;
        }
        return true;
    }



    void complete(JointTransform jointData) {
        if (this.scale == null) {
            this.scale = new Float[]{1f, 1f, 1f};
        }
        if (this.rotation == null) {
            this.rotation = new Float[3];
        }
        if (this.location == null) {
            this.location = new Float[3];
        }

        if (jointData.getLocation() != null) {
            if (this.location[0] == null && jointData.getLocation()[0] != null)
                this.location[0] = jointData.getLocation()[0];
            if (this.location[1] == null && jointData.getLocation()[1] != null)
                this.location[1] = jointData.getLocation()[1];
            if (this.location[2] == null && jointData.getLocation()[2] != null)
                this.location[2] = jointData.getLocation()[2];
        }

        if (jointData.getScale() != null) {
            if (this.scale[0] == null && jointData.getScale()[0] != null)
                this.scale[0] = jointData.getScale()[0];
            if (this.scale[1] == null && jointData.getScale()[1] != null)
                this.scale[1] = jointData.getScale()[1];
            if (this.scale[2] == null && jointData.getScale()[2] != null)
                this.scale[2] = jointData.getScale()[2];
        }

        if (jointData.getRotation() != null) {
            if (this.rotation[0] == null && jointData.getRotation()[0] != null)
                this.rotation[0] = jointData.getRotation()[0];
            if (this.rotation[1] == null && jointData.getRotation()[1] != null)
                this.rotation[1] = jointData.getRotation()[1];
            if (this.rotation[2] == null && jointData.getRotation()[2] != null)
                this.rotation[2] = jointData.getRotation()[2];
        }

        if (jointData.getQRotation() != null) {
            if (this.qRotation == null) {
                this.qRotation = jointData.getQRotation();
            }
        }
        refresh();
    }

    public Float[] getRotation() {
        return rotation;
    }

    public Quaternion getQRotation() {
        return qRotation;
    }

    public Float[] getLocation() {
        return location;
    }

    public Float[] getRotation2() {
        return rotation2;
    }

    public Float[] getRotation2Location() {
        return rotation2Location;
    }

    public Float[] getRotation1() {
        return rotation1;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return visible;
    }

    public boolean hasScaleX() {
        return scale != null && scale[0] != null;
    }

    public boolean hasScaleY() {
        return scale != null && scale[1] != null;
    }

    public boolean hasScaleZ() {
        return scale != null && scale[2] != null;
    }

    public boolean hasRotationX() {
        return rotation != null && rotation[0] != null;
    }

    public boolean hasRotationY() {
        return rotation != null && rotation[1] != null;
    }

    public boolean hasRotationZ() {
        return rotation != null && rotation[2] != null;
    }

    public boolean hasLocationX() {
        return location != null && location[0] != null;
    }

    public boolean hasLocationY() {
        return location != null && location[1] != null;
    }


    public boolean hasLocationZ() {
        return location != null && location[2] != null;
    }


    private static void add(Float[] result, Float[] extra) {
        if (extra[0] != null) {
            if (result[0] == null) result[0] = 0f;
            result[0] += extra[0];
        }
        if (extra[1] != null) {
            if (result[1] == null) result[1] = 0f;
            result[1] += extra[1];
        }
        if (extra[2] != null) {
            if (result[2] == null) result[2] = 0f;
            result[2] += extra[2];
        }
    }

    public void addScale(Float x, Float y, Float z) {
        addScale(new Float[]{x, y, z});
    }

    public void addScale(Float[] extra) {
        if (this.scale == null) {
            this.scale = extra;
        } else {
            add(this.scale, extra);
        }
        refresh();
    }

    public void addRotation(Float x, Float y, Float z) {
        addRotation(new Float[]{x, y, z});
    }

    public void addRotation(Float[] extra) {
        if (this.rotation == null) {
            this.rotation = extra;
        } else {
            add(this.rotation, extra);
        }

        this.qRotation = Quaternion.fromEulerD(
                rotation[0] != null ? rotation[0] : 0f,
                rotation[1] != null ? rotation[1] : 0f,
                rotation[2] != null ? rotation[2] : 0f).normalize();

        refresh();
    }

    public void addLocation(Float x, Float y, Float z) {
        addLocation(new Float[]{x, y, z});
    }

    public void addLocation(Float[] extra) {
        if (this.location == null) {
            this.location = extra;
        } else {
            add(this.location, extra);
        }
        refresh();
    }

    public float[] getTransform() {
        return transform;
    }

    /**
     * Linearly interpolates between two translations based on a "progression"
     * value.
     *
     * @param start       - the start translation.
     * @param end         - the end translation.
     * @param progression - a value between 0 and 1 indicating how far to interpolate
     *                    between the two translations.
     */
    private static void interpolateVector(Float[] ret, Float[] start, Float[] end, float progression) {
        if (progression == 0) {
            if (start != null) {
                if (ret[0] == null) ret[0] = start[0];
                if (ret[1] == null) ret[1] = start[1];
                if (ret[2] == null) ret[2] = start[2];
            }
        } else {
            if (start != null && end != null) {
                if (start[0] != null && end[0] != null) {
                    ret[0] = start[0] + (end[0] - start[0]) * progression;
                }
                if (start[1] != null && end[1] != null) {
                    ret[1] = start[1] + (end[1] - start[1]) * progression;
                }
                if (start[2] != null && end[2] != null) {
                    ret[2] = start[2] + (end[2] - start[2]) * progression;
                }
            }
        }
    }

    public void setLocation(float[] location) {
        if (location == null) return;
        this.location = new Float[3];
        this.location[0] = location[0];
        this.location[1] = location[1];
        this.location[2] = location[2];
        refresh();
    }

    private void refresh() {
    }


    @Override
    public String toString() {
        return "JointTransform{" +
                "location=" + Arrays.toString(location) +
                ", scale=" + Arrays.toString(scale) +
                ", rotation=" + Arrays.toString(rotation) +
                ", quaternion=" + qRotation +
                //", matrix=" + Arrays.toString(matrix) +
                '}';
    }


    // gltf - legacy
    public void setRotation(Quaternion quaternion) {
        this.qRotation = quaternion;
        if (quaternion != null) {
            this.rotation = quaternion.normalize().toAnglesF(this.rotation);
        }
        refresh();
    }


    @Deprecated
    public void setQuaternion(Quaternion quaternion) {
        setRotation(quaternion);
    }

    /*public JointTransform setRotation(Float[] angles) {
        this.rotation = angles;
        if (angles != null) {
            this.qRotation = Quaternion.fromEuler();
        }
        updateMatrix();
        return this;
    }*/

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        JointTransform that = (JointTransform) obj;

        if (!Arrays.equals(matrix, that.matrix)) return false;
        if (!Arrays.equals(scale, that.scale)) return false;
        if (!Arrays.equals(rotation, that.rotation)) return false;
        if (!Arrays.equals(location, that.location)) return false;
        return qRotation != null ? qRotation.equals(that.qRotation) : that.qRotation == null;
    }
}
