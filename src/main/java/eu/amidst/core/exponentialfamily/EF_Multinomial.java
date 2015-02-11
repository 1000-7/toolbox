package eu.amidst.core.exponentialfamily;

import eu.amidst.core.utils.ArrayVector;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.DistType;
import eu.amidst.core.variables.Variable;

import java.util.Random;

/**
 * Created by andresmasegosa on 13/11/14.
 */
public class EF_Multinomial extends EF_UnivariateDistribution {

    /**
     * The class constructor.
     * @param var The variable of the distribution.
     */
    public EF_Multinomial(Variable var) {

        if (!var.isMultinomial()) {
            throw new UnsupportedOperationException("Creating a Multinomial EF distribution for a non-multinomial variable.");
        }

        this.var=var;
        int nstates= var.getNumberOfStates();
        this.naturalParameters = this.createZeroedNaturalParameters();
        this.momentParameters = this.createZeroedMomentParameters();

        for (int i=0; i<nstates; i++){
            this.naturalParameters.set(i,-Math.log(nstates));
            this.momentParameters.set(i,1.0/nstates);
        }

    }


    @Override
    public double computeLogBaseMeasure(double val) {
        //return log(1);
        return 0;
    }

    @Override
    public double computeLogNormalizer() {
        double sum = 0;
        for (int i = 0; i < this.naturalParameters.size(); i++) {
            sum+=Math.exp(this.naturalParameters.get(i));
        }
        return Math.log(sum);
    }

    @Override
    public Vector createZeroedVector() {
        return new ArrayVector(this.var.getNumberOfStates());
    }

    @Override
    public SufficientStatistics getSufficientStatistics(double val) {
        SufficientStatistics vec = this.createZeroedSufficientStatistics();
        vec.set((int) val, 1);
        return vec;
    }

    @Override
    public void updateNaturalFromMomentParameters() {
        int nstates= var.getNumberOfStates();
        for (int i=0; i<nstates; i++){
            if (this.momentParameters.get(i) == 0)
                this.naturalParameters.set(i, -Double.MAX_VALUE);
            else
                this.naturalParameters.set(i, Math.log(this.momentParameters.get(i)));
        }
    }

    @Override
    public void setNaturalParameters(NaturalParameters parameters) {
        this.naturalParameters=parameters;//.copy(parameters);
        this.naturalParameters = Utils.logNormalize(this.naturalParameters);
        this.updateMomentFromNaturalParameters();
    }

    @Override
    public void updateMomentFromNaturalParameters() {
        int nstates= var.getNumberOfStates();
        for (int i=0; i<nstates; i++){
            this.momentParameters.set(i, Math.exp(this.naturalParameters.get(i) - this.computeLogNormalizer()));
        }
    }

    @Override
    public int sizeOfSufficientStatistics() {
        return this.var.getNumberOfStates();
    }


    @Override
    public EF_UnivariateDistribution deepCopy() {

        EF_Multinomial copy = new EF_Multinomial(this.getVariable());
        copy.getNaturalParameters().copy(this.getNaturalParameters());
        copy.getMomentParameters().copy(this.getMomentParameters());
        return copy;
    }

    @Override
    public EF_UnivariateDistribution randomInitialization(Random random) {
        double[] probabilities = new double[this.var.getNumberOfStates()];
        for (int i = 0; i < probabilities.length; i++) {
            probabilities[i] = random.nextDouble();
        }
        probabilities = Utils.normalize(probabilities);
        for (int i = 0; i < probabilities.length; i++) {
            this.getMomentParameters().set(i,probabilities[i]);
        }
        this.updateNaturalFromMomentParameters();

        return this;
    }
}
