package org.the3deer.util.math;

public class MathUtils {

    public static float sum(float[] array){
        return sum(array, 0, array.length);
    }

    public static float sum(float[] array, int offset, int count){
        float sum = 0;
        for (int i=0; i<count; i++){
            sum += array[i+offset];
        }
        return sum;
    }
}
