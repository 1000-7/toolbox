package eu.amidst.core.distribution;

import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created by andresmasegosa on 23/11/14.
 */
public class IndicatorDistribution extends ConditionalDistribution {

    private ConditionalDistribution conditionalDistribution;
    private DeltaDistribution deltaDist;
    private Variable indicatorVar;

    public IndicatorDistribution(Variable indicatorVar1, ConditionalDistribution conditionalDistribution1) {
        if (!indicatorVar1.isIndicator()) {
            throw new IllegalArgumentException("IndicatorVar_ should be of indicator type");
        }
        this.var = conditionalDistribution1.getVariable();
        this.parents = new ArrayList<>();
        for (Variable var: conditionalDistribution1.getConditioningVariables()){
            this.parents.add(var);
        }

        this.parents.add(indicatorVar1);
        this.conditionalDistribution=conditionalDistribution1;
        this.indicatorVar = indicatorVar1;
        this.deltaDist = new DeltaDistribution(this.getVariable(), 0.0);
    }

    public ConditionalDistribution getConditionalDistribution() {
        return conditionalDistribution;
    }

    public Variable getIndicatorVar() {
        return indicatorVar;
    }

    //TODO: I'm not sure about how to compute this
    @Override
    public int getNumberOfFreeParameters() {
        return 0;
    }

    @Override
    public double getLogConditionalProbability(Assignment assignment) {
        if (assignment.getValue(this.indicatorVar)==0.0) {
            return 0.0; //this.deltaDist.getLogProbability(assignment.getValue(this.var)); //Both the indicator and main var has, by definition, the same value.
        }else {
            return this.conditionalDistribution.getLogConditionalProbability(assignment);
        }
    }

    @Override
    public UnivariateDistribution getUnivariateDistribution(Assignment assignment) {
        if (assignment.getValue(this.indicatorVar)==0.0) {
            return this.deltaDist;
        }else{
            return this.conditionalDistribution.getUnivariateDistribution(assignment);
        }
    }

    public String label(){
        return "IndicatorDistribution of "+this.getConditionalDistribution().label();
    }

    @Override
    public void randomInitialization(Random random) {
        this.conditionalDistribution.randomInitialization(random);
    }

    @Override
    public boolean equalDist(Distribution dist, double threshold) {
        if (dist.getClass().getName().equals("eu.amidst.core.distribution.IndicatorDistribution"))
            return this.equalDist((IndicatorDistribution)dist,threshold);
        return false;
    }

    @Override
    //TODO
    public String toString() {
        return null;
    }

    public boolean equalDist(IndicatorDistribution dist, double threshold) {
        return this.getConditionalDistribution().equalDist(dist.getConditionalDistribution(),threshold);
    }
}
