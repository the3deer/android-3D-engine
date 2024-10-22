package org.the3deer.android_3d_model_engine.animation;

import android.opengl.Matrix;

import org.the3deer.android_3d_model_engine.model.Constants;
import org.the3deer.android_3d_model_engine.services.collada.entities.JointData;
import org.the3deer.util.math.Math3DUtils;
import org.the3deer.util.math.Quaternion;

import java.util.Arrays;

/**
 * Represents the local bone-space transform of a joint at a certain keyframe
 * during an animation. This includes the position and rotation of the joint,
 * relative to the parent joint (for the root joint it's relative to the model's
 * origin, seeing as the root joint has no parent). The transform is stored as a
 * position vector and a quaternion (rotation) so that these values can be
 * easily interpolated, a functionality that this class also provides.
 *
 * @author andresoviedo
 */

public class JointTransform {

    // remember, this position and rotation are relative to the parent bone!
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

    public JointTransform() {
        refresh();
    }

    public JointTransform(Float[] floats, Object o, Float[] floats1) {
        refresh();
    }

    public static JointTransform ofScale(Float[] scale) {
        return new JointTransform(scale, (Float[]) null, null);
    }

    public static JointTransform ofRotation(Float[] rotation) {
        return new JointTransform(null, rotation, null);
    }

    public static JointTransform ofLocation(Float[] location) {
        return new JointTransform(null, (Float[]) null, location);
    }

    static JointTransform ofIdentity() {
        return new JointTransform(new Float[]{1f,1f,1f}, new Float[]{0f,0f,0f}, new Float[]{0f,0f,0f});
    }

    static JointTransform ofNull() {
        return new JointTransform(new Float[3], new Float[3], new Float[3]);
    }


