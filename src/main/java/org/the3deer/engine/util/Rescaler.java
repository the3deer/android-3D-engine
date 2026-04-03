package org.the3deer.engine.util;

import org.the3deer.engine.model.AnimatedModel;
import org.the3deer.engine.model.Object3D;

import java.nio.FloatBuffer;
import java.util.logging.Logger;

public class Rescaler {

    private static final Logger logger = Logger.getLogger(Rescaler.class.getSimpleName());

    public static void rescale(Object3D object3D, float maxSize) {
        float leftPt = Float.MAX_VALUE, rightPt = -Float.MAX_VALUE; // on x-axis
        float topPt = -Float.MAX_VALUE, bottomPt = Float.MAX_VALUE; // on y-axis
        float farPt = Float.MAX_VALUE, nearPt = -Float.MAX_VALUE; // on z-axis

        FloatBuffer vertexBuffer = object3D.getVertexBuffer();
        if (vertexBuffer == null) {
            logger.finest("Scaling for '" + object3D.getId() + "' I found that there is no vertex data");
            return;
        }

        logger.finest("Rescaling '" + object3D.getId() + "...");
        for (int i = 0; i < vertexBuffer.capacity(); i += 3) {
            if (vertexBuffer.get(i) > rightPt)
                rightPt = vertexBuffer.get(i);
            else if (vertexBuffer.get(i) < leftPt)
                leftPt = vertexBuffer.get(i);
            if (vertexBuffer.get(i + 1) > topPt)
                topPt = vertexBuffer.get(i + 1);
            else if (vertexBuffer.get(i + 1) < bottomPt)
                bottomPt = vertexBuffer.get(i + 1);
            if (vertexBuffer.get(i + 2) > nearPt)
                nearPt = vertexBuffer.get(i + 2);
            else if (vertexBuffer.get(i + 2) < farPt)
                farPt = vertexBuffer.get(i + 2);
        } // end
        logger.finest("Dimensions for '" + object3D.getId() + " (X left, X right): (" + leftPt + "," + rightPt + ")");
        logger.finest("Dimensions for '" + object3D.getId() + " (Y top, Y bottom): (" + topPt + "," + bottomPt + ")");
        logger.finest("Dimensions for '" + object3D.getId() + " (Z near, Z far): (" + nearPt + "," + farPt + ")");

        // calculate center of 3D object
        float xc = (rightPt + leftPt) / 2.0f;
        float yc = (topPt + bottomPt) / 2.0f;
        float zc = (nearPt + farPt) / 2.0f;

        // this.setOriginalPosition(new float[]{-xc,-yc,-zc});

        // calculate largest dimension
        float height = topPt - bottomPt;
        float depth = nearPt - farPt;
        float largest = rightPt - leftPt;
        if (height > largest)
            largest = height;
        if (depth > largest)
            largest = depth;
        logger.finest("Largest dimension [" + largest + "]");

        // scale object

        // calculate a scale factor
        float scaleFactor = 1.0f;
        if (largest != 0.0f)
            scaleFactor = (maxSize / largest);
        logger.info("Scaling '" + object3D.getId() + "' to (" + xc + "," + yc + "," + zc + ") scale: '" + scaleFactor + "'");

        // this.setOriginalScale(new float[]{scaleFactor,scaleFactor,scaleFactor});

        // modify the model's vertices
        if (object3D instanceof AnimatedModel) {
            object3D.setScale(new float[]{scaleFactor, scaleFactor, scaleFactor});
        } else {
            for (int i = 0; i < vertexBuffer.capacity(); i += 3) {
                float x = vertexBuffer.get(i);
                float y = vertexBuffer.get(i + 1);
                float z = vertexBuffer.get(i + 2);
                x = (x - xc) * scaleFactor;
                y = (y - yc) * scaleFactor;
                z = (z - zc) * scaleFactor;
                vertexBuffer.put(i, x);
                vertexBuffer.put(i + 1, y);
                vertexBuffer.put(i + 2, z);
            }

            object3D.setVertexBuffer(vertexBuffer);
        }

        logger.config("New dimensions for '" + object3D.getId() + ": "+ object3D.getCurrentDimensions());
    }
}
