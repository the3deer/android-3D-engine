package org.the3deer.android.engine.model;

import android.opengl.Matrix;

import org.the3deer.util.math.Math3DUtils;
import org.the3deer.util.math.Quaternion;

public final class Transform {

    private final float[] matrix;
    private final Float[] scale;
    private final Float[] rotation;
    private final Float[] translation;
    private Quaternion quaternion;
    private final float[] transform;
    // lazy
    private float[] inverseTransform;

    /**
     * Identity transform
     */
    public Transform() {
        this.matrix = null;
        this.scale = null;
        this.rotation = null;
        this.translation = null;
        this.quaternion = null;
        this.transform = new float[16];
        Matrix.setIdentityM(this.transform, 0);
    }

    /**
     * Matrix transform
     *
     * @param matrix the 4x4 matrix
     */
    public Transform(float[] matrix) {
        this.matrix = matrix;
        this.quaternion = Quaternion.fromMatrix(matrix);
        this.scale = Math3DUtils.scaleFromMatrix(matrix);
        this.rotation = this.quaternion.toAnglesF(null);
        this.translation = Math3DUtils.extractTranslation2(matrix, null);
        this.transform = matrix;
    }

    /**
     * Default transform by components
     *
     * @param scale       the scale x,y,z
     * @param rotation    the axis-angle rotation x,y,z
     * @param translation the translation x,y,z
     */
    public Transform(Float[] scale, Float[] rotation, Float[] translation) {
        this.matrix = null;
        this.scale = scale;
        this.rotation = rotation;
        if (rotation != null) {
            this.quaternion = Quaternion.fromEulerD(rotation[0], rotation[1], rotation[2]);
        } else {
            this.quaternion = null;
        }
        this.translation = translation;

        // 1. Create a new 4x4 matrix, initialized to identity
        float[] tempTransform = new float[16];
        Matrix.setIdentityM(tempTransform, 0);

        // 1. Apply Translation
        if (translation != null) {
            Matrix.translateM(tempTransform, 0, translation[0], translation[1], translation[2]);
        }

        // 2. Apply Scale
        if (scale != null) {
            Matrix.scaleM(tempTransform, 0, scale[0], scale[1], scale[2]);
        }

        // 3. Apply Rotation from the quaternion
        // We get the rotation matrix from the quaternion and multiply it with our current transform
        //float[] rotationMatrix = this.quaternion.toMatrix();
        //Matrix.multiplyMM(tempTransform, 0, tempTransform, 0, rotationMatrix, 0);

        // 3. Apply Rotation
        if (rotation != null) {
            Matrix.rotateM(tempTransform, 0, rotation[2], 0, 0, 1);
            Matrix.rotateM(tempTransform, 0, rotation[1], 0, 1, 0);
            Matrix.rotateM(tempTransform, 0, rotation[0], 1, 0, 0);
        }



        this.transform = tempTransform;
    }

    /**
     * Default transform by components
     *
     * @param scale       the scale x,y,z
     * @param quaternion  the quaternion rotation
     * @param translation the translation x,y,z
     */
    public Transform(Float[] scale, Quaternion quaternion, Float[] translation) {
        this.matrix = null;
        this.scale = scale;
        this.quaternion = quaternion;
        if (quaternion != null) {
            this.rotation = quaternion.toAnglesF(null);
        } else {
            this.rotation = null;
        }
        this.translation = translation;

        // 1. Create a new 4x4 matrix, initialized to identity
        float[] tempTransform = new float[16];
        Matrix.setIdentityM(tempTransform, 0);

        // 4. Apply Translation
        if (translation != null) {
            if (translation[0] != null) Matrix.translateM(tempTransform, 0, translation[0], 0, 0);
            if (translation[1] != null) Matrix.translateM(tempTransform, 0, 0, translation[1], 0);
            if (translation[2] != null) Matrix.translateM(tempTransform, 0, 0, 0, translation[2]);
        }

        // 2. Apply Scale
        if (scale != null) {
            if (scale[0] != null) Matrix.scaleM(tempTransform, 0, scale[0], 1, 1);
            if (scale[1] != null) Matrix.scaleM(tempTransform, 0, 1, scale[1], 1);
            if (scale[2] != null) Matrix.scaleM(tempTransform, 0, 1, 1, scale[2]);
        }

        // 3. Apply Rotation
        if (rotation != null) {
            Matrix.rotateM(tempTransform, 0, rotation[2], 0, 0, 1);
            Matrix.rotateM(tempTransform, 0, rotation[1], 0, 1, 0);
            Matrix.rotateM(tempTransform, 0, rotation[0], 1, 0, 0);
        }
        // 3. Apply Rotation from the quaternion
            // We get the rotation matrix from the quaternion and multiply it with our current transform
        //float[] rotationMatrix = this.quaternion.toMatrix();
        //Matrix.multiplyMM(tempTransform, 0, tempTransform, 0, rotationMatrix, 0);


        this.transform = tempTransform;
    }


    public Float[] getScale() {
        return scale;
    }

    public Float[] getRotation() {
        return rotation;
    }

    public Float[] getTranslation() {
        return translation;
    }

    public float[] getMatrix() {
        return matrix;
    }

    public Quaternion getQuaternion() {
        return quaternion;
    }

    public void setQuaternion(Quaternion quaternion) {
        this.quaternion = quaternion;
    }

    public float[] getTransform() {
        return transform;
    }

    public float[] getInverseTransform() {
        if (this.inverseTransform == null) {
            this.inverseTransform = new float[16];
            Matrix.setIdentityM(this.inverseTransform, 0);
        }
        return inverseTransform;
    }

    public void setInverseTransform(float[] inverseBindTransform) {
        this.inverseTransform = inverseBindTransform;
    }

    /**
     * convert an array of primitive floats to Float wrapper array
     *
     * @param floats primitive float array
     * @return Float wrapper array
     */
    private static Float[] floatArrayToFloatWrapperArray(float[] floats) {
        if (floats == null) {
            return null; // Handle null case if needed
        }

        // 1. Create the new Float[] array with the same size.
        Float[] wrapperArray = new Float[floats.length];

        // 2. Loop through the primitive array and auto-box each float to Float.
        for (int i = 0; i < floats.length; i++) {
            wrapperArray[i] = floats[i]; // Auto-boxing occurs here
        }

        return wrapperArray;
    }

    public static Transform of(Float[] scale, Quaternion rotation, Float[] translation) {
        return new Transform(scale, rotation, translation);
    }
}
