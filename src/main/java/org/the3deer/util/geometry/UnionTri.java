package org.the3deer.util.geometry;

import android.opengl.GLES20;
import android.opengl.Matrix;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.poly2tri.Poly2Tri;
import org.poly2tri.triangulation.TriangulationPoint;
import org.poly2tri.triangulation.delaunay.DelaunayTriangle;
import org.poly2tri.triangulation.point.TPoint;
import org.poly2tri.triangulation.sets.ConstrainedPointSet;
import org.the3deer.android_3d_model_engine.collision.CollisionDetection;
import org.the3deer.android_3d_model_engine.model.Constants;
import org.the3deer.android_3d_model_engine.model.Object3DData;
import org.the3deer.util.io.IOUtils;
import org.the3deer.util.math.Math3DUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class UnionTri {

    /**
     * Merge all game objects into 1 single mesh
     *
     * @param objects game objects
     * @return 1 single mesh
     */
    public static Object3DData merge(List<Object3DData> objects) {
        int newCapacity = 0;
        for (Object3DData obj : objects) {
            newCapacity += obj.getVertexBuffer().capacity();
        }
        FloatBuffer vb = IOUtils.createFloatBuffer(newCapacity);
        for (Object3DData obj : objects) {
            // every game object is positioned somewhere (modelmatrix)
            FloatBuffer vertexBuffer = obj.getVertexBuffer().asReadOnlyBuffer();
            vertexBuffer.position(0);
            for (int i = 0; i < vertexBuffer.capacity(); i++) {
                vb.put(vertexBuffer.get());
            }
        }
        return new Object3DData(vb).setDrawMode(GLES20.GL_TRIANGLES);
    }

    public static Object3DData triangulate(Object3DData object) {

        List<float[]> newBuffer = new ArrayList<>();


        FloatBuffer vertexBuffer = object.getVertexBuffer().asReadOnlyBuffer();
        FloatBuffer vertexBuffer2 = object.getVertexBuffer().asReadOnlyBuffer();
        vertexBuffer.position(0);
        for (int i = 0; i < vertexBuffer.capacity(); i += 9) {
            float[][] U = new float[3][3];
            U[0] = new float[]{vertexBuffer.get(), vertexBuffer.get(), vertexBuffer.get()};
            U[1] = new float[]{vertexBuffer.get(), vertexBuffer.get(), vertexBuffer.get()};
            U[2] = new float[]{vertexBuffer.get(), vertexBuffer.get(), vertexBuffer.get()};

            // triangulation
            List<TriangulationPoint> points = new ArrayList<>();
            points.add(new TPoint(U[0][0], U[0][1], U[0][2]));
            points.add(new TPoint(U[1][0], U[1][1], U[1][2]));
            points.add(new TPoint(U[2][0], U[2][1], U[2][2]));

            List<DelaunayConstraint> constraints = new ArrayList<>();
            vertexBuffer2.position(0);
            for (int j = 0; j < vertexBuffer2.capacity(); j += 9) {
                if (i == j) {
                    vertexBuffer2.position(vertexBuffer2.position() + 9);
                    continue;
                }
                float[][] V = new float[3][3];
                V[0] = new float[]{vertexBuffer2.get(), vertexBuffer2.get(), vertexBuffer2.get()};
                V[1] = new float[]{vertexBuffer2.get(), vertexBuffer2.get(), vertexBuffer2.get()};
                V[2] = new float[]{vertexBuffer2.get(), vertexBuffer2.get(), vertexBuffer2.get()};

                // TODO: check if 2 triangles intersect

                // TODO: check if 2 triangles are coplanar?

                // add constraints
                constraints.addAll(breakTriangle(U[0], U[1], U[2], V[0], V[1]));
                constraints.addAll(breakTriangle(U[0], U[1], U[2], V[1], V[2]));
                constraints.addAll(breakTriangle(U[0], U[1], U[2], V[2], V[0]));
            }

            if (constraints.isEmpty()) {
                newBuffer.add(U[0]);
                newBuffer.add(U[1]);
                newBuffer.add(U[2]);
            } else {

                // deduplicate
                List<DelaunayConstraint> filtered = new ArrayList<>();
                for (Iterator<DelaunayConstraint> it1 = constraints.iterator(); it1.hasNext(); ) {
                    DelaunayConstraint c1 = it1.next();
                    if (!filtered.contains(c1)) {
                        filtered.add(c1);
                    }
                }
                constraints = filtered;

                // generate indices
                final int[] indices = new int[constraints.size() * 2];
                for (int p = 0; p < constraints.size(); p++) {
                    float[] c1 = constraints.get(p).v1;
                    float[] c2 = constraints.get(p).v2;


                    int indexc1 = -1;
                    int indexc2 = -1;
                    for (int searchIdx = 0; searchIdx < points.size(); searchIdx++) {
                        TriangulationPoint triangulationPoint = points.get(searchIdx);
                        if (
                                triangulationPoint.getXf() == c1[0] &&
                                        triangulationPoint.getYf() == c1[1] &&
                                        triangulationPoint.getZf() == c1[2]
                        ) {
                            indexc1 = searchIdx;
                        }
                        if (
                                triangulationPoint.getXf() == c2[0] &&
                                        triangulationPoint.getYf() == c2[1] &&
                                        triangulationPoint.getZf() == c2[2]
                        ) {
                            indexc2 = searchIdx;
                        }
                    }

                    if (indexc1 == -1) {
                        indexc1 = points.size();
                        points.add(indexc1, new TPoint(
                                c1[0],
                                c1[1],
                                c1[2]));
                    }
                    if (indexc2 == -1) {
                        indexc2 = points.size();
                        points.add(indexc2, new TPoint(
                                c2[0],
                                c2[1],
                                c2[2]));
                    }

                    indices[p * 2] = indexc1;
                    indices[p * 2 + 1] = indexc2;
                }
                try {

                    // normal
                    final float[] UNormal = Math3DUtils.calculateNormal(U[0], U[1], U[2]);

                    // convert to 2D for sending to poly2tri
                    float[] rotation = Math3DUtils.getRotation(U[0], U[1], U[2], Constants.Z_NORMAL);
                    float[] rotationInv = new float[16];
                    Matrix.invertM(rotationInv, 0, rotation, 0);
                    points = fixOrientation(U, points);

                    // send to poly2tri
                    ConstrainedPointSet cps = new ConstrainedPointSet(points, indices);
                    Poly2Tri.triangulate(cps);
                    List<DelaunayTriangle> triangles = cps.getTriangles();

                    for (int t = 0; t < triangles.size(); t++) {
                        DelaunayTriangle triangle = triangles.get(t);
                        for (TriangulationPoint tp : triangle.points) {
                            float[] v1 = {tp.getXf(), tp.getYf(), tp.getZf(), 1};
                            float[] temp = new float[4];
                            Matrix.multiplyMV(temp, 0, rotationInv, 0, v1, 0);
                            newBuffer.add(new float[]{temp[0], temp[1], temp[2]});
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }


        FloatBuffer newVertexBuffer = IOUtils.createFloatBuffer(newBuffer.size() * 3);
        for (int i = 0; i < newBuffer.size(); i++) {
            newVertexBuffer.put(newBuffer.get(i));
        }
        return new Object3DData(newVertexBuffer).setDrawMode(GLES20.GL_TRIANGLES);
    }

    private static List<TriangulationPoint> fixOrientation(float[][] U, List<TriangulationPoint> points) {
        // fix orientation
        float[] matrix = Math3DUtils.getRotation(U[0], U[1], U[2], Constants.Z_NORMAL);
        if (matrix == Math3DUtils.IDENTITY_MATRIX) {
            return points;
        }

        List<TriangulationPoint> ret = new ArrayList<>();
        float[] temp = new float[4];
        for (int i = 0; i < points.size(); i++) {
            TriangulationPoint triangulationPoint = points.get(i);
            float[] v1 = new float[]{triangulationPoint.getXf(), triangulationPoint.getYf(), triangulationPoint.getZf(), 1};
            Matrix.multiplyMV(temp, 0, matrix, 0, v1, 0);
            //points.add(new TPoint(temp[0], temp[1], temp[2]));
            ret.add(new TPoint(temp[0], temp[1], temp[2]));
        }
        return ret;
    }

    private static List<DelaunayConstraint> breakTriangle(float[] v1, float[] v2, float[] v3, float[] p1, float[] p2) {


        if (Math3DUtils.lineEquals(v1, v2, p1, p2) || Math3DUtils.lineEquals(v2, v3, p1, p2) || Math3DUtils.lineEquals(v3,
                v1, p1, p2)) {
            // some segment coincide. don't break
            return Collections.emptyList();
        }

        float[] n1 = Math3DUtils.calculateNormal(v1, v2, v3);
        float[] p12 = Math3DUtils.substract(p2, p1);

        if (Math3DUtils.dot(n1, p12) != 0) {
            // line is not coplanar
            return Collections.emptyList();
        }

        float[] i1 = CollisionDetection.lineLineIntersection(v1, v2, p1, p2);
        float[] i2 = CollisionDetection.lineLineIntersection(v2, v3, p1, p2);
        float[] i3 = CollisionDetection.lineLineIntersection(v3, v1, p1, p2);

        boolean p1_in = CollisionDetection.pointInTriangle(p1, v1, v2, v3);
        boolean p2_in = CollisionDetection.pointInTriangle(p2, v1, v2, v3);

        // line completely inside
        if (i1 == null && i2 == null && i3 == null) {
            if (p1_in && p2_in) {
                final List<DelaunayConstraint> constraints = new ArrayList<>();
                constraints.add(new DelaunayConstraint(p1, p2));
                return constraints;
            } else {
                // line not breaking triangle
                return Collections.emptyList();
            }
        }

        // line completely on border
        if (Math3DUtils.equals(i1, i2) && Math3DUtils.equals(i1, i3)) {
            // line not breaking triangle
            return Collections.emptyList();
        }

        // 1 line intersection
        if (i2 == null && i3 == null) { // i1
            return constraints1(v1, v2, v3, p1, p2, i1);
        } else if (i1 == null && i3 == null) { // i2
            return constraints1(v1, v2, v3, p1, p2, i2);
        } else if (i1 == null && i2 == null) { // i3
            return constraints1(v1, v2, v3, p1, p2, i3);
        }

        // 2 line intersection
        if (i3 == null) { // i1 && i2
            return constraints2(v1, v2, v3, p1, p2, i1, i2);
        } else if (i2 == null) { // i1 && i3
            return constraints2(v1, v2, v3, p1, p2, i1, i3);
        } else if (i1 == null) { // i2 && i3
            return constraints2(v1, v2, v3, p1, p2, i2, i3);
        }

        // 3 line intersection
        final List<DelaunayConstraint> constraints = new ArrayList<>();
        if (Math3DUtils.equals(i1, i2)) {
            //return new float[][]{v1, v2, i3, v3, i3, v2};
            constraints.add(new DelaunayConstraint(i1, i3));
        } else if (Math3DUtils.equals(i2, i3)) {
            //return new float[][]{v1, i1, v3, v3, i1, v2};
            constraints.add(new DelaunayConstraint(i1, i2));
        } else if (Math3DUtils.equals(i3, i1)) {
            //return new float[][]{v1, v2, i2, v1, i2, v3};
            constraints.add(new DelaunayConstraint(i2, i3));
        }

        return constraints;
    }

    @NonNull
    private static List<DelaunayConstraint> constraints2(float[] v1, float[] v2, float[] v3, float[] p1, float[] p2, float[] i1, float[] i2) {

        boolean p1_in = CollisionDetection.pointInTriangle(p1, v1, v2, v3);
        boolean p2_in = CollisionDetection.pointInTriangle(p2, v1, v2, v3);

        float[] va;
        float[] vb;
        if (Math3DUtils.equals(i1, i2)) {
            if (p1_in && p2_in) {
                if (Math3DUtils.equals(p1, i1)) {
                    va = p2;
                    vb = i1;
                } else {
                    va = p1;
                    vb = i1;
                }
            } else if (p1_in) {
                if (Math3DUtils.equals(p1, i1)) {
                    // exterior intersection at point
                    return Collections.emptyList();
                } else {
                    va = p1;
                    vb = i1;
                }
            } else if (p2_in) {
                if (Math3DUtils.equals(p2, i1)) {
                    // exterior intersection at point
                    return Collections.emptyList();
                } else {
                    va = p2;
                    vb = i1;
                }
            } else {
                // not possible
                return Collections.emptyList();
            }
        } else {
            va = i1;
            vb = i2;
        }
        //return new float[][]{v2, i2, i1, v1, i1, v3, v3, i1, i2};
        final List<DelaunayConstraint> constraints = new ArrayList<>();
        constraints.add(new DelaunayConstraint(va, vb));
        return constraints;
    }

    @Nullable
    private static List<DelaunayConstraint> constraints1(float[] v1, float[] v2, float[] v3, float[] p1, float[] p2, float[] cut) {

        // 1 line intersection
        boolean p1_in = CollisionDetection.pointInTriangle(p1, v1, v2, v3);
        boolean p2_in = CollisionDetection.pointInTriangle(p2, v1, v2, v3);

        final List<DelaunayConstraint> constraints = new ArrayList<>();
        float[] va;
        float[] vb;
        if (!p1_in && p2_in && Math3DUtils.equals(cut, p2)) { // p1-p2| border
            return Collections.emptyList();
        } else if (!p1_in && p2_in && !Math3DUtils.equals(cut, p2)) { // p1-|-p2  penetration
            va = cut;
            vb = p2;
        } else if (p1_in && p2_in && Math3DUtils.equals(cut, p1)) { // | p1-p2 inside
            va = cut;
            vb = p2;
        } else if (p1_in && p2_in && Math3DUtils.equals(cut, p2)) { // | p1-p2 inside
            va = p1;
            vb = cut;
        } else if (p1_in && !p2_in && Math3DUtils.equals(cut, p1)) { // border |p1-p2 out
            return Collections.emptyList();
        } else if (p1_in && !p2_in && !Math3DUtils.equals(cut, p1)) { // border |p1-p2 out
            va = p1;
            vb = cut;
        } else {
            return Collections.emptyList();
        }

        //return new float[][]{v1, i1, sp, v1, sp, v3, v3, sp, v2, v2, sp, i1};
        constraints.add(new DelaunayConstraint(va, vb));
        return constraints;
    }
}
