/**
 ******************* ISSUE LIST **************************
 *
 * 1. (Andres) getListOfVariables should return a Set instead of a List.
 *
 * ********************************************************
 */

package eu.amidst.core.models;

import eu.amidst.core.distribution.*;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.StaticVariables;
import eu.amidst.core.variables.Variable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Created by afa on 02/07/14.
 */


public final class BayesianNetwork {

    private List<ConditionalDistribution> distributions;

    private DAG dag;

    public static BayesianNetwork newBayesianNetwork(DAG dag) {
        return new BayesianNetwork(dag);
    }

    public static BayesianNetwork newBayesianNetwork(DAG dag, List<ConditionalDistribution> dists) {
        return new BayesianNetwork(dag, dists);
    }

    private BayesianNetwork(DAG dag) {
        this.dag = dag;
        initializeDistributions();
    }

    private BayesianNetwork(DAG dag, List<ConditionalDistribution> dists) {
        this.dag = dag;
        this.distributions = dists;
    }

    public <E extends ConditionalDistribution> E getDistribution(Variable var) {
        return (E) distributions.get(var.getVarID());
    }

    public int getNumberOfVars() {
        return this.getDAG().getStaticVariables().getNumberOfVars();
    }

    public StaticVariables getStaticVariables() {
        return this.getDAG().getStaticVariables();
    }

    public DAG getDAG() {
        return dag;
    }

    // public List<Variable> getListOfVariables() {
    //     return this.getStaticVariables().getListOfVariables();
    // }

    private void initializeDistributions() {


        this.distributions = new ArrayList(this.getNumberOfVars());


        /* Initialize the distribution for each variable depending on its distribution type
        as well as the distribution type of its parent set (if that variable has parents)
         */
        for (Variable var : getStaticVariables()) {
            ParentSet parentSet = this.getDAG().getParentSet(var);

            int varID = var.getVarID();
            this.distributions.add(varID, DistributionBuilder.newDistribution(var, parentSet.getParents()));
            parentSet.blockParents();
        }

        this.distributions = Collections.unmodifiableList(this.distributions);
    }

    public double getLogProbabiltyOfFullAssignment(Assignment assignment) {
        double logProb = 0;
        for (Variable var : this.getStaticVariables()) {
            if (assignment.getValue(var) == Utils.missingValue()) {
                throw new UnsupportedOperationException("This method can not compute the probabilty of a partial assignment.");
            }

            logProb += this.distributions.get(var.getVarID()).getLogConditionalProbability(assignment);
        }
        return logProb;
    }

    public List<ConditionalDistribution> getDistributions() {
        return this.distributions;
    }

    public String toString() {

        StringBuilder str = new StringBuilder();
        str.append("Bayesian Network:\n");

        for (Variable var : this.getStaticVariables()) {

            if (this.getDAG().getParentSet(var).getNumberOfParents() == 0) {
                str.append("P(" + var.getName() + " [" + var.getDistributionType().toString() + "]) follows a ");
                str.append(this.getDistribution(var).label() + "\n");
            } else {
                str.append("P(" + var.getName() + " [" + var.getDistributionType().toString() + "]" + " : ");

                for (Variable parent : this.getDAG().getParentSet(var)) {
                    str.append(parent.getName() + " [" + parent.getDistributionType().toString() + "], ");
                }
                if (this.getDAG().getParentSet(var).getNumberOfParents() > 0) {
                    str.substring(0, str.length() - 2);
                    str.append(") follows a ");
                    str.append(this.getDistribution(var).label() + "\n");
                }
            }
            //Variable distribution
            str.append(this.getDistribution(var).toString() + "\n");
        }
        return str.toString();
    }

    public void randomInitialization(Random random) {
        this.distributions.stream().forEach(w -> w.randomInitialization(random));
    }

    //public boolean equals(BayesianNetwork bayesianNetwork, double threshold) {

    //    this.getDistributions().stream().mapToInt(dist -> dist.equalDist(bayesianNetwork.getDistributions(),threshold));

    //}

}

