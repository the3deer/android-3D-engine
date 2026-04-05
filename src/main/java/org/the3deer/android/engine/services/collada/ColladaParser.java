package org.the3deer.android.engine.services.collada;

import org.the3deer.android.engine.animation.Animation;
import org.the3deer.android.engine.animation.JointTransform;
import org.the3deer.android.engine.animation.KeyFrame;
import org.the3deer.android.engine.util.Matrix;
import org.the3deer.android.engine.services.collada.entities.Controller;
import org.the3deer.android.engine.services.collada.entities.EffectData;
import org.the3deer.android.engine.services.collada.entities.Geometry;
import org.the3deer.android.engine.services.collada.entities.MaterialData;
import org.the3deer.android.engine.services.collada.entities.Mesh;
import org.the3deer.android.engine.services.collada.entities.Node;
import org.the3deer.android.engine.services.collada.entities.Skin;
import org.the3deer.android.engine.services.collada.entities.Source;
import org.the3deer.android.engine.services.collada.entities.Vertex;
import org.the3deer.android.engine.services.collada.entities.VertexWeights;
import org.the3deer.android.engine.util.HoleCutter;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ColladaParser {

    private static final Logger logger = Logger.getLogger(ColladaParser.class.getSimpleName());

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
        private int set;  // For TEXCOORD sets (0, 1, 2, etc.) - defaults to 0

        private Input(String semantic, String sourceId, int offset) {
            this(semantic, sourceId, offset, 0);
        }

        private Input(String semantic, String sourceId, int offset, int set) {
            this.semantic = semantic;
            this.sourceId = sourceId;
            this.offset = offset;
            this.set = set;
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
                        logger.config("Found <asset>. Parsing...");
                        this.authoringTool = parseAsset(parser);
                        logger.info("Authoring tool: " + this.authoringTool);
                        break;
                    case "library_geometries":
                        logger.config("Found <library_geometries>. Parsing...");
                        parseGeometriesLibrary(parser);
                        logger.config("Finished parsing <library_geometries>.");
                        break;
                    // NEW CASE for controllers
                    case "library_controllers":
                        logger.config("Found <library_controllers>. Parsing...");
                        parseControllersLibrary(parser);
                        logger.config("Finished parsing <library_controllers>.");
                        break;
                    // --- ADD THESE NEW CASES ---
                    case "library_images":
                        logger.config("Found <library_images>. Parsing...");
                        parseLibraryImages(parser);
                        break;
                    case "library_effects":
                        logger.config("Found <library_effects>. Parsing...");
                        parseLibraryEffects(parser);
                        break;
                    case "library_materials":
                        logger.config("Found <library_materials>. Parsing...");
                        parseLibraryMaterials(parser);
                        break;
                    case "library_visual_scenes":
                        logger.config("Found <library_visual_scenes>. Parsing...");
                        parseVisualScenes(parser);
                        break;
                    case "library_animations":
                        logger.config("Found <library_animations>. Parsing...");
                        parseAnimations(parser);
                        break;
                }
            }
            // Move to the next event in the XML file
            eventType = parser.next();
        }
        logger.info("Finished parsing DAE file. Geometries found: " + geometryLibrary.size() + ", Nodes found: " + nodeLibrary.size() +
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
                logger.config("Parsing <geometry> with id: " + geometryId);

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
                        logger.warning("Skipping unhandled <mesh> tag: " + meshTagName);
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
            logger.config("Geometry '" + geometryId + "' found. Meshes: " + geometry.getMeshes().size() + ".");
            geometry.assemble();
            geometryLibrary.put(geometryId, geometry);
        } else {
            logger.log(Level.SEVERE, "Geometry '" + geometryId + "' was parsed but resulted in no vertex data.");
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
        logger.config("Parsed <source> '" + finalSource.getId() + "' with stride " + finalSource.getStride());
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

        logger.config("Parsed <polygons> primitive with " + (unrolledPositions.size() / 3) + " vertices.");
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
    /**
     * Parses both <triangles> and <polylist> primitives with proper triangulation support.
     * Handles vcount for polylists and triangulates n-gons using triangle fan method.
     */
    private void parseMeshPrimitive(XmlPullParser parser, Map<String, Source> sources,
                                    Map<String, List<Input>> verticesLibrary, Geometry geometry) throws Exception {

        final Mesh mesh = new Mesh();
        List<Input> inputs = new ArrayList<>();
        int[] rawIndices = null;
        int[] vcount = null;
        String primitiveName = parser.getName(); // Either "triangles" or "polylist"
        int primitiveStartDepth = parser.getDepth();

        // Set the mesh ID and material
        mesh.setId(primitiveName + "#" + parser.getLineNumber());
        String materialId = parser.getAttributeValue(null, "material");
        if (materialId != null) {
            mesh.setMaterialId(materialId);
            logger.config("Bound material '" + materialId + "' to geometry '" + mesh.getId() + "'");
        }

        // Parse <input> tags and data
        while (parser.next() != XmlPullParser.END_TAG || parser.getDepth() > primitiveStartDepth) {
            if (parser.getEventType() != XmlPullParser.START_TAG) continue;

            switch (parser.getName()) {
                case "input":
                    inputs.add(parseInput(parser));
                    break;
                case "vcount":
                    String vcountData = parser.nextText();
                    String[] vcountStrings = vcountData.trim().split("\\s+");
                    vcount = new int[vcountStrings.length];
                    for (int i = 0; i < vcountStrings.length; i++) {
                        vcount[i] = Integer.parseInt(vcountStrings[i]);
                    }
                    break;
                case "p":
                    String indexData = parser.nextText();
                    String[] pStrings = indexData.trim().split("\\s+");
                    rawIndices = new int[pStrings.length];
                    for (int i = 0; i < pStrings.length; i++) {
                        rawIndices[i] = Integer.parseInt(pStrings[i]);
                    }
                    break;
            }
        }

        if (rawIndices == null || inputs.isEmpty()) {
            logger.log(Level.SEVERE, "No <p> indices or <input> tags found in mesh primitive");
            return;
        }

        // Resolve input offsets and sources
        Source positionSource = null;
        Source normalSource = null;
        Source texCoordSource = null;
        Source colorSource = null;

        int vertexOffset = -1, normalOffset = -1, texOffset = -1, colorOffset = -1;
        int stride = 0;

        for (Input input : inputs) {
            stride = Math.max(stride, input.offset + 1);

            if ("VERTEX".equals(input.semantic)) {
                vertexOffset = input.offset;
                List<Input> vertexInputs = verticesLibrary.get(input.sourceId);
                if (vertexInputs != null) {
                    for (Input vInput : vertexInputs) {
                        if ("POSITION".equals(vInput.semantic)) {
                            positionSource = sources.get(vInput.sourceId);
                        }
                    }
                }
            } else if ("NORMAL".equals(input.semantic)) {
                normalOffset = input.offset;
                normalSource = sources.get(input.sourceId);
            } else if ("TEXCOORD".equals(input.semantic)) {
                // Only use the first texture coordinate set (set="0")
                // Higher sets (set="1", etc.) are ignored for now as they typically contain
                // lightmaps, normal maps, or other secondary textures that need shader support
                if (input.set == 0) {
                    texOffset = input.offset;
                    texCoordSource = sources.get(input.sourceId);
                    logger.config("Using TEXCOORD set=0 for texture mapping");
                } else {
                    logger.warning("TEXCOORD set=" + input.set + " found but ignored. Currently only set=0 is supported.");
                }
            } else if ("COLOR".equals(input.semantic)) {
                colorOffset = input.offset;
                colorSource = sources.get(input.sourceId);
            }
        }

        if (positionSource == null) {
            logger.log(Level.SEVERE, "FATAL: Could not resolve position source for primitive.");
            return;
        }

        // Handle triangulation
        List<Float> unrolledPositions = new ArrayList<>();
        List<Float> unrolledNormals = new ArrayList<>();
        List<Float> unrolledTexCoords = new ArrayList<>();
        List<Float> unrolledColors = new ArrayList<>();
        List<Integer> indicesList = new ArrayList<>();

        if (vcount != null && vcount.length > 0) {
            // POLYLIST: Use vcount for triangulation
            int currentRawIndex = 0;
            for (int i = 0; i < vcount.length; i++) {
                int vertexCount = vcount[i];

                // Triangle fan triangulation
                for (int k = 0; k < vertexCount - 2; k++) {
                    addVertexToMesh(currentRawIndex, rawIndices, stride, vertexOffset, normalOffset, texOffset, colorOffset,
                            positionSource, normalSource, texCoordSource, colorSource,
                            unrolledPositions, unrolledNormals, unrolledTexCoords, unrolledColors, indicesList);

                    addVertexToMesh(currentRawIndex + k + 1, rawIndices, stride, vertexOffset, normalOffset, texOffset, colorOffset,
                            positionSource, normalSource, texCoordSource, colorSource,
                            unrolledPositions, unrolledNormals, unrolledTexCoords, unrolledColors, indicesList);

                    addVertexToMesh(currentRawIndex + k + 2, rawIndices, stride, vertexOffset, normalOffset, texOffset, colorOffset,
                            positionSource, normalSource, texCoordSource, colorSource,
                            unrolledPositions, unrolledNormals, unrolledTexCoords, unrolledColors, indicesList);
                }

                currentRawIndex += vertexCount;
            }
        } else {
            // TRIANGLES: Already triangulated, just unroll
            int triangleCount = rawIndices.length / stride / 3;
            for (int i = 0; i < triangleCount; i++) {
                for (int j = 0; j < 3; j++) {
                    int vertexIndex = i * 3 + j;
                    addVertexToMesh(vertexIndex, rawIndices, stride, vertexOffset, normalOffset, texOffset, colorOffset,
                            positionSource, normalSource, texCoordSource, colorSource,
                            unrolledPositions, unrolledNormals, unrolledTexCoords, unrolledColors, indicesList);
                }
            }
        }

        // Invert texture V-coordinates
        if (!unrolledTexCoords.isEmpty()) {
            for (int i = 1; i < unrolledTexCoords.size(); i += 2) {
                unrolledTexCoords.set(i, 1.0f - unrolledTexCoords.get(i));
            }
        }

        // Add default colors if not present
        /*if (unrolledColors.isEmpty() && colorSource == null) {
            int vertexCount = unrolledPositions.size() / 3;
            for (int i = 0; i < vertexCount; i++) {
                unrolledColors.add(1.0f);
                unrolledColors.add(1.0f);
                unrolledColors.add(1.0f);
                unrolledColors.add(1.0f);
            }
        }*/

        // Commit to mesh
        mesh.setVertices(floatListToArray(unrolledPositions));
        if (!unrolledNormals.isEmpty()) {
            mesh.setNormals(floatListToArray(unrolledNormals));
        }
        if (!unrolledTexCoords.isEmpty()) {
            mesh.setTextureCoords(floatListToArray(unrolledTexCoords));
        }
        if (!unrolledColors.isEmpty()) {
            mesh.setColors(floatListToArray(unrolledColors));
        }

        // Store both indices and indices map
        int[] indicesNew = new int[indicesList.size()];
        for (int i = 0; i < indicesList.size(); i++) {
            indicesNew[i] = i;
        }
        // mesh.setIndices(rawIndices);
        mesh.setIndices(indicesNew);

        int[] indicesMap = new int[indicesList.size()];
        for (int i = 0; i < indicesList.size(); i++) {
            indicesMap[i] = indicesList.get(i);
        }
        mesh.setIndicesMap(indicesMap);

        geometry.addMesh(mesh);
        logger.config("Assembled geometry '" + mesh.getId() + "' with " + (unrolledPositions.size() / 3) + " vertices.");
    }

    // Helper to unroll a single vertex with simplified signature for mesh primitive parsing
    private void addVertexToMesh(int vertexIndexInPoly, int[] indices, int stride,
                                 int vertexOffset, int normalOffset, int texOffset, int colorOffset,
                                 Source posSrc, Source normSrc, Source texSrc, Source colSrc,
                                 List<Float> outPos, List<Float> outNorm, List<Float> outTex, List<Float> outCol, List<Integer> outIndices) {

        int baseIndex = vertexIndexInPoly * stride;

        // Position (required)
        int pIdx = indices[baseIndex + vertexOffset];
        outPos.add(posSrc.floatData[pIdx * 3]);
        outPos.add(posSrc.floatData[pIdx * 3 + 1]);
        outPos.add(posSrc.floatData[pIdx * 3 + 2]);
        outIndices.add(pIdx);

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
            if (colSrc.stride >= 4) {
                outCol.add(colSrc.floatData[cIdx * colSrc.stride + 3]);
            } else {
                outCol.add(1.0f);
            }
        }
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
                    logger.config("Parsed controller '" + controller.getId() + "'");
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
        int inputCount = 0;

        // 1. Parse the <vertex_weights> block
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
                    vcount = parseIntArray(parser.nextText());
                    break;
                case "v":
                    v = parseIntArray(parser.nextText());
                    break;
            }
        }

        if (vcount == null || v == null || jointInput == null || weightInput == null) {
            logger.log(Level.SEVERE, "Incomplete <vertex_weights> data.");
            return null;
        }

        int vertexCount = vcount.length;
        Source weightsSource = sources.get(weightInput.sourceId);
        float[] rawWeights = weightsSource != null ? weightsSource.getFloatData() : new float[0];

        // We use a fixed 4 influences per vertex for the output arrays
        int[] finalJointIndices = new int[vertexCount * 4];
        float[] finalWeights = new float[vertexCount * 4];

        for (int i=0; i<finalJointIndices.length; i+=4){
            finalJointIndices[i] = 1;
            finalWeights[i] = 1;
        }

        int vPointer = 0; // Current position in the 'v' array
        for (int i = 0; i < vertexCount; i++) {
            int numInfluences = vcount[i];
            float totalWeight = 0;

            // Read influences for this vertex
            for (int j = 0; j < numInfluences; j++) {
                int jointIndex = v[vPointer + jointInput.offset];
                int weightIndex = v[vPointer + weightInput.offset];
                float weight = (weightIndex < rawWeights.length) ? rawWeights[weightIndex] : 0.0f;

                // We only store the first 4 influences
                if (j < 4) {
                    finalJointIndices[i * 4 + j] = jointIndex;
                    finalWeights[i * 4 + j] = weight;
                    totalWeight += weight;
                }

                // Move pointer to the next influence entry in 'v'
                vPointer += inputCount;
            }

            // Normalize weights if we have data
            if (totalWeight > 0) {
                for (int j = 0; j < Math.min(numInfluences, 4); j++) {
                    finalWeights[i * 4 + j] /= totalWeight;
                }
            }
        }

        logger.config("Assembled vertex weights for " + vertexCount + " vertices.");
        return new VertexWeights(finalJointIndices, finalWeights);
    }

    private int[] parseIntArray(String text) {
        if (text == null || text.trim().isEmpty()) return new int[0];
        String[] parts = text.trim().split("\\s+");
        int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Integer.parseInt(parts[i]);
        }
        return result;
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
                logger.config("Mapped Image ID '" + imageId + "' to file '" + fileName + "'");
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
                logger.config("Parsed Effect '" + effectId + "' with texture image ID '" + currentEffect.imageId + "'");
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
            logger.config("Newparam link created: '" + paramSid + "' -> '" + referencedId + "'");
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
                        if (parts.length >= 3) // Must have at least R, G, B
                        {
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
                    logger.log(Level.SEVERE, "Failed to parse color string inside <" + propertyName + ">", e);
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
                            logger.log(Level.SEVERE, "Failed to parse <float> for transparency", e);
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
     * Example: <input semantic="TEXCOORD" source="#U3DMesh-UV0" offset="1" set="0"/>
     * Example: <input semantic="TEXCOORD" source="#U3DMesh-UV1" offset="3" set="1"/>
     *
     * @param parser The XmlPullParser at the START_TAG of an <input>.
     * @return A new Input object with the parsed data.
     */
    private Input parseInput(XmlPullParser parser) {
        String semantic = parser.getAttributeValue(null, "semantic");
        String sourceId = cleanId(parser.getAttributeValue(null, "source"));
        String offsetStr = parser.getAttributeValue(null, "offset");
        int offset = (offsetStr != null) ? Integer.parseInt(offsetStr) : 0;

        // Read the optional 'set' attribute (for TEXCOORD, COLOR, etc.)
        String setStr = parser.getAttributeValue(null, "set");
        int set = (setStr != null) ? Integer.parseInt(setStr) : 0;

        logger.config("Parsed <input> with semantic: " + semantic + ", source: " + sourceId +
                   ", offset: " + offset + ", set: " + set);
        return new Input(semantic, sourceId, offset, set);
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
            logger.log(Level.SEVERE, "Matrix data has less than 16 values. Found: " + floatStrings.length);
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
        String sid = parser.getAttributeValue(null, "sid");
        Node currentNode = new Node(nodeId, nodeName);
        currentNode.setSid(sid);
        currentNode.setParent(parent);

        // --- NEW LOGIC: PARSE TRANSFORMATIONS ---
        float[] finalMatrix = new float[16];
        Matrix.setIdentityM(finalMatrix, 0); // Start with an identity matrix

        int startDepth = parser.getDepth();
        while (parser.next() != XmlPullParser.END_TAG || parser.getDepth() > startDepth) {
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
                        logger.warning("Instance tag <" + tagName + "> is missing a 'url' attribute for node '" + currentNode.getId() + "'.");
                        skipToEnd(parser, tagName);
                        break;
                    }

                    if ("instance_controller".equals(tagName)) {
                        logger.config("Found <instance_controller> for node '" + currentNode.getId() + "' with url: " + url);
                        currentNode.setInstanceControllerId(cleanId(url));
                    } else {
                        logger.config("Found <instance_geometry> for node '" + currentNode.getId() + "' with url: " + url);
                        currentNode.setInstanceGeometryId(cleanId(url));
                    }

                    // Traverse children to find <skeleton> or <instance_material>
                    while (parser.next() != XmlPullParser.END_TAG || !parser.getName().equals(tagName)) {
                        if (parser.getEventType() != XmlPullParser.START_TAG) continue;

                        String innerTagName = parser.getName();
                        if ("skeleton".equals(innerTagName)) {
                            // Skeleton root ID for skinned meshes
                            String skinRootId = parser.nextText();
                            logger.config("Found <skeleton> tag with root ID: " + skinRootId);
                            currentNode.setSkinId(cleanId(skinRootId));
                        } else if ("instance_material".equals(innerTagName)) {
                            // Material binding
                            String target = parser.getAttributeValue(null, "target");
                            if (target != null) {
                                logger.config("Found <instance_material> with target: " + target);
                                currentNode.setBindMaterialId(cleanId(target));
                            }
                        }
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
                    skipTag(parser);
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
            logger.info("No animation channels found.");
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

        logger.info("Successfully parsed 1 animation clip with " + keyFrames.length + " keyframes.");
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

        // Infer the stride from the actual data if possible. This is more robust than
        // relying on the targetTransform string only (which can be e.g. "transform").
        if (inputSourceId != null && outputSourceId != null) {
            float[] times = sources.get(inputSourceId);
            float[] values = sources.get(outputSourceId);
            if (times != null && values != null && times.length > 0) {
                // Compute number of floats per keyframe
                int computed = values.length / times.length;
                if (computed > 0) {
                    stride = computed;
                }
            } else {
                // Fallback: if the targetTransform looks like a matrix or generic transform assume 16
                if (targetTransform != null && ("matrix".equalsIgnoreCase(targetTransform) || "transform".equalsIgnoreCase(targetTransform))) {
                    stride = 16;
                } else {
                    stride = 1;
                }
            }

            float[] timesFinal = sources.get(inputSourceId);
            float[] valuesFinal = sources.get(outputSourceId);
            if (timesFinal != null && valuesFinal != null && targetId != null) {
                return new ChannelData(targetId, targetTransform, timesFinal, valuesFinal, stride);
            }
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
        if (channel == null || channel.targetTransform == null || jointTransform == null) {
            logger.warning("Skipping applyTransform due to null channel/target/transform");
            return;
        }

        int base = Math.max(0, timeIndex * Math.max(1, channel.stride));
        float value = 0f;
        if (channel.values != null && base < channel.values.length) {
            value = channel.values[base];
        }

        String target = channel.targetTransform.toUpperCase();

        switch (target) {
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
                if (channel.stride == 3 && channel.values != null && base + 2 < channel.values.length) {
                    float[] scale = new float[3];
                    System.arraycopy(channel.values, base, scale, 0, 3);
                    jointTransform.setScale(scale);
                } else {
                    // Fallback for uniform scale if stride is 1 or data missing
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
                if (channel.values != null && channel.stride >= 16 && base + 16 <= channel.values.length) {
                    float[] matrix = new float[16];
                    System.arraycopy(channel.values, base, matrix, 0, 16);
                    float[] transposed = new float[16];
                    Matrix.transposeM(transposed, 0, matrix, 0);
                    jointTransform.setTransform(transposed); // This will overwrite others, as expected for a matrix.
                } else {
                    logger.warning("Matrix animation channel has unexpected stride/length: stride=" + channel.stride + " valuesLength=" + (channel.values!=null?channel.values.length:0));
                }
                break;
            default:
                logger.warning("Unsupported animation transform target: " + channel.targetTransform);
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
        logger.config("Triangulating polygon with " + holes.size() + " holes. (NOTE: Holes are currently ignored)");
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
            logger.log(Level.SEVERE, "Triangulation failed", e);
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
