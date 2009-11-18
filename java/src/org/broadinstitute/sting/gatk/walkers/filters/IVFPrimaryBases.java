package org.broadinstitute.sting.gatk.walkers.filters;

import org.broadinstitute.sting.gatk.contexts.VariantContext;
import org.broadinstitute.sting.utils.MathUtils;
import org.broadinstitute.sting.utils.ReadBackedPileup;

public class IVFPrimaryBases implements IndependentVariantFeature {
    private double[] p = { 0.933, 0.972, 0.970, 0.960, 0.945, 0.990, 0.971, 0.943, 0.978, 0.928 };

    private double[] likelihoods;

    /**
     * Method so that features can initialize themselves based on a short argument string. At the moment, each feature is
     * responsible for interpreting their own argument string.
     *
     * @param arguments the arguments!
     */
    public void initialize(String arguments) {
        if (arguments != null && !arguments.isEmpty()) {
            String[] argPieces = arguments.split(",");

            for (int genotypeIndex = 0; genotypeIndex < 10; genotypeIndex++) {
                p[genotypeIndex] = Double.valueOf(argPieces[genotypeIndex]);
            }
        }
    }

    /**
     * Method to compute the result of this feature for each of the ten genotypes.  The return value must be a double array
     * of length 10 (one for each genotype) and the value must be in log10-space.
     *
     * @param contextWindow the context for the given locus
     * @return a ten-element array of log-likelihood result of the feature applied to each genotype
     */
    public void compute(VariantContextWindow contextWindow) {
        VariantContext context = contextWindow.getContext();
        likelihoods = new double[10];

        ReadBackedPileup pileup = new ReadBackedPileup(context.getReferenceContext().getBase(), context.getAlignmentContext(useZeroQualityReads()));
        String primaryBases = pileup.getBasesAsString();

        for (int genotypeIndex = 0; genotypeIndex < Genotype.values().length; genotypeIndex++) {
            char firstAllele = Genotype.values()[genotypeIndex].toString().charAt(0);
            char secondAllele = Genotype.values()[genotypeIndex].toString().charAt(1);

            int offTotal = 0;

            int onTotal = 0;

            for (int pileupIndex = 0; pileupIndex < primaryBases.length(); pileupIndex++) {
                char primaryBase = primaryBases.charAt(pileupIndex);

                if (primaryBase != firstAllele && primaryBase != secondAllele) {
                    offTotal++;
                } else {
                        onTotal++;
                }

            }

            int Total = onTotal + offTotal;

            double Prior = MathUtils.binomialProbability(offTotal, Total, p[genotypeIndex]);

            likelihoods[genotypeIndex] = Math.log10(Prior);
        }
    }

    public double[] getLikelihoods() {
        return likelihoods;
    }

    public String getStudyHeader() {
        return "";
    }

    public String getStudyInfo() {
        return "";
    }

    public boolean useZeroQualityReads() { return false; }    
}
