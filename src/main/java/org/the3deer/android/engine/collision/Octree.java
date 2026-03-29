package org.the3deer.android.engine.collision;

import android.util.Log;

import org.the3deer.android.engine.model.BoundingBox;
import org.the3deer.android.engine.model.Object3D;
import org.the3deer.util.math.Math3DUtils;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Andres on 21/12/2017.
 */

public class Octree {

    // The minimum size of the 3D space for individual boxes
    public static final double BOX_SIZE = 10;

    final BoundingBox boundingBox;
    private final List<float[]> pending = new ArrayList<>();
    private final List<float[]> triangles = new ArrayList<>();
    private final Octree[] children = new Octree[8];

    private Octree(BoundingBox box){
        this.boundingBox = box;
    }

    private void addChild(int octant, BoundingBox boundingBox, float[] triangle){
        if (children[octant] == null){
            children[octant] = new Octree(boundingBox);
        }
        children[octant].pending.add(triangle);
    }

    public Octree[] getChildren(){
        return children;
    }

    public List<float[]> getTriangles(){
        return triangles;
    }

    private void subdivide(){
        Log.v("Octree", "Subdividing octree...");
        for (Octree child : children){
            if (child != null){
                subdivide(child);
            }
        }
    }

    static Octree build(Object3D object){
        Log.d("Octree", "Building octree for "+object.getId());
        final Octree ret = new Octree(object.getBoundingBox());
        final FloatBuffer buffer = object.getVertexBuffer().asReadOnlyBuffer();
        
        if (object.getIndexBuffer() == null) {
            // vertex array contains vertex in sequence
            final List<float[]> triangles = new ArrayList<>(buffer.capacity() / 3 * 4);
            //final float[] modelMatrix = object.getModelMatrix();
            //final float[] modelMatrix = Math3DUtils.IDENTITY_MATRIX;
            buffer.position(0);
            for (int i = 0; i <= buffer.capacity()-9; i += 9) {
                float[] triangle = new float[]{
                        buffer.get(), buffer.get(), buffer.get(), 1,
                        buffer.get(), buffer.get(), buffer.get(), 1,
                        buffer.get(), buffer.get(), buffer.get(), 1
                };
                triangles.add(triangle);
            }
            ret.pending.addAll(triangles);
        } else {
            // Indexed: use indices to jump to the correct XYZ coordinates
            final Buffer drawOrder = object.getIndexBuffer();
            for (int i = 0; i < drawOrder.capacity()-2; i += 3) {
                int idx1, idx2, idx3;
                if (drawOrder instanceof IntBuffer) {
                    idx1 = ((IntBuffer)drawOrder).get(i);
                    idx2 = ((IntBuffer)drawOrder).get(i + 1);
                    idx3 = ((IntBuffer)drawOrder).get(i + 2);
                } else {
                    idx1 = Short.toUnsignedInt(((ShortBuffer) drawOrder).get(i));
                    idx2 = Short.toUnsignedInt(((ShortBuffer) drawOrder).get(i+1));
                    idx3 = Short.toUnsignedInt(((ShortBuffer) drawOrder).get(i+2));
                }
                
                float[] triangle = new float[]{
                        buffer.get(idx1 * 3), buffer.get(idx1 * 3 + 1), buffer.get(idx1 * 3 + 2), 1,
                        buffer.get(idx2 * 3), buffer.get(idx2 * 3 + 1), buffer.get(idx2 * 3 + 2), 1,
                        buffer.get(idx3 * 3), buffer.get(idx3 * 3 + 1), buffer.get(idx3 * 3 + 2), 1,
                };
                ret.pending.add(triangle);
            }
        }
        subdivide(ret);
        return ret;
    }

    private static void subdivide(Octree octree){
        Log.d("Octree", "Subdividing octree ("+octree.boundingBox+"): "+octree.pending.size());
        float[] min = octree.boundingBox.getMin();
        float[] max = octree.boundingBox.getMax();
        float[] mid = Math3DUtils.divide(Math3DUtils.add(max,min),2);
        BoundingBox[] octant = new BoundingBox[8];
        float xMin,yMin,zMin,xMax,yMax,zMax;
        xMin = min[0]; yMin = min[1]; zMin = min[2];
        xMax = mid[0]; yMax = mid[1]; zMax = mid[2];
        octant[0] = new BoundingBox("octree0",xMin,xMax,yMin,yMax,zMin,zMax);
        xMin = mid[0]; yMin = min[1]; zMin = min[2];
        xMax = max[0]; yMax = mid[1]; zMax = mid[2];
        octant[1] = new BoundingBox("octree1",xMin,xMax,yMin,yMax,zMin,zMax);
        xMin = min[0]; yMin = mid[1]; zMin = min[2];
        xMax = mid[0]; yMax = max[1]; zMax = mid[2];
        octant[2] = new BoundingBox("octree2",xMin,xMax,yMin,yMax,zMin,zMax);
        xMin = mid[0]; yMin = mid[1]; zMin = min[2];
        xMax = max[0]; yMax = max[1]; zMax = mid[2];
        octant[3] = new BoundingBox("octree3",xMin,xMax,yMin,yMax,zMin,zMax);
        xMin = min[0]; yMin = min[1]; zMin = min[2];
        xMax = mid[0]; yMax = mid[1]; zMax = max[2];
        octant[4] = new BoundingBox("octree4",xMin,xMax,yMin,yMax,zMin,zMax);
        xMin = mid[0]; yMin = min[1]; zMin = min[2];
        xMax = max[0]; yMax = mid[1]; zMax = max[2];
        octant[5] = new BoundingBox("octree5",xMin,xMax,yMin,yMax,zMin,zMax);
        xMin = min[0]; yMin = mid[1]; zMin = min[2];
        xMax = mid[0]; yMax = max[1]; zMax = max[2];
        octant[6] = new BoundingBox("octree6",xMin,xMax,yMin,yMax,zMin,zMax);
        xMin = mid[0]; yMin = mid[1]; zMin = min[2];
        xMax = max[0]; yMax = max[1]; zMax = max[2];
        octant[7] = new BoundingBox("octree7",xMin,xMax,yMin,yMax,zMin,zMax);
        boolean anyInOctant = false;
        for (Iterator<float[]> it = octree.pending.iterator(); it.hasNext(); ) {
            float[] triangle = it.next();
            boolean inOctant = false;
            for (int i = 0; i < 8; i++) {
                int inside = octant[i].insideBounds(triangle[0], triangle[1], triangle[2]) ? 1 : 0;
                inside += octant[i].insideBounds(triangle[4], triangle[5], triangle[6]) ? 1 : 0;
                inside += octant[i].insideBounds(triangle[8], triangle[9], triangle[10]) ? 1 : 0;
                if (inside == 3) {
                    inOctant = true;
                    octree.addChild(i, octant[i], triangle);
                    anyInOctant = true;
                }
            }
            if (!inOctant){
                octree.triangles.add(triangle);
            }
            it.remove();
        }
        if (anyInOctant){
            if ((mid[0]+min[0])/2 > BOX_SIZE && (mid[1]+min[1])/2 > BOX_SIZE && (mid[2]+min[2])/2 > BOX_SIZE) {
                octree.subdivide();
            }
            else{
                for (Octree child : octree.children) {
                    if (child == null) continue;
                    child.triangles.addAll(child.pending);
                }
            }
        }
    }
}
