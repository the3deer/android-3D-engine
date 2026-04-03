package org.the3deer.engine.services.stl;

import android.opengl.GLES20;

import org.the3deer.engine.model.Object3D;
import org.the3deer.engine.model.Scene;
import org.the3deer.engine.services.LoadListener;
import org.the3deer.engine.services.LoaderTask;
import org.the3deer.engine.services.collada.entities.MeshData;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

/**
 * + STL loader supported by the org.j3d STL parser
 *
 * @author andresoviedo
 */
public final class STLLoaderTask extends LoaderTask {

    private STLFileReader stlFileReader;

    public STLLoaderTask(URI url, LoadListener callback) {
        super(url, callback);
    }

    @Override
    protected List<Object3D> build() throws IOException {

        // current facet counter
        int counter = 0;

        // scene
        final Scene scene = new Scene();


        try {

            // log event
            logger.info("Parsing model...");
            super.publishProgress("Parsing model...");

            // Parse STL
            this.stlFileReader = new STLFileReader(uri);

            // get total facets
            int totalFaces = stlFileReader.getNumOfFacets()[0];

            // log event
            logger.info("Num of objects found: " + stlFileReader.getNumOfObjects());
            logger.info("Num facets found '" + totalFaces + "' facets");
            logger.info("Parsing messages: " + stlFileReader.getParsingMessages());

            // primitive data
            final List<float[]> vertices = new ArrayList<>();
            final List<float[]> normals = new ArrayList<>();

            // Parse all facets...
            double[] normal = new double[3];
            double[][] triangle = new double[3][3];

            // notify user
            super.publishProgress("Loading facets...");

            // load data
            while (stlFileReader.getNextFacet(normal, triangle) && counter++ < totalFaces) {

                normals.add(new float[]{(float)normal[0], (float)normal[1], (float)normal[2]});
                normals.add(new float[]{(float)normal[0], (float)normal[1], (float)normal[2]});
                normals.add(new float[]{(float)normal[0], (float)normal[1], (float)normal[2]});

                vertices.add(new float[]{(float)triangle[0][0],(float)triangle[0][1],(float)triangle[0][2]});
                vertices.add(new float[]{(float)triangle[1][0],(float)triangle[1][1],(float)triangle[1][2]});
                vertices.add(new float[]{(float)triangle[2][0],(float)triangle[2][1],(float)triangle[2][2]});
            }

            // log event
            logger.info("Loaded model. Facets: " + counter + ", vertices:" +vertices.size()+", normals: "+normals.size());

            // build data
            final MeshData mesh = new MeshData.Builder().vertices(vertices).normals(normals).build();

            // fix missing or wrong normals
            super.publishProgress("Validating data...");
            mesh.fixNormals();

            // notify succeded!
            Object3D data = new Object3D(mesh.getVertexBuffer()).setVertexNormalsArrayBuffer(mesh.getNormalsBuffer());
            data.setMeshData(mesh);
            data.setIndexed(false);
            data.setDrawMode(GLES20.GL_TRIANGLES);
            data.setId(uri.toString());

            // super.publishProgress("Loading facets... "+counter+"/"+totalFaces);
            callback.onLoadObject(scene, data);

            callback.onLoadScene(scene);

            return Collections.singletonList(data);
        } catch (Exception e) {
            logger.log(Level.SEVERE,  "Face '" + counter + "'" + e.getMessage(), e);
            throw e;
        } finally {
            try {
                if (stlFileReader != null) {
                    stlFileReader.close();
                }
            } catch (IOException e) {
                throw e;
            }
        }
    }

    private static ByteBuffer createNativeByteBuffer(int length) {
        // initialize vertex byte buffer for shape coordinates
        ByteBuffer bb = ByteBuffer.allocateDirect(length);
        // use the device hardware's native byte order
        bb.order(ByteOrder.nativeOrder());
        return bb;
    }
}
