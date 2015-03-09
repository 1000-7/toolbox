package eu.amidst.core.inference;

import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.datastream.DynamicDataInstance;
import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.exponentialfamily.*;
import eu.amidst.core.inference.VMP_.Node;
import eu.amidst.core.io.DynamicDataStreamLoader;
import eu.amidst.core.learning.DynamicNaiveBayesClassifier;
import eu.amidst.core.models.DynamicBayesianNetwork;
import eu.amidst.core.utils.Utils;
import eu.amidst.core.variables.DynamicAssignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by andresmasegosa on 12/02/15.
 */
public class DynamicVMP implements InferenceAlgorithmForDBN {


    DynamicBayesianNetwork model;
    EF_DynamicBayesianNetwork ef_model;
    DynamicAssignment assignment = new HashMapAssignment(0);

    List<Node>  nodesTimeT;
    List<Node>  nodesClone;

    VMP vmpTime0;
    VMP vmpTimeT;

    int timeID;
    int sequenceID;

    public DynamicVMP(){
        this.vmpTime0 = new VMP();
        this.vmpTimeT = new VMP();
        this.setParallelMode(false);
        this.setSeed(0);
        this.timeID=-1;
    }
    public boolean isParallelMode() {
        return this.vmpTimeT.isParallelMode();
    }

    public void setParallelMode(boolean parallelMode) {
        this.vmpTime0.setParallelMode(parallelMode);
        this.vmpTimeT.setParallelMode(parallelMode);
    }

    public int getSeed() {
        return this.vmpTime0.getSeed();
    }

    public void setSeed(int seed) {
        this.vmpTime0.setSeed(seed);
        this.vmpTimeT.setSeed(seed);
    }

    @Override
    public void setModel(DynamicBayesianNetwork model_) {
        model = model_;
        ef_model = new EF_DynamicBayesianNetwork(this.model);

        this.vmpTime0.setEFModel(ef_model.getBayesianNetworkTime0());

        nodesTimeT = this.ef_model.getBayesianNetworkTimeT().getDistributionList()
                .stream()
                .map(dist -> {Node node = new Node(dist); node.setSeed(this.getSeed()); return node;})
                .collect(Collectors.toList());

        nodesClone = this.ef_model.getBayesianNetworkTime0().getDistributionList()
                .stream()
                .map(dist -> {
                    Variable temporalClone = this.model.getDynamicVariables().getTemporalClone(dist.getVariable());
                    EF_UnivariateDistribution uni = temporalClone.getDistributionType().newUnivariateDistribution().toEFUnivariateDistribution();

                    EF_ConditionalDistribution pDist = new EF_BaseDistribution_MultinomialParents(new ArrayList<Variable>(),
                            Arrays.asList(uni));

                    Node node = new Node(pDist);
                    node.setSeed(this.getSeed());
                    node.setActive(false);
                    return node;
                })
                .collect(Collectors.toList());

        List<Node> allNodes = new ArrayList();
        allNodes.addAll(nodesTimeT);
        allNodes.addAll(nodesClone);
        this.vmpTimeT.setNodes(allNodes);

    }

    @Override
    public DynamicBayesianNetwork getOriginalModel() {
        return this.model;
    }

    @Override
    public void reset(){
        this.timeID=-1;
        this.sequenceID=-1;
        this.vmpTime0.getNodes().stream().forEach(Node::resetQDist);
        this.vmpTimeT.getNodes().stream().forEach(Node::resetQDist);
    }

    @Override
    public void addDynamicEvidence(DynamicAssignment assignment_) {
        if (this.sequenceID!= -1 && this.sequenceID != assignment_.getSequenceID())
            throw new IllegalArgumentException("The sequence ID does not match. If you want to change the sequence, invoke reset method");

        if (this.timeID>= assignment_.getTimeID())
            throw new IllegalArgumentException("The provided assignment is not posterior to the previous provided assignment.");

        this.assignment = assignment_;
    }

    @Override
    public <E extends UnivariateDistribution> E getFilteredPosterior(Variable var) {
        return (getTimeIDOfPosterior()==0)? this.vmpTime0.getPosterior(var): this.vmpTimeT.getPosterior(var);
    }

    private static void moveNodeQDist(Node toTemporalCloneNode, Node fromNode){
            EF_UnivariateDistribution uni = fromNode.getQDist().deepCopy();
            ((EF_BaseDistribution_MultinomialParents)toTemporalCloneNode.getPDist()).setBaseEFDistribution(0,uni);
            toTemporalCloneNode.setQDist(uni);
    }

    @Override
    public <E extends UnivariateDistribution> E getPredictivePosterior(Variable var, int nTimesAhead) {

        if (timeID==-1){
            this.vmpTime0.setEvidence(null);
            this.vmpTime0.runInference();
            this.vmpTime0.getNodes().stream().filter(node -> !node.isObserved()).forEach(node -> {
                Variable temporalClone = this.model.getDynamicVariables().getTemporalClone(node.getMainVariable());
                moveNodeQDist(this.vmpTimeT.getNodeOfVar(temporalClone), node);
            });
            this.moveWindow(nTimesAhead-1);
            E resultQ = this.getFilteredPosterior(var);
            this.vmpTime0.getNodes().stream().forEach(Node::resetQDist);
            this.vmpTimeT.getNodes().stream().forEach(Node::resetQDist);

            return resultQ;
        }else {

            Map<Variable, EF_UnivariateDistribution> map = new HashMap<>();

            //Create at copy of Qs
            this.vmpTimeT.getNodes().stream().filter(node -> !node.isObserved()).forEach(node -> map.put(node.getMainVariable(), node.getQDist().deepCopy()));

            this.moveWindow(nTimesAhead);
            E resultQ = this.getFilteredPosterior(var);

            //Come to the original state
            map.entrySet().forEach(e -> this.vmpTimeT.getNodeOfVar(e.getKey()).setQDist(e.getValue()));

            return resultQ;
        }
    }