    public JointTransform(float[] matrix) {
        this.matrix = matrix;

        this.qRotation = Quaternion.fromMatrix(matrix);
        this.scale = Math3DUtils.scaleFromMatrix(matrix);
        if (matrix != null) {
            this.rotation = Quaternion.fromMatrix(matrix).normalize().toAngles2(null);
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

    private JointTransform(Float[] scale, Quaternion qRotation, Float[] location) {
        this.scale = scale;
        this.qRotation = qRotation;
        this.rotation = null;
        this.location = location;

        this.visible = true;
        //this.calculatedMatrix = null;

        refresh();
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

    void complete(JointData jointData) {
        if (this.scale == null) {
            this.scale = new Float[]{1f, 1f, 1f};
        }
        if (this.rotation == null) {
            this.rotation = new Float[3];
        }
        if (this.location == null) {
            this.location = new Float[3];
        }

        if (jointData == null){
            this.rotation[0] = this.rotation[1] = this.rotation[2] = 0f;
            this.location[0] = this.location[1] = this.location[2] = 0f;
        }

        if (jointData.getBindLocalTranslation() != null) {
            if (this.location[0] == null && jointData.getBindLocalTranslation()[0] != null)
                this.location[0] = jointData.getBindLocalTranslation()[0];
            if (this.location[1] == null && jointData.getBindLocalTranslation()[1] != null)
                this.location[1] = jointData.getBindLocalTranslation()[1];
            if (this.location[2] == null && jointData.getBindLocalTranslation()[2] != null)
                this.location[2] = jointData.getBindLocalTranslation()[2];
        }

        if (jointData.getBindLocalScale() != null) {
            if (this.scale[0] == null && jointData.getBindLocalScale()[0] != null)
                this.scale[0] = jointData.getBindLocalScale()[0];
            if (this.scale[1] == null && jointData.getBindLocalScale()[1] != null)
                this.scale[1] = jointData.getBindLocalScale()[1];
            if (this.scale[2] == null && jointData.getBindLocalScale()[2] != null)
                this.scale[2] = jointData.getBindLocalScale()[2];
        }

        if (jointData.getBindLocalQuaternion() != null) {
            if (this.qRotation == null) {
                this.qRotation = jointData.getBindLocalQuaternion();
            }
        }

        if (jointData.getBindLocalRotation() != null) {
            if (this.rotation[0] == null && jointData.getBindLocalRotation()[0] != null)
                this.rotation[0] = jointData.getBindLocalRotation()[0];
            if (this.rotation[1] == null && jointData.getBindLocalRotation()[1] != null)
                this.rotation[1] = jointData.getBindLocalRotation()[1];
            if (this.rotation[2] == null && jointData.getBindLocalRotation()[2] != null)
                this.rotation[2] = jointData.getBindLocalRotation()[2];
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
        return qRotation != null || rotation != null && rotation[0] != null;
    }

    public boolean hasRotationY() {
        return qRotation != null || rotation != null && rotation[1] != null;
    }

    public boolean hasRotationZ() {
        return qRotation != null || rotation != null && rotation[2] != null;
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

    public void addScale(Float[] extra) {
        if (this.scale == null) {
            this.scale = extra;
        } else {
            add(this.scale, extra);
        }
        refresh();
    }

    public void addRotation(Float[] extra) {
        if (this.rotation == null) {
            this.rotation = extra;
        } else {
            add(this.rotation, extra);
        }
        refresh();
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

        if (Constants.PREFER_QUATERNION && rotationAX.qRotation != null) {
            final Quaternion qRotation = new Quaternion(0, 0, 0, 1);
            Quaternion.interpolate(qRotation, rotationAX.qRotation, rotationBX.qRotation, rotationProgressionX);
            return new JointTransform(scale, qRotation, location);
        } else {
            final Float[] rotation = new Float[3];
            interpolateVector(rotation, rotationAX.rotation, rotationBX.rotation, rotationProgressionX);
            interpolateVector(rotation, rotationAY.rotation, rotationBY.rotation, rotationProgressionY);
            interpolateVector(rotation, rotationAZ.rotation, rotationBZ.rotation, rotationProgressionZ);

            return new JointTransform(scale, rotation, location);
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

        if (Constants.PREFER_QUATERNION && Constants.PREFER_QUATERNION_MATRIX && frameA.qRotation != null) {
            tempQRotation.setIdentity();
            Quaternion.interpolate(tempQRotation, frameA.qRotation, frameB.qRotation, progression);
            tempQRotation.normalize();
            Matrix.multiplyMM(ret, 0, ret, 0, tempQRotation.toRotationMatrix(), 0);
        } else if (Constants.PREFER_QUATERNION && frameA.qRotation != null) {
            tempQRotation.setIdentity();
            Quaternion.interpolate(tempQRotation, frameA.qRotation, frameB.qRotation, progression);
            tempQRotation.normalize();
            Matrix.rotateM(ret, 0, tempQRotation.toAngles(null)[2], 0, 0, 1);
            Matrix.rotateM(ret, 0, tempQRotation.toAngles(null)[1], 0, 1, 0);
            Matrix.rotateM(ret, 0, tempQRotation.toAngles(null)[0], 1, 0, 0);
        } else {
            interpolateVector(tempRotation, frameA.rotation, frameB.rotation, progression);
            if (tempRotation[2] != null)
                Matrix.rotateM(ret, 0, tempRotation[2], 0, 0, 1);
            if (tempRotation[1] != null)
                Matrix.rotateM(ret, 0, tempRotation[1], 0, 1, 0);
            if (tempRotation[0] != null)
                Matrix.rotateM(ret, 0, tempRotation[0], 1, 0, 0);
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
        if (Constants.PREFER_QUATERNION && this.qRotation != null) {
            //this.qRotation.normalize();
            //Matrix.multiplyMM(transform,0, transform, 0, this.qRotation.getMatrix(), 0);
            Matrix.rotateM(transform, 0, this.qRotation.toAngles(null)[2], 0, 0, 1);
            Matrix.rotateM(transform, 0, this.qRotation.toAngles(null)[1], 0, 1, 0);
            Matrix.rotateM(transform, 0, this.qRotation.toAngles(null)[0], 1, 0, 0);
        }
        else if (this.rotation != null) {
            if (this.rotation[2] != null)
                Matrix.rotateM(transform, 0, rotation[2], 0, 0, 1);
            if (this.rotation[1] != null)
                Matrix.rotateM(transform, 0, rotation[1], 0, 1, 0);
            if (this.rotation[0] != null)
                Matrix.rotateM(transform, 0, rotation[0], 1, 0, 0);
        }

        if (matrix != null){
            Matrix.multiplyMM(tempMatrix, 0, tempMatrix, 0, matrix, 0);
        }
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


    public void setQuaternion(Quaternion quaternion) {
        this.qRotation = quaternion;
        if (quaternion != null) {
            this.rotation = quaternion.toAngles2(this.rotation);
        }
        refresh();
    }

    /*public JointTransform setRotation(Float[] angles) {
        this.rotation = angles;
        if (angles != null) {
            this.qRotation = Quaternion.fromEuler();
        }
        updateMatrix();
        return this;
    }*/
}
