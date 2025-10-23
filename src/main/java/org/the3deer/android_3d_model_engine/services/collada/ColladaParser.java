package org.the3deer.android_3d_model_engine.services.collada;

import android.opengl.Matrix;
import android.util.Log;

import org.the3deer.android_3d_model_engine.model.Material;
import org.the3deer.android_3d_model_engine.model.Texture;
import org.the3deer.android_3d_model_engine.model.Transform;
import org.the3deer.android_3d_model_engine.services.collada.entities.Controller;
import org.the3deer.android_3d_model_engine.services.collada.entities.Geometry;
import org.the3deer.android_3d_model_engine.services.collada.entities.Node;
import org.the3deer.android_3d_model_engine.services.collada.entities.Skin;
import org.the3deer.android_3d_model_engine.services.collada.entities.Source;
import org.the3deer.android_3d_model_engine.services.collada.entities.VertexWeights;
import org.the3deer.util.io.IOUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ColladaParser {

    private static final String TAG = ColladaParser.class.getSimpleName();

    // This will hold the parsed geometry data, keyed by their ID (e.g., "U3DMesh-GEOMETRY")
    private final Map<String, Geometry> geometryLibrary = new HashMap<>();

    // Add a new map at the top of the class to hold the parsed controllers
    private final Map<String, Controller> controllerLibrary = new HashMap<>();

    private final Map<String, Material> materialLibrary = new HashMap<>();

    // NEW MAPS for resolving material -> effect -> image
    private final Map<String, String> effectIdToImageIdMap = new HashMap<>();
    private final Map<String, String> imageIdToFileNameMap = new HashMap<>();

    // Node library
    private Map<String, Node> nodeLibrary = new HashMap<>();
    // ADD this new list to hold the top-level nodes of the active scene.
    private final List<Node> rootNodes = new ArrayList<>();

    private static class Accessor {
        final int stride;

        // Add count, source, etc. if you need them later.
        Accessor(int stride) {
            this.stride = stride;
        }
    }

    private static class Input {
        private String semantic;
        private String sourceId;
        private final int offset;

        private Input(String semantic, String sourceId, int offset) {
            this.semantic = semantic;
            this.sourceId = sourceId;
            this.offset = offset;
        }
    }


    /**
     * Parses the given COLLADA (.dae) file stream and populates the internal data libraries.
     *
     * @param daeStream The InputStream of the .dae file.
     * @throws Exception if parsing fails.
     */
    public void parse(InputStream daeStream) throws Exception {
        // 1. Initialize the XmlPullParser
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true); // COLLADA files use namespaces
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(daeStream, null);

        // 2. Start the event loop
        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            // We are only interested in the start of tags
            if (eventType == XmlPullParser.START_TAG) {
                String tagName = parser.getName();

                // 3. Act as a dispatcher based on the tag name
                switch (tagName) {
                    case "library_geometries":
                        Log.d(TAG, "Found <library_geometries>. Parsing...");
                        parseGeometriesLibrary(parser);
                        Log.d(TAG, "Finished parsing <library_geometries>.");
                        break;
                    // NEW CASE for controllers
                    case "library_controllers":
                        Log.d(TAG, "Found <library_controllers>. Parsing...");
                        parseControllersLibrary(parser);
                        Log.d(TAG, "Finished parsing <library_controllers>.");
                        break;
                    // --- ADD THESE NEW CASES ---
                    case "library_images":
                        Log.d(TAG, "Found <library_images>. Parsing...");
                        parseLibraryImages(parser);
                        break;
                    case "library_effects":
                        Log.d(TAG, "Found <library_effects>. Parsing...");
                        parseLibraryEffects(parser);
                        break;
                    case "library_materials":
                        Log.d(TAG, "Found <library_materials>. Parsing...");
                        parseLibraryMaterials(parser);
                        break;
                    case "library_visual_scenes":
                        Log.d(TAG, "Found <library_visual_scenes>. Parsing...");
                        parseVisualScenes(parser);
                        break;
                }
            }
            // Move to the next event in the XML file
            eventType = parser.next();
        }
        Log.i(TAG, "Finished parsing DAE file. Geometries found: " + geometryLibrary.size() + ", Nodes found: " + nodeLibrary.size() +
                ", Controllers found: " + controllerLibrary.size());
    }

    /**
     * Parses the <library_geometries> section of the COLLADA file.
     * This method assumes the parser is currently at the START_TAG of <library_geometries>.
     *
     * @param parser The XmlPullParser instance.
     * @throws Exception if parsing fails.
     */
    private void parseGeometriesLibrary(XmlPullParser parser) throws Exception {
        // Loop until we find the closing </library_geometries> tag
        while (parser.next() != XmlPullParser.END_TAG || !parser.getName().equals("library_geometries")) {
            // Skip anything that isn't the start of a new tag
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            // We found a tag inside <library_geometries>, we only care about <geometry>
            if (parser.getName().equals("geometry")) {
                String geometryId = parser.getAttributeValue(null, "id");
                Log.d(TAG, "Parsing <geometry> with id: " + geometryId);

                // Call the method to parse the contents of this <geometry> tag
                parseGeometry(parser);
            }
        }
    }

