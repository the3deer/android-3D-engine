package org.the3deer.util.math;

import android.opengl.Matrix;
import android.util.Log;

import androidx.annotation.Nullable;

import org.the3deer.android_3d_model_engine.model.Constants;

/**
 * A quaternion simply represents a 3D rotation. The maths behind it is quite
 * complex (literally; it involves complex numbers) so I wont go into it in too
 * much detail. The important things to note are that it represents a 3d
 * rotation, it's very easy to interpolate between two quaternion rotations
 * (which would not be easy to do correctly with Euler rotations or rotation
 * matrices), and you can convert to and from matrices fairly easily. So when we
 * need to interpolate between rotations we'll represent them as quaternions,
 * but when we need to apply the rotations to anything we'll convert back to a
 * matrix.
 * <p>
 * An quick introduction video to quaternions:
 * https://www.youtube.com/watch?v=SCbpxiCN0U0
 * <p>
 * and a slightly longer one:
 * https://www.youtube.com/watch?v=fKIss4EV6ME&t=0s
 *
 * @author Karl
 */
public class Quaternion {

    private float[] matrix;
    private float x, y, z, w;
    private boolean normalized;

    public Quaternion(float[] matrix) {
        this.matrix = matrix;
    }

    public Quaternion() {
        this(0, 0, 0, 1);
    }

