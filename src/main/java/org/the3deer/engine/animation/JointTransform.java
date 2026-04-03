package org.the3deer.engine.animation;

import androidx.annotation.Nullable;

import org.the3deer.engine.model.Constants;
import org.the3deer.engine.model.Node;
import org.the3deer.util.math.Math3DUtils;
import org.the3deer.util.math.Matrix;
import org.the3deer.util.math.Quaternion;

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

    void complete(Node node) {
        if (this.scale == null) {
            this.scale = new Float[]{1f, 1f, 1f};
        }
        if (this.rotation == null) {
            this.rotation = new Float[3];
        }
        if (this.location == null) {
            this.location = new Float[3];
        }

        if (node != null) {

            if (node.getBindLocalTranslation() != null) {
                if (this.location[0] == null && node.getBindLocalTranslation()[0] != null)
                    this.location[0] = node.getBindLocalTranslation()[0];
                if (this.location[1] == null && node.getBindLocalTranslation()[1] != null)
                    this.location[1] = node.getBindLocalTranslation()[1];
                if (this.location[2] == null && node.getBindLocalTranslation()[2] != null)
                    this.location[2] = node.getBindLocalTranslation()[2];
            }

            if (node.getBindLocalScale() != null) {
                if (this.scale[0] == null && node.getBindLocalScale()[0] != null)
                    this.scale[0] = node.getBindLocalScale()[0];
                if (this.scale[1] == null && node.getBindLocalScale()[1] != null)
                    this.scale[1] = node.getBindLocalScale()[1];
                if (this.scale[2] == null && node.getBindLocalScale()[2] != null)
                    this.scale[2] = node.getBindLocalScale()[2];
            }

            if (node.getBindLocalQuaternion() != null) {
                if (this.qRotation == null) {
                    this.qRotation = node.getBindLocalQuaternion();
                }
            }

            if (node.getBindLocalRotation() != null) {
                if (this.rotation[0] == null && node.getBindLocalRotation()[0] != null)
                    this.rotation[0] = node.getBindLocalRotation()[0];
                if (this.rotation[1] == null && node.getBindLocalRotation()[1] != null)
                    this.rotation[1] = node.getBindLocalRotation()[1];
                if (this.rotation[2] == null && node.getBindLocalRotation()[2] != null)
                    this.rotation[2] = node.getBindLocalRotation()[2];
            }
        }

        refresh();
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
     * Interpolates between two transforms based on the progression value. The
     * result is a new transform which is part way between the two original
     * transforms. The translation can simply be linearly interpolated, but the
     * rotation interpolation is slightly more complex, using a method called
     * "SLERP" to spherically-linearly interpolate between 2 quaternions
     * (rotations). This gives a much much better result than trying to linearly
     * interpolate between Euler rotations.
     *
     * @param scaleAX           - the previous transform
     * @param scaleBX           - the next transform
     * @param scaleProgressionX - a number between 0 and 1 indicating how far between the two
     *                          transforms to interpolate. A progression value of 0 would
     *                          return a transform equal to "frameA", a value of 1 would
     *                          return a transform equal to "frameB". Everything else gives a
     *                          transform somewhere in-between the two.
     * @return the new interpolated Transformation
     */
    static JointTransform ofInterpolation(JointTransform scaleAX, JointTransform scaleBX, float scaleProgressionX,
                                          JointTransform scaleAY, JointTransform scaleBY, float scaleProgressionY,
                                          JointTransform scaleAZ, JointTransform scaleBZ, float scaleProgressionZ,
                                          JointTransform rotationAX, JointTransform rotationBX, float rotationProgressionX,
                                          JointTransform rotationAY, JointTransform rotationBY, float rotationProgressionY,
                                          JointTransform rotationAZ, JointTransform rotationBZ, float rotationProgressionZ,
                                          JointTransform locationAX, JointTransform locationBX, float locationProgressionX,
                                          JointTransform locationAY, JointTransform locationBY, float locationProgressionY,
                                          JointTransform locationAZ, JointTransform locationBZ, float locationProgressionZ) {

        final Float[] scale = new Float[3];
        final Float[] location = new Float[3];

        interpolateVector(scale, scaleAX.scale, scaleBX.scale, scaleProgressionX);
        interpolateVector(scale, scaleAY.scale, scaleBY.scale, scaleProgressionY);
        interpolateVector(scale, scaleAZ.scale, scaleBZ.scale, scaleProgressionZ);

        interpolateVector(location, locationAX.location, locationBX.location, locationProgressionX);
        interpolateVector(location, locationAY.location, locationBY.location, locationProgressionY);
        interpolateVector(location, locationAZ.location, locationBZ.location, locationProgressionZ);

        if (!Constants.PREFER_QUATERNION && rotationAX.isComposite() && rotationBX.isComposite()
                && rotationAY.isComposite() && rotationBY.isComposite()
                && rotationAZ.isComposite() && rotationBZ.isComposite()) {
            final Float[] rotation = new Float[3];
            interpolateVector(rotation, rotationAX.rotation, rotationBX.rotation, rotationProgressionX);
            interpolateVector(rotation, rotationAY.rotation, rotationBY.rotation, rotationProgressionY);
            interpolateVector(rotation, rotationAZ.rotation, rotationBZ.rotation, rotationProgressionZ);
            return new JointTransform(scale, rotation, location);
        } else {

            // FIXME: is this incomplete?
            /*final Quaternion qRotation = new Quaternion(0, 0, 0, 1);
            Quaternion.interpolate(qRotation, rotationAX.qRotation, rotationBX.qRotation, rotationProgressionX);
            return new JointTransform(scale, qRotation, location);*/

            // 1. Interpolate each axis independently
            Quaternion qX = new Quaternion(0, 0, 0, 1); // Identity
            if (rotationAX.qRotation != null && rotationBX.qRotation != null) {
                Quaternion.interpolate(qX, rotationAX.qRotation, rotationBX.qRotation, rotationProgressionX);
            }

            Quaternion qY = new Quaternion(0, 0, 0, 1); // Identity
            if (rotationAY.qRotation != null && rotationBY.qRotation != null) {
                Quaternion.interpolate(qY, rotationAY.qRotation, rotationBY.qRotation, rotationProgressionY);
            }

            Quaternion qZ = new Quaternion(0, 0, 0, 1); // Identity
            if (rotationAZ.qRotation != null && rotationBZ.qRotation != null) {
                Quaternion.interpolate(qZ, rotationAZ.qRotation, rotationBZ.qRotation, rotationProgressionZ);
            }

            // 2. Combine them into one final quaternion.
            // ORDER MATTERS: This depends on your engine's convention (e.g., Z * Y * X)
            // If these came from separate <rotate> tags in COLLADA, they apply in order.
            // Assuming standard Z-Y-X application:
            final Quaternion qRotation = new Quaternion(0, 0, 0, 1);

            // qRotation = qZ * qY * qX
            qRotation.multiply(qZ).multiply(qY).multiply(qX).normalize();

            return new JointTransform(scale, qRotation, location);
        }
    }

    static void interpolate(JointTransform frameA, JointTransform frameB, float progression, float[] ret) {

        interpolateVector(tempScale, frameA.scale, frameB.scale, progression);
        interpolateVector(tempLocation, frameA.location, frameB.location, progression);

        Matrix.setIdentityM(ret, 0);

        if (tempLocation[0] != null)
            Matrix.translateM(ret, 0, tempLocation[0], 0, 0);
        if (tempLocation[1] != null)
            Matrix.translateM(ret, 0, 0, tempLocation[1], 0);
        if (tempLocation[2] != null)
            Matrix.translateM(ret, 0, 0, 0, tempLocation[2]);

        if (tempScale[0] != null)
            Matrix.scaleM(ret, 0, tempScale[0], 1, 1);
        if (tempScale[1] != null)
            Matrix.scaleM(ret, 0, 1, tempScale[1], 1);
        if (tempScale[2] != null)
            Matrix.scaleM(ret, 0, 1, 1, tempScale[2]);

        interpolateVector(tempRotation, frameA.rotation, frameB.rotation, progression);

        if (!Constants.PREFER_QUATERNION && frameA.isComposite() && frameB.isComposite()) {
            interpolateVector(tempRotation, frameA.rotation, frameB.rotation, progression);
            if (tempRotation[2] != null)
                Matrix.rotateM(ret, 0, tempRotation[2], 0, 0, 1);
            if (tempRotation[1] != null)
                Matrix.rotateM(ret, 0, tempRotation[1], 0, 1, 0);
            if (tempRotation[0] != null)
                Matrix.rotateM(ret, 0, tempRotation[0], 1, 0, 0);
        } else {
            tempQRotation.setIdentity();
            Quaternion.interpolate(tempQRotation, frameA.qRotation, frameB.qRotation, progression);
            tempQRotation.normalize();
            System.arraycopy(ret, 0, tempMatrix, 0, 16);
            Matrix.multiplyMM(ret, 0, tempMatrix, 0, tempQRotation.toRotationMatrix(), 0);
        }

        // INFO: cleanup - otherwise next interpolation will have undefined results
        tempScale[0] = tempScale[1] = tempScale[2] = null;
        tempRotation[0] = tempRotation[1] = tempRotation[2] = null;
        tempLocation[0] = tempLocation[1] = tempLocation[2] = null;
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
        if (transform == null) {
            transform = new float[16];
        }
        Matrix.setIdentityM(transform, 0);
        if (this.location != null) {
            if (this.location[0] != null)
                Matrix.translateM(transform, 0, location[0], 0, 0);
            if (this.location[1] != null)
                Matrix.translateM(transform, 0, 0, location[1], 0);
            if (this.location[2] != null)
                Matrix.translateM(transform, 0, 0, 0, location[2]);
        }

        if (this.scale != null) {
            if (this.scale[0] != null)
                Matrix.scaleM(transform, 0, scale[0], 1, 1);
            if (this.scale[1] != null)
                Matrix.scaleM(transform, 0, 1, scale[1], 1);
            if (this.scale[2] != null)
                Matrix.scaleM(transform, 0, 1, 1, scale[2]);
        }

        if (!Constants.PREFER_QUATERNION && isComposite()) {
            if (this.rotation != null) {
                if (this.rotation[2] != null)
                    Matrix.rotateM(transform, 0, rotation[2], 0, 0, 1);
                if (this.rotation[1] != null)
                    Matrix.rotateM(transform, 0, rotation[1], 0, 1, 0);
                if (this.rotation[0] != null)
                    Matrix.rotateM(transform, 0, rotation[0], 1, 0, 0);
            } else if (this.qRotation != null) {
                System.arraycopy(transform, 0, tempMatrix, 0, 16);
                Matrix.multiplyMM(transform, 0, tempMatrix, 0, this.qRotation.getMatrix(), 0);
            }
        } else {

            if (this.qRotation != null) {
                System.arraycopy(transform, 0, tempMatrix, 0, 16);
                Matrix.multiplyMM(transform, 0, tempMatrix, 0, this.qRotation.getMatrix(), 0);
            }
        }
        /*else if (Constants.PREFER_QUATERNION && this.qRotation != null) {
            //this.qRotation.normalize();
            //Matrix.multiplyMM(transform,0, transform, 0, this.qRotation.getMatrix(), 0);
            Matrix.rotateM(transform, 0, this.qRotation.toAngles(null)[2], 0, 0, 1);
            Matrix.rotateM(transform, 0, this.qRotation.toAngles(null)[1], 0, 1, 0);
            Matrix.rotateM(transform, 0, this.qRotation.toAngles(null)[0], 1, 0, 0);
        }*/
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
