package org.the3deer.android.engine.services.stl;

import android.opengl.GLES20;

import org.the3deer.android.engine.model.Object3D;

import org.the3deer.android.engine.model.Scene;
import org.the3deer.android.engine.services.LoadListener;
import org.the3deer.android.engine.services.LoaderTask;
import org.the3deer.util.io.IOUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.FloatBuffer;
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
            final FloatBuffer vertices = IOUtils.createFloatBuffer(totalFaces * 3 * 3);
            final FloatBuffer normals = IOUtils.createFloatBuffer(totalFaces * 3 * 3);

            // Parse all facets...
            double[] normal = new double[3];
            double[][] triangle = new double[3][3];

            // notify user
            super.publishProgress("Loading facets...");

            // load data
            while (stlFileReader.getNextFacet(normal, triangle) && counter < totalFaces) {
                counter++;

                float nx = (float) normal[0];
                float ny = (float) normal[1];
                float nz = (float) normal[2];
                normals.put(nx).put(ny).put(nz);
                normals.put(nx).put(ny).put(nz);
                normals.put(nx).put(ny).put(nz);

                vertices.put((float) triangle[0][0]).put((float) triangle[0][1]).put((float) triangle[0][2]);
                vertices.put((float) triangle[1][0]).put((float) triangle[1][1]).put((float) triangle[1][2]);
                vertices.put((float) triangle[2][0]).put((float) triangle[2][1]).put((float) triangle[2][2]);
            }
            vertices.flip();
            normals.flip();

            // log event
            logger.info("Loaded model. Facets: " + counter + ", vertices:" + vertices.limit() / 3 + ", normals: " + normals.limit() / 3);

            // build data
            final STLMeshData mesh = new STLMeshData(vertices, normals);

            // fix missing or wrong normals
            super.publishProgress("Validating data...");
            mesh.fixNormals();
            mesh.smooth();

            // notify succeded!
            Object3D data = new Object3D(mesh.getVertexBuffer()).setVertexNormalsArrayBuffer(mesh.getNormalsBuffer());
            //data.setMeshData(mesh);
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
}
