package org.the3deer.engine.services.wavefront;

import org.the3deer.engine.model.Material;
import org.the3deer.engine.model.Materials;
import org.the3deer.engine.model.Texture;
import org.the3deer.util.math.Math3DUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

final class WavefrontMaterialsParser {

    private static final Logger logger = Logger.getLogger(WavefrontMaterialsParser.class.getSimpleName());

    /*
     * Parse the MTL file line-by-line, building Material objects which are collected in the materials ArrayList.
     */
    Materials parse(String id, InputStream inputStream) {

        logger.finest("Parsing materials... ");

        final Materials materials = new Materials(id);
        try {

            final BufferedReader isReader = new BufferedReader(new InputStreamReader(inputStream));

            String line;

            Material currMaterial = new Material(); // current material
            boolean createNewMaterial = false;

            while (((line = isReader.readLine()) != null)) {

                // read next line
                line = line.trim();

                // ignore empty lines
                if (line.length() == 0) continue;

                // parse line
                if (line.startsWith("newmtl ")) { // new material

                    // new material next iteration
                    if (createNewMaterial) {

                        // add current material to the list
                        materials.add(currMaterial.getName(), currMaterial);

                        // prepare next material
                        currMaterial = new Material();
                    }

                    // create next material next time
                    createNewMaterial = true;

                    // configure material
                    currMaterial.setName(line.substring(6).trim());

                    // log event
                    logger.finest("New material found: " + currMaterial.getName());

                } else if (line.startsWith("map_Kd ")) { // texture filename

                    // bind texture
                    currMaterial.setColorTexture(new Texture().setFile(line.substring(6).trim()));

                    // log event
                    logger.finest("Texture found: " + currMaterial.getColorTexture().getFile());

                } else if (line.startsWith("Ka ")) {

                    // ambient colour
                    currMaterial.setAmbient(Math3DUtils.parseFloat(line.substring(2).trim().split(" ")));

                    // log event
                    logger.finest("Ambient color: " + Arrays.toString(currMaterial.getAmbient()));
                } else if (line.startsWith("Kd ")) {

                    // diffuse colour
                    currMaterial.setDiffuse(Math3DUtils.parseFloat(line.substring(2).trim().split(" ")));

                    // log event
                    logger.finest("Diffuse color: " + Arrays.toString(currMaterial.getDiffuse()));
                } else if (line.startsWith("Ks ")) {

                    // specular colour
                    currMaterial.setSpecular(Math3DUtils.parseFloat(line.substring(2).trim().split(" ")));

                    // log event
                    logger.finest("Specular color: " + Arrays.toString(currMaterial.getSpecular()));
                } else if (line.startsWith("Ns ")) {

                    // shininess
                    float val = Float.parseFloat(line.substring(3));
                    currMaterial.setShininess(val);

                    // log event
                    logger.finest("Shininess: " + currMaterial.getShininess());

                } else if (line.charAt(0) == 'd') {

                    // alpha
                    float val = Float.parseFloat(line.substring(2));
                    currMaterial.setAlpha(val);

                    // log event
                    logger.finest("Alpha: " + currMaterial.getAlpha());

                } else if (line.startsWith("Tr ")) {

                    // Transparency (inverted)
                    currMaterial.setAlpha(1 - Float.parseFloat(line.substring(3)));

                    // log event
                    logger.finest("Transparency (1-Alpha): " + currMaterial.getAlpha());

                } else if (line.startsWith("illum ")) {

                    // illumination model
                    logger.finest("Ignored line: " + line);

                } else if (line.charAt(0) == '#') { // comment line

                    // log comment
                    logger.finest(line);

                } else {

                    // log event
                    logger.finest("Ignoring line: " + line);
                }

            }

            // add last processed material
            materials.add(currMaterial.getName(), currMaterial);

        } catch (Exception e) {
            logger.log(Level.SEVERE,  e.getMessage(), e);
        }

        // log event
        logger.config("Parsed materials: "+materials);

        return materials;
    }
}
