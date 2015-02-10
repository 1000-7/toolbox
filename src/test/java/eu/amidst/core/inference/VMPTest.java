package eu.amidst.core.inference;

import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.distribution.Multinomial_MultinomialParents;
import eu.amidst.core.exponentialfamily.EF_DistributionBuilder;
import eu.amidst.core.exponentialfamily.EF_Multinomial;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.MultinomialIndex;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.StaticVariables;
import eu.amidst.core.variables.Variable;
import junit.framework.TestCase;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class VMPTest extends TestCase {


    public static void test1() {

        StaticVariables variables = new StaticVariables();
        Variable varA = variables.addHiddenMultionomialVariable("A", 2);
        Variable varB = variables.addHiddenMultionomialVariable("B", 2);

        DAG dag = new DAG(variables);

        dag.getParentSet(varB).addParent(varA);

        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);

        Multinomial_MultinomialParents distA = bn.getDistribution(varA);
        Multinomial_MultinomialParents distB = bn.getDistribution(varB);

        distA.getMultinomial(0).setProbabilities(new double[]{0.9, 0.1});
        distB.getMultinomial(0).setProbabilities(new double[]{0.75, 0.25});
        distB.getMultinomial(1).setProbabilities(new double[]{0.25, 0.75});

        //bn.randomInitialization(new Random(0));

        double[] pA = distA.getMultinomial(0).getProbabilities();
        double[][] pB = new double[2][];
        pB[0] = distB.getMultinomial(0).getProbabilities();
        pB[1] = distB.getMultinomial(1).getProbabilities();

        System.out.println(bn.toString());


        HashMapAssignment assignment = new HashMapAssignment(1);
        assignment.setValue(varB, 1.0);

        VMP vmp = new VMP();
        InferenceEngineForBN.setInferenceAlgorithmForBN(vmp);
        InferenceEngineForBN.setModel(bn);

        EF_Multinomial qADist = ((EF_Multinomial) vmp.nodes.get(0).getQDist());
        EF_Multinomial qBDist = ((EF_Multinomial) vmp.nodes.get(1).getQDist());

        double[] qA = new double[2];
        qA[0] = qADist.getMomentParameters().get(0);
        qA[1] = qADist.getMomentParameters().get(1);

        double[] qB = new double[2];
        qB[0] = qBDist.getMomentParameters().get(0);
        qB[1] = qBDist.getMomentParameters().get(1);

        //InferenceEngineForBN.setEvidence(assignment);
        InferenceEngineForBN.compileModel();

        Multinomial postA = InferenceEngineForBN.getPosterior(varA);
        System.out.println("P(A) = " + postA.toString());
        Multinomial postB = InferenceEngineForBN.getPosterior(varB);
        System.out.println("P(B) = " + postB.toString());


        boolean convergence = false;
        double oldvalue = 0;
        while (!convergence) {

            qA[0] = Math.exp(qB[0] * Math.log(pB[0][0]) + qB[1] * Math.log(pB[0][1]) + Math.log(pA[0]));
            qA[1] = Math.exp(qB[0] * Math.log(pB[1][0]) + qB[1] * Math.log(pB[1][1]) + Math.log(pA[1]));

            Utils.normalize(qA);

            qB[0] = Math.exp(qA[0] * Math.log(pB[0][0] * pA[0]) + qA[1] * Math.log(pB[1][0] * pA[1]));
            qB[1] = Math.exp(qA[0] * Math.log(pB[0][1] * pA[0]) + qA[1] * Math.log(pB[1][1] * pA[1]));

            Utils.normalize(qB);

            if (Math.abs(qA[0] + qB[0] - oldvalue) < 0.001) {
                convergence = true;
            }

            oldvalue = qA[0] + qB[0];


        }
        System.out.println(qA[0]);
        System.out.println(qB[0]);

        assertEquals(postA.getProbabilities()[0], qA[0], 0.01);
        assertEquals(postB.getProbabilities()[0], qB[0], 0.01);

    }


    public static void test2() {

        StaticVariables variables = new StaticVariables();
        Variable varA = variables.addHiddenMultionomialVariable("A", 2);
        Variable varB = variables.addHiddenMultionomialVariable("B", 2);

        DAG dag = new DAG(variables);

        dag.getParentSet(varB).addParent(varA);

        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);

        Multinomial_MultinomialParents distA = bn.getDistribution(varA);
        Multinomial_MultinomialParents distB = bn.getDistribution(varB);

        distA.getMultinomial(0).setProbabilities(new double[]{0.5, 0.5});
        distB.getMultinomial(0).setProbabilities(new double[]{0.75, 0.25});
        distB.getMultinomial(1).setProbabilities(new double[]{0.25, 0.75});

        //bn.randomInitialization(new Random(0));

        System.out.println(bn.toString());


        HashMapAssignment assignment = new HashMapAssignment(1);
        assignment.setValue(varB, 0.0);

        InferenceEngineForBN.setInferenceAlgorithmForBN(new VMP());
        InferenceEngineForBN.setModel(bn);

        InferenceEngineForBN.setEvidence(assignment);
        InferenceEngineForBN.compileModel();

        Multinomial postA = InferenceEngineForBN.getPosterior(varA);
        System.out.println("P(A) = " + postA.toString());

        assertEquals(postA.getProbabilities()[0], 0.75, 0.01);

    }

    public static void test3() {

        StaticVariables variables = new StaticVariables();
        Variable varA = variables.addHiddenMultionomialVariable("A", 2);
        Variable varB = variables.addHiddenMultionomialVariable("B", 2);
        Variable varC = variables.addHiddenMultionomialVariable("C", 2);

        DAG dag = new DAG(variables);

        dag.getParentSet(varC).addParent(varA);
        dag.getParentSet(varC).addParent(varB);

        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);

        Multinomial_MultinomialParents distA = bn.getDistribution(varA);
        Multinomial_MultinomialParents distB = bn.getDistribution(varB);
        Multinomial_MultinomialParents distC = bn.getDistribution(varC);

        distA.getMultinomial(0).setProbabilities(new double[]{0.5, 0.5});
        distB.getMultinomial(0).setProbabilities(new double[]{0.5, 0.5});

        distC.getMultinomial(0).setProbabilities(new double[]{0.25, 0.75});
        distC.getMultinomial(1).setProbabilities(new double[]{0.2, 0.8});
        distC.getMultinomial(2).setProbabilities(new double[]{0.9, 0.1});
        distC.getMultinomial(3).setProbabilities(new double[]{0.7, 0.3});

        bn.randomInitialization(new Random(0));

        System.out.println(bn.toString());


        HashMapAssignment assignment = new HashMapAssignment(1);
        assignment.setValue(varC, 0.0);

        VMP vmp = new VMP();
        InferenceEngineForBN.setInferenceAlgorithmForBN(vmp);
        InferenceEngineForBN.setModel(bn);

        Multinomial qADist = EF_DistributionBuilder.toDistribution((EF_Multinomial) vmp.nodes.get(0).getQDist());
        Multinomial qBDist = EF_DistributionBuilder.toDistribution((EF_Multinomial) vmp.nodes.get(1).getQDist());
        Multinomial qCDist = EF_DistributionBuilder.toDistribution((EF_Multinomial) vmp.nodes.get(2).getQDist());


        //InferenceEngineForBN.setEvidence(assignment);
        InferenceEngineForBN.compileModel();

        System.out.println("P(A) = " + InferenceEngineForBN.getPosterior(varA).toString());
        System.out.println("P(B) = " + InferenceEngineForBN.getPosterior(varB).toString());
        System.out.println("P(C) = " + InferenceEngineForBN.getPosterior(varC).toString());

        //assertEquals(postA.getProbabilities()[0],0.75,0.01);

        List<Variable> vars = Arrays.asList(varA, varB, varC);
        boolean convergence = false;
        double oldvalue = 0;


        while (!convergence) {

            qADist.setProbabilities(averageLog(varA, varB, varC, qBDist, qCDist, bn));

            qBDist.setProbabilities(averageLog(varB, varA, varC, qADist, qCDist, bn));

            qCDist.setProbabilities(averageLog(varC, varA, varB, qADist, qBDist, bn));

            if (Math.abs(qADist.getProbabilityOfState(0) + qBDist.getProbabilityOfState(0) + qCDist.getProbabilityOfState(0) - oldvalue) < 0.00001) {
                convergence = true;
            }

            oldvalue = qADist.getProbabilityOfState(0) + qBDist.getProbabilityOfState(0) + qCDist.getProbabilityOfState(0);
        }

        System.out.println("P'(A) = " + qADist.toString());
        System.out.println("P'(B) = " + qBDist.toString());
        System.out.println("P'(C) = " + qCDist.toString());

        assertTrue(InferenceEngineForBN.getPosterior(varA).equalDist(qADist, 0.01));
        assertTrue(InferenceEngineForBN.getPosterior(varB).equalDist(qBDist, 0.01));
        assertTrue(InferenceEngineForBN.getPosterior(varC).equalDist(qCDist, 0.01));


    }

    private static double[] averageLog(Variable mainVar, Variable var1, Variable var2, Multinomial q1, Multinomial q2, BayesianNetwork bn) {
        List<Variable> vars = Arrays.asList(mainVar, var1, var2);
        int n = MultinomialIndex.getNumberOfPossibleAssignments(vars);

        double[] output = new double[mainVar.getNumberOfStates()];
        for (int k = 0; k < output.length; k++) {
            double val = 0;
            for (int i = 0; i < n; i++) {
                Assignment localAssignment = MultinomialIndex.getVariableAssignmentFromIndex(vars, i);
                if (localAssignment.getValue(mainVar) != k)
                    continue;

                val += q1.getProbabilityOfState((int) localAssignment.getValue(var1)) * q2.getProbabilityOfState((int) localAssignment.getValue(var2)) * bn.getLogProbabiltyOf(localAssignment);
            }
            output[k] = Math.exp(val);
        }
        return Utils.normalize(output);
    }

    public static void test4() {

        StaticVariables variables = new StaticVariables();
        Variable varA = variables.addHiddenMultionomialVariable("A", 2);
        Variable varB = variables.addHiddenMultionomialVariable("B", 2);
        Variable varC = variables.addHiddenMultionomialVariable("C", 2);

        DAG dag = new DAG(variables);

        dag.getParentSet(varC).addParent(varA);
        dag.getParentSet(varC).addParent(varB);

        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);

        Multinomial_MultinomialParents distA = bn.getDistribution(varA);
        Multinomial_MultinomialParents distB = bn.getDistribution(varB);
        Multinomial_MultinomialParents distC = bn.getDistribution(varC);

        distA.getMultinomial(0).setProbabilities(new double[]{0.5, 0.5});
        distB.getMultinomial(0).setProbabilities(new double[]{0.5, 0.5});

        distC.getMultinomial(0).setProbabilities(new double[]{0.25, 0.75});
        distC.getMultinomial(1).setProbabilities(new double[]{0.2, 0.8});
        distC.getMultinomial(2).setProbabilities(new double[]{0.9, 0.1});
        distC.getMultinomial(3).setProbabilities(new double[]{0.7, 0.3});

        bn.randomInitialization(new Random(0));

        System.out.println(bn.toString());


        VMP vmp = new VMP();
        InferenceEngineForBN.setInferenceAlgorithmForBN(vmp);
        InferenceEngineForBN.setModel(bn);

        Multinomial qADist = EF_DistributionBuilder.toDistribution((EF_Multinomial) vmp.nodes.get(0).getQDist());
        Multinomial qBDist = EF_DistributionBuilder.toDistribution((EF_Multinomial) vmp.nodes.get(1).getQDist());


        HashMapAssignment assignment = new HashMapAssignment(1);
        assignment.setValue(varC, 0.0);
        InferenceEngineForBN.setEvidence(assignment);
        InferenceEngineForBN.compileModel();

        System.out.println("P(A) = " + InferenceEngineForBN.getPosterior(varA).toString());
        System.out.println("P(B) = " + InferenceEngineForBN.getPosterior(varB).toString());


        List<Variable> vars = Arrays.asList(varA, varB, varC);
        boolean convergence = false;
        double oldvalue = 0;


        while (!convergence) {

            qADist.setProbabilities(averageLogObserved(varA, varB, varC, assignment.getValue(varC), qBDist, bn));

            qBDist.setProbabilities(averageLogObserved(varB, varA, varC, assignment.getValue(varC), qADist, bn));

            if (Math.abs(qADist.getProbabilityOfState(0) + qBDist.getProbabilityOfState(0) - oldvalue) < 0.00001) {
                convergence = true;
            }

            oldvalue = qADist.getProbabilityOfState(0) + qBDist.getProbabilityOfState(0);
        }

        System.out.println("P'(A) = " + qADist.toString());
        System.out.println("P'(B) = " + qBDist.toString());

        assertTrue(InferenceEngineForBN.getPosterior(varA).equalDist(qADist, 0.01));
        assertTrue(InferenceEngineForBN.getPosterior(varB).equalDist(qBDist, 0.01));

    }

    private static double[] averageLogObserved(Variable mainVar, Variable var1, Variable observedVar, double observedVal, Multinomial q1, BayesianNetwork bn) {
        List<Variable> vars = Arrays.asList(mainVar, var1, observedVar);
        int n = MultinomialIndex.getNumberOfPossibleAssignments(vars);

        double[] output = new double[mainVar.getNumberOfStates()];
        for (int k = 0; k < output.length; k++) {
            double val = 0;
            for (int i = 0; i < n; i++) {
                Assignment localAssignment = MultinomialIndex.getVariableAssignmentFromIndex(vars, i);
                if (localAssignment.getValue(mainVar) != k || localAssignment.getValue(observedVar) != observedVal)
                    continue;

                val += q1.getProbabilityOfState((int) localAssignment.getValue(var1)) * bn.getLogProbabiltyOf(localAssignment);
            }
            output[k] = Math.exp(val);
        }
        return Utils.normalize(output);
    }

    public static void test5() {

        StaticVariables variables = new StaticVariables();
        Variable varA = variables.addHiddenMultionomialVariable("A", 2);
        Variable varB = variables.addHiddenMultionomialVariable("B", 2);
        Variable varC = variables.addHiddenMultionomialVariable("C", 2);

        DAG dag = new DAG(variables);

        dag.getParentSet(varC).addParent(varA);
        dag.getParentSet(varC).addParent(varB);
        dag.getParentSet(varB).addParent(varA);

        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);

        Multinomial_MultinomialParents distA = bn.getDistribution(varA);
        Multinomial_MultinomialParents distB = bn.getDistribution(varB);
        Multinomial_MultinomialParents distC = bn.getDistribution(varC);

        bn.randomInitialization(new Random(0));

        System.out.println(bn.toString());


        HashMapAssignment assignment = new HashMapAssignment(1);
        assignment.setValue(varC, 0.0);

        VMP vmp = new VMP();
        InferenceEngineForBN.setInferenceAlgorithmForBN(vmp);
        InferenceEngineForBN.setModel(bn);

        Multinomial qADist = EF_DistributionBuilder.toDistribution((EF_Multinomial) vmp.nodes.get(0).getQDist());
        Multinomial qBDist = EF_DistributionBuilder.toDistribution((EF_Multinomial) vmp.nodes.get(1).getQDist());
        Multinomial qCDist = EF_DistributionBuilder.toDistribution((EF_Multinomial) vmp.nodes.get(2).getQDist());


        //InferenceEngineForBN.setEvidence(assignment);
        InferenceEngineForBN.compileModel();

        System.out.println("P(A) = " + InferenceEngineForBN.getPosterior(varA).toString());
        System.out.println("P(B) = " + InferenceEngineForBN.getPosterior(varB).toString());
        System.out.println("P(C) = " + InferenceEngineForBN.getPosterior(varC).toString());

        //assertEquals(postA.getProbabilities()[0],0.75,0.01);

        List<Variable> vars = Arrays.asList(varA, varB, varC);
        boolean convergence = false;
        double oldvalue = 0;


        while (!convergence) {

            qADist.setProbabilities(averageLog(varA, varB, varC, qBDist, qCDist, bn));

            qBDist.setProbabilities(averageLog(varB, varA, varC, qADist, qCDist, bn));

            qCDist.setProbabilities(averageLog(varC, varA, varB, qADist, qBDist, bn));

            if (Math.abs(qADist.getProbabilityOfState(0) + qBDist.getProbabilityOfState(0) + qCDist.getProbabilityOfState(0) - oldvalue) < 0.00001) {
                convergence = true;
            }

            oldvalue = qADist.getProbabilityOfState(0) + qBDist.getProbabilityOfState(0) + qCDist.getProbabilityOfState(0);
        }

        System.out.println("P'(A) = " + qADist.toString());
        System.out.println("P'(B) = " + qBDist.toString());
        System.out.println("P'(C) = " + qCDist.toString());

        assertTrue(InferenceEngineForBN.getPosterior(varA).equalDist(qADist, 0.01));
        assertTrue(InferenceEngineForBN.getPosterior(varB).equalDist(qBDist, 0.01));
        assertTrue(InferenceEngineForBN.getPosterior(varC).equalDist(qCDist, 0.01));


    }

    public static void test6() {

        StaticVariables variables = new StaticVariables();
        Variable varA = variables.addHiddenMultionomialVariable("A", 2);
        Variable varB = variables.addHiddenMultionomialVariable("B", 2);
        Variable varC = variables.addHiddenMultionomialVariable("C", 2);

        DAG dag = new DAG(variables);

        dag.getParentSet(varA).addParent(varC);
        dag.getParentSet(varB).addParent(varC);

        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);

        Multinomial_MultinomialParents distA = bn.getDistribution(varA);
        Multinomial_MultinomialParents distB = bn.getDistribution(varB);
        Multinomial_MultinomialParents distC = bn.getDistribution(varC);

        bn.randomInitialization(new Random(0));

        System.out.println(bn.toString());


        HashMapAssignment assignment = new HashMapAssignment(1);
        assignment.setValue(varC, 0.0);

        VMP vmp = new VMP();
        InferenceEngineForBN.setInferenceAlgorithmForBN(vmp);
        InferenceEngineForBN.setModel(bn);

        Multinomial qADist = EF_DistributionBuilder.toDistribution((EF_Multinomial) vmp.nodes.get(0).getQDist());
        Multinomial qBDist = EF_DistributionBuilder.toDistribution((EF_Multinomial) vmp.nodes.get(1).getQDist());
        Multinomial qCDist = EF_DistributionBuilder.toDistribution((EF_Multinomial) vmp.nodes.get(2).getQDist());


        //InferenceEngineForBN.setEvidence(assignment);
        InferenceEngineForBN.compileModel();

        System.out.println("P(A) = " + InferenceEngineForBN.getPosterior(varA).toString());
        System.out.println("P(B) = " + InferenceEngineForBN.getPosterior(varB).toString());
        System.out.println("P(C) = " + InferenceEngineForBN.getPosterior(varC).toString());

        //assertEquals(postA.getProbabilities()[0],0.75,0.01);

        List<Variable> vars = Arrays.asList(varA, varB, varC);
        boolean convergence = false;
        double oldvalue = 0;


        while (!convergence) {

            qADist.setProbabilities(averageLog(varA, varB, varC, qBDist, qCDist, bn));

            qBDist.setProbabilities(averageLog(varB, varA, varC, qADist, qCDist, bn));

            qCDist.setProbabilities(averageLog(varC, varA, varB, qADist, qBDist, bn));

            if (Math.abs(qADist.getProbabilityOfState(0) + qBDist.getProbabilityOfState(0) + qCDist.getProbabilityOfState(0) - oldvalue) < 0.00001) {
                convergence = true;
            }

            oldvalue = qADist.getProbabilityOfState(0) + qBDist.getProbabilityOfState(0) + qCDist.getProbabilityOfState(0);
        }

        System.out.println("P'(A) = " + qADist.toString());
        System.out.println("P'(B) = " + qBDist.toString());
        System.out.println("P'(C) = " + qCDist.toString());

        assertTrue(InferenceEngineForBN.getPosterior(varA).equalDist(qADist, 0.01));
        assertTrue(InferenceEngineForBN.getPosterior(varB).equalDist(qBDist, 0.01));
        assertTrue(InferenceEngineForBN.getPosterior(varC).equalDist(qCDist, 0.01));


    }

    public static void test7() {

        StaticVariables variables = new StaticVariables();
        Variable varA = variables.addHiddenMultionomialVariable("A", 2);
        Variable varB = variables.addHiddenMultionomialVariable("B", 2);
        Variable varC = variables.addHiddenMultionomialVariable("C", 2);

        DAG dag = new DAG(variables);

        dag.getParentSet(varA).addParent(varC);
        dag.getParentSet(varB).addParent(varC);
        dag.getParentSet(varB).addParent(varA);


        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);

        Multinomial_MultinomialParents distA = bn.getDistribution(varA);
        Multinomial_MultinomialParents distB = bn.getDistribution(varB);
        Multinomial_MultinomialParents distC = bn.getDistribution(varC);

        bn.randomInitialization(new Random(0));

        System.out.println(bn.toString());


        HashMapAssignment assignment = new HashMapAssignment(1);
        assignment.setValue(varC, 0.0);

        VMP vmp = new VMP();
        InferenceEngineForBN.setInferenceAlgorithmForBN(vmp);
        InferenceEngineForBN.setModel(bn);

        Multinomial qADist = EF_DistributionBuilder.toDistribution((EF_Multinomial) vmp.nodes.get(0).getQDist());
        Multinomial qBDist = EF_DistributionBuilder.toDistribution((EF_Multinomial) vmp.nodes.get(1).getQDist());
        Multinomial qCDist = EF_DistributionBuilder.toDistribution((EF_Multinomial) vmp.nodes.get(2).getQDist());


        //InferenceEngineForBN.setEvidence(assignment);
        InferenceEngineForBN.compileModel();

        System.out.println("P(A) = " + InferenceEngineForBN.getPosterior(varA).toString());
        System.out.println("P(B) = " + InferenceEngineForBN.getPosterior(varB).toString());
        System.out.println("P(C) = " + InferenceEngineForBN.getPosterior(varC).toString());

        //assertEquals(postA.getProbabilities()[0],0.75,0.01);

        List<Variable> vars = Arrays.asList(varA, varB, varC);
        boolean convergence = false;
        double oldvalue = 0;


        while (!convergence) {

            qADist.setProbabilities(averageLog(varA, varB, varC, qBDist, qCDist, bn));

            qBDist.setProbabilities(averageLog(varB, varA, varC, qADist, qCDist, bn));

            qCDist.setProbabilities(averageLog(varC, varA, varB, qADist, qBDist, bn));

            if (Math.abs(qADist.getProbabilityOfState(0) + qBDist.getProbabilityOfState(0) + qCDist.getProbabilityOfState(0) - oldvalue) < 0.00001) {
                convergence = true;
            }

            oldvalue = qADist.getProbabilityOfState(0) + qBDist.getProbabilityOfState(0) + qCDist.getProbabilityOfState(0);
        }

        System.out.println("P'(A) = " + qADist.toString());
        System.out.println("P'(B) = " + qBDist.toString());
        System.out.println("P'(C) = " + qCDist.toString());

        assertTrue(InferenceEngineForBN.getPosterior(varA).equalDist(qADist, 0.01));
        assertTrue(InferenceEngineForBN.getPosterior(varB).equalDist(qBDist, 0.01));
        assertTrue(InferenceEngineForBN.getPosterior(varC).equalDist(qCDist, 0.01));


    }

    public static void test8() {

        StaticVariables variables = new StaticVariables();
        Variable varA = variables.addHiddenMultionomialVariable("A", 2);
        Variable varB = variables.addHiddenMultionomialVariable("B", 2);
        Variable varC = variables.addHiddenMultionomialVariable("C", 2);

        DAG dag = new DAG(variables);

        dag.getParentSet(varA).addParent(varC);
        dag.getParentSet(varB).addParent(varC);

        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);

        Multinomial_MultinomialParents distA = bn.getDistribution(varA);
        Multinomial_MultinomialParents distB = bn.getDistribution(varB);
        Multinomial_MultinomialParents distC = bn.getDistribution(varC);


        bn.randomInitialization(new Random(0));

        System.out.println(bn.toString());


        VMP vmp = new VMP();
        InferenceEngineForBN.setInferenceAlgorithmForBN(vmp);
        InferenceEngineForBN.setModel(bn);

        Multinomial qADist = EF_DistributionBuilder.toDistribution((EF_Multinomial) vmp.nodes.get(0).getQDist());
        Multinomial qBDist = EF_DistributionBuilder.toDistribution((EF_Multinomial) vmp.nodes.get(1).getQDist());


        HashMapAssignment assignment = new HashMapAssignment(1);
        assignment.setValue(varC, 0.0);
        InferenceEngineForBN.setEvidence(assignment);
        InferenceEngineForBN.compileModel();

        System.out.println("P(A) = " + InferenceEngineForBN.getPosterior(varA).toString());
        System.out.println("P(B) = " + InferenceEngineForBN.getPosterior(varB).toString());


        List<Variable> vars = Arrays.asList(varA, varB, varC);
        boolean convergence = false;
        double oldvalue = 0;


        while (!convergence) {

            qADist.setProbabilities(averageLogObserved(varA, varB, varC, assignment.getValue(varC), qBDist, bn));

            qBDist.setProbabilities(averageLogObserved(varB, varA, varC, assignment.getValue(varC), qADist, bn));

            if (Math.abs(qADist.getProbabilityOfState(0) + qBDist.getProbabilityOfState(0) - oldvalue) < 0.00001) {
                convergence = true;
            }

            oldvalue = qADist.getProbabilityOfState(0) + qBDist.getProbabilityOfState(0);
        }

        System.out.println("P'(A) = " + qADist.toString());
        System.out.println("P'(B) = " + qBDist.toString());

        assertTrue(InferenceEngineForBN.getPosterior(varA).equalDist(qADist, 0.01));
        assertTrue(InferenceEngineForBN.getPosterior(varB).equalDist(qBDist, 0.01));

    }


    public static void test9() {

        StaticVariables variables = new StaticVariables();
        Variable varA = variables.addHiddenMultionomialVariable("A", 2);
        Variable varB = variables.addHiddenMultionomialVariable("B", 2);
        Variable varC = variables.addHiddenMultionomialVariable("C", 2);

        DAG dag = new DAG(variables);

        dag.getParentSet(varA).addParent(varC);
        dag.getParentSet(varB).addParent(varC);

        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);

        Multinomial_MultinomialParents distA = bn.getDistribution(varA);
        Multinomial_MultinomialParents distB = bn.getDistribution(varB);
        Multinomial_MultinomialParents distC = bn.getDistribution(varC);


        bn.randomInitialization(new Random(0));

        System.out.println(bn.toString());


        VMP vmp = new VMP();
        InferenceEngineForBN.setInferenceAlgorithmForBN(vmp);
        InferenceEngineForBN.setModel(bn);

        Multinomial qCDist = EF_DistributionBuilder.toDistribution((EF_Multinomial) vmp.nodes.get(varC.getVarID()).getQDist());
        Multinomial qBDist = EF_DistributionBuilder.toDistribution((EF_Multinomial) vmp.nodes.get(varB.getVarID()).getQDist());


        HashMapAssignment assignment = new HashMapAssignment(1);
        assignment.setValue(varA, 0.0);
        InferenceEngineForBN.setEvidence(assignment);
        InferenceEngineForBN.compileModel();

        System.out.println("P(C) = " + InferenceEngineForBN.getPosterior(varC).toString());
        System.out.println("P(B) = " + InferenceEngineForBN.getPosterior(varB).toString());


        List<Variable> vars = Arrays.asList(varA, varB, varC);
        boolean convergence = false;
        double oldvalue = 0;


        while (!convergence) {

            qCDist.setProbabilities(averageLogObserved(varC, varB, varA, assignment.getValue(varA), qBDist, bn));

            qBDist.setProbabilities(averageLogObserved(varB, varC, varA, assignment.getValue(varA), qCDist, bn));

            if (Math.abs(qCDist.getProbabilityOfState(0) + qBDist.getProbabilityOfState(0) - oldvalue) < 0.00001) {
                convergence = true;
            }

            oldvalue = qCDist.getProbabilityOfState(0) + qBDist.getProbabilityOfState(0);
        }

        System.out.println("P'(C) = " + qCDist.toString());
        System.out.println("P'(B) = " + qBDist.toString());

        assertTrue(InferenceEngineForBN.getPosterior(varC).equalDist(qCDist, 0.01));
        assertTrue(InferenceEngineForBN.getPosterior(varB).equalDist(qBDist, 0.01));

    }


    public static void test10() {

        StaticVariables variables = new StaticVariables();
        Variable varA = variables.addHiddenMultionomialVariable("A", 2);
        Variable varB = variables.addHiddenMultionomialVariable("B", 2);
        Variable varC = variables.addHiddenMultionomialVariable("C", 2);

        DAG dag = new DAG(variables);

        dag.getParentSet(varA).addParent(varC);
        dag.getParentSet(varB).addParent(varC);

        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);

        Multinomial_MultinomialParents distA = bn.getDistribution(varA);
        Multinomial_MultinomialParents distB = bn.getDistribution(varB);
        Multinomial_MultinomialParents distC = bn.getDistribution(varC);

        distC.getMultinomial(0).setProbabilities(new double[]{0.5, 0.5});

        distA.getMultinomial(0).setProbabilities(new double[]{0.7, 0.3});
        distA.getMultinomial(1).setProbabilities(new double[]{0.2, 0.8});

        distB.getMultinomial(0).setProbabilities(new double[]{0.75, 0.25});
        distB.getMultinomial(1).setProbabilities(new double[]{0.25, 0.75});

        System.out.println(bn.toString());


        VMP vmp = new VMP();
        InferenceEngineForBN.setInferenceAlgorithmForBN(vmp);
        InferenceEngineForBN.setModel(bn);

        Multinomial qCDist = EF_DistributionBuilder.toDistribution((EF_Multinomial) vmp.nodes.get(varC.getVarID()).getQDist());
        Multinomial qBDist = EF_DistributionBuilder.toDistribution((EF_Multinomial) vmp.nodes.get(varB.getVarID()).getQDist());


        HashMapAssignment assignment = new HashMapAssignment(1);
        assignment.setValue(varA, 0.0);
        assignment.setValue(varB, 0.0);

        InferenceEngineForBN.setEvidence(assignment);
        InferenceEngineForBN.compileModel();

        System.out.println("P(C) = " + InferenceEngineForBN.getPosterior(varC).toString());

        double result = 0.7 * 0.75 * 0.5 / (0.7 * 0.75 * 0.5 + 0.2 * 0.25 * 0.5);
        assertEquals(InferenceEngineForBN.getPosterior(varC).getProbability(0), result, 0.01);

    }

    public static void test11() {

        StaticVariables variables = new StaticVariables();
        Variable varA = variables.addHiddenMultionomialVariable("A", 2);
        Variable varB = variables.addHiddenMultionomialVariable("B", 2);
        Variable varC = variables.addHiddenMultionomialVariable("C", 2);

        DAG dag = new DAG(variables);

        dag.getParentSet(varB).addParent(varA);
        dag.getParentSet(varC).addParent(varB);


        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);

        Multinomial_MultinomialParents distA = bn.getDistribution(varA);
        Multinomial_MultinomialParents distB = bn.getDistribution(varB);
        Multinomial_MultinomialParents distC = bn.getDistribution(varC);

        bn.randomInitialization(new Random(0));

        System.out.println(bn.toString());


        HashMapAssignment assignment = new HashMapAssignment(1);
        assignment.setValue(varC, 0.0);

        VMP vmp = new VMP();
        InferenceEngineForBN.setInferenceAlgorithmForBN(vmp);
        InferenceEngineForBN.setModel(bn);

        Multinomial qADist = EF_DistributionBuilder.toDistribution((EF_Multinomial) vmp.nodes.get(0).getQDist());
        Multinomial qBDist = EF_DistributionBuilder.toDistribution((EF_Multinomial) vmp.nodes.get(1).getQDist());
        Multinomial qCDist = EF_DistributionBuilder.toDistribution((EF_Multinomial) vmp.nodes.get(2).getQDist());


        //InferenceEngineForBN.setEvidence(assignment);
        InferenceEngineForBN.compileModel();

        System.out.println("P(A) = " + InferenceEngineForBN.getPosterior(varA).toString());
        System.out.println("P(B) = " + InferenceEngineForBN.getPosterior(varB).toString());
        System.out.println("P(C) = " + InferenceEngineForBN.getPosterior(varC).toString());

        //assertEquals(postA.getProbabilities()[0],0.75,0.01);

        List<Variable> vars = Arrays.asList(varA, varB, varC);
        boolean convergence = false;
        double oldvalue = 0;


        while (!convergence) {

            qADist.setProbabilities(averageLog(varA, varB, varC, qBDist, qCDist, bn));

            qBDist.setProbabilities(averageLog(varB, varA, varC, qADist, qCDist, bn));

            qCDist.setProbabilities(averageLog(varC, varA, varB, qADist, qBDist, bn));

            if (Math.abs(qADist.getProbabilityOfState(0) + qBDist.getProbabilityOfState(0) + qCDist.getProbabilityOfState(0) - oldvalue) < 0.00001) {
                convergence = true;
            }

            oldvalue = qADist.getProbabilityOfState(0) + qBDist.getProbabilityOfState(0) + qCDist.getProbabilityOfState(0);
        }

        System.out.println("P'(A) = " + qADist.toString());
        System.out.println("P'(B) = " + qBDist.toString());
        System.out.println("P'(C) = " + qCDist.toString());

        assertTrue(InferenceEngineForBN.getPosterior(varA).equalDist(qADist, 0.01));
        assertTrue(InferenceEngineForBN.getPosterior(varB).equalDist(qBDist, 0.01));
        assertTrue(InferenceEngineForBN.getPosterior(varC).equalDist(qCDist, 0.01));


    }

    public static void test12() {

        StaticVariables variables = new StaticVariables();
        Variable varA = variables.addHiddenMultionomialVariable("A", 2);
        Variable varB = variables.addHiddenMultionomialVariable("B", 2);

        DAG dag = new DAG(variables);

        dag.getParentSet(varB).addParent(varA);

        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);

        Multinomial_MultinomialParents distA = bn.getDistribution(varA);
        Multinomial_MultinomialParents distB = bn.getDistribution(varB);

        distA.getMultinomial(0).setProbabilities(new double[]{0.9, 0.1});
        distB.getMultinomial(0).setProbabilities(new double[]{1.0, 0.0});
        distB.getMultinomial(1).setProbabilities(new double[]{0.0, 1.0});

        //bn.randomInitialization(new Random(0));

        double[] pA = distA.getMultinomial(0).getProbabilities();
        double[][] pB = new double[2][];
        pB[0] = distB.getMultinomial(0).getProbabilities();
        pB[1] = distB.getMultinomial(1).getProbabilities();

        System.out.println(bn.toString());


        HashMapAssignment assignment = new HashMapAssignment(1);
        assignment.setValue(varB, 1.0);

        VMP vmp = new VMP();
        InferenceEngineForBN.setInferenceAlgorithmForBN(vmp);
        InferenceEngineForBN.setModel(bn);

        EF_Multinomial qADist = ((EF_Multinomial) vmp.nodes.get(0).getQDist());
        EF_Multinomial qBDist = ((EF_Multinomial) vmp.nodes.get(1).getQDist());

        double[] qA = new double[2];
        qA[0] = qADist.getMomentParameters().get(0);
        qA[1] = qADist.getMomentParameters().get(1);

        double[] qB = new double[2];
        qB[0] = qBDist.getMomentParameters().get(0);
        qB[1] = qBDist.getMomentParameters().get(1);

        //InferenceEngineForBN.setEvidence(assignment);
        InferenceEngineForBN.compileModel();

        Multinomial postA = InferenceEngineForBN.getPosterior(varA);
        System.out.println("P(A) = " + postA.toString());
        Multinomial postB = InferenceEngineForBN.getPosterior(varB);
        System.out.println("P(B) = " + postB.toString());


        boolean convergence = false;
        double oldvalue = 0;
        while (!convergence) {

            qA[0] = Math.exp(qB[0] * Math.log(pB[0][0]) + qB[1] * Math.log(pB[0][1]) + Math.log(pA[0]));
            qA[1] = Math.exp(qB[0] * Math.log(pB[1][0]) + qB[1] * Math.log(pB[1][1]) + Math.log(pA[1]));

            Utils.normalize(qA);

            qB[0] = Math.exp(qA[0] * Math.log(pB[0][0] * pA[0]) + qA[1] * Math.log(pB[1][0] * pA[1]));
            qB[1] = Math.exp(qA[0] * Math.log(pB[0][1] * pA[0]) + qA[1] * Math.log(pB[1][1] * pA[1]));

            Utils.normalize(qB);

            if (Math.abs(qA[0] + qB[0] - oldvalue) < 0.001) {
                convergence = true;
            }

            oldvalue = qA[0] + qB[0];


        }
        System.out.println(qA[0]);
        System.out.println(qB[0]);

        assertEquals(postA.getProbabilities()[0], qA[0], 0.01);
        assertEquals(postB.getProbabilities()[0], qB[0], 0.01);

    }


    public static void test13() {

        StaticVariables variables = new StaticVariables();

        Variable varA = variables.addHiddenMultionomialVariable("A", 4);

        int nVar = 3;
        for (int i = 0; i < nVar; i++) {
            variables.addHiddenMultionomialVariable(i+"", 4);
        }

        DAG dag = new DAG(variables);

        for (int i = 0; i < nVar; i++) {
            //dag.getParentSet(variables.getVariableByName(i+"")).addParent(varA);
            dag.getParentSet(varA).addParent(variables.getVariableByName(i+""));

        }

        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);
        bn.randomInitialization(new Random(0));

        InferenceEngineForBN.setModel(bn);
        InferenceEngineForBN.compileModel();

        System.out.println("P(A) = " + InferenceEngineForBN.getPosterior(varA).toString());

    }

}