    @Override
    public int getTimeIDOfPosterior() {
        return this.timeID;
    }

    @Override
    public int getTimeIDOfLastEvidence(){
        return this.assignment.getTimeID();
    }

    @Override
    public void runInference(){

        if (this.timeID==-1 && assignment.getTimeID()>0) {
            this.vmpTime0.setEvidence(null);
            this.vmpTime0.runInference();
            this.timeID=0;
            this.vmpTime0.getNodes().stream().filter(node -> !node.isObserved()).forEach(node -> {
                Variable temporalClone = this.model.getDynamicVariables().getTemporalClone(node.getMainVariable());
                moveNodeQDist(this.vmpTimeT.getNodeOfVar(temporalClone), node);
            });
        }

        if (assignment.getTimeID()==0) {

            this.vmpTime0.setEvidence(this.assignment);
            this.vmpTime0.runInference();
            this.timeID=0;

            this.vmpTime0.getNodes().stream().filter(node -> !node.isObserved()).forEach(node -> {
                Variable temporalClone = this.model.getDynamicVariables().getTemporalClone(node.getMainVariable());
                moveNodeQDist(this.vmpTimeT.getNodeOfVar(temporalClone), node);
            });

        }else{

            if ((this.assignment.getTimeID() - this.timeID)>1)
                this.moveWindow(this.assignment.getTimeID() - this.timeID - 1);

            this.timeID=this.assignment.getTimeID();
            this.vmpTimeT.setEvidence(this.assignment);
            this.vmpTimeT.runInference();
            this.vmpTimeT.getNodes().stream()
                    .filter(node -> !node.getMainVariable().isTemporalClone())
                    .filter(node -> !node.isObserved())
                    .forEach(node -> {
                        Variable temporalClone = this.model.getDynamicVariables().getTemporalClone(node.getMainVariable());
                        moveNodeQDist(this.vmpTimeT.getNodeOfVar(temporalClone), node);
                    });
        }

    }

    private void moveWindow(int nsteps){
        //The first step we need to manually move the evidence from master to clone variables.
        HashMapAssignment newassignment =null;

        if (this.assignment!=null) {
            newassignment=new HashMapAssignment(this.model.getNumberOfDynamicVars());
            for (Variable var : this.model.getDynamicVariables()) {
                newassignment.setValue(this.model.getDynamicVariables().getTemporalClone(var), this.assignment.getValue(var));
                newassignment.setValue(var, Utils.missingValue());
            }
        }

        for (int i = 0; i < nsteps; i++) {
            this.vmpTimeT.setEvidence(newassignment);
            this.vmpTimeT.runInference();
            this.vmpTimeT.getNodes().stream()
                    .filter(node -> !node.getMainVariable().isTemporalClone())
                    .filter(node -> !node.isObserved())
                    .forEach(node -> {
                        Variable temporalClone = this.model.getDynamicVariables().getTemporalClone(node.getMainVariable());
                        moveNodeQDist(this.vmpTimeT.getNodeOfVar(temporalClone), node);
                    });
            newassignment=null;
        }
    }


    public static void main(String[] arguments) throws IOException, ClassNotFoundException {

        String file = "./datasets/bank_data_train_small.arff";
        DataStream<DynamicDataInstance> data = DynamicDataStreamLoader.loadFromFile(file);

        DynamicNaiveBayesClassifier model = new DynamicNaiveBayesClassifier();
        model.setClassVarID(data.getAttributes().getNumberOfAttributes() - 3);//We set -3 to account for time id and seq_id
        model.setParallelMode(true);
        model.learn(data);
        DynamicBayesianNetwork bn = model.getDynamicBNModel();

        file = "./datasets/bank_data_predict.arff";
        data = DynamicDataStreamLoader.loadFromFile(file);


        // The value of the timeWindow must be sampleSize-1 at maximum
        int timeSlices = 9;


        System.out.println("Computing Probabilities of Defaulting for 10 clients using Hugin API:\n");


        InferenceEngineForDBN.setInferenceAlgorithmForDBN(new DynamicVMP());
        InferenceEngineForDBN.setModel(bn);
        Variable defaultVar = bn.getDynamicVariables().getVariableByName("DEFAULT");
        UnivariateDistribution dist = null;
        UnivariateDistribution distAhead = null;

        for(DynamicDataInstance instance: data){

            if (instance.getTimeID()==0 && dist != null) {
                System.out.println(dist.toString());
                System.out.println(distAhead.toString());
                InferenceEngineForDBN.reset();
            }
            instance.setValue(defaultVar, Utils.missingValue());
            InferenceEngineForDBN.addDynamicEvidence(instance);
            InferenceEngineForDBN.runInference();
            dist = InferenceEngineForDBN.getFilteredPosterior(defaultVar);
            distAhead = InferenceEngineForDBN.getPredictivePosterior(defaultVar,1);
        }
    }
}