    /**
     * Creates a quaternion and normalizes it.
     *
     * @param x
     * @param y
     * @param z
     * @param w
     */
    public Quaternion(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public static Quaternion fromEuler(double roll, double pitch, double yaw) {
        // This is not in game format, it is in mathematical format.
            // Abbreviations for the various angular functions

            double cr = Math.cos(roll * 0.5);
            double sr = Math.sin(roll * 0.5);
            double cp = Math.cos(pitch * 0.5);
            double sp = Math.sin(pitch * 0.5);
            double cy = Math.cos(yaw * 0.5);
            double sy = Math.sin(yaw * 0.5);

            Quaternion q = new Quaternion();
            q.w = (float) (cr * cp * cy + sr * sp * sy);
            q.x = (float) (sr * cp * cy - cr * sp * sy);
            q.y = (float) (cr * sp * cy + sr * cp * sy);
            q.z = (float) (cr * cp * sy - sr * sp * cy);

            return q;
    }

    private void fixWeight() {
        if (this.x == 0f && this.y == 0 && this.z == 0 && w == -1) {
            this.w = 1f;
        }
    }

    public static Quaternion fromArray(float[] rotation) {
        if (rotation == null) return null;
        return new Quaternion(rotation[0], rotation[1], rotation[2], rotation[3]);
    }

    /**
     * Extracts the rotation part of the 4x4 matrix into a Quaternion
     *
     * @param matrix column-major ordered matrix
     * @return the corresponding quaternion rotation
     */
    public static Quaternion fromMatrix(float[] matrix) {
        if (Constants.STRATEGY_NEW) {
            return fromRotationMatrix(matrix);
        } else {
            return fromMatrix1(matrix);
        }
    }

    // FIXME
    private float[] toRotationMatrix(float[] matrix) {
        if (Constants.STRATEGY_NEW) {
            //return toRotationMatrixWikipedia(matrix);
            return toRotationMatrixPy(matrix);
            //return toRotationMatrix2(matrix);
        } else {
            return toRotationMatrix2(matrix);
        }
    }

    /**
     * Sets the quaternion from the specified rotation matrix.
     *
     * <p>Does not verify that the argument is a valid rotation matrix.
     * Positive scaling is compensated for, but not reflection or shear.
     *
     * @param matrix the input matrix (not null, unaffected)
     * @return the (modified) current instance (for chaining)
     */
    private static Quaternion fromRotationMatrix(float[] matrix) {
        //return fromRotationMatrix(
        //matrix.m00, matrix.m01, matrix.m02,
        //matrix.m10, matrix.m11, matrix.m12,
        //matrix.m20, matrix.m21, matrix.m22);
        return fromRotationMatrix(matrix[0], matrix[4], matrix[8],
                matrix[1], matrix[5], matrix[9],
                matrix[2], matrix[6], matrix[10]);
    }

    /**
     * Extracts the rotation part of a transformation matrix and converts it to
     * a quaternion using the magic of maths.
     * <p>
     * More detailed explanation here:
     * <a href="http://www.euclideanspace.com/maths/geometry/rotations/conversions/matrixToQuaternion/index.htm">...</a>
     *
     * @param matrix - the transformation matrix containing the rotation which this
     *               quaternion shall represent.
     */
    public static Quaternion fromMatrix1(float[] matrix) {

    // check
        if (matrix == null) return null;

        float w, x, y, z;
        float diagonal = matrix[0] + matrix[5] + matrix[10];
        if (diagonal > 0) {
            float w4 = (float) (Math.sqrt(diagonal + 1f) * 2f);
            w = w4 / 4f;
            x = (matrix[9] - matrix[6]) / w4;
            y = (matrix[2] - matrix[8]) / w4;
            z = (matrix[4] - matrix[1]) / w4;
        } else if ((matrix[0] > matrix[5]) && (matrix[0] > matrix[10])) {
            float x4 = (float) (Math.sqrt(1f + matrix[0] - matrix[5] - matrix[10]) * 2f);
            w = (matrix[9] - matrix[6]) / x4;
            x = x4 / 4f;
            y = (matrix[1] + matrix[4]) / x4;
            z = (matrix[2] + matrix[8]) / x4;
        } else if (matrix[5] > matrix[10]) {
            float y4 = (float) (Math.sqrt(1f + matrix[5] - matrix[0] - matrix[10]) * 2f);
            w = (matrix[2] - matrix[8]) / y4;
            x = (matrix[1] + matrix[4]) / y4;
            y = y4 / 4f;
            z = (matrix[6] + matrix[9]) / y4;
        } else {
            float z4 = (float) (Math.sqrt(1f + matrix[10] - matrix[0] - matrix[5]) * 2f);
            w = (matrix[4] - matrix[1]) / z4;
            x = (matrix[2] + matrix[8]) / z4;
            y = (matrix[6] + matrix[9]) / z4;
            z = z4 / 4f;
        }
        return new Quaternion(x, y, z, w);
    }

    /**
     * Sets the quaternion from a rotation matrix with the specified elements.
     *
     * <p>Does not verify that the arguments form a valid rotation matrix.
     * Positive scaling is compensated for, but not reflection or shear.
     *
     * @param m00 the matrix element in row 0, column 0
     * @param m01 the matrix element in row 0, column 1
     * @param m02 the matrix element in row 0, column 2
     * @param m10 the matrix element in row 1, column 0
     * @param m11 the matrix element in row 1, column 1
     * @param m12 the matrix element in row 1, column 2
     * @param m20 the matrix element in row 2, column 0
     * @param m21 the matrix element in row 2, column 1
     * @param m22 the matrix element in row 2, column 2
     * @return the (modified) current instance (for chaining)
     */
    private static Quaternion fromRotationMatrix(float m00, float m01, float m02,
                                         float m10, float m11, float m12, float m20, float m21, float m22) {
        // first normalize the forward (F), up (U) and side (S) vectors of the rotation matrix
        // so that positive scaling does not affect the rotation
        double lengthSquared = m00 * m00 + m10 * m10 + m20 * m20;
        if (lengthSquared != 1f && lengthSquared != 0f) {
            lengthSquared = (1.0f / Math.sqrt(lengthSquared));
            m00 *= lengthSquared;
            m10 *= lengthSquared;
            m20 *= lengthSquared;
        }
        lengthSquared = m01 * m01 + m11 * m11 + m21 * m21;
        if (lengthSquared != 1f && lengthSquared != 0f) {
            lengthSquared = 1.0f / Math.sqrt(lengthSquared);
            m01 *= lengthSquared;
            m11 *= lengthSquared;
            m21 *= lengthSquared;
        }
        lengthSquared = m02 * m02 + m12 * m12 + m22 * m22;
        if (lengthSquared != 1f && lengthSquared != 0f) {
            lengthSquared = 1.0f / Math.sqrt(lengthSquared);
            m02 *= lengthSquared;
            m12 *= lengthSquared;
            m22 *= lengthSquared;
        }

        // Use the Graphics Gems code, from
        // ftp://ftp.cis.upenn.edu/pub/graphics/shoemake/quatut.ps.Z
        // *NOT* the "Matrix and Quaternions FAQ", which has errors!

        // the trace is the sum of the diagonal elements; see
        // http://mathworld.wolfram.com/MatrixTrace.html
        float t = m00 + m11 + m22;

        Quaternion q = new Quaternion();

        // we protect the division by s by ensuring that s>=1
        if (t >= 0) { // |w| >= .5
            float s = (float) Math.sqrt(t + 1); // |s|>=1 ...
            q.w = 0.5f * s;
            s = 0.5f / s;                 // so this division isn't bad
            q.x = (m21 - m12) * s;
            q.y = (m02 - m20) * s;
            q.z = (m10 - m01) * s;
        } else if ((m00 > m11) && (m00 > m22)) {
            float s = (float) Math.sqrt(1.0f + m00 - m11 - m22); // |s|>=1
            q.x = s * 0.5f; // |x| >= .5
            s = 0.5f / s;
            q.y = (m10 + m01) * s;
            q.z = (m02 + m20) * s;
            q.w = (m21 - m12) * s;
        } else if (m11 > m22) {
            float s = (float) Math.sqrt(1.0f + m11 - m00 - m22); // |s|>=1
            q.y = s * 0.5f; // |y| >= .5
            s = 0.5f / s;
            q.x = (m10 + m01) * s;
            q.z = (m21 + m12) * s;
            q.w = (m02 - m20) * s;
        } else {
            float s = (float) Math.sqrt(1.0f + m22 - m00 - m11); // |s|>=1
            q.z = s * 0.5f; // |z| >= .5
            s = 0.5f / s;
            q.x = (m02 + m20) * s;
            q.y = (m21 + m12) * s;
            q.w = (m10 - m01) * s;
        }

        return q;
    }

    public void setIdentity() {
        this.x = 0;
        this.y = 0;
        this.z = 0;
        this.w = 1;
        normalized = false;
    }

    public boolean isIdentity() {
        return this.x == 0 && this.y == 0 && this.z == 0 && w == 1;
    }

    private Quaternion setMatrix(float[] matrix) {
        this.matrix = matrix;
        return this;
    }

    public float[] getMatrix(){
        if (matrix == null){
            matrix = toRotationMatrix(null);
        }
        return matrix;
    }

    public float[] toRotationMatrix() {
        matrix = toRotationMatrix(matrix);
        return matrix;
    }

    public void update(Quaternion other) {
        this.matrix = other.matrix;
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
        this.w = other.w;
    }


    /**
     * Normalizes the quaternion.
     */
    public Quaternion normalize() {

        // check
        if (normalized) return this;

        float sqw = w * w;
        float sqx = x * x;
        float sqy = y * y;
        float sqz = z * z;
        float checksum = sqx + sqy + sqz + sqw; // if normalized is one, otherwise
        if (checksum == 1){
            normalized = true;
            return this;
        }

        double mag = Math.sqrt(w * w + x * x + y * y + z * z);
        w /= (float) mag;
        x /= (float) mag;
        y /= (float) mag;
        z /= (float) mag;

        normalized = true;

        sqw = w * w;
        sqx = x * x;
        sqy = y * y;
        sqz = z * z;
        checksum = sqx + sqy + sqz + sqw; // if normalized is one, otherwise

        if (Math.abs(1 - checksum) > 0.00001){
            Log.e("Quaternion", "Unexpected checksum '"+checksum+"',  "+this);
        }

        //fixWeight();
        return this;
    }

    /**
     * <p>From a quaternion to an orthogonal matrix</p>
     *
     * <p>
     * The orthogonal matrix corresponding to a rotation by the unit quaternion
     * <code>z = a + bi + cj + dk</code> (with <code>|z| = 1</code>)
     * when post-multiplying with a column vector is given by a 3x3 matrix
     * </p>
     * <p>
 * An efficient calculation in which the quaternion does not need to be unit normalized is given by
     *
     * <pre>
     * 1 - cc - dd  bc - ad      bd + ac
     * bc + ad      1 - bb - dd  cd - ab
     * bd - ac      cd + ab      1 - bb - cc
     *
     * where the following intermediate quantities have been defined:
     *
     * s = 2 / (a.a + b.b + c.c + d.d)
     * bs = b.s     cs = c.s     ds = d.s
     * ab = a.bs    ac = a.cs    ad = a.ds
     * bb = b.bs    bc = b.cs    bd = b.ds
     * cc = c.cs    cd = c.ds    dd = d.ds
     * </pre>
     * <p>
     * The returned matrix is a column-major ordered matrix.
     *
     * <a href="https://en.wikipedia.org/wiki/Quaternions_and_spatial_rotation">wikipedia link</a>
     *
     * @param m the data buffer
     * @return the rotation matrix corresponding to this quaternion
     */
    private float[] toRotationMatrixWikipedia(float[] m) {
        if (m == null) {
            m = new float[16];
        }

        float a = w;
        float b = x;
        float c = y;
        float d = z;

        // @formatter:off
        float s = 2 / (a * a + b * b + c * c + d * d);
        float bs = b * s;	float cs = c * s;   float ds = d * s;
        float ab = a * bs;  float ac = a * cs;  float ad = a * ds;
        float bb = b * bs; 	float bc = b * cs;	float bd = b * ds;
        float cc = c * cs;  float cd = c * ds;	float dd = d * ds;
        // @formatter:on

        // @formatter:off
        m[0]=1 - cc - d;	m[4]=bc - ad;       m[8]=bd + ac;       m[12]=0;
        m[1]=bc + ad;       m[5]=1 - bb - dd;	m[9]=cd - ab;       m[13]=0;
        m[2]=bd - ac;       m[6]=cd + ab;       m[10]=1 - bb - cc;  m[14]=0;
        m[3]=0;             m[7]=0;             m[11]=0;            m[15]=1;
        // @formatter:on

        return m;
    }

    /**
     * This has a problem. when x=0.7, y=0, z=0, w=0.7 (that is +180 degrees)
     * the returned matrix contains a negative angle.
     * @param matrix the data buffer
     * @return the corresponding matrix
     */
    private float[] toRotationMatrix4(float[] matrix) {

        if (matrix == null) {
            matrix = new float[16];
        }

        Quaternion q = this;

        double sqw = q.w * q.w;
        double sqx = q.x * q.x;
        double sqy = q.y * q.y;
        double sqz = q.z * q.z;

        // invs (inverse square length) is only required if quaternion is not already normalised
        double invs = 1 / (sqx + sqy + sqz + sqw);
        float m00 = (float) ((sqx - sqy - sqz + sqw) * invs); // since sqw + sqx + sqy + sqz =1/invs*invs
        float m11 = (float) ((-sqx + sqy - sqz + sqw) * invs);
        float m22 = (float) ((-sqx - sqy + sqz + sqw) * invs);

        double tmp1 = q.x * q.y;
        double tmp2 = q.z * q.w;
        float m10 = (float) (2.0 * (tmp1 + tmp2) * invs);
        float m01 = (float) (2.0 * (tmp1 - tmp2) * invs);

        tmp1 = q.x * q.z;
        tmp2 = q.y * q.w;
        float m20 = (float) (2.0 * (tmp1 - tmp2) * invs);
        float m02 = (float) (2.0 * (tmp1 + tmp2) * invs);
        tmp1 = q.y * q.z;
        tmp2 = q.x * q.w;
        float m21 = (float) (2.0 * (tmp1 + tmp2) * invs);
        float m12 = (float) (2.0 * (tmp1 - tmp2) * invs);

        matrix[0] = m00;
        matrix[1] = m01;
        matrix[2] = m02;
        matrix[3] = 0;
        matrix[4] = m10;
        matrix[5] = m11;
        matrix[6] = m12;
        matrix[7] = 0;
        matrix[8] = m20;
        matrix[9] = m21;
        matrix[10] = m22;
        matrix[11] = 0;
        matrix[12] = 0;
        matrix[13] = 0;
        matrix[14] = 0;
        matrix[15] = 1;

        return matrix;
    }

    /**
     *  """
     *         Covert a quaternion into a full three-dimensional rotation matrix.
     *
     *         Input
     *         :param Q: A 4 element array representing the quaternion (q0,q1,q2,q3)
     *
     *         Output
     *         :return: A 3x3 element matrix representing the full 3D rotation matrix.
     *                  This rotation matrix converts a point in the local reference
     *                  frame to a point in the global reference frame.
     *         """
     * @param matrix
     */
    public float[] toRotationMatrixPy(float[] matrix){

        if (matrix == null) {
            matrix = new float[16];
        }
        Matrix.setIdentityM(matrix,0);

        // check
        if (x == 0 && y == 0 && z == 0 && w == 1) return matrix;

    // Extract the values from Q
        float q0 = w;
        float q1 = x;
        float q2 = y;
        float q3 = z;

    // First row of the rotation matrix
        float r00 = 2 * (q0 * q0 + q1 * q1) - 1;
        float r01 = 2 * (q1 * q2 - q0 * q3);
        float r02 = 2 * (q1 * q3 + q0 * q2);

    // Second row of the rotation matrix
        float r10 = 2 * (q1 * q2 + q0 * q3);
        float r11 = 2 * (q0 * q0 + q2 * q2) - 1;
        float r12 = 2 * (q2 * q3 - q0 * q1);

    // Third row of the rotation matrix
        float r20 = 2 * (q1 * q3 - q0 * q2);
        float r21 = 2 * (q2 * q3 + q0 * q1);
        float r22 = 2 * (q0 * q0 + q3 * q3) - 1;



        // 3x3 rotation matrix
                //rot_matrix = np.array([[r00, r01, r02],
//                           [r10, r11, r12],
//                           [r20, r21, r22]])

        matrix[0] = r00;
        matrix[1] = r10;
        matrix[2] = r20;
        matrix[4] = r01;
        matrix[5] = r11;
        matrix[6] = r21;
        matrix[8] = r02;
        matrix[9] = r12;
        matrix[10] = r22;

        return matrix;
    }

    /**
     * This has a problem. when x=0.7, y=0, z=0, w=0.7 (that is +180 degrees)
     * the returned matrix contains a negative angle.
     * @param matrix the data buffer
     * @return the corresponding matrix
     */
    private float[] toRotationMatrix2(float[] matrix) {
        if (matrix == null) {
            matrix = new float[16];
        }

        final float xy = x * y;
        final float xz = x * z;
        final float xw = x * w;
        final float yz = y * z;
        final float yw = y * w;
        final float zw = z * w;
        final float xSquared = x * x;
        final float ySquared = y * y;
        final float zSquared = z * z;
        matrix[0] = 1 - 2 * (ySquared + zSquared);
        matrix[1] = 2 * (xy - zw);
        matrix[2] = 2 * (xz + yw);
        matrix[3] = 0;
        matrix[4] = 2 * (xy + zw);
        matrix[5] = 1 - 2 * (xSquared + zSquared);
        matrix[6] = 2 * (yz - xw);
        matrix[7] = 0;
        matrix[8] = 2 * (xz - yw);
        matrix[9] = 2 * (yz + xw);
        matrix[10] = 1 - 2 * (xSquared + ySquared);
        matrix[11] = 0;
        matrix[12] = 0;
        matrix[13] = 0;
        matrix[14] = 0;
        matrix[15] = 1;
        return matrix;
    }


    /**
     * Interpolates between two quaternion rotations and returns the resulting
     * quaternion rotation. The interpolation method here is "nlerp", or
     * "normalized-lerp". Another mnethod that could be used is "slerp", and you
     * can see a comparison of the methods here:
     * <a href="https://keithmaggio.wordpress.com/2011/02/15/math-magician-lerp-slerp-and-nlerp/">...</a>
     * <p>
     * and here:
     * <a href="http://number-none.com/product/Understanding%20Slerp,%20Then%20Not%20Using%20It/">...</a>
     *
     * @param result resulting interpolation
     * @param a
     * @param b
     * @param blend  - a value between 0 and 1 indicating how far to interpolate
     *               between the two quaternions.
     * @return The resulting interpolated rotation in quaternion format.
     */
    public static void interpolate(Quaternion result, Quaternion a, Quaternion b, float blend) {
        if (a == null || b == null) {
            //Log.v("Quaternion","you passed in a null quaternion");
            return;
        }

        float dot = a.w * b.w + a.x * b.x + a.y * b.y + a.z * b.z;
        float blendI = 1f - blend;
        if (dot < 0) {
            result.w = blendI * a.w + blend * -b.w;
            result.x = blendI * a.x + blend * -b.x;
            result.y = blendI * a.y + blend * -b.y;
            result.z = blendI * a.z + blend * -b.z;
        } else {
            result.w = blendI * a.w + blend * b.w;
            result.x = blendI * a.x + blend * b.x;
            result.y = blendI * a.y + blend * b.y;
            result.z = blendI * a.z + blend * b.z;
        }
        //result.normalize();
    }

    /*public static void interpolate(Quaternion a, Quaternion b, float blend, float[] output) {
        Quaternion result = new Quaternion(0, 0, 0, 1);
        float dot = a.w * b.w + a.x * b.x + a.y * b.y + a.z * b.z;
        float blendI = 1f - blend;
        if (dot < 0) {
            result.w = blendI * a.w + blend * -b.w;
            result.x = blendI * a.x + blend * -b.x;
            result.y = blendI * a.y + blend * -b.y;
            result.z = blendI * a.z + blend * -b.z;
        } else {
            result.w = blendI * a.w + blend * b.w;
            result.x = blendI * a.x + blend * b.x;
            result.y = blendI * a.y + blend * b.y;
            result.z = blendI * a.z + blend * b.z;
        }
        result.normalize();
        result.toRotationMatrix(output);
    }*/

    @Override
    public String toString() {
        return "Quaternion{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", w=" + w +
                '}';
    }

    /*public float[] toEuler() {

        Quaternion q = this;
        // roll (x-axis rotation)
        double sinr_cosp = 2 * (q.w * q.x + q.y * q.z);
        double cosr_cosp = 1 - 2 * (q.x * q.x + q.y * q.y);
        float roll = (float) Math.atan2(sinr_cosp, cosr_cosp);

        // pitch (y-axis rotation)
        double sinp = 2 * (q.w * q.y - q.z * q.x);
        final float pitch;
        if (Math.abs(sinp) >= 1)
            pitch = (float) Math.copySign (Math.PI / 2, sinp); // use 90 degrees if out of range
        else
            pitch = (float) Math.asin(sinp);

        // yaw (z-axis rotation)
        double siny_cosp = 2 * (q.w * q.z + q.x * q.y);
        double cosy_cosp = 1 - 2 * (q.y * q.y + q.z * q.z);
        float yaw = (float) Math.atan2(siny_cosp, cosy_cosp);

        return  new float[]{roll, pitch, yaw, 1};
    }*/

    /**
     * Return the corresponding euler angles in radians
     * @param angles buffer data
     * @return the corresponding euler angles in radians
     */
    public float[] toAngles(float[] angles) {
        if (angles == null){
            angles = new float[3];
        }
        if (Constants.STRATEGY_NEW) {
            if (getDefaultAngle(angles) != null){
                return angles;
            } else {
                return toEulerAnglesWikipedia(angles);
                //return toEulerAnglesES(angles);
                //return toAngles1(angles);
            }
        } else {
            return toAngles1(angles);
        }
    }

    /**
     *
     * Heading – ψ: rotation about the Z-axis
     * Pitch –   θ: rotation about the new Y-axis
     * Bank – ϕ: rotation about the new X-axis
     *
     * x = roll = bank
     * y = pitch = pitch
     * z = yaw = heading
     *
     * @param angles
     * @return
     */
    private float[] toEulerAnglesES(float[] angles){

        if (angles == null){
            angles = new float[3];
        }

        Quaternion q1 = this;

        if (getDefaultAngle(angles) != null){
            return angles;
        }

        float sqw = q1.w*q1.w;
        float sqx = q1.x*q1.x;
        float sqy = q1.y*q1.y;
        float sqz = q1.z*q1.z;
        double heading = Math.atan2(2.0 * (q1.x*q1.y + q1.z*q1.w),(sqx - sqy - sqz + sqw));
        double bank = Math.atan2(2.0 * (q1.y*q1.z + q1.x*q1.w),(-sqx - sqy + sqz + sqw));
        double attitude = Math.asin(-2.0 * (q1.x*q1.z - q1.y*q1.w)/sqx + sqy + sqz + sqw);

        if (Double.isNaN(heading) || Double.isNaN(bank) || Double.isNaN(attitude)) {
            //Log.e("Quaternion", "NaN: "+this);
            angles[0] = angles[1] = angles[2] = 0f;
            return angles;
        }

        angles[0] = (float) Math.toDegrees(bank);
        angles[1] = (float) Math.toDegrees(attitude);
        angles[2] = (float) Math.toDegrees(heading);

        return angles;
    }

    private float[] toEulerAnglesWikipedia(float[] angles){
        if (angles == null){
            angles = new float[3];
        }

        Quaternion q = this;

        // roll (x-axis rotation)
        double sinr_cosp = 2 * (q.w * q.x + q.y * q.z);
        double cosr_cosp = 1 - 2 * (q.x * q.x + q.y * q.y);
        angles[0] = (float) Math.atan2(sinr_cosp, cosr_cosp);

        // pitch (y-axis rotation)
        double sinp = Math.sqrt(1 + 2 * (q.w * q.y - q.x * q.z));
        double cosp = Math.sqrt(1 - 2 * (q.w * q.y - q.x * q.z));
        angles[1] = (float) (2 * Math.atan2(sinp, cosp) - Math.PI / 2);

        // yaw (z-axis rotation)
        double siny_cosp = 2 * (q.w * q.z + q.x * q.y);
        double cosy_cosp = 1 - 2 * (q.y * q.y + q.z * q.z);
        angles[2] = (float) Math.atan2(siny_cosp, cosy_cosp);

        if (Float.isNaN(angles[0])){
            angles[0] = 0f;
        }
        if (Float.isNaN(angles[1])){
            angles[1] = 0f;
        }
        if (Float.isNaN(angles[2])){
            angles[2] = 0f;
        }

        // conver to degrees
        angles[0] = (float) Math.toDegrees(angles[0]);
        angles[1] = (float) Math.toDegrees(angles[1]);
        angles[2] = (float) Math.toDegrees(angles[2]);

        return angles;
    }

    /**
     * wrong implementation
     *
     * @param angles
     * @return
     */
    public float[] toEulerAngles3(float[] angles){
            if (angles == null){
                angles = new float[3];
            }

            Quaternion q = this;

            // roll / x
            double sinr_cosp = 2 * (q.w * q.x + q.y * q.z);
            double cosr_cosp = 1 - 2 * (q.x * q.x + q.y * q.y);
            angles[0] = (float)Math.atan2(sinr_cosp, cosr_cosp);

            // pitch / y
            double sinp = 2 * (q.w * q.y - q.z * q.x);
            if (Math.abs(sinp) >= 1)
            {
                angles[1] = (float)Math.copySign(Math.PI / 2, sinp);
            }
            else
            {
                angles[1] = (float)Math.asin(sinp);
            }

            // yaw / z
            double siny_cosp = 2 * (q.w * q.z + q.x * q.y);
            double cosy_cosp = 1 - 2 * (q.z * q.y + q.z * q.z);
            angles[2] = (float)Math.atan2(siny_cosp, cosy_cosp);

            // conver to degrees
            angles[0] = (float) Math.toDegrees(angles[0]);
            angles[1] = (float) Math.toDegrees(angles[1]);
            angles[2] = (float) Math.toDegrees(angles[2]);

            return angles;
    }

    /**
     * Return the corresponding euler angles in radians
     * @param angles buffer data
     * @return the corresponding euler angles in radians
     */
    public float[] toAngles1(float[] angles) {
        if (angles == null) {
            angles = new float[3];
        } else if (angles.length != 3) {
            throw new IllegalArgumentException("Angles array must have three elements");
        }

        /*float[] standardAngle = getDefaultAngle(angles);
        if (standardAngle != null) return standardAngle;*/

        float sqw = w * w;
        float sqx = x * x;
        float sqy = y * y;
        float sqz = z * z;
        float unit = sqx + sqy + sqz + sqw; // if normalized is one, otherwise
        // is correction factor
        float test = x * y + z * w;
        if (test > 0.499 * unit) { // singularity at north pole
            angles[1] = (float) (2 * Math.atan2(x, w));
            angles[2] = (float) (Math.PI / 2);
            angles[0] = 0;
        } else if (test < -0.499 * unit) { // singularity at south pole
            angles[1] = (float) (-2 * Math.atan2(x, w));
            angles[2] = -(float) (Math.PI / 2);
            angles[0] = 0;
        } else {
            angles[1] = (float) Math.atan2(2 * y * w - 2 * x * z, sqx - sqy - sqz + sqw); // yaw or heading
            angles[2] = (float) Math.asin(2 * test / unit); // roll or bank
            angles[0] = (float) Math.atan2(2 * x * w - 2 * y * z, -sqx + sqy - sqz + sqw); // pitch or attitude
        }


        // conver to degrees
        angles[0] = (float) Math.toDegrees(angles[0]);
        angles[1] = (float) Math.toDegrees(angles[1]);
        angles[2] = (float) Math.toDegrees(angles[2]);

        return angles;
    }

    @Nullable
    private float[] getDefaultAngle(float[] angles) {

        // null rotation
        if (x == 0 && y == 0 && z == 0 && (w == -1 || w == 1)) {  // https://quaternions.online/
            angles[0] = 0;
            angles[1] = 0;
            angles[2] = 0;
            return angles;
        } else if (x == 1 && y == 0 && z == 0 && w < 0) {
            angles[0] = -180;
            angles[1] = 0;
            angles[2] = 0;
            return angles;
        } else if (x == 1 && y == 0 && z == 0 && w > 0) {
            angles[0] = 180;
            angles[1] = 0;
            angles[2] = 0;
            return angles;
        } else if (y == 1 && x == 0 && z == 0 && w < 0) {
            angles[0] = 0;
            angles[1] = -180;
            angles[2] = 0;
            return angles;
        } else if (y == 1 && x == 0 && z == 0 && w > 0) {
            angles[0] = 0;
            angles[1] = 180;
            angles[2] = 0;
            return angles;
        } else if (z == 1 && x == 0 && y== 0 && w < 0) {
            angles[0] = 0;
            angles[1] = 0;
            angles[2] = -180;
            return angles;
        } else if (z == 1 && x == 0 && y == 0 && w > 0) {
            angles[0] = 0;
            angles[1] = 0;
            angles[2] = 180;
            return angles;
        }
        return null;
    }

    /**
     * Return the corresponding euler angles (degrees)
     * @param dest buffer data
     * @return the corresponding euler angles (degrees)
     */
    public Float[] toAngles2(Float[] dest) {
        float[] angles = toAngles(null);
        if (dest == null){
            dest = new Float[3];
        }
        dest[0] = angles[0];
        dest[1] = angles[1];
        dest[2] = angles[2];
        return dest;
    }

    /**
     * Returns the conjugate quaternion of the instance.
     *
     * @return the conjugate quaternion
     */
    public Quaternion getConjugate() {
        return new Quaternion(-this.x, -this.y, -this.z, this.w);
    }

    /**
     * Returns the Hamilton product of two quaternions.
     *
     * @param q1 First quaternion.
     * @param q2 Second quaternion.
     * @return the product {@code q1} and {@code q2}, in that order.
     */
    public static Quaternion multiply(final Quaternion q1, final Quaternion q2) {
        // Components of the first quaternion.
        final double q1a = q1.getW();
        final double q1b = q1.getX();
        final double q1c = q1.getY();
        final double q1d = q1.getZ();

        // Components of the second quaternion.
        final double q2a = q2.getW();
        final double q2b = q2.getX();
        final double q2c = q2.getY();
        final double q2d = q2.getZ();

        // Components of the product.
        final double w = q1a * q2a - q1b * q2b - q1c * q2c - q1d * q2d;
        final double x = q1a * q2b + q1b * q2a + q1c * q2d - q1d * q2c;
        final double y = q1a * q2c - q1b * q2d + q1c * q2a + q1d * q2b;
        final double z = q1a * q2d + q1b * q2c - q1c * q2b + q1d * q2a;

        return new Quaternion((float) x, (float) y, (float) z, (float) w);
    }

    public static Quaternion getQuaternion(float[] axis, float angle) {
        float w = (float) Math.cos(angle / 2f);
        float x = (float) (axis[0] * Math.sin(angle / 2f));
        float y = (float) (axis[1] * Math.sin(angle / 2f));
        float z = (float) (axis[2] * Math.sin(angle / 2f));
        return new Quaternion(x, y, z, w);
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getZ() {
        return z;
    }

    public void setZ(float z) {
        this.z = z;
    }

    public float getW() {
        return w;
    }

    public float[] toAxisAngle() {
        if (w > 1)
            normalize(); // if w>1 acos and sqrt will produce errors, this cant happen if quaternion is normalised

        double angle = 2 * Math.acos(w);

        float[] ret = new float[]{x, y, z, (float) Math.toDegrees(angle)};

        double s = Math.sqrt(1 - w * w); // assuming quaternion normalised then w is less than 1, so term always positive.
        if (s >= 0.001) { // test to avoid divide by zero, s is always positive due to sqrt
            Math3DUtils.normalizeVector(ret);
        } else {
            // if s close to zero then direction of axis not important
            //x = q1.x; // if it is important that axis is normalised then replace with x=1; y=z=0;
            //y = q1.y;
            //z = q1.z;
        }
        return ret;
    }

    public float getAngle() {
        return (float) (2 * Math.acos(w));
    }

    public Quaternion getInverse() {
        return new Quaternion(-this.x, -this.y, -this.z, this.w);
    }



}
