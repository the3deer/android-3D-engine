package org.the3deer.opengl;

/**
 * Android Matrix replacement to be used in non-android environments.
 *
 * @author andresoviedo
 */
public class Matrix {

    /**
     * {@link android.opengl.Matrix#setRotateM(float[], int, float, float, float, float)}
     */
    public static void setRotateM(float[] rm, int rmOffset,
                                  float a, float x, float y, float z) {
        rm[rmOffset + 3] = 0;
        rm[rmOffset + 7] = 0;
        rm[rmOffset + 11] = 0;
        rm[rmOffset + 12] = 0;
        rm[rmOffset + 13] = 0;
        rm[rmOffset + 14] = 0;
        rm[rmOffset + 15] = 1;
        a *= (float) (Math.PI / 180.0f);
        float s = (float) Math.sin(a);
        float c = (float) Math.cos(a);
        if (1.0f == x && 0.0f == y && 0.0f == z) {
            rm[rmOffset + 5] = c;
            rm[rmOffset + 10] = c;
            rm[rmOffset + 6] = s;
            rm[rmOffset + 9] = -s;
            rm[rmOffset + 1] = 0;
            rm[rmOffset + 2] = 0;
            rm[rmOffset + 4] = 0;
            rm[rmOffset + 8] = 0;
            rm[rmOffset + 0] = 1;
        } else if (0.0f == x && 1.0f == y && 0.0f == z) {
            rm[rmOffset + 0] = c;
            rm[rmOffset + 10] = c;
            rm[rmOffset + 8] = s;
            rm[rmOffset + 2] = -s;
            rm[rmOffset + 1] = 0;
            rm[rmOffset + 4] = 0;
            rm[rmOffset + 6] = 0;
            rm[rmOffset + 9] = 0;
            rm[rmOffset + 5] = 1;
        } else if (0.0f == x && 0.0f == y && 1.0f == z) {
            rm[rmOffset + 0] = c;
            rm[rmOffset + 5] = c;
            rm[rmOffset + 1] = s;
            rm[rmOffset + 4] = -s;
            rm[rmOffset + 2] = 0;
            rm[rmOffset + 6] = 0;
            rm[rmOffset + 8] = 0;
            rm[rmOffset + 9] = 0;
            rm[rmOffset + 10] = 1;
        } else {
            float len = length(x, y, z);
            if (1.0f != len) {
                float recipLen = 1.0f / len;
                x *= recipLen;
                y *= recipLen;
                z *= recipLen;
            }
            float nc = 1.0f - c;
            float xy = x * y;
            float yz = y * z;
            float zx = z * x;
            float xs = x * s;
            float ys = y * s;
            float zs = z * s;
            rm[rmOffset + 0] = x * x * nc + c;
            rm[rmOffset + 4] = xy * nc - zs;
            rm[rmOffset + 8] = zx * nc + ys;
            rm[rmOffset + 1] = xy * nc + zs;
            rm[rmOffset + 5] = y * y * nc + c;
            rm[rmOffset + 9] = yz * nc - xs;
            rm[rmOffset + 2] = zx * nc - ys;
            rm[rmOffset + 6] = yz * nc + xs;
            rm[rmOffset + 10] = z * z * nc + c;
        }
    }

