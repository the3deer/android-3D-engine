// WavefrontLoader.java
// Andrew Davison, February 2007, ad@fivedots.coe.psu.ac.th

/* Load the OBJ model from MODEL_DIR, centering and scaling it.
 The scale comes from the sz argument in the constructor, and
 is implemented by changing the vertices of the loaded model.

 The model can have vertices, normals and tex coordinates, and
 refer to materials in a MTL file.

 The OpenGL commands for rendering the model are stored in 
 a display list (modelDispList), which is drawn by calls to
 draw().

 Information about the model is printed to stdout.

 Based on techniques used in the OBJ loading code in the
 JautOGL multiplayer racing game by Evangelos Pournaras 
 (http://today.java.net/pub/a/today/2006/10/10/
 development-of-3d-multiplayer-racing-game.html 
 and https://jautogl.dev.java.net/), and the 
 Asteroids tutorial by Kevin Glass 
 (http://www.cokeandcode.com/asteroidstutorial)

 CHANGES (Feb 2007)
 - a global flipTexCoords boolean
 - drawToList() sets and uses flipTexCoords
 */

package org.the3deer.android.engine.services.wavefront;

import android.opengl.GLES20;

import androidx.annotation.Nullable;

import org.the3deer.android.engine.model.Element;
import org.the3deer.android.engine.model.Material;
import org.the3deer.android.engine.model.Materials;
import org.the3deer.android.engine.model.Object3D;
import org.the3deer.android.engine.model.Scene;
import org.the3deer.android.engine.services.LoadListener;
import org.the3deer.android.engine.services.collada.entities.MeshData;
import org.the3deer.android.engine.services.collada.entities.Vertex;
import org.the3deer.util.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

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

            // log event
            logger.config("Parsing geometries... ");

            // open stream, parse model, then close stream
            final InputStream is = modelURI.toURL().openStream();
            final List<MeshData> meshes = loadModel(modelURI.toString(), is);
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
                final MeshData meshData = meshes.get(i);

                // notify listener
                callback.onProgress("Processing normals...");

                // fix missing or wrong normals
                meshData.fixNormals();

                // check we didn't brake normals
                meshData.validate();

                // create 3D object
                Object3D data3D = new Object3D(meshData.getId(), meshData.getVertexBuffer());
                data3D.setUri(modelURI);
                //data3D.setMeshData(meshData);
                data3D.setName(meshData.getName());
                data3D.setVertexNormalsArrayBuffer(meshData.getNormalsBuffer());
                data3D.setTextureCoordsArrayBuffer(meshData.getTextureBuffer());
                data3D.setElements(meshData.getElements());
                data3D.setIndexed(true);
                data3D.setDrawMode(GLES20.GL_TRIANGLES);

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

    private void loadMaterials(URI modelURI, MeshData meshData) {

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
                            logger.config("Reading texture file... " + elementMaterial.getColorTexture().getFile());

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

    private List<MeshData> loadModel(String uri, InputStream is) {

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

            // primitive data
            final List<float[]> vertexList = new ArrayList<>();
            final List<float[]> normalsList = new ArrayList<>();
            final List<float[]> textureList = new ArrayList<>();

            // mesh data
            final List<MeshData> meshes = new ArrayList<>();
            final List<Vertex> verticesAttributes = new ArrayList<>();

            // material file
            String mtllib = null;

            // smoothing groups
            final Map<String,List<Vertex>> smoothingGroups = new HashMap<>();
            List<Vertex> currentSmoothingList = null;

            // mesh current
            MeshData.Builder meshCurrent = new MeshData.Builder().id(uri);
            Element.Builder elementCurrent = new Element.Builder().id("default");
            List<Integer> indicesCurrent = new ArrayList<>();
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
                        if (buildNewMesh) {
                            // build mesh
                            meshCurrent.vertices(vertexList).normals(normalsList).textures(textureList)
                                    .vertexAttributes(verticesAttributes)
				    .materialFile(mtllib)
                                    .addElement(elementCurrent.indices(indicesCurrent).build());

                            // add current mesh
                            final MeshData build = meshCurrent.build();
                            meshes.add(build);

                            // log event
                            logger.config("Loaded mesh. id:" + build.getId() + ", indices: " + indicesCurrent.size()
                                    + ", vertices:" + vertexList.size()
                                    + ", normals: " + normalsList.size()
                                    + ", textures:" + textureList.size()
                                    + ", elements: " + build.getElements());

                            // next mesh
                            meshCurrent = new MeshData.Builder().id(line.substring(1).trim());

                            // next element
                            elementCurrent = new Element.Builder();
                            indicesCurrent = new ArrayList<>();
                        } else {
                            meshCurrent.id(line.substring(1).trim());
                            buildNewMesh = true;
                        }
                    } else if (line.charAt(0) == 'g') { // group name
                        if (buildNewElement && indicesCurrent.size() > 0) {

                            // add current element
                            elementCurrent.indices(indicesCurrent);
                            meshCurrent.addElement(elementCurrent.build());

                            // log event
                            logger.config("New element. indices: " + indicesCurrent.size());

                            // prepare next element
                            indicesCurrent = new ArrayList<>();
                            elementCurrent = new Element.Builder().id(line.substring(1).trim());
                        } else {
                            elementCurrent.id(line.substring(1).trim());
                            buildNewElement = true;
                        }
                    } else if (line.startsWith("f ")) { // face
                        parseFace(verticesAttributes, indicesCurrent, vertexList, normalsList, textureList, line.substring(2), currentSmoothingList);
                    } else if (line.startsWith("mtllib ")) {// build material
                        try {
                            mtllib = modelURI.resolve(line.substring(7)).toString();
                        } catch (Exception e) {
                            logger.log(Level.SEVERE,  "Error reading line: " + lineNum + " : " + line+", message: "+e.getMessage());
                        }
                    } else if (line.startsWith("usemtl ")) {// use material
                        if (elementCurrent.getMaterialId() != null) {

                            // change element since we are dealing with different material
                            elementCurrent.indices(indicesCurrent);
                            meshCurrent.addElement(elementCurrent.build());

                            // log event
                            logger.finest("New material: " + line);

                            // prepare next element
                            indicesCurrent = new ArrayList<>();
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
                final Element element = elementCurrent.indices(indicesCurrent).build();
                final MeshData meshData = meshCurrent.vertices(vertexList).normals(normalsList).textures(textureList)
                        .vertexAttributes(verticesAttributes).materialFile(mtllib)
                        .addElement(element).smoothingGroups(smoothingGroups).build();

                logger.config("Loaded mesh. id:" + meshData.getId() + ", indices: " + indicesCurrent.size()
                        + ", vertices:" + vertexList.size()
                        + ", normals: " + normalsList.size()
                        + ", textures:" + textureList.size()
                        + ", elements: " + meshData.getElements());

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

    /**
     * List of texture coordinates, in (u, [,v ,w]) coordinates, these will vary between 0 and 1. v, w are optional and default to 0.
     * There may only be 1 tex coords  on the line, which is determined by looking at the first tex coord line.
     */
    private void parseVector(final List<float[]> vectorList, final String line) {
        try {
            final StringTokenizer st = new StringTokenizer(line, " ");
            final float[] vector = new float[3];
            int i = 0;
            while (st.hasMoreTokens() && i < 3) {
                vector[i++] = Float.parseFloat(st.nextToken());
            }
            vectorList.add(vector);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error parsing vector '" + line + "': " + ex.getMessage());
            vectorList.add(new float[3]);
        }
    }

    /**
     * List of texture coordinates, in (u, [,v ,w]) coordinates, these will vary between 0 and 1. v, w are optional and default to 0.
     * There may only be 1 tex coords  on the line, which is determined by looking at the first tex coord line.
     */
    private void parseVariableVector(final List<float[]> textureList, final String line) {
        try {
            // StringTokenizer is significantly faster than line.split(" +")
            // because it avoids regex compilation and array allocation.
            final StringTokenizer st = new StringTokenizer(line, " ");
            final float[] vector = new float[2];

            if (st.hasMoreTokens()) {
                vector[0] = Float.parseFloat(st.nextToken());
            }
            if (st.hasMoreTokens()) {
                vector[1] = Float.parseFloat(st.nextToken());
            }

            // ignore 3d coordinate (w) if present, as per requirements

            textureList.add(vector);
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error parsing texture vector '" + line + "': " + ex.getMessage());
            textureList.add(new float[2]);
        }
    }


    /**
     * get this face's indicies from line "f v/vt/vn ..." with vt or vn index values perhaps being absent.
     */
    private void parseFace(List<Vertex> vertexAttributes, List<Integer> indices,
                           List<float[]> vertexList, List<float[]> normalsList, List<float[]> texturesList,
                           String line, List<Vertex> currentSmoothingList) {
        try {

            // cpu optimization
            final String[] tokens;
            if (line.contains("  ")) {
                tokens = line.split(" +");
            } else {
                tokens = line.split(" ");
            }

            // number of v/vt/vn tokens
            final int numTokens = tokens.length;

            for (int i = 0, faceIndex = 0; i < numTokens; i++, faceIndex++) {

                // convert to triangles all polygons
                if (faceIndex > 2) {
                    // Converting polygon to triangle
                    faceIndex = 0;

                    i -= 2;
                }

                // triangulate polygon
                final String faceToken;
                if (this.triangulationMode == GLES20.GL_TRIANGLE_FAN) {
                    // In FAN mode all meshObject shares the initial vertex
                    if (faceIndex == 0) {
                        faceToken = tokens[0];// get a v/vt/vn
                    } else {
                        faceToken = tokens[i]; // get a v/vt/vn
                    }
                } else {
                    // GL.GL_TRIANGLES | GL.GL_TRIANGLE_STRIP
                    faceToken = tokens[i]; // get a v/vt/vn
                }

                // parse index tokens
                // how many '/'s are there in the token
                final String[] faceTokens = faceToken.split("/");
                final int numSeps = faceTokens.length;

                int vertIdx = Integer.parseInt(faceTokens[0]);
                // A valid vertex index matches the corresponding vertex elements of a previously defined vertex list.
                // If an index is positive then it refers to the offset in that vertex list, starting at 1.
                // If an index is negative then it relatively refers to the end of the vertex list,
                // -1 referring to the last element.
                if (vertIdx < 0) {
                    vertIdx = vertexList.size() + vertIdx;
                } else {
                    vertIdx--;
                }

                int textureIdx = -1;
                if (numSeps > 1 && faceTokens[1].length() > 0) {
                    textureIdx = Integer.parseInt(faceTokens[1]);
                    if (textureIdx < 0) {
                        textureIdx = texturesList.size() + textureIdx;
                    } else {
                        textureIdx--;
                    }
                }
                int normalIdx = -1;
                if (numSeps > 2 && faceTokens[2].length() > 0) {
                    normalIdx = Integer.parseInt(faceTokens[2]);
                    if (normalIdx < 0) {
                        normalIdx = normalsList.size() + normalIdx;
                    } else {
                        normalIdx--;
                    }
                }

                // create VertexAttribute
                final Vertex vertexAttribute = new Vertex(vertIdx);
                vertexAttribute.setNormalIndex(normalIdx);
                vertexAttribute.setTextureIndex(textureIdx);

                // add VertexAtribute
                final int idx = vertexAttributes.size();
                vertexAttributes.add(idx, vertexAttribute);

                // store the indices for this face
                indices.add(idx);

                // smoothing
                if (currentSmoothingList != null){
                    currentSmoothingList.add(vertexAttribute);
                }
            }
        } catch (NumberFormatException e) {
            logger.log(Level.SEVERE,  e.getMessage(), e);
        }
    }

}
