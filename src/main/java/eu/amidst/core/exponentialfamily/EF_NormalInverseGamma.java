package eu.amidst.core.exponentialfamily;

import eu.amidst.core.utils.ArrayVector;
import eu.amidst.core.utils.Vector;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;

import java.util.List;
import java.util.Map;

/**
 * Created by ana@cs.aau.dk on 27/02/15.
 */
public class EF_NormalInverseGamma extends EF_ConditionalDistribution{

    Variable meanVariable;
    Variable invGammaVariable;

    public EF_NormalInverseGamma(Variable var_, Variable mean, Variable invGamma){
        this.var = var_;
        this.meanVariable = mean;
        this.invGammaVariable = invGamma;
        this.parents.add(mean);
        this.parents.add(invGamma);

        if (!var_.isNormal())
            throw new UnsupportedOperationException("Creating a Normal-Inverse-Gamma EF distribution for a non-gaussian child variable.");


        if(!mean.isNormal()){
            throw new UnsupportedOperationException("Creating a Normal-Inverse-Gamma EF distribution for a non-gaussian parent variable.");
        }

        if(!invGammaVariable.isInverseGamma()){
            throw new UnsupportedOperationException("Creating a Normal-Inverse-Gamma EF distribution for a non-inverse-gamma parent variable.");
        }
    }


    /**
     * Of the second form (message from all parents to X variable). Needed to calculate the lower bound.
     *
     * @param momentParents
     * @return
     */
    @Override
    public double getExpectedLogNormalizer(Map<Variable, MomentParameters> momentParents) {
        double mean = momentParents.get(meanVariable).get(0);
        double invVariance = momentParents.get(invGammaVariable).get(1);

        return 0.5*mean*invVariance - 0.5*Math.log(invVariance);
    }

    /**
     * Of the second form (message from all parents to X variable).
     * @param momentParents
     * @return
     */
    @Override
    public NaturalParameters getExpectedNaturalFromParents(Map<Variable, MomentParameters> momentParents) {

        NaturalParameters naturalParameters = new ArrayVector(2);

        double mean = momentParents.get(meanVariable).get(0);
        double invVariance = momentParents.get(invGammaVariable).get(1);

        naturalParameters.set(0,mean*invVariance);
        naturalParameters.set(1,-0.5*invVariance);

        return naturalParameters;
    }

    /**
     * It is the message to one node to its parent @param parent, taking into account the suff. stat. if it is observed
     * or the moment parameters if not, and incorporating the message (with moment param.) received from all co-parents.
     * (Third form EF equations).
     *
     * @param parent
     * @param momentChildCoParents
     * @return
     */
    @Override
    public NaturalParameters getExpectedNaturalToParent(Variable parent, Map<Variable, MomentParameters> momentChildCoParents) {

        NaturalParameters naturalParameters = new ArrayVector(2);

        double X = momentChildCoParents.get(var).get(0);
        double invVariance = momentChildCoParents.get(invGammaVariable).get(1);
        double mean = momentChildCoParents.get(meanVariable).get(0);

        // Message to the mean (gaussian) variable
        if(meanVariable == parent){
            naturalParameters.set(0, X*invVariance);
            naturalParameters.set(1, -0.5*invVariance);
        // Message to the inv-Gamma variable
        }else{
            naturalParameters.set(0,-0.5);
            naturalParameters.set(1,-0.5*Math.pow(X-mean,2));
        }

        return naturalParameters;
    }

    @Override
    public double getExpectedLogNormalizer(Variable parent, Map<Variable, MomentParameters> momentChildCoParents) {
        throw new UnsupportedOperationException("No Implemented. This method is no really needed");
    }

    @Override
    public void updateNaturalFromMomentParameters() {
        throw new UnsupportedOperationException("No Implemented. NormalInverseGamma distribution should only be used for learning, use EF_Normal for inference.");
    }

    @Override
    public void updateMomentFromNaturalParameters() {
        throw new UnsupportedOperationException("No Implemented. NormalInverseGamma distribution should only be used for learning, use EF_Normal for inference.");
    }

    @Override
    public SufficientStatistics getSufficientStatistics(Assignment data) {
        throw new UnsupportedOperationException("No Implemented. NormalInverseGamma distribution should only be used for learning, use EF_Normal for inference.");
    }

    @Override
    public int sizeOfSufficientStatistics() {
        throw new UnsupportedOperationException("No Implemented. NormalInverseGamma distribution should only be used for learning, use EF_Normal for inference.");
    }

    @Override
    public double computeLogBaseMeasure(Assignment dataInstance) {
        throw new UnsupportedOperationException("No Implemented. NormalInverseGamma distribution should only be used for learning, use EF_Normal for inference.");
    }

    @Override
    public double computeLogNormalizer() {
        throw new UnsupportedOperationException("No Implemented. NormalInverseGamma distribution should only be used for learning, use EF_Normal for inference.");
    }

    @Override
    public Vector createZeroedVector() {
        throw new UnsupportedOperationException("No Implemented. NormalInverseGamma distribution should only be used for learning, use EF_Normal for inference.");
    }
}