// Add these methods to your ColladaParser.java

    /**
     * Parses a <geometry> block, which contains the mesh data.
     * Assumes the parser is at the START_TAG of <geometry>.
     */
    // In ColladaParser.java
    // In ColladaParser.java
    private void parseGeometry(XmlPullParser parser) throws Exception {
        String geometryId = parser.getAttributeValue(null, "id");
        Geometry geometry = new Geometry(geometryId);

        // These maps are local to this geometry block
        Map<String, Source> sources = new HashMap<>();
        Map<String, List<Input>> verticesLibrary = new HashMap<>();

        // Loop until we are at the end of the <geometry> tag.
        while (parser.next() != XmlPullParser.END_TAG || !parser.getName().equals("geometry")) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            if (parser.getName().equals("mesh")) {
                // Found the mesh tag. Now, FIRST, parse all sources and vertices inside it.
                while (parser.next() != XmlPullParser.END_TAG || !parser.getName().equals("mesh")) {
                    if (parser.getEventType() != XmlPullParser.START_TAG) {
                        continue;
                    }
                    String meshTagName = parser.getName();
                    if ("source".equals(meshTagName)) {
                        Source source = parseSource(parser);
                        sources.put(source.getId(), source);
                        // In parseGeometry(...) method's loop

                    } else if ("vertices".equals(meshTagName)) {
                        String verticesId = parser.getAttributeValue(null, "id");
                        // Call our new method and store the list of inputs in the library
                        verticesLibrary.put(verticesId, parseVerticesInputs(parser));
                    } else if ("polylist".equals(meshTagName) || "triangles".equals(meshTagName)) {
                        // SECOND, now that we have all sources and vertices, parse the primitive.
                        // This call is now guaranteed to succeed.
                        parseMeshPrimitive(parser, sources, verticesLibrary, geometry);
                    } else {
                        // Skip any unhandled tags like <extra> to avoid parsing errors
                        Log.w(TAG, "Skipping unhandled <mesh> tag: " + meshTagName);
                        int depth = 1;
                        while (depth != 0) {
                            switch (parser.next()) {
                                case XmlPullParser.END_TAG:
                                    depth--;
                                    break;
                                case XmlPullParser.START_TAG:
                                    depth++;
                                    break;
                            }
                        }
                    }
                }
            }
        }

        // Add the fully populated geometry to our main library
        if (geometry.getPositions() != null) {
            geometryLibrary.put(geometryId, geometry);
            Log.d(TAG, "Finished parsing geometry '" + geometryId + "'.");
        } else {
            Log.e(TAG, "Geometry '" + geometryId + "' was parsed but resulted in no vertex data.");
        }
    }


    /**
     * Parses a <source> block, which contains raw data like vertex positions or normals.
     * Assumes the parser is at the START_TAG of <source>.
     */
    /**
     * Parses a <source> block, which can contain raw data like floats or strings.
     * Assumes the parser is at the START_TAG of <source>.
     */
    // In ColladaParser.java
    // In ColladaParser.java
// REPLACE the old parseSource method with this one.

    /**
     * Parses a <source> block, which contains raw data arrays and an accessor
     * that defines how to interpret that data.
     * Assumes the parser is at the START_TAG of <source>.
     *
     * @param parser The XmlPullParser instance.
     * @return A fully populated Source object.
     * @throws Exception if parsing fails.
     */
    // In ColladaParser.java
