package org.broadinstitute.sting.gatk.walkers.filters;

import org.broadinstitute.sting.gatk.refdata.RodGeliText;
import org.broadinstitute.sting.utils.ReadBackedPileup;
import org.broadinstitute.sting.utils.Utils;
import org.broadinstitute.sting.utils.Pair;

import java.util.HashMap;

public class VECAlleleBalance extends RatioFilter {

    private double lowThreshold = 0.25, highThreshold = 0.75;
    private double ratio;

    public VECAlleleBalance() {
        super("Allele Balance Ratio", VECAlleleBalance.class, Tail.TwoTailed);
    }

    public void initialize(HashMap<String,String> args) {
        if ( args.get("analysis") != null ) {
            if ( args.get("analysis").equalsIgnoreCase("fair_coin_test") )
                analysis = AnalysisType.FAIR_COIN_TEST;
            else if ( args.get("analysis").equalsIgnoreCase("point_estimate") )
                analysis = AnalysisType.POINT_ESTIMATE;
        }
        if ( args.get("pvalue") != null )
            setIntegralPvalue(Double.valueOf(args.get("pvalue")));
        if ( args.get("low") != null )
            lowThreshold = Double.valueOf(args.get("low"));
        if ( args.get("high") != null )
            highThreshold = Double.valueOf(args.get("high"));
        if ( args.get("confidence") != null )
            minGenotypeConfidenceToTest = Double.valueOf(args.get("confidence"));
        setLowThreshold(lowThreshold);
        setHighThreshold(highThreshold);
    }

    /**
     * Return the count of bases matching the major (first) and minor (second) alleles as a pair.
     *
     */
    protected Pair<Integer, Integer> getRatioCounts(char ref, ReadBackedPileup pileup, RodGeliText variant) {
        final String genotype = variant.getBestGenotype();
        if ( genotype.length() > 2 )
            throw new IllegalArgumentException(String.format("Can only handle diploid genotypes: %s", genotype));

        final String bases = pileup.getBasesAsString();
        if ( bases.length() == 0 ) {
            ratio = 0.0;
            return new Pair<Integer, Integer>(0, 0);
        }

        char a = genotype.toUpperCase().charAt(0);
        char b = genotype.toUpperCase().charAt(1);
        int aCount = Utils.countOccurrences(a, bases.toUpperCase());
        int bCount = Utils.countOccurrences(b, bases.toUpperCase());

        int refCount = a == ref ? aCount : bCount;
        int altCount = a == ref ? bCount : aCount;

        ratio = (double)refCount / (double)(refCount + altCount);
        return new Pair<Integer, Integer>(refCount, altCount);
    }

    protected boolean excludeHetsOnly() { return true; }

    public boolean useZeroQualityReads() { return false; }

    public double inclusionProbability() {
        // A hack for now until this filter is actually converted to an empirical filter
        return exclude ? 0.0 : 1.0;
    }

    public String getStudyHeader() {
        return "AlleleBalance("+lowThreshold+","+highThreshold+")\tRefRatio";
    }

    public String getStudyInfo() {
        return (exclude ? "fail" : "pass") + "\t" + ratio;
    }

    public String getVCFFilterString() {
        return "AlleleBalance";
    }

    public String getScoreString() {
        return String.format("%.2f", ratio);
    }
}
