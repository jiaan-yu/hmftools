package com.hartwig.hmftools.svanalysis.analyser;

import static com.hartwig.hmftools.svanalysis.analyser.SvTestHelper.createBnd;
import static com.hartwig.hmftools.svanalysis.analyser.SvTestHelper.createDel;
import static com.hartwig.hmftools.svanalysis.analyser.SvTestHelper.createDup;
import static com.hartwig.hmftools.svanalysis.analyser.SvTestHelper.createIns;
import static com.hartwig.hmftools.svanalysis.analyser.SvTestHelper.createInv;
import static com.hartwig.hmftools.svanalysis.analyser.SvTestHelper.createSgl;
import static com.hartwig.hmftools.svanalysis.analysis.SvUtilities.calcConsistency;
import static com.hartwig.hmftools.svanalysis.analysis.SvUtilities.makeChrArmStr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.hartwig.hmftools.svanalysis.stats.FisherExactTest;
import com.hartwig.hmftools.svanalysis.types.SvVarData;

import org.junit.Test;

public class MiscTests
{
    @Test
    public void testProximityMethods()
    {
        // assertEquals(0.08, FittingConfig.MIN_PURITY_DEFAULT, EPSILON);
        //assertEquals(1.0, FittingConfig.MAX_PURITY_DEFAULT, EPSILON);

    }

    @Test
    public void testConsistency()
    {
        final SvVarData del = createDel("1", "1", 100, 200);
        assertEquals(calcConsistency(del), 0);

        final SvVarData ins = createIns("1", "1", 100, 200);
        assertEquals(calcConsistency(ins), 0);

        final SvVarData dup = createDup("1", "1", 100, 200);
        assertEquals(calcConsistency(dup), 0);

        final SvVarData inv = createInv("1", "1", 100, 200, 1);
        assertEquals(calcConsistency(inv), 2);

        final SvVarData bnd = createBnd("1", "1", 100, 1, "2", 100, -1);
        assertEquals(calcConsistency(bnd), 0);

        final SvVarData sgl = createSgl("1", "1", 100, 1, false);
        assertEquals(calcConsistency(sgl), 1);
    }

    @Test
    public void testMiscMethods()
    {
        assertTrue(makeChrArmStr("1", "P").equals("1_P"));
    }


    @Test
    public void testStatsRoutines()
    {
        FisherExactTest fetCalc = new FisherExactTest();
        fetCalc.initialise(1000);

        int withAwithB = 11;
        int withANoB = 27;
        int noAWithB = 2;
        int noAnoB = 170;
        double expectedCount = 5;

        double fisherProb = fetCalc.getLeftTailedP(withAwithB, noAWithB, withANoB, noAnoB);
        fisherProb = fetCalc.getRightTailedP(withAwithB, noAWithB, withANoB, noAnoB);

    }

}