    /**
     * {@link android.opengl.Matrix#length(float, float, float)}
     */
    public static float length(float x, float y, float z) {
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    /**
     * {@link android.opengl.Matrix#setIdentityM(float[], int)}
     */
    public static void setIdentityM(float[] sm, int smOffset) {
        for (int i=0; i<16; i++) {
            sm[smOffset + i] = 0;
        }
        for (int i=0; i<16; i+=5) {
            sm[smOffset + i] = 1.0f;
        }
    }

    /**
     * {@link android.opengl.Matrix#multiplyMV(float[], int, float[], int, float[], int)}
     */
    public static void multiplyMV(float[] resultVec, int resultVecOffset,
                                  float[] lhsMat, int lhsMatOffset,
                                  float[] rhsVec, int rhsVecOffset) {
        float x = rhsVec[rhsVecOffset + 0];
        float y = rhsVec[rhsVecOffset + 1];
        float z = rhsVec[rhsVecOffset + 2];
        float w = rhsVec[rhsVecOffset + 3];
        resultVec[resultVecOffset + 0] = lhsMat[lhsMatOffset + 0] * x + lhsMat[lhsMatOffset + 4] * y + lhsMat[lhsMatOffset + 8] * z + lhsMat[lhsMatOffset + 12] * w;
        resultVec[resultVecOffset + 1] = lhsMat[lhsMatOffset + 1] * x + lhsMat[lhsMatOffset + 5] * y + lhsMat[lhsMatOffset + 9] * z + lhsMat[lhsMatOffset + 13] * w;
        resultVec[resultVecOffset + 2] = lhsMat[lhsMatOffset + 2] * x + lhsMat[lhsMatOffset + 6] * y + lhsMat[lhsMatOffset + 10] * z + lhsMat[lhsMatOffset + 14] * w;
        resultVec[resultVecOffset + 3] = lhsMat[lhsMatOffset + 3] * x + lhsMat[lhsMatOffset + 7] * y + lhsMat[lhsMatOffset + 11] * z + lhsMat[lhsMatOffset + 15] * w;
    }

    /**
     * {@link android.opengl.Matrix#translateM(float[], int, float, float, float)}
     */
    public static void translateM(float[] m, int mOffset,
                                  float x, float y, float z) {
        for (int i=0; i<4; i++) {
            int mi = mOffset + i;
            m[12 + mi] += m[mi] * x + m[4 + mi] * y + m[8 + mi] * z;
        }
    }

    /**
     * {@link android.opengl.Matrix#setLookAtM(float[], int, float, float, float, float, float, float, float, float, float)}
     */
    public static void setLookAtM(float[] rm, int rmOffset,
                                  float eyeX, float eyeY, float eyeZ,
                                  float centerX, float centerY, float centerZ,
                                  float upX, float upY, float upZ) {

        float fx = centerX - eyeX;
        float fy = centerY - eyeY;
        float fz = centerZ - eyeZ;

        // Normalize f
        float rlf = 1.0f / length(fx, fy, fz);
        fx *= rlf;
        fy *= rlf;
        fz *= rlf;

        // compute s = f x up
        float sx = fy * upZ - fz * upY;
        float sy = fz * upX - fx * upZ;
        float sz = fx * upY - fy * upX;

        // and normalize s
        float rls = 1.0f / length(sx, sy, sz);
        sx *= rls;
        sy *= rls;
        sz *= rls;

        // compute u = s x f
        float ux = sy * fz - sz * fy;
        float uy = sz * fx - sx * fz;
        float uz = sx * fy - sy * fx;

        rm[rmOffset + 0] = sx;
        rm[rmOffset + 1] = ux;
        rm[rmOffset + 2] = -fx;
        rm[rmOffset + 3] = 0.0f;

        rm[rmOffset + 4] = sy;
        rm[rmOffset + 5] = uy;
        rm[rmOffset + 6] = -fy;
        rm[rmOffset + 7] = 0.0f;

        rm[rmOffset + 8] = sz;
        rm[rmOffset + 9] = uz;
        rm[rmOffset + 10] = -fz;
        rm[rmOffset + 11] = 0.0f;

        rm[rmOffset + 12] = 0.0f;
        rm[rmOffset + 13] = 0.0f;
        rm[rmOffset + 14] = 0.0f;
        rm[rmOffset + 15] = 1.0f;

        translateM(rm, rmOffset, -eyeX, -eyeY, -eyeZ);
    }

    /**
     * {@link android.opengl.Matrix#invertM(float[], int, float[], int)}
     */
    public static boolean invertM(float[] mInv, int mInvOffset, float[] m,
                                  int mOffset) {
        // Inversion is done via companion matrix and determinant
        // (these expressions were generated by Mesa)
        float x00 = m[mOffset + 0];
        float x01 = m[mOffset + 1];
        float x02 = m[mOffset + 2];
        float x03 = m[mOffset + 3];
        float x04 = m[mOffset + 4];
        float x05 = m[mOffset + 5];
        float x06 = m[mOffset + 6];
        float x07 = m[mOffset + 7];
        float x08 = m[mOffset + 8];
        float x09 = m[mOffset + 9];
        float x10 = m[mOffset + 10];
        float x11 = m[mOffset + 11];
        float x12 = m[mOffset + 12];
        float x13 = m[mOffset + 13];
        float x14 = m[mOffset + 14];
        float x15 = m[mOffset + 15];

        float a0 = x00 * x05 - x01 * x04;
        float a1 = x00 * x06 - x02 * x04;
        float a2 = x00 * x07 - x03 * x04;
        float a3 = x01 * x06 - x02 * x05;
        float a4 = x01 * x07 - x03 * x05;
        float a5 = x02 * x07 - x03 * x06;
        float b0 = x08 * x13 - x09 * x12;
        float b1 = x08 * x14 - x10 * x12;
        float b2 = x08 * x15 - x11 * x12;
        float b3 = x09 * x14 - x10 * x13;
        float b4 = x09 * x15 - x11 * x13;
        float b5 = x10 * x15 - x11 * x14;

        float det = a0 * b5 - a1 * b4 + a2 * b3 + a3 * b2 - a4 * b1 + a5 * b0;
        if (det == 0.0f) {
            return false;
        }

        float invDet = 1.0f / det;
        mInv[mInvOffset + 0] = (x05 * b5 - x06 * b4 + x07 * b3) * invDet;
        mInv[mInvOffset + 1] = (-x01 * b5 + x02 * b4 - x03 * b3) * invDet;
        mInv[mInvOffset + 2] = (x13 * a5 - x14 * a4 + x15 * a3) * invDet;
        mInv[mInvOffset + 3] = (-x09 * a5 + x10 * a4 - x11 * a3) * invDet;
        mInv[mInvOffset + 4] = (-x04 * b5 + x06 * b2 - x07 * b1) * invDet;
        mInv[mInvOffset + 5] = (x00 * b5 - x02 * b2 + x03 * b1) * invDet;
        mInv[mInvOffset + 6] = (-x12 * a5 + x14 * a2 - x15 * a1) * invDet;
        mInv[mInvOffset + 7] = (x08 * a5 - x10 * a2 + x11 * a1) * invDet;
        mInv[mInvOffset + 8] = (x04 * b4 - x05 * b2 + x07 * b0) * invDet;
        mInv[mInvOffset + 9] = (-x00 * b4 + x01 * b2 - x03 * b0) * invDet;
        mInv[mInvOffset + 10] = (x12 * a4 - x13 * a2 + x15 * a0) * invDet;
        mInv[mInvOffset + 11] = (-x08 * a4 + x09 * a2 - x11 * a0) * invDet;
        mInv[mInvOffset + 12] = (-x04 * b3 + x05 * b1 - x06 * b0) * invDet;
        mInv[mInvOffset + 13] = (x00 * b3 - x01 * b1 + x02 * b0) * invDet;
        mInv[mInvOffset + 14] = (-x12 * a3 + x13 * a1 - x14 * a0) * invDet;
        mInv[mInvOffset + 15] = (x08 * a3 - x09 * a1 + x10 * a0) * invDet;

        return true;
    }

    /**
     * {@link android.opengl.Matrix#frustumM(float[], int, float, float, float, float, float, float)}
     */
    public static void frustumM(float[] m, int offset, float left, float right, float bottom, float top, float near, float far) {
        if (left == right) {
            throw new IllegalArgumentException("left == right");
        }
        if (top == bottom) {
            throw new IllegalArgumentException("top == bottom");
        }
        if (near == far) {
            throw new IllegalArgumentException("near == far");
        }
        if (near <= 0.0f) {
            throw new IllegalArgumentException("near <= 0.0f");
        }
        if (far <= 0.0f) {
            throw new IllegalArgumentException("far <= 0.0f");
        }
        final float r_width  = 1.0f / (right - left);
        final float r_height = 1.0f / (top - bottom);
        final float r_depth  = 1.0f / (near - far);
        final float x = 2.0f * (near * r_width);
        final float y = 2.0f * (near * r_height);
        final float A = (right + left) * r_width;
        final float B = (top + bottom) * r_height;
        final float C = (far + near) * r_depth;
        final float D = 2.0f * (far * near * r_depth);
        m[offset + 0] = x;
        m[offset + 5] = y;
        m[offset + 8] = A;
        m[offset +  9] = B;
        m[offset + 10] = C;
        m[offset + 14] = D;
        m[offset + 11] = -1.0f;
        m[offset +  1] = 0.0f;
        m[offset +  2] = 0.0f;
        m[offset +  3] = 0.0f;
        m[offset +  4] = 0.0f;
        m[offset +  6] = 0.0f;
        m[offset +  7] = 0.0f;
        m[offset + 12] = 0.0f;
        m[offset + 13] = 0.0f;
        m[offset + 15] = 0.0f;
    }

    /**
     * {@link android.opengl.Matrix#perspectiveM(float[], int, float, float, float, float)}
     */
    public static void perspectiveM(float[] m, int offset, float fovy, float aspect, float zNear, float zFar) {
        float f = 1.0f / (float) Math.tan(fovy * (Math.PI / 360.0));
        float rangeReciprocal = 1.0f / (zNear - zFar);

        m[offset + 0] = f / aspect;
        m[offset + 1] = 0.0f;
        m[offset + 2] = 0.0f;
        m[offset + 3] = 0.0f;

        m[offset + 4] = 0.0f;
        m[offset + 5] = f;
        m[offset + 6] = 0.0f;
        m[offset + 7] = 0.0f;

        m[offset + 8] = 0.0f;
        m[offset + 9] = 0.0f;
        m[offset + 10] = (zFar + zNear) * rangeReciprocal;
        m[offset + 11] = -1.0f;

        m[offset + 12] = 0.0f;
        m[offset + 13] = 0.0f;
        m[offset + 14] = 2.0f * zFar * zNear * rangeReciprocal;
        m[offset + 15] = 0.0f;
    }

    /**
     * {@link android.opengl.Matrix#multiplyMM(float[], int, float[], int, float[], int)}
     */
    public static void multiplyMM(float[] dest, int destOffset, float[] lhs, int lhsOffset, float[] rhs, int rhsOffset)
    {
        // first column
        dest[destOffset + 0] = lhs[lhsOffset + 0] * rhs[rhsOffset + 0] + lhs[lhsOffset + 4] * rhs[rhsOffset + 1] + lhs[lhsOffset + 8] * rhs[rhsOffset + 2] + lhs[lhsOffset + 12] * rhs[rhsOffset + 3];
        dest[destOffset + 1] = lhs[lhsOffset + 1] * rhs[rhsOffset + 0] + lhs[lhsOffset + 5] * rhs[rhsOffset + 1] + lhs[lhsOffset + 9] * rhs[rhsOffset + 2] + lhs[lhsOffset + 13] * rhs[rhsOffset + 3];
        dest[destOffset + 2] = lhs[lhsOffset + 2] * rhs[rhsOffset + 0] + lhs[lhsOffset + 6] * rhs[rhsOffset + 1] + lhs[lhsOffset + 10] * rhs[rhsOffset + 2] + lhs[lhsOffset + 14] * rhs[rhsOffset + 3];
        dest[destOffset + 3] = lhs[lhsOffset + 3] * rhs[rhsOffset + 0] + lhs[lhsOffset + 7] * rhs[rhsOffset + 1] + lhs[lhsOffset + 11] * rhs[rhsOffset + 2] + lhs[lhsOffset + 15] * rhs[rhsOffset + 3];

        // second column
        dest[destOffset + 4] = lhs[lhsOffset + 0] * rhs[rhsOffset + 4] + lhs[lhsOffset + 4] * rhs[rhsOffset + 5] + lhs[lhsOffset + 8] * rhs[rhsOffset + 6] + lhs[lhsOffset + 12] * rhs[rhsOffset + 7];
        dest[destOffset + 5] = lhs[lhsOffset + 1] * rhs[rhsOffset + 4] + lhs[lhsOffset + 5] * rhs[rhsOffset + 5] + lhs[lhsOffset + 9] * rhs[rhsOffset + 6] + lhs[lhsOffset + 13] * rhs[rhsOffset + 7];
        dest[destOffset + 6] = lhs[lhsOffset + 2] * rhs[rhsOffset + 4] + lhs[lhsOffset + 6] * rhs[rhsOffset + 5] + lhs[lhsOffset + 10] * rhs[rhsOffset + 6] + lhs[lhsOffset + 14] * rhs[rhsOffset + 7];
        dest[destOffset + 7] = lhs[lhsOffset + 3] * rhs[rhsOffset + 4] + lhs[lhsOffset + 7] * rhs[rhsOffset + 5] + lhs[lhsOffset + 11] * rhs[rhsOffset + 6] + lhs[lhsOffset + 15] * rhs[rhsOffset + 7];

        // third column
        dest[destOffset + 8] = lhs[lhsOffset + 0] * rhs[rhsOffset + 8] + lhs[lhsOffset + 4] * rhs[rhsOffset + 9] + lhs[lhsOffset + 8] * rhs[rhsOffset + 10] + lhs[lhsOffset + 12] * rhs[rhsOffset + 11];
        dest[destOffset + 9] = lhs[lhsOffset + 1] * rhs[rhsOffset + 8] + lhs[lhsOffset + 5] * rhs[rhsOffset + 9] + lhs[lhsOffset + 9] * rhs[rhsOffset + 10] + lhs[lhsOffset + 13] * rhs[rhsOffset + 11];
        dest[destOffset + 10] = lhs[lhsOffset + 2] * rhs[rhsOffset + 8] + lhs[lhsOffset + 6] * rhs[rhsOffset + 9] + lhs[lhsOffset + 10] * rhs[rhsOffset + 10] + lhs[lhsOffset + 14] * rhs[rhsOffset + 11];
        dest[destOffset + 11] = lhs[lhsOffset + 3] * rhs[rhsOffset + 8] + lhs[lhsOffset + 7] * rhs[rhsOffset + 9] + lhs[lhsOffset + 11] * rhs[rhsOffset + 10] + lhs[lhsOffset + 15] * rhs[rhsOffset + 11];

        // forth column
        dest[destOffset + 12] = lhs[lhsOffset + 0] * rhs[rhsOffset + 12] + lhs[lhsOffset + 4] * rhs[rhsOffset + 13] + lhs[lhsOffset + 8] * rhs[rhsOffset + 14] + lhs[lhsOffset + 12] * rhs[rhsOffset + 15];
        dest[destOffset + 13] = lhs[lhsOffset + 1] * rhs[rhsOffset + 12] + lhs[lhsOffset + 5] * rhs[rhsOffset + 13] + lhs[lhsOffset + 9] * rhs[rhsOffset + 14] + lhs[lhsOffset + 13] * rhs[rhsOffset + 15];
        dest[destOffset + 14] = lhs[lhsOffset + 2] * rhs[rhsOffset + 12] + lhs[lhsOffset + 6] * rhs[rhsOffset + 13] + lhs[lhsOffset + 10] * rhs[rhsOffset + 14] + lhs[lhsOffset + 14] * rhs[rhsOffset + 15];
        dest[destOffset + 15] = lhs[lhsOffset + 3] * rhs[rhsOffset + 12] + lhs[lhsOffset + 7] * rhs[rhsOffset + 13] + lhs[lhsOffset + 11] * rhs[rhsOffset + 14] + lhs[lhsOffset + 15] * rhs[rhsOffset + 15];
    };

    /**
     * {@link android.opengl.Matrix#setRotateEulerM(float[], int, float, float, float)}
     */
    public static void setRotateEulerM(float[] rm, int rmOffset,
                                       float x, float y, float z) {
        x *= (float) (Math.PI / 180.0f);
        y *= (float) (Math.PI / 180.0f);
        z *= (float) (Math.PI / 180.0f);
        float cx = (float) Math.cos(x);
        float sx = (float) Math.sin(x);
        float cy = (float) Math.cos(y);
        float sy = (float) Math.sin(y);
        float cz = (float) Math.cos(z);
        float sz = (float) Math.sin(z);
        float cxcz = cx * cz;
        float cxsz = cx * sz;
        float sxcz = sx * cz;
        float sxsz = sx * sz;

        rm[rmOffset + 0]  = cy * cz;
        rm[rmOffset + 1]  = cy * sz;
        rm[rmOffset + 2]  = -sy;
        rm[rmOffset + 3]  = 0.0f;

        rm[rmOffset + 4]  = sxcz * sy - cxsz;
        rm[rmOffset + 5]  = sxsz * sy + cxcz;
        rm[rmOffset + 6]  = sx * cy;
        rm[rmOffset + 7]  = 0.0f;

        rm[rmOffset + 8]  = cxcz * sy + sxsz;
        rm[rmOffset + 9]  = cxsz * sy - sxcz;
        rm[rmOffset + 10] = cx * cy;
        rm[rmOffset + 11] = 0.0f;

        rm[rmOffset + 12] = 0.0f;
        rm[rmOffset + 13] = 0.0f;
        rm[rmOffset + 14] = 0.0f;
        rm[rmOffset + 15] = 1.0f;
    }

    /**
     * {@link android.opengl.Matrix#rotateM(float[], int, float, float, float, float)}
     */
    public static void rotateM(float[] m, int mOffset,
                               float a, float x, float y, float z) {
        float[] temp = new float[32];
        setRotateM(temp, 0, a, x, y, z);
        multiplyMM(temp, 16, m, mOffset, temp, 0);
        System.arraycopy(temp, 16, m, mOffset, 16);
    }

    /**
     * {@link android.opengl.Matrix#scaleM(float[], int, float, float, float)}
     */
    public static void scaleM(float[] m, int mOffset,
                              float x, float y, float z) {
        for (int i=0; i<4; i++) {
            int mi = mOffset + i;
            m[mi] *= x;
            m[4 + mi] *= y;
            m[8 + mi] *= z;
        }
    }

    /**
     * {@link android.opengl.Matrix#transposeM(float[], int, float[], int)}
     */
    public static void transposeM(float[] mTrans, int mTransOffset, float[] m,
                                  int mOffset) {
        for (int i = 0; i < 4; i++) {
            int mBase = i * 4 + mOffset;
            mTrans[i + mTransOffset] = m[mBase];
            mTrans[i + 4 + mTransOffset] = m[mBase + 1];
            mTrans[i + 8 + mTransOffset] = m[mBase + 2];
            mTrans[i + 12 + mTransOffset] = m[mBase + 3];
        }
    }

    /**
     * {@link android.opengl.Matrix#orthoM(float[], int, float, float, float, float, float, float)}
     */
    public static void orthoM(float[] m, int offset,
                              float left, float right, float bottom, float top,
                              float near, float far) {
        if (left == right) {
            throw new IllegalArgumentException("left == right");
        }
        if (bottom == top) {
            throw new IllegalArgumentException("bottom == top");
        }
        if (near == far) {
            throw new IllegalArgumentException("near == far");
        }

        float r_width  = 1.0f / (right - left);
        float r_height = 1.0f / (top - bottom);
        float r_depth  = 1.0f / (far - near);
        float x =  2.0f * r_width;
        float y =  2.0f * r_height;
        float z = -2.0f * r_depth;
        float tx = -(right + left) * r_width;
        float ty = -(top + bottom) * r_height;
        float tz = -(far + near) * r_depth;
        m[offset + 0] = x;
        m[offset + 5] = y;
        m[offset + 10] = z;
        m[offset + 12] = tx;
        m[offset + 13] = ty;
        m[offset + 14] = tz;
        m[offset + 15] = 1.0f;
        m[offset + 1] = 0.0f;
        m[offset + 2] = 0.0f;
        m[offset + 3] = 0.0f;
        m[offset + 4] = 0.0f;
        m[offset + 6] = 0.0f;
        m[offset + 7] = 0.0f;
        m[offset + 8] = 0.0f;
        m[offset + 9] = 0.0f;
        m[offset + 11] = 0.0f;
    }
}
