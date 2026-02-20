package org.the3deer.android_3d_model_engine.services.collada;

import android.opengl.Matrix;
import android.util.Log;

import org.the3deer.android_3d_model_engine.animation.Animation;
import org.the3deer.android_3d_model_engine.animation.JointTransform;
import org.the3deer.android_3d_model_engine.animation.KeyFrame;
import org.the3deer.android_3d_model_engine.services.collada.entities.Controller;
import org.the3deer.android_3d_model_engine.services.collada.entities.EffectData;
import org.the3deer.android_3d_model_engine.services.collada.entities.Geometry;
import org.the3deer.android_3d_model_engine.services.collada.entities.MaterialData;
import org.the3deer.android_3d_model_engine.services.collada.entities.Mesh;
import org.the3deer.android_3d_model_engine.services.collada.entities.Node;
import org.the3deer.android_3d_model_engine.services.collada.entities.Skin;
import org.the3deer.android_3d_model_engine.services.collada.entities.Source;
import org.the3deer.android_3d_model_engine.services.collada.entities.Vertex;
import org.the3deer.android_3d_model_engine.services.collada.entities.VertexWeights;
import org.the3deer.android_3d_model_engine.util.HoleCutter;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class ColladaParser {

    private static final String TAG = ColladaParser.class.getSimpleName();

    // This will hold the parsed geometry data, keyed by their ID (e.g., "U3DMesh-GEOMETRY")
    private final Map<String, Geometry> geometryLibrary = new HashMap<>();

    // Add a new map at the top of the class to hold the parsed controllers
    private final Map<String, Controller> controllerLibrary = new HashMap<>();

    private final Map<String, MaterialData> materialLibrary = new HashMap<>();

    // Maps an effect ID to its full set of parsed data
    private final Map<String, EffectData> effectLibrary = new HashMap<>();
    private final Map<String, String> imageIdToFileNameMap = new HashMap<>();

    // Node library
    private final Map<String, Node> nodeLibrary = new HashMap<>();
    // ADD this new list to hold the top-level nodes of the active scene.
    private final List<Node> rootNodes = new ArrayList<>();

    // metadata
    private String authoringTool;

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

    private static class ChannelData {
        String targetNodeId;
        String targetTransform; // e.g., "matrix", "rotateZ.ANGLE", "translate.X"
        float[] times;
        float[] values;
        int stride; // Number of floats per value (1 for rotation, 3 for translation, 16 for matrix)

        ChannelData(String targetNodeId, String targetTransform, float[] times, float[] values, int stride) {
            this.targetNodeId = targetNodeId;
            this.targetTransform = targetTransform;
            this.times = times;
            this.values = values;
            this.stride = stride;
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
                    case "asset":
                        Log.d(TAG, "Found <asset>. Parsing...");
                        this.authoringTool = parseAsset(parser);
                        Log.i(TAG, "Authoring tool: " + this.authoringTool);
                        break;
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
                    case "library_animations":
                        Log.d(TAG, "Found <library_animations>. Parsing...");
                        parseAnimations(parser);
                        break;
                }
            }
            // Move to the next event in the XML file
            eventType = parser.next();
        }
        Log.i(TAG, "Finished parsing DAE file. Geometries found: " + geometryLibrary.size() + ", Nodes found: " + nodeLibrary.size() +
                ", Controllers found: " + controllerLibrary.size());
    }

    public String getAuthoringTool() {
        return authoringTool;
    }

    /**
     * Parses the <asset> block to find metadata, specifically the <authoring_tool>.
     * This is a direct port of the old loader's logic.
     * Assumes the parser is at the START_TAG of <asset>.
     *
     * @param parser The XmlPullParser instance.
     * @return The text content of the <authoring_tool> tag, or null if not found.
     * @throws Exception if parsing fails.
     */
    private String parseAsset(XmlPullParser parser) throws Exception {
        String foundAuthoringTool = null;
        boolean assetParsed = false; // Flag to indicate we are done

        // Loop until we are at the end of the <asset> tag
        while (parser.next() != XmlPullParser.END_TAG || !parser.getName().equals("asset")) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }

            // We are looking for <contributor>
            if ("contributor".equals(parser.getName())) {
                // Now look for <authoring_tool> inside <contributor>
                while (parser.next() != XmlPullParser.END_TAG || !parser.getName().equals("contributor")) {
                    if (parser.getEventType() != XmlPullParser.START_TAG) {
                        continue;
                    }

                    if ("authoring_tool".equals(parser.getName())) {
                        // Found it. Get the text.
                        foundAuthoringTool = parser.nextText();
                        // We found what we came for. We can exit all loops.
                        assetParsed = true;
                        break; // Exit the inner loop
                    } else {
                        // It's a tag inside <contributor> we don't care about, so skip it.
                        skipTag(parser);
                    }
                }
            } else {
                // It's a tag inside <asset> we don't care about, so skip it.
                skipTag(parser);
            }
        }

        return foundAuthoringTool;
    }

    // You will also need this helper method if you don't have it already.
    private void skipTag(XmlPullParser parser) throws Exception {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
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
                        //parsePolylistPrimitive(parser, sources, verticesLibrary, geometry);
                    } else if ("polygons".equals(meshTagName)) {
                        parsePolygonsPrimitive(parser, sources, verticesLibrary, geometry);
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
        if (!geometry.getMeshes().isEmpty()) {
            Log.d(TAG, "Geometry '" + geometryId + "' found. Meshes: " + geometry.getMeshes().size() + ".");
            geometry.assemble();
            geometryLibrary.put(geometryId, geometry);
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
     * Parses a <polygons> primitive, which may contain standard polygons <p>
     * or polygons with holes <ph>.
     */
    private void parsePolygonsPrimitive(XmlPullParser parser, Map<String, Source> sources,
                                        Map<String, List<Input>> verticesLibrary, Geometry geometry) throws Exception {

        List<Input> inputs = new ArrayList<>();
        String materialId = parser.getAttributeValue(null, "material");
        int count = Integer.parseInt(parser.getAttributeValue(null, "count"));

        final Mesh mesh = new Mesh();
        mesh.setId(parser.getName());
        mesh.setMaterialId(materialId);

        // 1. Parse Inputs (Semantics)
        int primitiveStartDepth = parser.getDepth();
        while (parser.next() != XmlPullParser.END_TAG || parser.getDepth() > primitiveStartDepth) {
            if (parser.getEventType() != XmlPullParser.START_TAG) continue;

            if ("input".equals(parser.getName())) {
                inputs.add(parseInput(parser));
            } else if ("ph".equals(parser.getName()) || "p".equals(parser.getName())) {
                // We found data, break input parsing loop to process data
                break;
            }
        }

        // Resolve inputs (Vertex, Normal, TexCoord, etc.)
        // This is the same logic as parseMeshPrimitive uses
        Source positionSource = null;
        Source normalSource = null;
        Source texCoordSource = null;
        Source colorSource = null;

        int vertexOffset = -1, normalOffset = -1, texOffset = -1, colorOffset = -1;
        int stride = 0;

        for (Input input : inputs) {
            int offset = input.offset;
            stride = Math.max(stride, offset + 1);

            if ("VERTEX".equals(input.semantic)) {
                vertexOffset = offset;
                // Look up the <vertices> tag to find the actual POSITION source
                List<Input> vertexInputs = verticesLibrary.get(input.sourceId);
                if (vertexInputs != null) {
                    for (Input vInput : vertexInputs) {
                        if ("POSITION".equals(vInput.semantic)) {
                            positionSource = sources.get(vInput.sourceId);
                        }
                    }
                }
            } else if ("NORMAL".equals(input.semantic)) {
                normalOffset = offset;
                normalSource = sources.get(input.sourceId);
            } else if ("TEXCOORD".equals(input.semantic)) {
                texOffset = offset;
                texCoordSource = sources.get(input.sourceId);
            } else if ("COLOR".equals(input.semantic)) {
                colorOffset = offset;
                colorSource = sources.get(input.sourceId);
            }
        }

        // Prepare lists to hold the unrolled data
        List<Float> unrolledPositions = new ArrayList<>();
        List<Float> unrolledNormals = new ArrayList<>();
        List<Float> unrolledTexCoords = new ArrayList<>();
        List<Float> unrolledColors = new ArrayList<>();

        // 2. Process <p> and <ph> tags
        // The parser is currently at the first <p> or <ph> tag from the previous loop
        do {
            if (parser.getEventType() == XmlPullParser.START_TAG) {
                if ("p".equals(parser.getName())) {
                    // Standard Polygon
                    String indexData = parser.nextText();
                    String[] pStrings = indexData.trim().split("\\s+");
                    int[] indices = null;
                    indices = new int[pStrings.length];
                    for (int i = 0; i < pStrings.length; i++) {
                        indices[i] = Integer.parseInt(pStrings[i]);
                    }
                } else if ("ph".equals(parser.getName())) {
                    // Polygon with Holes
                     processPolygonWithHoles(parser, stride, vertexOffset, normalOffset, texOffset, colorOffset,
                            positionSource, normalSource, texCoordSource, colorSource,
                            unrolledPositions, unrolledNormals, unrolledTexCoords, unrolledColors);
                }
            }
            // Advance to next tag
            if (parser.next() == XmlPullParser.END_TAG && "polygons".equals(parser.getName())) {
                break;
            }
        } while (parser.getDepth() >= primitiveStartDepth);


        // 3. Commit data to Geometry
        mesh.setVertices(floatListToArray(unrolledPositions));
        if (unrolledNormals.size() > 0) mesh.setNormals(floatListToArray(unrolledNormals));
        if (unrolledTexCoords.size() > 0) mesh.setTextureCoords(floatListToArray(unrolledTexCoords));
        if (unrolledColors.size() > 0) mesh.setColors(floatListToArray(unrolledColors));

        // Add the mesh to the geometry
        geometry.addMesh(mesh);

        Log.d(TAG, "Parsed <polygons> primitive with " + (unrolledPositions.size() / 3) + " vertices.");
    }

    private float[] floatListToArray(List<Float> unrolledPositions) {
        float[] primitiveFloatArray = new float[unrolledPositions.size()];
        for (int i = 0; i < unrolledPositions.size(); i++) {
            primitiveFloatArray[i] = unrolledPositions.get(i);
        }
        return primitiveFloatArray;
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
        int[] vcount = null;
        String primitiveName = parser.getName(); // Either "triangles" or "polylist"

        final Mesh mesh = new Mesh();
        mesh.setId(primitiveName+"#"+parser.getLineNumber());

        // Read the 'material' attribute from the <polylist> or <triangles> tag.
        String materialId = parser.getAttributeValue(null, "material");
        if (materialId != null) {
            mesh.setMaterialId(materialId);
            Log.d(TAG, "Bound material '" + materialId + "' to geometry '" + mesh.getId() + "'");
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
                    String vcountData = parser.nextText();
                    String[] vStrings = vcountData.trim().split("\\s+");
                    vcount = new int[vStrings.length];
                    for (int i = 0; i < vStrings.length; i++) {
                        vcount[i] = Integer.parseInt(vStrings[i]);
                    }
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

        final int inputCount = inputs.size();
        final int vertexCountFromIndices = indices.length / inputCount;

        // Parse 'count'
        final String countAtr = parser.getAttributeValue(null, "count");
        final int vertexCount = (countAtr != null) ? Integer.parseInt(countAtr) : -1;

        // check
        if (vertexCountFromIndices != vertexCount) {
            Log.w(TAG, "WARNING: vertexCountFromIndices <> vertexCount. This may indicate an issue with the input data or how we are interpreting it." +
                    "vertexCountFromIndices=" + vertexCountFromIndices + ", vertexCount=" + vertexCount);
        }

        // Allocate final "unrolled" buffers
        float[] finalIndices = new float[vertexCountFromIndices];
        float[] finalPositions = new float[vertexCountFromIndices * 3];
        float[] finalNormals = (normalSource != null) ? new float[vertexCountFromIndices * 3] : null;
        float[] finalTexCoords = (texCoordSource != null) ? new float[vertexCountFromIndices * 2] : null;
        int[] indicesMap = new int[vertexCountFromIndices];

        // Final buffer is ALWAYS RGBA (stride 4) to match the legacy loader and renderer expectations
        float[] finalColors = new float[vertexCountFromIndices * 4];

        // Unroll all vertex attributes into the final buffers
        for (int i = 0; i < vertexCountFromIndices; i++) {
            int p_base = i * inputCount;

            int positionIndex = indices[p_base + vertexInput.offset];
            int normalIndex = (normalInput != null) ? indices[p_base + normalInput.offset] : -1;
            int texCoordIndex = (texCoordInput != null) ? indices[p_base + texCoordInput.offset] : -1;
            int colorIndex = (colorInput != null) ? indices[p_base + colorInput.offset] : -1;

            finalIndices[i] = i;
            indicesMap[i] = positionIndex;

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
        mesh.setVertices(finalPositions);
        if (finalNormals != null) {
            mesh.setNormals(finalNormals);
        }
        if (finalTexCoords != null) {
            mesh.setTextureCoords(finalTexCoords);
        }
        if (colorSource != null) {
            mesh.setColors(finalColors); // Always set the RGBA color buffer
        }
        mesh.setIndices(indices); // Save the skinning map
        mesh.setIndicesMap(indicesMap); // Save the skinning map

        // Add the mesh to the geometry
        geometry.addMesh(mesh);

        Log.d(TAG, "Assembled unrolled geometry '" + mesh.getId() + "' with " + vertexCountFromIndices + " vertices.");
    }

    /**
     * Parses a <polylist> primitive.
     * Crucial for the Iris model because it uses Quads (vcount=4), not Triangles.
     */
    private void parsePolylistPrimitive(XmlPullParser parser, Map<String, Source> sources,
                                        Map<String, List<Input>> verticesLibrary, Geometry geometry) throws Exception {

        final Mesh mesh = new Mesh();

        List<Input> inputs = new ArrayList<>();
        String materialId = parser.getAttributeValue(null, "material");
        if (materialId != null) mesh.setMaterialId(materialId);

        // 1. Parse Inputs (Semantics)
        int primitiveStartDepth = parser.getDepth();
        while (parser.next() != XmlPullParser.END_TAG || parser.getDepth() > primitiveStartDepth) {
            if (parser.getEventType() != XmlPullParser.START_TAG) continue;

            if ("input".equals(parser.getName())) {
                inputs.add(parseInput(parser));
            } else if ("vcount".equals(parser.getName()) || "p".equals(parser.getName())) {
                break; // Stop input parsing, we hit data
            }
        }

        // Resolve inputs (Vertex, Normal, TexCoord, etc.)
        Source positionSource = null;
        Source normalSource = null;
        Source texCoordSource = null;
        Source colorSource = null;
        Source jointSource = null;
        Source weightSource = null;

        int vertexOffset = -1, normalOffset = -1, texOffset = -1, colorOffset = -1, jointOffset = -1, weightOffset = -1;
        int stride = 0;

        for (Input input : inputs) {
            int offset = input.offset;
            stride = Math.max(stride, offset + 1);

            if ("VERTEX".equals(input.semantic)) {
                vertexOffset = offset;
                List<Input> vertexInputs = verticesLibrary.get(input.sourceId);
                if (vertexInputs != null) {
                    for (Input vInput : vertexInputs) {
                        if ("POSITION".equals(vInput.semantic)) {
                            positionSource = sources.get(vInput.sourceId);
                        }
                    }
                }
            } else if ("NORMAL".equals(input.semantic)) {
                normalOffset = offset;
                normalSource = sources.get(input.sourceId);
            } else if ("TEXCOORD".equals(input.semantic)) {
                texOffset = offset;
                texCoordSource = sources.get(input.sourceId);
            } else if ("COLOR".equals(input.semantic)) {
                colorOffset = offset;
                colorSource = sources.get(input.sourceId);
            } else if ("JOINT".equals(input.semantic)) {
                jointOffset = offset;
                jointSource = sources.get(input.sourceId);
            } else if ("WEIGHT".equals(input.semantic)) {
                weightOffset = offset;
                weightSource = sources.get(input.sourceId);
            }
        }

        List<Integer> vCounts = new ArrayList<>();
        List<Integer> rawIndices = new ArrayList<>();

        // 2. Parse Data Tags (<vcount> and <p>)
        do {
            if (parser.getEventType() == XmlPullParser.START_TAG) {
                if ("vcount".equals(parser.getName())) {
                    String[] data = parser.nextText().trim().split("\\s+");
                    for (String s : data) vCounts.add(Integer.parseInt(s));
                } else if ("p".equals(parser.getName())) {
                    String[] data = parser.nextText().trim().split("\\s+");
                    for (String s : data) rawIndices.add(Integer.parseInt(s));
                }
            }
            if (parser.next() == XmlPullParser.END_TAG && "polylist".equals(parser.getName())) {
                break;
            }
        } while (parser.getDepth() >= primitiveStartDepth);

        // 3. Process Indices (Triangulation)
        List<Float> unrolledPositions = new ArrayList<>();
        List<Float> unrolledNormals = new ArrayList<>();
        List<Float> unrolledTexCoords = new ArrayList<>();
        List<Float> unrolledColors = new ArrayList<>();
        List<Integer> unrolledJoints = new ArrayList<>();
        List<Float> unrolledWeights = new ArrayList<>();

        int[] indicesArray = new int[rawIndices.size()];
        for (int i = 0; i < rawIndices.size(); i++) indicesArray[i] = rawIndices.get(i);

        int currentRawIndex = 0;

        // Iterate over every polygon face defined in vcount
        for (int i = 0; i < vCounts.size(); i++) {
            int vertexCount = vCounts.get(i); // e.g., 4 for a Quad

            // TRIANGULATION LOGIC (Triangle Fan)
            // A Quad (0,1,2,3) becomes Triangle(0,1,2) and Triangle(0,2,3)
            for (int k = 0; k < vertexCount - 2; k++) {
                // Vertex 0 (Pivot)
                addVertex(currentRawIndex, indicesArray, stride, vertexOffset, normalOffset, texOffset, colorOffset, jointOffset, weightOffset,
                        positionSource, normalSource, texCoordSource, colorSource, jointSource, weightSource,
                        unrolledPositions, unrolledNormals, unrolledTexCoords, unrolledColors, unrolledJoints, unrolledWeights);

                // Vertex k+1
                addVertex(currentRawIndex + k + 1, indicesArray, stride, vertexOffset, normalOffset, texOffset, colorOffset, jointOffset, weightOffset,
                        positionSource, normalSource, texCoordSource, colorSource, jointSource, weightSource,
                        unrolledPositions, unrolledNormals, unrolledTexCoords, unrolledColors, unrolledJoints, unrolledWeights);

                // Vertex k+2
                addVertex(currentRawIndex + k + 2, indicesArray, stride, vertexOffset, normalOffset, texOffset, colorOffset, jointOffset, weightOffset,
                        positionSource, normalSource, texCoordSource, colorSource, jointSource, weightSource,
                        unrolledPositions, unrolledNormals, unrolledTexCoords, unrolledColors, unrolledJoints, unrolledWeights);
            }

            // Advance pointer by the number of vertices in this polygon
            currentRawIndex += vertexCount;
        }

        // 4. Commit to Geometry
        mesh.setVertices(floatListToArray(unrolledPositions));
        mesh.setIndices(indicesArray);
        if (!unrolledNormals.isEmpty()) mesh.setNormals(floatListToArray(unrolledNormals));
        if (!unrolledTexCoords.isEmpty())
            mesh.setTextureCoords(floatListToArray(unrolledTexCoords));
        if (!unrolledColors.isEmpty()) mesh.setColors(floatListToArray(unrolledColors));

        geometry.addMesh(mesh);
    }

    // Helper to unroll a single vertex from raw indices
    private void addVertex(int vertexIndexInPoly, int[] indices, int stride,
                           int vertexOffset, int normalOffset, int texOffset, int colorOffset, int jointOffset, int weightOffset,
                           Source posSrc, Source normSrc, Source texSrc, Source colSrc, Source jointSrc, Source weightSrc,
                           List<Float> outPos, List<Float> outNorm, List<Float> outTex, List<Float> outCol, List<Integer> outJoints, List<Float> outWeights) {

        int baseIndex = vertexIndexInPoly * stride;

        // Position
        int pIdx = indices[baseIndex + vertexOffset];
        outPos.add(posSrc.floatData[pIdx * 3]);
        outPos.add(posSrc.floatData[pIdx * 3 + 1]);
        outPos.add(posSrc.floatData[pIdx * 3 + 2]);

        // Normal
        if (normalOffset >= 0 && normSrc != null) {
            int nIdx = indices[baseIndex + normalOffset];
            outNorm.add(normSrc.floatData[nIdx * 3]);
            outNorm.add(normSrc.floatData[nIdx * 3 + 1]);
            outNorm.add(normSrc.floatData[nIdx * 3 + 2]);
        }
        // TexCoord
        if (texOffset >= 0 && texSrc != null) {
            int tIdx = indices[baseIndex + texOffset];
            outTex.add(texSrc.floatData[tIdx * 2]);
            outTex.add(texSrc.floatData[tIdx * 2 + 1]);
        }
        // Color
        if (colorOffset >= 0 && colSrc != null) {
            int cIdx = indices[baseIndex + colorOffset];
            outCol.add(colSrc.floatData[cIdx * 3]);
            outCol.add(colSrc.floatData[cIdx * 3 + 1]);
            outCol.add(colSrc.floatData[cIdx * 3 + 2]);
            if (colSrc.stride >= 4) outCol.add(colSrc.floatData[cIdx * 3 + 3]);
            else outCol.add(1.0f);
        }
        // Joints & Weights (for completeness, though Iris doesn't use them)
        if (jointOffset >= 0 && jointSrc != null) {
            outJoints.add(indices[baseIndex + jointOffset]);
        }
        if (weightOffset >= 0 && weightSrc != null) {
            outWeights.add(weightSrc.floatData[indices[baseIndex + weightOffset]]);
        }
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
    // In ColladaParser.java, REPLACE the entire parseLibraryEffects method with this one.

    /**
     * Parses the <library_effects> section. This is a deep parsing method that extracts
     * diffuse color, transparency, and texture references from an <effect>.
     */
    private void parseLibraryEffects(XmlPullParser parser) throws Exception {
        while (parser.next() != XmlPullParser.END_TAG || !parser.getName().equals("library_effects")) {
            if (parser.getEventType() != XmlPullParser.START_TAG || !parser.getName().equals("effect")) {
                continue;
            }
            String effectId = parser.getAttributeValue(null, "id");
            EffectData currentEffect = new EffectData(effectId);

            // Navigate into profile_COMMON
            while (parser.next() != XmlPullParser.END_TAG || !parser.getName().equals("effect")) {
                if (parser.getEventType() != XmlPullParser.START_TAG) continue;

                if ("profile_COMMON".equals(parser.getName())) {
                    parseEffectProfile(parser, currentEffect);
                }
            }

            if (effectId != null) {
                effectLibrary.put(effectId, currentEffect);
                Log.d(TAG, "Parsed Effect '" + effectId + "' with texture image ID '" + currentEffect.imageId + "'");
            }
        }
    }

    /**
     * Parses the <profile_COMMON> tag within an effect to find technique, newparam, etc.
     */
    private void parseEffectProfile(XmlPullParser parser, EffectData currentEffect) throws Exception {

        // This map is the KEY. It stores SID -> referenced ID.
        // e.g., "character_Texture_png-sampler" -> "character_Texture_png-surface"
        // e.g., "character_Texture_png-surface" -> "character_Texture_png"
        Map<String, String> newparamLinks = new HashMap<>();

        while (parser.next() != XmlPullParser.END_TAG || !parser.getName().equals("profile_COMMON")) {
            if (parser.getEventType() != XmlPullParser.START_TAG) continue;

            String tagName = parser.getName();
            if ("newparam".equals(tagName)) {
                // Parse the <newparam> and add its link to our map.
                parseNewparam(parser, newparamLinks);
            } else if ("technique".equals(tagName)) {
                // Pass the linking map to the technique parser.
                parseTechnique(parser, currentEffect, newparamLinks);
            } else {
                skipTag(parser);
            }
        }
    }

    /**
     * A new helper to parse a <newparam> block and update the linking map.
     * @param parser The XML parser, at the start of a <newparam> tag.
     * @param links The map to add the new link to.
     * @throws Exception
     */
    private void parseNewparam(XmlPullParser parser, Map<String, String> links) throws Exception {
        String paramSid = parser.getAttributeValue(null, "sid");
        String referencedId = null;

        while (parser.next() != XmlPullParser.END_TAG || !parser.getName().equals("newparam")) {
            if (parser.getEventType() != XmlPullParser.START_TAG) continue;

            if ("surface".equals(parser.getName())) {
                // This is a surface parameter, it points to an image ID
                while (parser.next() != XmlPullParser.END_TAG || !parser.getName().equals("surface")) {
                    if (parser.getEventType() == XmlPullParser.START_TAG && "init_from".equals(parser.getName())) {
                        referencedId = parser.nextText();
                        break;
                    }
                }
            } else if ("sampler2D".equals(parser.getName())) {
                // This is a sampler parameter, it points to a surface SID
                while (parser.next() != XmlPullParser.END_TAG || !parser.getName().equals("sampler2D")) {
                    if (parser.getEventType() == XmlPullParser.START_TAG && "source".equals(parser.getName())) {
                        referencedId = parser.nextText();
                        break;
                    }
                }
            }
        }

        if (paramSid != null && referencedId != null) {
            links.put(paramSid, referencedId);
            Log.d(TAG, "Newparam link created: '" + paramSid + "' -> '" + referencedId + "'");
        }
    }


    // In ColladaParser.java
// ADD this new parseTechnique method.


    // In ColladaParser.java
// ADD this new method to handle the contents of <phong>, <lambert>, etc.


// In ColladaParser.java
// ADD this small helper method.

    // --- PASTE THIS NEW HELPER METHOD INTO ColladaParser.java ---

    /**
     * Parses a material property tag (like <diffuse>) that can contain either a <color>
     * or a <texture>. If a <texture> is found, it resolves the texture's image ID. If a
     * <color> is found, it parses the float array and sets the appropriate field in the EffectData.
     *
     * @param parser The XML parser, at the start of the property tag (e.g., <diffuse>).
     * @param currentEffect The EffectData object to be populated.
     * @param newparamLinks The map for resolving texture indirections.
     * @return The final resolved image ID if a texture was found, otherwise null.
     * @throws Exception If there is a parsing error.
     */
    private String parseTextureOrColor(XmlPullParser parser, EffectData currentEffect, Map<String, String> newparamLinks) throws Exception {
        final String propertyName = parser.getName(); // e.g., "diffuse", "specular"
        String imageId = null;

        while (parser.next() != XmlPullParser.END_TAG || !parser.getName().equals(propertyName)) {
            if (parser.getEventType() != XmlPullParser.START_TAG) continue;

            String tagName = parser.getName();

            if ("texture".equals(tagName)) {
                // --- TEXTURE LOGIC (similar to your old parseTextureProperty) ---
                String initialTextureId = parser.getAttributeValue(null, "texture");
                if (initialTextureId != null) {
                    // Resolve the full chain of links (sampler -> surface -> image)
                    String currentId = initialTextureId;
                    int depth = 0; // Safety break for rare infinite loops
                    while (newparamLinks.containsKey(currentId) && depth < 5) {
                        currentId = newparamLinks.get(currentId);
                        depth++;
                    }
                    imageId = currentId; // This is the final image ID
                }
                skipTag(parser); // Skip to the end of the <texture> tag

            } else if ("color".equals(tagName)) {
                // --- COLOR LOGIC (the new part) ---
                try {
                    String colorString = parser.nextText();
                    if (colorString != null) {
                        String[] parts = colorString.trim().split("\\s+");
                        if (parts.length >= 3) { // Must have at least R, G, B
                            float[] color = new float[4];
                            color[0] = Float.parseFloat(parts[0]);
                            color[1] = Float.parseFloat(parts[1]);
                            color[2] = Float.parseFloat(parts[2]);
                            color[3] = (parts.length > 3) ? Float.parseFloat(parts[3]) : 1.0f; // Default alpha to 1.0

                            // Set the color on the correct property based on the parent tag name
                            if ("diffuse".equals(propertyName)) {
                                currentEffect.diffuseColor = color;
                            } else if ("specular".equals(propertyName)) {
                                currentEffect.specularColor = color;
                            } else if ("emission".equals(propertyName)) {
                                currentEffect.emissionColor = color;
                            } else if ("ambient".equals(propertyName)) {
                                currentEffect.ambientColor = color;
                            }
                        }
                    }
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Failed to parse color string inside <" + propertyName + ">", e);
                }
                // parser.nextText() already moved the cursor past the </color> tag
            } else {
                // In case of other unexpected tags inside <diffuse>, etc.
                skipTag(parser);
            }
        }
        // The parser is now at the END_TAG of the property (e.g., </diffuse>)
        return imageId;
    }


    // In ColladaParser.java
// Modify parseTechnique to accept and pass the map.

    private void parseTechnique(XmlPullParser parser, EffectData currentEffect, Map<String, String> newparamLinks) throws Exception {
        while (parser.next() != XmlPullParser.END_TAG || !parser.getName().equals("technique")) {
            if (parser.getEventType() != XmlPullParser.START_TAG) continue;

            String shaderType = parser.getName();
            if ("phong".equals(shaderType) || "lambert".equals(shaderType) || "blinn".equals(shaderType)) {
                // Pass the map down one more level
                parseShaderType(parser, currentEffect, shaderType, newparamLinks);
            } else {
                skipTag(parser);
            }
        }
    }


    // In ColladaParser.java, replace the entire parseShaderType method with this one.
    private void parseShaderType(XmlPullParser parser, EffectData currentEffect, String shaderType, Map<String, String> newparamLinks) throws Exception {
        while (parser.next() != XmlPullParser.END_TAG || !parser.getName().equals(shaderType)) {
            if (parser.getEventType() != XmlPullParser.START_TAG) continue;

            String propertyName = parser.getName();
            if ("diffuse".equals(propertyName)) {
                // First, try to see if the diffuse property is defined by a texture.
                String imageId = parseTextureOrColor(parser, currentEffect, newparamLinks);
                if (imageId != null) {
                    currentEffect.imageId = imageId; // Set the final image ID
                }
                // If parseTextureOrColor found a color instead, currentEffect.diffuseColor is already set.

            } else if ("transparency".equals(propertyName)){
                while (parser.next() != XmlPullParser.END_TAG || !parser.getName().equals("transparency")) {
                    if (parser.getEventType() != XmlPullParser.START_TAG) continue;

                    if ("float".equals(parser.getName())) {
                        try {
                            String floatString = parser.nextText();
                            // The transparency value in COLLADA (0=opaque, 1=transparent) is the
                            // inverse of what is commonly used for alpha in OpenGL (1=opaque, 0=transparent).
                            // We calculate the alpha value here.
                            float transparency = Float.parseFloat(floatString);
                            currentEffect.transparency = transparency; // Assuming you have a setter for this
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to parse <float> for transparency", e);
                        }
                    } else {
                        skipTag(parser);
                    }
                }
            } else if ("specular".equals(propertyName)) {
                // The same logic can be applied for specular or other properties.
                String textureId = parseTextureOrColor(parser, currentEffect, newparamLinks);
                if (textureId != null) {
                    currentEffect.specularTextureId = textureId;
                }
            } else if ("emission".equals(propertyName) || "ambient".equals(propertyName)) {
                // Example of handling other color properties if needed in the future.
                parseTextureOrColor(parser, currentEffect, newparamLinks);
            }
            else {
                skipTag(parser);
            }
        }
    }

    /**
     * Parses the <library_materials> section. This creates the final Material objects
     * by connecting a material ID to an effect ID, and then resolving the texture file.
     */
    // In ColladaParser.java, REPLACE the method at your caret with this.

    /**
     * Parses the <library_materials> section. This creates the final Material objects
     * by connecting a material ID to a pre-parsed effect, and then resolving the texture file.
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

                // Create the material
                MaterialData material = new MaterialData(materialId);
                material.effectId = cleanId(effectUrl);

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
        String nodeName = parser.getAttributeValue(null, "name");
        Node currentNode = new Node(nodeId, nodeName);
        currentNode.setParent(parent);

        // --- NEW LOGIC: PARSE TRANSFORMATIONS ---
        float[] finalMatrix = new float[16];
        Matrix.setIdentityM(finalMatrix, 0); // Start with an identity matrix

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
                    Matrix.multiplyMM(finalMatrix, 0, finalMatrix, 0, translationMatrix, 0);
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
                    Matrix.multiplyMM(finalMatrix, 0, finalMatrix, 0, rotationMatrix, 0);
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
                    for (int i = 0; i < 16; i++) matrix[i] = Float.parseFloat(values[i]);
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
                    if (url == null) {
                        Log.w(TAG, "Instance tag <" + tagName + "> is missing a 'url' attribute for node '" + currentNode.getId() + "'.");
                        skipToEnd(parser, tagName);
                        break;
                    }

                    if ("instance_controller".equals(tagName)) {

                        Log.d(TAG, "Found <instance_controller> for node '" + currentNode.getId() + "' with url: " + url);
                        currentNode.setInstanceControllerId(cleanId(url));

                        String skinRootId = null; // Variable to hold the skeleton root ID

                        // Look inside <instance_controller> for the <skin> tag
                        while (parser.next() != XmlPullParser.END_TAG || !parser.getName().equals("instance_controller")) {
                            if (parser.getEventType() == XmlPullParser.START_TAG && "skeleton".equals(parser.getName())) {
                                // The content of the <skin> tag is the ID of the skeleton's root joint
                                skinRootId = parser.nextText(); // Get the ID (e.g., "#Torso")
                                Log.d(TAG, "Found <skeleton> tag with root ID: " + skinRootId);
                                // We don't break here because we still need to reach the end of the instance_controller tag
                            }
                        }

                        // Pass the newly found skin root ID to the Node DTO
                        if (skinRootId != null) {
                            currentNode.setSkinId(cleanId(skinRootId));
                        } else {
                            Log.w(TAG, "Incomplete <instance_controller> for node '" + currentNode.getId() + "'. Missing <skin> tag inside.");
                        }

                        // The while loop above already consumed the tag, so we are done.

                    } else { // This is an "instance_geometry"
                        Log.d(TAG, "Found <instance_geometry> for node '" + currentNode.getId() + "' with url: " + url);

                        // This is a static, non-skinned mesh. Just set the geometry ID.
                        currentNode.setInstanceGeometryId(cleanId(url));

                        // We still need to skip the rest of the tag (e.g., <bind_material>)
                        skipToEnd(parser, tagName);
                    }
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

    // In ColladaParser.java, replace the empty parseAnimations method

    /**
     * Parses the <library_animations> section and builds a list of Animation objects.
     */
    private void parseAnimations(XmlPullParser parser) throws Exception {
        List<ChannelData> allChannels = new ArrayList<>();

        while (parser.next() != XmlPullParser.END_TAG || !parser.getName().equals("library_animations")) {
            if (parser.getEventType() != XmlPullParser.START_TAG) continue;

            if ("animation".equals(parser.getName())) {
                // An <animation> tag can contain other <animation> tags (nested) or the actual channel data.
                parseAnimationTag(parser, allChannels);
            }
        }

        if (allChannels.isEmpty()) {
            Log.i(TAG, "No animation channels found.");
            return; // No animations to build
        }

        // --- Assemble the parsed channels into KeyFrame objects ---

        // 1. Find all unique timestamps across all channels
        TreeSet<Float> uniqueTimestamps = new TreeSet<>();
        for (ChannelData channel : allChannels) {
            for (float time : channel.times) {
                uniqueTimestamps.add(time);
            }
        }
        List<Float> keyTimes = new ArrayList<>(uniqueTimestamps);
        if (keyTimes.isEmpty()) {
            return;
        }


        // 2. Create a KeyFrame for each unique timestamp
        KeyFrame[] keyFrames = new KeyFrame[keyTimes.size()];
        for (int i = 0; i < keyTimes.size(); i++) {
            keyFrames[i] = new KeyFrame(keyTimes.get(i), new HashMap<>());
        }

        // 3. Populate each KeyFrame's pose map
        for (ChannelData channel : allChannels) {
            for (int i = 0; i < channel.times.length; i++) {
                float time = channel.times[i];
                int keyFrameIndex = keyTimes.indexOf(time);
                if (keyFrameIndex == -1) continue;

                KeyFrame keyFrame = keyFrames[keyFrameIndex];
                Map<String, JointTransform> pose = keyFrame.getPose();

                JointTransform jointTransform = pose.computeIfAbsent(channel.targetNodeId, k -> new JointTransform());

                // Apply the transformation from this channel
                applyTransform(jointTransform, channel, i);
            }
        }

        // 4. Create the final Animation object
        // For now, we create one animation clip containing all keyframes.
        float duration = keyTimes.get(keyTimes.size() - 1);
        Animation animation = new Animation("COLLADA_Animation", duration, keyFrames);
        animations.add(animation); // Assuming you have a `List<Animation> animations` field in the parser

        Log.i(TAG, "Successfully parsed 1 animation clip with " + keyFrames.length + " keyframes.");
    }

    /**
     * Recursively parses an <animation> tag. It can contain other <animation> tags
     * or the actual source/sampler/channel data for a single transformation.
     */
    private void parseAnimationTag(XmlPullParser parser, List<ChannelData> channels) throws Exception {
        // Look for nested <animation> tags first
        int startDepth = parser.getDepth();
        while (parser.next() != XmlPullParser.END_TAG || parser.getDepth() > startDepth) {
            if (parser.getEventType() != XmlPullParser.START_TAG) continue;

            if ("animation".equals(parser.getName())) {
                // It's a nested animation, recurse into it
                parseAnimationTag(parser, channels);
            } else if ("source".equals(parser.getName())) {
                // This <animation> tag contains the actual data. Let's parse it as a single channel.
                ChannelData channel = parseChannel(parser);
                if (channel != null) {
                    channels.add(channel);
                }
                // We've processed this as a data-holding animation, so we can break the loop.
                break;
            }
        }
    }

    /**
     * Parses an <animation> tag that is expected to contain source, sampler, and channel info.
     * The parser must be at the START_TAG of the first <source>.
     */
    private ChannelData parseChannel(XmlPullParser parser) throws Exception {
        Map<String, float[]> sources = new HashMap<>();
        String inputSourceId = null;
        String outputSourceId = null;
        String targetId = null;
        String targetTransform = null;
        int stride = 1;

        // The parser is already at the first <source>. Parse it and any subsequent ones.
        do {
            if (parser.getEventType() == XmlPullParser.START_TAG && "source".equals(parser.getName())) {
                String sourceId = parser.getAttributeValue(null, "id");
                float[] data = parseSourceData(parser);
                if (sourceId != null && data != null) {
                    sources.put(sourceId, data);
                }
            }
            parser.next();
        } while (parser.getEventType() != XmlPullParser.START_TAG || !"sampler".equals(parser.getName()));

        // Now at the <sampler> tag
        while (parser.next() != XmlPullParser.END_TAG || !parser.getName().equals("sampler")) {
            if (parser.getEventType() != XmlPullParser.START_TAG || !parser.getName().equals("input"))
                continue;
            String semantic = parser.getAttributeValue(null, "semantic");
            String sourceUrl = cleanId(parser.getAttributeValue(null, "source"));
            if ("INPUT".equals(semantic)) {
                inputSourceId = sourceUrl;
            } else if ("OUTPUT".equals(semantic)) {
                outputSourceId = sourceUrl;
            }
        }

        // Now find the <channel> tag
        while (parser.getEventType() != XmlPullParser.START_TAG || !"channel".equals(parser.getName())) {
            parser.next();
        }
        String target = parser.getAttributeValue(null, "target");
        if (target != null) {
            String[] parts = target.split("/");
            targetId = parts[0];
            if (parts.length > 1) {
                targetTransform = parts[1];
            }
        }

        // Find the stride from the output source's accessor
        for (Map.Entry<String, float[]> entry : sources.entrySet()) {
            if (entry.getKey().equals(outputSourceId)) {
                // This is a simplification; a full implementation would parse the <accessor>
                // For door.dae, the stride is always 1 for single-axis transforms.
                // For matrix, it would be 16. We will infer it from the target string.
                if ("matrix".equalsIgnoreCase(targetTransform)) {
                    stride = 16;
                } else {
                    stride = 1;
                }
            }
        }


        if (inputSourceId != null && outputSourceId != null && targetId != null) {
            float[] times = sources.get(inputSourceId);
            float[] values = sources.get(outputSourceId);
            return new ChannelData(targetId, targetTransform, times, values, stride);
        }

        return null;
    }

    /**
     * Parses a <source> tag and returns its float_array data.
     */
    private float[] parseSourceData(XmlPullParser parser) throws Exception {
        while (parser.next() != XmlPullParser.END_TAG || !parser.getName().equals("source")) {
            if (parser.getEventType() == XmlPullParser.START_TAG && "float_array".equals(parser.getName())) {
                String text = parser.nextText();
                String[] floatStrings = text.trim().split("\\s+");
                float[] data = new float[floatStrings.length];
                for (int i = 0; i < floatStrings.length; i++) {
                    data[i] = Float.parseFloat(floatStrings[i]);
                }
                return data;
            }
        }
        return null;
    }


    /**
     * Applies a single transformation from a channel to a JointTransform at a specific time index.
     */
    private void applyTransform(JointTransform jointTransform, ChannelData channel, int timeIndex) {
        float value = channel.values[timeIndex];

        switch (channel.targetTransform.toUpperCase()) {
            case "ROTATEZ.ANGLE":
            case "ROTATIONZ.ANGLE":
                jointTransform.addRotation(null, null, value);
                break;
            case "ROTATEY.ANGLE":
            case "ROTATIONY.ANGLE":
                jointTransform.addRotation(null, value, null);
                break;
            case "ROTATEX.ANGLE":
            case "ROTATIONX.ANGLE":
                jointTransform.addRotation(value, null, null);
                break;
            case "TRANSLATE.X":
            case "LOCATION.X":
                jointTransform.addLocation(value, null, null);
                break;
            case "TRANSLATE.Y":
            case "LOCATION.Y":
                jointTransform.addLocation(null, value, null);
                break;
            case "TRANSLATE.Z":
            case "LOCATION.Z":
                jointTransform.addLocation(null, null, value);
                break;
            case "SCALE":
                if (channel.stride == 3) {
                    float[] scale = new float[3];
                    System.arraycopy(channel.values, timeIndex * 3, scale, 0, 3);
                    jointTransform.setScale(scale);
                } else {
                    // Fallback for uniform scale if stride is 1 but target is generic SCALE
                    jointTransform.setScale(new float[]{value, value, value});
                }
                break;
            case "SCALE.X":
                jointTransform.addScale(value, null, null);
                break;
            case "SCALE.Y":
                jointTransform.addScale(null, value, null);
                break;
            case "SCALE.Z":
                jointTransform.addScale(null, null, value);
                break;
            case "MATRIX":
            case "TRANSFORM":
                float[] matrix = new float[16];
                System.arraycopy(channel.values, timeIndex * 16, matrix, 0, 16);
                float[] transposed = new float[16];
                Matrix.transposeM(transposed, 0, matrix, 0);
                jointTransform.setTransform(transposed); // This will overwrite others, as expected for a matrix.
                break;
            default:
                Log.w(TAG, "Unsupported animation transform target: " + channel.targetTransform);
                break;
        }
    }

    // Add this helper method somewhere within ColladaParser.java

    /**
     * Triangulates a polygon with holes.
     * This is where you will place your legacy triangulation logic.
     *
     * @param polygonLoop The indices for the main polygon's outer loop.
     * @param holes       A list of index arrays, where each array is a hole.
     * @return A list of integers representing the triangulated indices.
     */
    private List<Integer> triangulatePolygonWithHoles(List<Integer> polygonLoop, List<List<Integer>> holes) {
        //
        // --- THIS IS WHERE YOUR LEGACY TRIANGULATION LOGIC GOES ---
        //
        // For now, we will implement a simple fan triangulation of the main polygon
        // and ignore the holes, just to have a working structure.
        //
        Log.d(TAG, "Triangulating polygon with " + holes.size() + " holes. (NOTE: Holes are currently ignored)");
        List<Integer> triangles = new ArrayList<>();
        if (polygonLoop.size() < 3) {
            return triangles; // Not a valid polygon
        }

        // Simple fan triangulation from the first vertex
        int rootVertex = polygonLoop.get(0);
        for (int i = 1; i < polygonLoop.size() - 1; i++) {
            triangles.add(rootVertex);
            triangles.add(polygonLoop.get(i));
            triangles.add(polygonLoop.get(i + 1));
        }
        return triangles;
    }

    private void processPolygonWithHoles(XmlPullParser parser, int stride,
                                         int vertexOffset, int normalOffset, int texOffset, int colorOffset,
                                         Source posSrc, Source normSrc, Source texSrc, Source colSrc,
                                         List<Float> outPos, List<Float> outNorm, List<Float> outTex, List<Float> outCol) throws Exception {

        List<Vertex> localOuterBoundary = new ArrayList<>();
        List<List<Vertex>> localHoles = new ArrayList<>();

        // A <ph> contains one <p> and one or more <h>
        int phDepth = parser.getDepth();
        while(parser.next() != XmlPullParser.END_TAG || parser.getDepth() > phDepth) {
            if(parser.getEventType() != XmlPullParser.START_TAG) continue;

            if("p".equals(parser.getName())) {
                String[] indices = parser.nextText().trim().split("\\s+");
                localOuterBoundary = parseLoopIndices(indices, stride, vertexOffset, normalOffset, texOffset, colorOffset);
            } else if ("h".equals(parser.getName())) {
                String[] indices = parser.nextText().trim().split("\\s+");
                localHoles.add(parseLoopIndices(indices, stride, vertexOffset, normalOffset, texOffset, colorOffset));
            }
        }

        // Triangulate
        try {
            // 1. Extract 3D coords for triangulation logic
            List<float[]> outerLoopCoords = new ArrayList<>();
            for (Vertex v : localOuterBoundary) {
                // Get actual XYZ from source based on index
                float[] xyz = new float[] {
                        posSrc.floatData[v.getVertexIndex() * 3],
                        posSrc.floatData[v.getVertexIndex() * 3 + 1],
                        posSrc.floatData[v.getVertexIndex() * 3 + 2]
                };
                outerLoopCoords.add(xyz);
            }

            List<List<float[]>> holeLoopsCoords = new ArrayList<>();
            for (List<Vertex> hole : localHoles) {
                List<float[]> hCoords = new ArrayList<>();
                for (Vertex v : hole) {
                    float[] xyz = new float[] {
                            posSrc.floatData[v.getVertexIndex() * 3],
                            posSrc.floatData[v.getVertexIndex() * 3 + 1],
                            posSrc.floatData[v.getVertexIndex() * 3 + 2]
                    };
                    hCoords.add(xyz);
                }
                holeLoopsCoords.add(hCoords);
            }

            // 2. Call the legacy ear-clipping algorithm
            // Returns indices local to the combined list of (Outer + Hole1 + Hole2...)
            List<Integer> triangulatedIndices = HoleCutter.pierce(outerLoopCoords, holeLoopsCoords);

            // 3. Flatten all vertices into one list for easy indexing
            List<Vertex> allPolygonVertices = new ArrayList<>(localOuterBoundary);
            for(List<Vertex> hole : localHoles) {
                allPolygonVertices.addAll(hole);
            }

            // 4. Unroll based on triangulation results
            for (Integer localIndex : triangulatedIndices) {
                Vertex v = allPolygonVertices.get(localIndex);

                // Add Position (XYZ)
                int pIdx = v.getVertexIndex();
                outPos.add(posSrc.floatData[pIdx * 3]);
                outPos.add(posSrc.floatData[pIdx * 3 + 1]);
                outPos.add(posSrc.floatData[pIdx * 3 + 2]);

                // Add Normal
                if (normalOffset >= 0 && normSrc != null) {
                    int nIdx = v.getNormalIndex();
                    outNorm.add(normSrc.floatData[nIdx * 3]);
                    outNorm.add(normSrc.floatData[nIdx * 3 + 1]);
                    outNorm.add(normSrc.floatData[nIdx * 3 + 2]);
                }
                // Add Texture
                if (texOffset >= 0 && texSrc != null) {
                    int tIdx = v.getTextureIndex();
                    outTex.add(texSrc.floatData[tIdx * 2]);
                    outTex.add(texSrc.floatData[tIdx * 2 + 1]);
                }
                // Add Color
                if (colorOffset >= 0 && colSrc != null) {
                    int cIdx = v.getColorIndex();
                    outCol.add(colSrc.floatData[cIdx * 3]); // Assuming RGB
                    outCol.add(colSrc.floatData[cIdx * 3 + 1]);
                    outCol.add(colSrc.floatData[cIdx * 3 + 2]);
                    // Handle Alpha if stride is 4
                    if (colSrc.getStride() == 4) outCol.add(colSrc.floatData[cIdx * 3 + 3]);
                    else outCol.add(1.0f);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Triangulation failed", e);
        }
    }

    // Helper to parse a string array of indices into Vertex objects
    private List<Vertex> parseLoopIndices(String[] indices, int stride, int vOff, int nOff, int tOff, int cOff) {
        List<Vertex> loop = new ArrayList<>();
        for (int i = 0; i < indices.length; i += stride) {
            Vertex v = new Vertex(Integer.parseInt(indices[i + vOff]));
            if (nOff >= 0) v.setNormalIndex(Integer.parseInt(indices[i + nOff]));
            if (tOff >= 0) v.setTextureIndex(Integer.parseInt(indices[i + tOff]));
            if (cOff >= 0) v.setColorIndex(Integer.parseInt(indices[i + cOff]));
            loop.add(v);
        }
        return loop;
    }


    // You'll also need to add a List<Animation> field to your parser class
// and a getter for it.
    private List<Animation> animations = new ArrayList<>();

    public List<Animation> getAnimationLibrary() {
        return animations;
    }


    // Change the getter to return the LIST of nodes
    public List<Node> getRootNodes() {
        return rootNodes;
    }


// --- ALSO, ADD THIS GETTER METHOD TO THE END OF ColladaParser.java ---

    public Map<String, MaterialData> getMaterialLibrary() {
        return materialLibrary;
    }


    public Map<String, Geometry> getGeometryLibrary() {
        return geometryLibrary;
    }

    public Map<String, Controller> getControllerLibrary() {
        return controllerLibrary;
    }

    public Map<String, EffectData> getEffectLibrary() {
        return effectLibrary;
    }

    public Map<String, String> getImagesLibrary() {
        return imageIdToFileNameMap;
    }

    public Map<String, Node> getNodeLibrary() {
        return nodeLibrary;
    }
}