// REPLACE the old parseSource method with this one.

    /**
     * Parses a <source> block, which contains raw data arrays and an accessor
     * that defines how to interpret that data.
     * Assumes the parser is at the START_TAG of <source>.
     */
    private Source parseSource(XmlPullParser parser) throws Exception {
        String sourceId = parser.getAttributeValue(null, "id");

        // Data holders - we find all the pieces first, then process.
        Accessor accessor = null;
        float[] floatData = null;
        String[] stringData = null;

        int sourceStartDepth = parser.getDepth();
        while (parser.next() != XmlPullParser.END_TAG || parser.getDepth() > sourceStartDepth) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            String tagName = parser.getName();
            switch (tagName) {
                case "float_array":
                    String countStr = parser.getAttributeValue(null, "count");
                    int count = Integer.parseInt(countStr);
                    floatData = new float[count];
                    String textData = parser.nextText();

                    try (java.util.Scanner scanner = new java.util.Scanner(textData)) {
                        for (int i = 0; i < count; i++) {
                            if (scanner.hasNextFloat()) {
                                floatData[i] = scanner.nextFloat();
                            }
                        }
                    }
                    break;

                case "Name_array":
                    String nameCountStr = parser.getAttributeValue(null, "count");
                    int nameCount = Integer.parseInt(nameCountStr);
                    stringData = new String[nameCount];
                    String namesTextData = parser.nextText();
                    try (java.util.Scanner scanner = new java.util.Scanner(namesTextData)) {
                        for (int i = 0; i < nameCount; i++) {
                            if (scanner.hasNext()) {
                                stringData[i] = scanner.next();
                            }
                        }
                    }
                    break;

                case "technique_common":
                    int techStartDepth = parser.getDepth();
                    while (parser.next() != XmlPullParser.END_TAG || parser.getDepth() > techStartDepth) {
                        if (parser.getEventType() == XmlPullParser.START_TAG && "accessor".equals(parser.getName())) {
                            String strideStr = parser.getAttributeValue(null, "stride");
                            int stride = (strideStr != null) ? Integer.parseInt(strideStr) : 1;
                            accessor = new Accessor(stride);
                        }
                    }
                    break;
            }
        }

        // --- Final Assembly ---
        int finalStride = (accessor != null) ? accessor.stride : 1;

        Source finalSource = new Source(sourceId, floatData, stringData, finalStride);
        Log.d(TAG, "Parsed <source> '" + finalSource.getId() + "' with stride " + finalSource.getStride());
        return finalSource;
    }


    /**
     * Parses a <vertices> block and returns a list of the <input> tags it contains.
     */
    private List<Input> parseVerticesInputs(XmlPullParser parser) throws Exception {
        List<Input> inputs = new ArrayList<>();
        int verticesStartDepth = parser.getDepth();

        // Loop through all tags inside the <vertices> block
        while (parser.next() != XmlPullParser.END_TAG || parser.getDepth() > verticesStartDepth) {
            if (parser.getEventType() == XmlPullParser.START_TAG && "input".equals(parser.getName())) {
                // It's an <input> tag. Parse it and add it to our list.
                inputs.add(parseInput(parser));
            }
        }
        return inputs;
    }

    /**
     * Parses a mesh primitive tag like <triangles> or <polylist>.
     * It populates the provided Geometry object with the final vertex data.
     * Assumes the parser is at the START_TAG of <triangles> or <polylist>.
     */
    // In ColladaParser.java
    private void parseMeshPrimitive(XmlPullParser parser, Map<String, Source> sources,
                                    Map<String, List<Input>> verticesLibrary, Geometry geometry) throws Exception {

        List<Input> inputs = new ArrayList<>();
        int[] indices = null;
        String primitiveName = parser.getName(); // Either "triangles" or "polylist"

        // Read the 'material' attribute from the <polylist> or <triangles> tag.
        String materialId = parser.getAttributeValue(null, "material");
        if (materialId != null) {
            geometry.setMaterialId(materialId);
            Log.d(TAG, "Bound material '" + materialId + "' to geometry '" + geometry.getId() + "'");
        }

        // This loop parses all the <input> and the <p> tags.
        while (parser.next() != XmlPullParser.END_TAG || !parser.getName().equals(primitiveName)) {
            if (parser.getEventType() != XmlPullParser.START_TAG) continue;
            switch (parser.getName()) {
                case "input":
                    // --- THIS IS THE FIX ---
                    // Actually call the parseInput method
                    inputs.add(parseInput(parser));
                    // --- END OF FIX ---
                    break;
                case "p":
                    String indexData = parser.nextText();
                    String[] pStrings = indexData.trim().split("\\s+");
                    indices = new int[pStrings.length];
                    for (int i = 0; i < pStrings.length; i++) {
                        indices[i] = Integer.parseInt(pStrings[i]);
                    }
                    break;
                case "vcount":
                    // Polylist has a vcount, triangles do not. We can ignore it for our unrolling logic.
                    parser.nextText(); // Consume it
                    break;
            }
        }

        if (indices == null || inputs.isEmpty()) {
            Log.e(TAG, "No <p> indices or <input> tags found in mesh primitive");
            return;
        }

        // Helper function to resolve the real source ID, handling <vertices> indirection.
        // In parseMeshPrimitive(...)

        java.util.function.Function<Input, Source> getSourceFromInput = (input) -> {
            if (input == null) return null;

            // Is the input's source ID an indirection (a key in our verticesLibrary)?
            if (verticesLibrary.containsKey(input.sourceId)) {
                // YES. Get the list of inputs defined in that <vertices> block.
                List<Input> indirectInputs = verticesLibrary.get(input.sourceId);

                // --- THIS IS THE FIX ---
                // Determine the REAL semantic we are looking for.
                // If the incoming semantic is "VERTEX", we are actually looking for "POSITION".
                // Otherwise, we are looking for the same semantic (e.g., NORMAL -> NORMAL).
                final String targetSemantic = "VERTEX".equals(input.semantic) ? "POSITION" : input.semantic;
                // --- END OF FIX ---

                // Find the specific input that matches the TARGET semantic.
                Input realInput = indirectInputs.stream()
                        .filter(i -> i.semantic.equals(targetSemantic)) // Use the corrected targetSemantic
                        .findFirst()
                        .orElse(null);

                if (realInput != null) {
                    // We found the real input, now get its source from the main sources map.
                    return sources.get(realInput.sourceId);
                }

                // This can happen if, for example, a <vertices> block doesn't define a normal.
                Log.w(TAG, "Indirection '" + input.sourceId + "' did not contain an input for semantic '" + targetSemantic + "'");
                return null;
            } else {
                // NO. This is a direct link. Look it up directly.
                return sources.get(input.sourceId);
            }
        };

// ... the rest of the method is unchanged ...


        // Get input metadata
        Input vertexInput = inputs.stream().filter(i -> "VERTEX".equals(i.semantic)).findFirst().orElse(null);
        Input normalInput = inputs.stream().filter(i -> "NORMAL".equals(i.semantic)).findFirst().orElse(null);
        Input texCoordInput = inputs.stream().filter(i -> "TEXCOORD".equals(i.semantic)).findFirst().orElse(null);
        Input colorInput = inputs.stream().filter(i -> "COLOR".equals(i.semantic)).findFirst().orElse(null);

        // Get raw data sources using our new universal helper
        Source positionSource = getSourceFromInput.apply(vertexInput);
        Source normalSource = getSourceFromInput.apply(normalInput);
        Source texCoordSource = getSourceFromInput.apply(texCoordInput);
        Source colorSource = getSourceFromInput.apply(colorInput);

        // Add a check to prevent crashes if the vertex source is not found
        if (positionSource == null) {
            Log.e(TAG, "FATAL: Could not resolve position source for primitive.");
            return; // Cannot proceed without positions
        }

        int inputCount = inputs.size();
        int finalVertexCount = indices.length / inputCount;

        // Allocate final "unrolled" buffers
        float[] finalPositions = new float[finalVertexCount * 3];
        float[] finalNormals = (normalSource != null) ? new float[finalVertexCount * 3] : null;
        float[] finalTexCoords = (texCoordSource != null) ? new float[finalVertexCount * 2] : null;
        int[] vertexJointIndices = new int[finalVertexCount];

        // Final buffer is ALWAYS RGBA (stride 4) to match the legacy loader and renderer expectations
        float[] finalColors = new float[finalVertexCount * 4];

        // Unroll all vertex attributes into the final buffers
        for (int i = 0; i < finalVertexCount; i++) {
            int p_base = i * inputCount;

            int positionIndex = indices[p_base + vertexInput.offset];
            int normalIndex = (normalInput != null) ? indices[p_base + normalInput.offset] : -1;
            int texCoordIndex = (texCoordInput != null) ? indices[p_base + texCoordInput.offset] : -1;
            int colorIndex = (colorInput != null) ? indices[p_base + colorInput.offset] : -1;

            vertexJointIndices[i] = positionIndex;

            // Unroll Positions (XYZ)
            finalPositions[i * 3] = positionSource.getFloatData()[positionIndex * 3];
            finalPositions[i * 3 + 1] = positionSource.getFloatData()[positionIndex * 3 + 1];
            finalPositions[i * 3 + 2] = positionSource.getFloatData()[positionIndex * 3 + 2];

            // Unroll Normals (XYZ)
            if (normalIndex != -1 && finalNormals != null) {
                finalNormals[i * 3] = normalSource.getFloatData()[normalIndex * 3];
                finalNormals[i * 3 + 1] = normalSource.getFloatData()[normalIndex * 3 + 1];
                finalNormals[i * 3 + 2] = normalSource.getFloatData()[normalIndex * 3 + 2];
            }

            // Unroll Texture Coords (UV)
            if (texCoordIndex != -1 && finalTexCoords != null) {
                finalTexCoords[i * 2] = texCoordSource.getFloatData()[texCoordIndex * 2];
                finalTexCoords[i * 2 + 1] = texCoordSource.getFloatData()[texCoordIndex * 2 + 1];
            }

            // --- CONVERT RGB SOURCE TO RGBA DESTINATION ---
            if (colorIndex != -1 && colorSource != null) {
                int sourceColorStride = colorSource.getStride(); // This will be 3 for our file
                // Copy the R, G, B values
                finalColors[i * 4] = colorSource.getFloatData()[colorIndex * sourceColorStride];
                finalColors[i * 4 + 1] = colorSource.getFloatData()[colorIndex * sourceColorStride + 1];
                finalColors[i * 4 + 2] = colorSource.getFloatData()[colorIndex * sourceColorStride + 2];
                // Manually add the Alpha component
                finalColors[i * 4 + 3] = 1.0f;
            } else {
                // If no color source, fill with default white RGBA
                finalColors[i * 4] = 1.0f;
                finalColors[i * 4 + 1] = 1.0f;
                finalColors[i * 4 + 2] = 1.0f;
                finalColors[i * 4 + 3] = 1.0f;
            }
        }

        // --- NEW: Invert Texture V-Coordinate ---
        // This loop replicates the behavior of the legacy parser to fix texture mapping.
        if (finalTexCoords != null) {
            // The texture coordinates are stored as [U, V, U, V, ...].
            // We only need to modify the V component, which is at every odd index (1, 3, 5, ...).
            for (int i = 1; i < finalTexCoords.length; i += 2) {
                finalTexCoords[i] = 1.0f - finalTexCoords[i];
            }
            Log.d(TAG, "Inverted V-coordinate for " + (finalTexCoords.length / 2) + " texture coordinates.");
        }
        // --- END OF NEW LOGIC ---

        // Set the final, unrolled buffers on the geometry object
        geometry.setPositions(IOUtils.createFloatBuffer(finalPositions));
        if (finalNormals != null) {
            geometry.setNormals(IOUtils.createFloatBuffer(finalNormals));
        }
        if (finalTexCoords != null) {
            geometry.setTexCoords(IOUtils.createFloatBuffer(finalTexCoords));
        }
        geometry.setColors(IOUtils.createFloatBuffer(finalColors)); // Always set the RGBA color buffer
        geometry.setIndices(null); // This is a non-indexed model
        geometry.setVertexJointIndices(vertexJointIndices); // Save the skinning map

        Log.d(TAG, "Assembled unrolled geometry '" + geometry.getId() + "' with " + finalVertexCount + " vertices.");

    }


    private String cleanId(String rawSourceId) {
        if (rawSourceId != null && rawSourceId.length() > 1) {
            return rawSourceId.substring(1);
        }
        return rawSourceId;
    }


    /**
     * Parses the <library_controllers> section, which contains all skinning and animation data.
     */
    private void parseControllersLibrary(XmlPullParser parser) throws Exception {
        while (parser.next() != XmlPullParser.END_TAG || !parser.getName().equals("library_controllers")) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            if (parser.getName().equals("controller")) {
                // The parseController method now returns the fully parsed controller
                Controller controller = parseController(parser);
                if (controller != null) {
                    controllerLibrary.put(controller.getId(), controller);
                    Log.d(TAG, "Parsed controller '" + controller.getId() + "'");
                }
            }
        }
    }

    /**
     * Parses a complete <controller> block. This block contains a <skin> which in turn
     * links together all the data (matrices, joint names, weights) for an animated model.
     * This method replaces the old parseController and parseSkin methods.
     *
     * @return A fully populated Controller object, or null if parsing fails.
     */
    // In ColladaParser.java

    /**
     * Parses a complete <controller> block. This block contains a <skin> which in turn
     * links together all the data (matrices, joint names, weights) for an animated model.
     * This method replaces the old parseController and parseSkin methods.
     *
     * @return A fully populated Controller object, or null if parsing fails.
     */
    private Controller parseController(XmlPullParser parser) throws Exception {
        String controllerId = parser.getAttributeValue(null, "id");
        Controller controller = null;

        while (parser.next() != XmlPullParser.END_TAG || !parser.getName().equals("controller")) {
            if (parser.getEventType() != XmlPullParser.START_TAG || !parser.getName().equals("skin")) {
                continue;
            }

            // We are inside the <skin> tag
            String skinSourceId = cleanId(parser.getAttributeValue(null, "source"));

            // --- THIS IS THE FIX ---
            // Use the default constructor and the new setter
            Skin skin = new Skin();
            skin.setSource(skinSourceId);
            // --- END OF FIX ---

            controller = new Controller(controllerId, skin);

            // This map holds sources parsed inside this <skin> tag only
            Map<String, Source> sources = new HashMap<>();

            while (parser.next() != XmlPullParser.END_TAG || !parser.getName().equals("skin")) {
                if (parser.getEventType() != XmlPullParser.START_TAG) continue;

                String tagName = parser.getName();
                switch (tagName) {
                    case "bind_shape_matrix":
                        skin.setBindShapeMatrix(readMatrix(parser.nextText()));
                        break;
                    case "source":
                        Source source = parseSource(parser);
                        sources.put(source.getId(), source);
                        break;
                    case "joints":
                        Input jointNameInput = null;
                        Input invBindMatrixInput = null;
                        while (parser.next() != XmlPullParser.END_TAG || !parser.getName().equals("joints")) {
                            if (parser.getEventType() != XmlPullParser.START_TAG || !parser.getName().equals("input"))
                                continue;
                            String semantic = parser.getAttributeValue(null, "semantic");
                            String sourceId = cleanId(parser.getAttributeValue(null, "source"));
                            if ("JOINT".equals(semantic)) {
                                jointNameInput = new Input(semantic, sourceId, 0);
                            } else if ("INV_BIND_MATRIX".equals(semantic)) {
                                invBindMatrixInput = new Input(semantic, sourceId, 0);
                            }
                        }
                        // Use the inputs to find the data in our sources map and populate the controller
                        if (jointNameInput != null) {
                            Source jointNamesSource = sources.get(jointNameInput.sourceId);
                            if (jointNamesSource != null && jointNamesSource.getStringData() != null) {
                                // --- THIS IS THE FIX ---
                                // Set the joint names on the skin object, not the controller
                                skin.setJointNames(Arrays.asList(jointNamesSource.getStringData()));
                                // --- END OF FIX ---
                            }
                        }
                        if (invBindMatrixInput != null) {
                            Source invBindMatricesSource = sources.get(invBindMatrixInput.sourceId);
                            if (invBindMatricesSource != null && invBindMatricesSource.getFloatData() != null) {
                                // This was correct already, we set it on the skin
                                skin.setInverseBindMatrices(invBindMatricesSource.getFloatData());
                            }
                        }
                        break;
                    case "vertex_weights":
                        skin.setWeights(parseVertexWeights(parser, sources));
                        break;
                }
            }
        }
        return controller;
    }

    // In ColladaParser.java, replace the entire parseVertexWeights method

    /**
     * Parses the <vertex_weights> block to create the final joint and weight buffers for each vertex.
     *
     * @param parser  The XmlPullParser instance.
     * @param sources The map of available <source> data parsed within the current <skin>.
     * @return A VertexWeights object containing the final joint and weight data.
     */
    private VertexWeights parseVertexWeights(XmlPullParser parser, Map<String, Source> sources) throws Exception {
        Input jointInput = null;
        Input weightInput = null;
        int[] vcount = null;
        int[] v = null;
        int inputCount = 0; // Number of inputs (usually 2: JOINT and WEIGHT)

        // 1. Find all the <input>, <vcount>, and <v> tags
        while (parser.next() != XmlPullParser.END_TAG || !parser.getName().equals("vertex_weights")) {
            if (parser.getEventType() != XmlPullParser.START_TAG) continue;

            String tagName = parser.getName();
            switch (tagName) {
                case "input":
                    inputCount++;
                    String semantic = parser.getAttributeValue(null, "semantic");
                    String sourceId = cleanId(parser.getAttributeValue(null, "source"));
                    int offset = Integer.parseInt(parser.getAttributeValue(null, "offset"));
                    if ("JOINT".equals(semantic)) {
                        jointInput = new Input(semantic, sourceId, offset);
                    } else if ("WEIGHT".equals(semantic)) {
                        weightInput = new Input(semantic, sourceId, offset);
                    }
                    break;
                case "vcount":
                    String vcountText = parser.nextText();
                    String[] vcountStrings = vcountText.trim().split("\\s+");
                    vcount = new int[vcountStrings.length];
                    for (int i = 0; i < vcountStrings.length; i++) {
                        vcount[i] = Integer.parseInt(vcountStrings[i]);
                    }
                    break;
                case "v":
                    String vText = parser.nextText();
                    String[] vStrings = vText.trim().split("\\s+");
                    v = new int[vStrings.length];
                    for (int i = 0; i < vStrings.length; i++) {
                        v[i] = Integer.parseInt(vStrings[i]);
                    }
                    break;
            }
        }

        if (vcount == null || v == null || jointInput == null || weightInput == null) {
            Log.e(TAG, "Incomplete <vertex_weights> data. Missing v, vcount, or inputs.");
            return null;
        }

        // --- THIS IS THE SECOND FIX ---
        // The vertex count is simply the length of the vcount array.
        int vertexCount = vcount.length;
        // --- END OF FIX ---


        // 2. Get the raw source data for weights
        Source weightsSource = sources.get(weightInput.sourceId);
        if (weightsSource == null) {
            Log.e(TAG, "Could not find weight source: " + weightInput.sourceId);
            return null;
        }
        float[] rawWeights = weightsSource.getFloatData();

        // 3. Process vcount and v to create normalized joint and weight arrays
        // We will assume a max of 4 influences per vertex, which is standard.
        int[] finalJointIndices = new int[vertexCount * 4];
        float[] finalWeights = new float[vertexCount * 4];
        int vIndex = 0;

        for (int i = 0; i < vertexCount; i++) {
            int numInfluences = vcount[i];
            float totalWeight = 0;

            // First pass: read up to 4 influences and accumulate total weight for normalization
            for (int j = 0; j < numInfluences && j < 4; j++) {
                int jointIndex = v[vIndex + jointInput.offset];
                int weightIndex = v[vIndex + weightInput.offset];
                float weight = rawWeights[weightIndex];

                finalJointIndices[i * 4 + j] = jointIndex;
                finalWeights[i * 4 + j] = weight;
                totalWeight += weight;

                // Move to the next (joint, weight) pair
                vIndex += inputCount;
            }

            // Normalize the weights for this vertex if the total is greater than 0
            if (totalWeight > 0) {
                for (int j = 0; j < Math.min(numInfluences, 4); j++) {
                    finalWeights[i * 4 + j] /= totalWeight;
                }
            }

            // If a vertex had more than 4 influences, we must advance the vIndex past them to stay in sync
            if (numInfluences > 4) {
                vIndex += (numInfluences - 4) * inputCount;
            }
        }

        Log.d(TAG, "Assembled vertex weights for " + vertexCount + " vertices.");
        return new VertexWeights(finalJointIndices, finalWeights);
    }


    /**
     * Parses the <library_images> section to map image IDs to their file names.
     * Example: <image id="cowboy_png" name="cowboy_png"><init_from>cowboy.png</init_from></image>
     */
    private void parseLibraryImages(XmlPullParser parser) throws Exception {
        while (parser.next() != XmlPullParser.END_TAG || !parser.getName().equals("library_images")) {
            if (parser.getEventType() != XmlPullParser.START_TAG || !parser.getName().equals("image")) {
                continue;
            }
            String imageId = parser.getAttributeValue(null, "id");
            String fileName = null;

            while (parser.next() != XmlPullParser.END_TAG || !parser.getName().equals("image")) {
                if (parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equals("init_from")) {
                    fileName = parser.nextText();
                }
            }

            if (imageId != null && fileName != null) {
                imageIdToFileNameMap.put(imageId, fileName);
                Log.d(TAG, "Mapped Image ID '" + imageId + "' to file '" + fileName + "'");
            }
        }
    }

    /**
     * Parses the <library_effects> section to map effect IDs to image IDs.
     * This is the bridge between materials and images.
     */
    private void parseLibraryEffects(XmlPullParser parser) throws Exception {
        while (parser.next() != XmlPullParser.END_TAG || !parser.getName().equals("library_effects")) {
            if (parser.getEventType() != XmlPullParser.START_TAG || !parser.getName().equals("effect")) {
                continue;
            }
            String effectId = parser.getAttributeValue(null, "id");
            String imageId = null;

            // Navigate through profile_COMMON -> newparam (surface) -> init_from (image ID)
            while (parser.next() != XmlPullParser.END_TAG || !parser.getName().equals("effect")) {
                if (parser.getEventType() != XmlPullParser.START_TAG) continue;

                // Looking for <newparam sid="...-surface">
                if ("newparam".equals(parser.getName())) {
                    while (parser.next() != XmlPullParser.END_TAG || !parser.getName().equals("newparam")) {
                        if (parser.getEventType() != XmlPullParser.START_TAG) continue;
                        // Looking for <init_from> inside <surface>
                        if ("init_from".equals(parser.getName())) {
                            imageId = parser.nextText();
                            // We found the image id, we can break out of this inner loop
                            break;
                        }
                    }
                }
                if (imageId != null) break; // Found it, exit the main effect loop
            }

            if (effectId != null && imageId != null) {
                effectIdToImageIdMap.put(effectId, imageId);
                Log.d(TAG, "Mapped Effect ID '" + effectId + "' to Image ID '" + imageId + "'");
            }
        }
    }

    /**
     * Parses the <library_materials> section. This creates the final Material objects
     * by connecting a material ID to an effect ID, and then resolving the texture file.
     */
    private void parseLibraryMaterials(XmlPullParser parser) throws Exception {
        while (parser.next() != XmlPullParser.END_TAG || !parser.getName().equals("library_materials")) {
            if (parser.getEventType() != XmlPullParser.START_TAG || !parser.getName().equals("material")) {
                continue;
            }
            String materialId = parser.getAttributeValue(null, "id");
            String effectUrl = null;

            while (parser.next() != XmlPullParser.END_TAG || !parser.getName().equals("material")) {
                if (parser.getEventType() == XmlPullParser.START_TAG && parser.getName().equals("instance_effect")) {
                    effectUrl = parser.getAttributeValue(null, "url");
                }
            }

            if (materialId != null && effectUrl != null) {
                String effectId = cleanId(effectUrl);

                // Create the material
                Material material = new Material(materialId);

                // Resolve the texture file name through the maps
                String imageId = effectIdToImageIdMap.get(effectId);
                if (imageId != null) {
                    String fileName = imageIdToFileNameMap.get(imageId);
                    if (fileName != null) {
                        material.setColorTexture(new Texture().setFile(fileName));
                        Log.d(TAG, "Created material '" + materialId + "' with texture '" + fileName + "'");
                    }
                }

                // Add the final, complete material to the library
                materialLibrary.put(materialId, material);
            }
        }
    }

    // --- PASTE THIS METHOD INTO ColladaParser.java ---

    /**
     * Parses an <input> tag, which defines a data stream.
     * Example: <input semantic="VERTEX" source="#Cube-mesh-vertices" offset="0"/>
     * @param parser The XmlPullParser at the START_TAG of an <input>.
     * @return A new Input object with the parsed data.
     * @throws Exception If the <input> tag is not correctly closed.
     */
    // --- ADD THIS METHOD TO ColladaParser.java ---
