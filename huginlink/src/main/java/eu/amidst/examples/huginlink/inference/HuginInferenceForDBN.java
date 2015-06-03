package eu.amidst.examples.huginlink.inference;

import COM.hugin.HAPI.*;
import COM.hugin.HAPI.Class;
import eu.amidst.examples.core.distribution.Multinomial;
import eu.amidst.examples.core.distribution.UnivariateDistribution;
import eu.amidst.examples.core.inference.InferenceAlgorithmForDBN;
import eu.amidst.examples.core.models.DynamicBayesianNetwork;
import eu.amidst.examples.core.variables.DynamicAssignment;
import eu.amidst.examples.core.variables.HashMapAssignment;
import eu.amidst.examples.core.variables.Variable;
import eu.amidst.examples.huginlink.converters.DBNConverterToHugin;

import java.util.List;

/**
 * This class provides an interface to perform inference over Dynamic Bayesian networks using the Hugin inference
 * engine.
 *
 * @author Antonio Fernández
 * @version 1.0
 * @since 23/2/15
 */
public class HuginInferenceForDBN implements InferenceAlgorithmForDBN {

    /**
     * The Dynamic Bayesian network model in AMIDST format.
     */
    public DynamicBayesianNetwork amidstDBN;

    /**
     * The Dynamic Bayesian network model in Hugin format.
     */
    public Class huginDBN;

    /**
     * The expanded dynamic model in which the inference is performed.
     */
    public Domain domainObject;

    /**
     * The number of time slices in which the model is expanded. For now we assume a <code>timeWindow</code> equals to 1.
     */
    public static final int timeWindow = 1;

    /**
     * The AMIDST assignment to be evidenced into the Hugin model.
     */
    private DynamicAssignment assignment = new HashMapAssignment(0);

    /**
     * The time ID of the current assignment being processed.
     */
    int timeID;

    /**
     * The sequence ID of the current assignment being processed. For the Cajamar case this corresponds to the client ID.
     */
    int sequenceID;

    /**
     * Class constructor.
     */
    public HuginInferenceForDBN() {
        this.timeID=-1;
    }

    @Override
    public DynamicBayesianNetwork getOriginalModel() {
        return this.amidstDBN;
    }

    @Override
    public void setModel(DynamicBayesianNetwork model) {

        this.amidstDBN = model;

        try {
            this.huginDBN = DBNConverterToHugin.convertToHugin(model);
        } catch (ExceptionHugin exceptionHugin) {
            exceptionHugin.printStackTrace();
        }

        try {
            this.domainObject = this.huginDBN.createDBNDomain(timeWindow);
        } catch (ExceptionHugin exceptionHugin) {
            exceptionHugin.printStackTrace();
        }
    }

    @Override
    public void addDynamicEvidence(DynamicAssignment assignment_) {


        if (this.sequenceID != -1 && this.sequenceID != assignment_.getSequenceID())
            throw new IllegalArgumentException("The sequence ID does not match. If you want to change the sequence, invoke reset method");

        if (this.timeID >= assignment_.getTimeID())
            throw new IllegalArgumentException("The provided assignment is not posterior to the previous provided assignment.");

        this.assignment = assignment_;

    }
    @Override
    public void runInference() {
        try {
            domainObject.uncompile();
            if (assignment.getTimeID()==0) {
                this.setAssignmentToHuginModel(this.assignment, 0);
                this.timeID = 0;
            } else{
                this.timeID = this.getTimeIDOfLastEvidence();
                this.setAssignmentToHuginModel(this.assignment, 1);
            }

            domainObject.triangulateDBN(Domain.H_TM_TOTAL_WEIGHT);
            domainObject.compile();

        } catch (ExceptionHugin exceptionHugin) {
            exceptionHugin.printStackTrace();
        }
    }

    /**
     * Sets the AMIDST evidence into a given time slice of the expanded Hugin model.
     * @param assignment the evidence to be propagated.
     * @param time the time slice in which the evidence is entered.
     */
    public void setAssignmentToHuginModel(DynamicAssignment assignment, int time) {
        List<Variable> dynamicVariables = amidstDBN.getDynamicVariables().getListOfDynamicVariables();
        for (Variable var : dynamicVariables) {
            //Skip non-observed variables
            if(!Double.isNaN(assignment.getValue(var))){
                if (var.isMultinomial()) {
                    try {
                        LabelledDCNode node = (LabelledDCNode) domainObject.getNodeByName("T" + time + "." + var.getName());
                        node.selectState((long) assignment.getValue(var));
                    } catch (ExceptionHugin exceptionHugin) {
                        exceptionHugin.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public void reset() {

        // Retract evidence for all nodes of domain, move the time window back to its initial position,
        // and establish the initial state of the inference engine.
        //--------------------------------------------------------------------------------------------------------------
        try {
            this.domainObject.initializeDBNWindow();
        } catch (ExceptionHugin exceptionHugin) {
            exceptionHugin.printStackTrace();
        }
        //--------------------------------------------------------------------------------------------------------------

        this.timeID = -1;
        this.sequenceID=-1;
    }

    @Override
    public <E extends UnivariateDistribution> E getFilteredPosterior(Variable var) {
        UnivariateDistribution posteriorDistribution = null;

        String targetVariableName = (this.timeID==0)? "T0." : "T"+this.timeWindow+".";
        try {
            LabelledDCNode node = (LabelledDCNode) domainObject.getNodeByName(targetVariableName + var.getName());
            posteriorDistribution = var.newUnivariateDistribution();

            double[] probabilities = new double[(int) node.getNumberOfStates()];
            for (int i = 0; i < node.getNumberOfStates(); i++) {
                probabilities[i] = node.getBelief(i);
            }
            ((Multinomial) posteriorDistribution).setProbabilities(probabilities);
            domainObject.moveDBNWindow(1);

        } catch (ExceptionHugin exceptionHugin) {
            exceptionHugin.printStackTrace();
        }
        return ((E) posteriorDistribution);
    }

    @Override
    public <E extends UnivariateDistribution> E getPredictivePosterior(Variable var, int nTimesAhead) {

        UnivariateDistribution posteriorDistribution = null;

        try {
            LabelledDCNode node = (LabelledDCNode) domainObject.getNodeByName("T"+this.timeWindow+"." + var.getName());
            this.domainObject.computeDBNPredictions(nTimesAhead);
            posteriorDistribution = var.newUnivariateDistribution();

            double[] probabilities = new double[(int) node.getNumberOfStates()];
            for (int i = 0; i < node.getNumberOfStates(); i++) {
                probabilities[i] = node.getPredictedBelief(i,nTimesAhead-1);
            }

            ((Multinomial) posteriorDistribution).setProbabilities(probabilities);
        } catch (ExceptionHugin exceptionHugin) {
            exceptionHugin.printStackTrace();
        }
        return (E)posteriorDistribution;
    }

    @Override
    public int getTimeIDOfLastEvidence() {
        return this.assignment.getTimeID();
    }

    @Override
    public int getTimeIDOfPosterior() {
        return this.timeID;
    }
}
