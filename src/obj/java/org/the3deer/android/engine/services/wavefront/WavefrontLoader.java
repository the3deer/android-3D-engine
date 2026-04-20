package org.the3deer.android.engine.services.wavefront;

import android.opengl.GLES20;

import androidx.annotation.Nullable;

import org.the3deer.android.engine.model.Element;
import org.the3deer.android.engine.model.Material;
import org.the3deer.android.engine.model.Materials;
import org.the3deer.android.engine.model.Object3D;
import org.the3deer.android.engine.model.Scene;
import org.the3deer.android.engine.services.LoadListener;
import org.the3deer.util.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>High-Performance wavefront parser</p>
 *
 * @author Andres, Gemini (AI)
 */
public class WavefrontLoader {

    private static final Logger logger = Logger.getLogger(WavefrontLoader.class.getSimpleName());

    private final int triangulationMode;
    private final LoadListener callback;

    public WavefrontLoader(int triangulationMode, LoadListener callback) {
        this.triangulationMode = triangulationMode;
        this.callback = callback;
    }

    @Nullable
    public static String getMaterialLib(URI url) {
        return getParameter(url, "mtllib ");
    }

    @Nullable
    public static String getTextureFile(URI url) {
        return getParameter(url, "map_Kd ");
    }

    @Nullable
    private static String getParameter(URI url, String parameter) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(url.toURL().openStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith(parameter)) {
                    return line.substring(parameter.length()).trim();
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE,  "Problem reading file '" + url + "': " + e.getMessage(), e);
            throw new RuntimeException(e);
        }
        return null;
    }

    public List<Object3D> load(URI modelURI) throws IOException {
        try {

            // log event
            logger.info("Loading model... " + modelURI);

            // pre-scan model to count elements and optimize memory
            final ModelConfig config = preScan(modelURI);
            logger.info("Pre-scan finished: " + config);

            // log event
            logger.config("Parsing geometries... ");

            // open stream, parse model, then close stream
            final InputStream is = modelURI.toURL().openStream();
            final List<WavefrontMeshData> meshes = loadModel(modelURI.toString(), is, config);
            is.close();

            // 3D meshes
            final List<Object3D> ret = new ArrayList<>();

            // log event
            logger.config("Processing geometries... ");

            // notify listener
            callback.onProgress("Processing geometries...");

            // scene
            final Scene scene = new Scene("default");

            // process all meshes
            for (int i=0; i< meshes.size(); i++) {

                // get each mesh
                final WavefrontMeshData meshData = meshes.get(i);

                // notify listener
                callback.onProgress("Processing normals...");

                // fix missing or wrong normals
                meshData.fixNormals();

                // check we didn't brake normals
                meshData.validate();

                // create 3D object
                Object3D data3D = new Object3D(meshData.getId(), meshData.getVertexBuffer());
                data3D.setUri(modelURI);
                data3D.setName(meshData.getName());
                data3D.setVertexNormalsArrayBuffer(meshData.getNormalsBuffer());
                data3D.setTextureCoordsArrayBuffer(meshData.getTextureBuffer());
                data3D.setElements(meshData.getElements());
                data3D.setIndexed(true);
                data3D.setDrawMode(GLES20.GL_TRIANGLES);

                // Clear temporary data to free memory
                meshData.clearTemporaryData();

                // add model to scene
                callback.onLoadObject(scene, data3D);

                // notify listener
                callback.onProgress("Loading materials...");

                // load colors and textures
                loadMaterials(modelURI, meshData);

                callback.onLoadObject(scene, data3D);

                // add model to scene
                ret.add(data3D);
            }

            // log event
            logger.info("Finished loading. Geometries: " + ret.size());

            callback.onLoadScene(scene);

            return ret;
        } catch (Exception ex) {
            logger.log(Level.SEVERE,  ex.getMessage(), ex);
            callback.onLoadError(ex);
            throw ex;
        }
    }

    private void loadMaterials(URI modelURI, WavefrontMeshData meshData) {

        // process materials
        if (meshData.getMaterialFile() == null) return;

        // log event
        logger.config("Parsing materials... ");

        try {

            // get materials stream
            final URL materialUrl = modelURI.resolve(meshData.getMaterialFile()).toURL();

            final InputStream inputStream = materialUrl.openStream();

            // parse materials
            final WavefrontMaterialsParser materialsParser = new WavefrontMaterialsParser();
            final Materials materials = materialsParser.parse(meshData.getMaterialFile(), inputStream);

            // check if there is any material
            if (materials.size() > 0) {

                // bind materials
                for (int e = 0; e < meshData.getElements().size(); e++) {

                    // get element
                    final Element element = meshData.getElements().get(e);

                    // log event
                    logger.config("Processing element... " + element.getId());

                    // get material id
                    final String elementMaterialId = element.getMaterialId();

                    // check if element has material
                    if (elementMaterialId != null && materials.contains(elementMaterialId)) {

                        // get material for element
                        final Material elementMaterial = materials.get(elementMaterialId);

                        // bind material
                        element.setMaterial(elementMaterial);

                        // check if element has texture mapped
                        if (elementMaterial != null && elementMaterial.getColorTexture() != null
                            && elementMaterial.getColorTexture().getFile() != null) {

                            // log event
                            logger.info("Reading texture file... " + elementMaterial.getColorTexture().getFile());

                            // build color url
                            final URL diffuseUrl = modelURI.resolve(elementMaterial.getColorTexture().getFile()).toURL();

                            // read texture data
                            try (InputStream stream = diffuseUrl.openStream()) {

                                // read data
                                elementMaterial.getColorTexture().setData(IOUtils.read(stream));

                                // log event
                                logger.config("Texture linked... " + elementMaterial.getColorTexture().getFile());

                            } catch (Exception ex) {
                                logger.log(Level.SEVERE,  String.format("Error reading texture file: %s", ex.getMessage()));
                            }
                        }
                    }
                }
            }
        } catch (IOException ex) {
            logger.log(Level.SEVERE,  "Error loading materials... file: " + meshData.getMaterialFile()+", error: "+ex.getMessage());
        }
    }

    private List<WavefrontMeshData> loadModel(String uri, InputStream is, ModelConfig config) {

        // log event
        logger.info("Loading model... " + uri);

        // parse Uri
        final URI modelURI = URI.create(uri);

        // String fnm = MODEL_DIR + modelNm + ".obj";
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(is));

            // debug model purposes
            int lineNum = 0;
            String line = null;

            // primitive data pools (pre-allocated)
            final FloatBuffer vertexList = IOUtils.createFloatBuffer(config.vertexCount * 3);
            final FloatBuffer normalsList = IOUtils.createFloatBuffer(config.normalCount * 3);
            final FloatBuffer textureList = IOUtils.createFloatBuffer(config.textureCount * 2);

            // mesh data
            final List<WavefrontMeshData> meshes = new ArrayList<>();

            // We use a flat IntBuffer for vertex attributes: each vertex is represented by 3 ints: v, vt, vn
            final IntBuffer verticesAttributes = IOUtils.createIntBuffer(config.faceCount * 3);

            // material file
            String mtllib = null;

            // smoothing groups (keeping as Integer indices into verticesAttributes)
            final Map<String, List<Integer>> smoothingGroups = new HashMap<>();
            List<Integer> currentSmoothingList = null;

            // mesh current
            WavefrontMeshData.Builder meshCurrent = new WavefrontMeshData.Builder().id(uri.toString());
            Element.Builder elementCurrent = new Element.Builder().id("default");
            IntBuffer indicesCurrent = IOUtils.createIntBuffer(config.faceCount);
            int meshAttributesStart = 0;
            //String mtllib = null;
            boolean buildNewMesh = false;
            boolean buildNewElement = false;

            try {
                while (((line = br.readLine()) != null)) {
                    lineNum++;
                    line = line.trim();
                    if (line.length() == 0) continue;
                    if (line.startsWith("v ")) { // vertex
                        parseVector(vertexList, line.substring(2).trim());
                    } else if (line.startsWith("vn")) { // normal
                        parseVector(normalsList, line.substring(3).trim());
                    } else if (line.startsWith("vt")) { // tex coord
                        parseVariableVector(textureList, line.substring(3).trim());
                    } else if (line.charAt(0) == 'o') { // object group
                        if (indicesCurrent.position() > 0) {
                            // build current mesh
                            elementCurrent.indices(indicesCurrent.flip());
                            meshCurrent.addElement(elementCurrent.build());

                            IntBuffer attributesSlice = verticesAttributes.duplicate();
                            attributesSlice.position(meshAttributesStart);
                            attributesSlice.limit(verticesAttributes.position());
                            final WavefrontMeshData meshData = meshCurrent.vertices(vertexList).normals(normalsList).textures(textureList)
                                    .vertexAttributes(attributesSlice.slice()).materialFile(mtllib)
                                    .smoothingGroups(smoothingGroups).build();
                            meshes.add(meshData);

                            // start new mesh
                            meshCurrent = new WavefrontMeshData.Builder().id(line.substring(1).trim());
                            elementCurrent = new Element.Builder().id("default");
                            indicesCurrent = IOUtils.createIntBuffer(config.faceCount - (verticesAttributes.position() / 3));
                            meshAttributesStart = verticesAttributes.position();
                        } else {
                            meshCurrent.id(line.substring(1).trim());
                            buildNewMesh = true;
                        }
                    } else if (line.charAt(0) == 'g') { // group name
                        if (buildNewElement && indicesCurrent.position() > 0) {

                            // add current element
                            elementCurrent.indices(indicesCurrent.flip());
                            meshCurrent.addElement(elementCurrent.build());

                            // log event
                            logger.config("New element. indices: " + indicesCurrent.limit());

                            // prepare next element
                            indicesCurrent = IOUtils.createIntBuffer(config.faceCount - (verticesAttributes.position() / 3));
                            elementCurrent = new Element.Builder().id(line.substring(1).trim());
                        } else {
                            elementCurrent.id(line.substring(1).trim());
                            buildNewElement = true;
                        }
                    } else if (line.startsWith("f ")) { // face
                        parseFace(verticesAttributes, meshAttributesStart, indicesCurrent, vertexList, normalsList, textureList, line.substring(2).trim(), currentSmoothingList);
                    } else if (line.startsWith("mtllib ")) {// build material
                        try {
                            mtllib = modelURI.resolve(line.substring(7)).toString();
                        } catch (Exception e) {
                            logger.log(Level.SEVERE,  "Error reading line: " + lineNum + " : " + line+", message: "+e.getMessage());
                        }
                    } else if (line.startsWith("usemtl ")) {// use material
                        if (indicesCurrent.position() > 0) {

                            // change element since we are dealing with different material
                            elementCurrent.indices(indicesCurrent.flip());
                            meshCurrent.addElement(elementCurrent.build());

                            // log event
                            logger.finest("New material: " + line);

                            // prepare next element
                            indicesCurrent = IOUtils.createIntBuffer(config.faceCount - (verticesAttributes.position() / 3));
                            elementCurrent = new Element.Builder().id(elementCurrent.getId());
                        }

                        elementCurrent.materialId(line.substring(7));
                    } else if (line.charAt(0) == 's') { // smoothing group
                        final String smoothingGroupId = line.substring(1).trim();
                        if ("0".equals(smoothingGroupId) || "off".equals(smoothingGroupId)){
                            currentSmoothingList = null;
                        } else {
                            currentSmoothingList = new ArrayList<>();
                            smoothingGroups.put(smoothingGroupId, currentSmoothingList);
                        }
                    } else if (line.charAt(0) == '#') { // comment line
                        logger.finest(line);
                    } else
                        logger.warning("Ignoring line " + lineNum + " : " + line);

                }

                // build mesh
                if (indicesCurrent.position() > 0) {
                    elementCurrent.indices(indicesCurrent.flip());
                    meshCurrent.addElement(elementCurrent.build());
                }

                IntBuffer attributesSlice = verticesAttributes.duplicate();
                attributesSlice.position(meshAttributesStart);
                attributesSlice.limit(verticesAttributes.position());
                final WavefrontMeshData meshData = meshCurrent.vertices(vertexList).normals(normalsList).textures(textureList)
                        .vertexAttributes(attributesSlice.slice()).materialFile(mtllib)
                        .smoothingGroups(smoothingGroups).build();

                logger.config("Loaded mesh. id:" + meshData.getId() + ", indices: " + indicesCurrent.limit()
                        + ", vertices:" + vertexList.capacity()
                        + ", normals: " + normalsList.capacity()
                        + ", textures:" + textureList.capacity());

                // add mesh
                meshes.add(meshData);

                // return all meshes
                return meshes;

            } catch (Exception e) {
                logger.log(Level.SEVERE,  "Error reading line: " + lineNum + ":" + line, e);
                logger.log(Level.SEVERE,  e.getMessage(), e);
                throw new RuntimeException(e);
            }
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    logger.log(Level.SEVERE,  e.getMessage(), e);
                }
            }
        }
    }

    private static class ModelConfig {
        int vertexCount = 0;
        int normalCount = 0;
        int textureCount = 0;
        int faceCount = 0;

        @Override
        public String toString() {
            return "ModelConfig{" +
                    "vertices=" + vertexCount +
                    ", normals=" + normalCount +
                    ", textures=" + textureCount +
                    ", faceCorners=" + faceCount +
                    '}';
        }
    }

    private ModelConfig preScan(URI uri) {
        final ModelConfig config = new ModelConfig();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(uri.toURL().openStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("v ")) {
                    config.vertexCount++;
                } else if (line.startsWith("vn")) {
                    config.normalCount++;
                } else if (line.startsWith("vt")) {
                    config.textureCount++;
                } else if (line.startsWith("f ")) {
                    final StringTokenizer st = new StringTokenizer(line, " ");
                    final int tokens = st.countTokens() - 1;
                    if (tokens >= 3) {
                        config.faceCount += (tokens - 2) * 3;
                    }
                }
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error during pre-scan: " + e.getMessage(), e);
        }
        return config;
    }

    /**
     * List of texture coordinates, in (u, [,v ,w]) coordinates, these will vary between 0 and 1. v, w are optional and default to 0.
     */
    private void parseVector(final FloatBuffer vectorBuffer, final String line) {
        try {
            final StringTokenizer st = new StringTokenizer(line, " ");
            int i = 0;
            while (st.hasMoreTokens() && i < 3) {
                vectorBuffer.put(Float.parseFloat(st.nextToken()));
                i++;
            }
            // Fill remaining if less than 3 tokens
            while (i < 3) {
                vectorBuffer.put(0);
                i++;
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error parsing vector '" + line + "': " + ex.getMessage());
            vectorBuffer.put(0).put(0).put(0);
        }
    }

    /**
     * List of texture coordinates, in (u, [,v ,w]) coordinates, these will vary between 0 and 1. v, w are optional and default to 0.
     */
    private void parseVariableVector(final FloatBuffer textureBuffer, final String line) {
        try {
            final StringTokenizer st = new StringTokenizer(line, " ");
            int i = 0;
            if (st.hasMoreTokens()) {
                textureBuffer.put(Float.parseFloat(st.nextToken()));
                i++;
            }
            if (st.hasMoreTokens()) {
                textureBuffer.put(Float.parseFloat(st.nextToken()));
                i++;
            }
            // Fill remaining if less than 2 tokens
            while (i < 2) {
                textureBuffer.put(0);
                i++;
            }
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error parsing texture vector '" + line + "': " + ex.getMessage());
            textureBuffer.put(0).put(0);
        }
    }



    private void parseFace(IntBuffer vertexAttributes, int attributesStart, IntBuffer indices,
                           FloatBuffer vertexList, FloatBuffer normalsList, FloatBuffer texturesList,
                           String line, List<Integer> currentSmoothingList) {
        try {

            // cpu optimization
            final StringTokenizer st = new StringTokenizer(line, " ");
            final int numTokens = st.countTokens();
            if (numTokens < 3) return;

            final String[] tokens = new String[numTokens];
            for (int i = 0; i < numTokens; i++) {
                tokens[i] = st.nextToken();
            }

            // Triangulate polygon into triangles
            // (0, 1, 2), (0, 2, 3), (0, 3, 4), ...
            for (int i = 1; i < numTokens - 1; i++) {
                final int[] triangleIndices = {0, i, i + 1};

                for (int triIdx : triangleIndices) {
                    final String faceToken = tokens[triIdx];
                    final String[] faceTokens = faceToken.split("/", -1);
                    final int numSeps = faceTokens.length;

                    int vertIdx = Integer.parseInt(faceTokens[0]);
                    if (vertIdx < 0) {
                        vertIdx = (vertexList.position() / 3) + vertIdx;
                    } else {
                        vertIdx--;
                    }

                    int textureIdx = -1;
                    if (numSeps > 1 && faceTokens[1].length() > 0) {
                        textureIdx = Integer.parseInt(faceTokens[1]);
                        if (textureIdx < 0) {
                            textureIdx = (texturesList.position() / 2) + textureIdx;
                        } else {
                            textureIdx--;
                        }
                    }

                    int normalIdx = -1;
                    if (numSeps > 2 && faceTokens[2].length() > 0) {
                        normalIdx = Integer.parseInt(faceTokens[2]);
                        if (normalIdx < 0) {
                            normalIdx = (normalsList.position() / 3) + normalIdx;
                        } else {
                            normalIdx--;
                        }
                    }

                    // store the indices for this face
                    final int idx = (vertexAttributes.position() - attributesStart) / 3;
                    indices.put(idx);

                    // store VertexAttribute (v, vt, vn)
                    vertexAttributes.put(vertIdx);
                    vertexAttributes.put(textureIdx);
                    vertexAttributes.put(normalIdx);

                    // smoothing
                    if (currentSmoothingList != null) {
                        currentSmoothingList.add(idx);
                    }
                }
            }
        } catch (NumberFormatException e) {
            logger.log(Level.SEVERE,  e.getMessage(), e);
        }
    }

}