// --- AND DELETE THE OLD, BROKEN parseInput ---

    /**
     * Parses an <input> tag, which defines a data stream.
     * This is a simple, non-looping version that correctly handles self-closing tags.
     * Example: <input semantic="VERTEX" source="#Cube-mesh-vertices" offset="0"/>
     *
     * @param parser The XmlPullParser at the START_TAG of an <input>.
     * @return A new Input object with the parsed data.
     */
    private Input parseInput(XmlPullParser parser) {
        String semantic = parser.getAttributeValue(null, "semantic");
        String sourceId = cleanId(parser.getAttributeValue(null, "source"));
        String offsetStr = parser.getAttributeValue(null, "offset");
        int offset = (offsetStr != null) ? Integer.parseInt(offsetStr) : 0;

        Log.d(TAG, "Parsed <input> with semantic: " + semantic + ", source: " + sourceId + ", offset: " + offset);
        return new Input(semantic, sourceId, offset);
    }


    // Add this method to ColladaParser.java

    /**
     * Reads a 4x4 matrix from a space-separated string of16 floats.
     *
     * @param textData The string containing the 16 float values.
     * @return A float array of size 16.
     */
    private float[] readMatrix(String textData) {
        if (textData == null || textData.isEmpty()) {
            return null;
        }

        String[] floatStrings = textData.trim().split("\\s+");
        if (floatStrings.length < 16) {
            Log.e(TAG, "Matrix data has less than 16 values. Found: " + floatStrings.length);
            return null;
        }

        float[] matrix = new float[16];
        for (int i = 0; i < 16; i++) {
            matrix[i] = Float.parseFloat(floatStrings[i]);
        }
        return matrix;
    }

    // Add this new parsing method and its helper to ColladaParser.java
    private void parseVisualScenes(XmlPullParser parser) throws Exception {
        while (parser.next() != XmlPullParser.END_TAG || !parser.getName().equals("library_visual_scenes")) {
            if (parser.getEventType() != XmlPullParser.START_TAG) continue;
            if ("visual_scene".equals(parser.getName())) {
                // We assume there is only one <visual_scene> for now
                parseVisualScene(parser);
            }
        }
    }

    private void parseVisualScene(XmlPullParser parser) throws Exception {
        while (parser.next() != XmlPullParser.END_TAG || !parser.getName().equals("visual_scene")) {
            if (parser.getEventType() != XmlPullParser.START_TAG) continue;
            if ("node".equals(parser.getName())) {
                // This will recursively parse a top-level node and all its children.
                // A top-level node has no parent, so we pass null.
                Node topLevelNode = parseNode(parser, null);

                // --- THIS IS THE CORRECT FIX ---
                // Add every top-level node we find directly to our list of roots.
                rootNodes.add(topLevelNode);
                // --- END OF FIX ---
            }
        }
    }

    private Node parseNode(XmlPullParser parser, Node parent) throws Exception {
        String nodeId = parser.getAttributeValue(null, "id");
        Node currentNode = new Node(nodeId);
        currentNode.setParent(parent);

        // --- NEW LOGIC: PARSE TRANSFORMATIONS ---
        float[] finalMatrix = new float[16];
        Matrix.setIdentityM(finalMatrix, 0); // Start with an identity matrix

        List<Transform> transforms = new ArrayList<>();
        // --- END NEW LOGIC ---

        while (parser.next() != XmlPullParser.END_TAG || !parser.getName().equals("node")) {
            if (parser.getEventType() != XmlPullParser.START_TAG) continue;

            String tagName = parser.getName();
            switch (tagName) {
                case "translate": {
                    String[] values = parser.nextText().trim().split("\\s+");
                    float x = Float.parseFloat(values[0]);
                    float y = Float.parseFloat(values[1]);
                    float z = Float.parseFloat(values[2]);
                    float[] translationMatrix = new float[16];
                    Matrix.setIdentityM(translationMatrix, 0);
                    Matrix.translateM(translationMatrix, 0, x, y, z);
                    Matrix.multiplyMM(finalMatrix, 0, translationMatrix, 0, finalMatrix, 0);
                    break;
                }
                case "rotate": {
                    String[] values = parser.nextText().trim().split("\\s+");
                    float x = Float.parseFloat(values[0]);
                    float y = Float.parseFloat(values[1]);
                    float z = Float.parseFloat(values[2]);
                    float angle = Float.parseFloat(values[3]);
                    float[] rotationMatrix = new float[16];
                    Matrix.setIdentityM(rotationMatrix, 0);
                    Matrix.setRotateM(rotationMatrix, 0, angle, x, y, z);
                    Matrix.multiplyMM(finalMatrix, 0, rotationMatrix, 0, finalMatrix, 0);
                    break;
                }
                case "scale": {
                    String[] values = parser.nextText().trim().split("\\s+");
                    float x = Float.parseFloat(values[0]);
                    float y = Float.parseFloat(values[1]);
                    float z = Float.parseFloat(values[2]);
                    float[] scaleMatrix = new float[16];
                    Matrix.setIdentityM(scaleMatrix, 0);
                    Matrix.scaleM(scaleMatrix, 0, x, y, z);
                    Matrix.multiplyMM(finalMatrix, 0, scaleMatrix, 0, finalMatrix, 0);
                    break;
                }
                case "matrix": {
                    String[] values = parser.nextText().trim().split("\\s+");
                    float[] matrix = new float[16];
                    for (int i=0; i<16; i++) matrix[i] = Float.parseFloat(values[i]);
                    // COLLADA matrices are column-major, Android's are too. But we need to transpose
                    // when reading from the file because of the order.
                    float[] transposedMatrix = new float[16];
                    Matrix.transposeM(transposedMatrix, 0, matrix, 0);
                    Matrix.multiplyMM(finalMatrix, 0, transposedMatrix, 0, finalMatrix, 0);
                    break;
                }
                case "instance_geometry":
                case "instance_controller": {
                    String url = parser.getAttributeValue(null, "url");
                    if (url != null && url.startsWith("#")) {
                        String instanceId = url.substring(1);
                        if ("instance_geometry".equals(tagName)) {
                            currentNode.setInstanceGeometryId(instanceId);
                        } else {
                            currentNode.setInstanceControllerId(instanceId);
                        }
                        Log.d(TAG, "Node '" + nodeId + "' instances '" + instanceId + "'");
                    }
                    // Skip the inner <bind_material> for now
                    skipToEnd(parser, tagName);
                    break;
                }
                case "node": {
                    Node childNode = parseNode(parser, currentNode);
                    currentNode.addChild(childNode);
                    break;
                }
                default:
                    // Skip other tags like <instance_light>
                    skipToEnd(parser, tagName);
                    break;
            }
        }

        currentNode.setTransform(finalMatrix);
        nodeLibrary.put(nodeId, currentNode);
        return currentNode;
    }

    // Add this small helper method to skip unused tags cleanly
    private void skipToEnd(XmlPullParser parser, String tagName) throws Exception {
        while (parser.next() != XmlPullParser.END_TAG || !parser.getName().equals(tagName)) {
            // do nothing
        }
    }


    // Change the getter to return the LIST of nodes
    public List<Node> getRootNodes() {
        return rootNodes;
    }


// --- ALSO, ADD THIS GETTER METHOD TO THE END OF ColladaParser.java ---

    public Map<String, Material> getMaterialLibrary() {
        return materialLibrary;
    }


    public Map<String, Geometry> getGeometryLibrary() {
        return geometryLibrary;
    }

    public Map<String, Controller> getControllerLibrary() {
        return controllerLibrary;
    }
}
