package com.hartwig.hmftools.bamslicer.clustering;

import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.MathArrays;

public class ChunkDistanceMeasure implements DistanceMeasure {
    @Override
    public double compute(final double[] one, final double[] other) throws DimensionMismatchException {
        MathArrays.checkEqualLength(one, other);
        if (overlaps(one, other)) {
            return 0;
        } else {
            final double maxStart = FastMath.max(one[0], other[0]);
            final double minEnd = FastMath.min(one[1], other[1]);
            return FastMath.abs(maxStart - minEnd);
        }
    }

    //MIVO: assumes that params are well-formed intervals (interval[0] <= interval[1])
    private static boolean overlaps(final double[] one, final double[] other) {
        return one[0] <= other[1] && other[0] <= one[1];
    }
}
