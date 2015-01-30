package eu.amidst.core.inference;

import eu.amidst.core.distribution.Normal;
import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;

import java.util.Arrays;
import java.util.Random;

/**
 * Created by andresmasegosa on 30/01/15.
 */
public final class InferenceEngineForBN {

    private static InferenceAlgorithmForBN inferenceAlgorithmForBN = new RandomInferenceAlgorithm();

    public static void setInferenceAlgorithmForBN(InferenceAlgorithmForBN inferenceAlgorithmForBN) {
        InferenceEngineForBN.inferenceAlgorithmForBN = inferenceAlgorithmForBN;
    }

    public static void compileModel(){
        inferenceAlgorithmForBN.compileModel();
    }

    public static void setModel(BayesianNetwork model){
        inferenceAlgorithmForBN.setModel(model);
    }

    public static void setEvidence(Assignment assignment){
        inferenceAlgorithmForBN.setEvidence(assignment);
    }

    public static <E extends UnivariateDistribution> E getPosterior(Variable var){
        return inferenceAlgorithmForBN.getPosterior(var);
    }

    public static void main(String[] arguments){

        BayesianNetworkGenerator.setNumberOfContinuousVars(2);
        BayesianNetworkGenerator.setNumberOfDiscreteVars(1);
        BayesianNetwork bn = BayesianNetworkGenerator.generateNaiveBayes(2);

        HashMapAssignment assignment = new HashMapAssignment(1);

        Variable varB = bn.getStaticVariables().getVariableById(0);

        assignment.setValue(varB, 0.7);

        Variable varA = bn.getStaticVariables().getVariableById(1);

        InferenceEngineForBN.setModel(bn);
        InferenceEngineForBN.setEvidence(assignment);
        InferenceEngineForBN.compileModel();

        Normal posteriorOfA = InferenceEngineForBN.getPosterior(varA);

        System.out.println("P(A|B=0.7) = " + posteriorOfA.toString());
    }
}
