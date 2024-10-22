package org.the3deer.util.geometry;

import org.the3deer.util.math.Math3DUtils;

import java.util.Arrays;

final class DelaunayConstraint {
    final float[] v1;
    final float[] v2;

    public DelaunayConstraint(float[] v1, float[] v2) {
        this.v1 = v1;
        this.v2 = v2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DelaunayConstraint that = (DelaunayConstraint) o;
        float[] sum = Math3DUtils.add(v1, v2);
        float[] thatSum = Math3DUtils.add(that.v1, that.v2);
        return Arrays.equals(sum, thatSum);
    }

    @Override
    public int hashCode() {
        float[] sum = Math3DUtils.add(v1, v2);
        return Arrays.hashCode(sum);
    }

    @Override
    public String toString() {
        return "DelaunayConstraint{" +
                "v1=" + Arrays.toString(v1) +
                ", v2=" + Arrays.toString(v2) +
                '}';
    }
}
