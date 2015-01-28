package eu.amidst.examples;


import eu.amidst.core.database.Attribute;
import eu.amidst.core.database.DataOnDisk;
import eu.amidst.core.database.filereaders.DynamicDataOnDiskFromFile;
import eu.amidst.core.database.filereaders.arffFileReader.ARFFDataReader;
import eu.amidst.core.models.*;
import eu.amidst.core.variables.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class contains examples about how we can create Verdande's dynamic models using the AMIDST Toolbox.
 * It show how to create 2T-DBNs over Multinomial, Gassuian and Logistic variables.
 *
 * The models here included can be found on Figures 4.28 and 4.29 of Deliverable 2.1.
 *
 * Created by andresmasegosa on 22/11/14.
 */
public final class VerdandeModels {

    private VerdandeModels(){
        //Not called
    }

    /**
     * In this example we show how to create an input-output SKF (as in Figure 4.28 of Deliverable 2.1).
     */
    public static void VerdandeInputOutputSKF() throws IOException {

        /**
         * 1. Our data is on disk and does not fit in memory. So, we use a DataOnDisk object.
         * 2. Our data is dynamic and is on file, so we create the DataOnDisk using a DynamicDataOnDiskFromFile object.
         * 3. Our data is in Weka format, so we use a WekaDataFileReader.
         */
        DataOnDisk data = new DynamicDataOnDiskFromFile(new ARFFDataReader("datasets/syntheticDataVerdandeScenario1.arff"));

        /**
         * 1. Once the data is loaded, we create random dynamic variables for some of the attributes (i.e. data columns)
         * in our data. In this case, we use the method "addObservedDynamicVariable" of the Dynamic Variable class.
         *
         * Here dynamic variable has the same type than static variables. However, there are two main differences
         *      - If we called to the method "isDynamic" of Variable class, it will return true.
         *      - They have a temporal clone (similarly to Hugin). This temporal clone is another variable object. It is
         *      automatically created for each dynamic variable. It can be queried by the method "getTemporalClone" of the
         *      DynamicVariable class.
         *
         * 2. DynamicVariables is the class for doing that. It takes a list of Attributes and internally creates
         * all the variables. We create the variables using DynamicVariables class to guarantee that each variable
         * has a different ID number and make it transparent for the user.
         *
         * 3. We can extract the Variable objects by using the method getVariableByName();
         *
         * 4. The created variables are dynamic. Here in AMIDST toolbox, it implies they have a temporal clone (similarly to Hugin).
         * This temporal clone is automatically created for each dynamic variable.
         */
        Attribute attTRQ = data.getAttributes().getAttributeByName("TRQ");
        Attribute attROP = data.getAttributes().getAttributeByName("ROP");

        List<Attribute> attributeList = new ArrayList();
        attributeList.add(attTRQ);
        attributeList.add(attROP);

        DynamicVariables dynamicVariables = new DynamicVariables();

        Variable observedROP = dynamicVariables.addObservedDynamicVariable(attROP);
        Variable observedTRQ = dynamicVariables.addObservedDynamicVariable(attTRQ);


        /**
         * 1. We now create the hidden variables. If a hidden variable can be created from an real observed Variable
         * we use addRealDynamicVariable directly. Otherwise, we make use of the class VariableBuilder. When
         * a variable is created from an Attribute object, it contains all the information we need (e.g.
         * the name, the type, etc). But hidden variables does not have an associated attribute
         * and, for this reason, we use now this VariableBuilder to provide this information to
         * DynamicVariables object.
         *
         * 2. Using VariableBuilder, we define the hidden variables and we explicitly indicate if the are Multinomial,
         * Gaussian or Multinomial_Logistic (i.e. a multinomial variable with continuous parents).
         *
         * 3. We finally create the hidden variable using the method "addHiddenDynamicVariable".
         *
         * 4. Variables RealTRQ and HiddenVar are part of the continuous sub-netwok of figure 4.28 of Deliverable 2.1
         */

        Variable realTRQ = dynamicVariables.addRealDynamicVariable(observedTRQ);

        VariableBuilder variableBuilder = new VariableBuilder();
        variableBuilder.setName("HiddenVar");
        variableBuilder.setObservable(false);
        variableBuilder.setStateSpace(new RealStateSpace());
        variableBuilder.setDistributionType(DistType.GAUSSIAN);
        Variable hidden = dynamicVariables.addHiddenDynamicVariable(variableBuilder);

        variableBuilder = new VariableBuilder();
        variableBuilder.setName("Normal_Abnormal");
        variableBuilder.setObservable(false);
        variableBuilder.setStateSpace(new FiniteStateSpace(Arrays.asList("Normal", "Abnormal")));
        variableBuilder.setDistributionType(DistType.MULTINOMIAL_LOGISTIC);
        Variable normalAbnormal = dynamicVariables.addHiddenDynamicVariable(variableBuilder);


        /**
         * 1. Once we have defined your DynamicVariables object the next step is to create a dynamic DAG
         * structure over this set of variables.
         *
         * 2. To create a dynamic DAG, we just define the transition graph.
         *
         * 4. To add parents to each dynamic variable, we first recover the ParentSet object by the method
         * getParentSetTimeT(Variable var) and then call the method addParent(Variable var).
         *
         * 4. We can define temporal dependencies by referring to the temporal clones of the variables.
         *
         */
        DynamicDAG dynamicDAG = new DynamicDAG(dynamicVariables);

        dynamicDAG.getParentSetTimeT(observedTRQ).addParent(observedROP);
        dynamicDAG.getParentSetTimeT(observedTRQ).addParent(realTRQ);
        dynamicDAG.getParentSetTimeT(observedTRQ).addParent(hidden);

        dynamicDAG.getParentSetTimeT(realTRQ).addParent(dynamicVariables.getTemporalClone(realTRQ));
        dynamicDAG.getParentSetTimeT(realTRQ).addParent(normalAbnormal);

        dynamicDAG.getParentSetTimeT(hidden).addParent(normalAbnormal);
        dynamicDAG.getParentSetTimeT(hidden).addParent(dynamicVariables.getTemporalClone(hidden));


        dynamicDAG.getParentSetTimeT(normalAbnormal).addParent(dynamicVariables.getTemporalClone(normalAbnormal));
        dynamicDAG.getParentSetTimeT(normalAbnormal).addParent(observedROP);


        /**
         * 1. We print the graph to see if it is properly created.
         *
         * 2. The printed graph is structured in two layers. We first display the graph structure for time 0 (no
         * temporal dependencies) and, the, we time t (with temporal dependencies).
         */
        System.out.println("Input-output SKF (Figure 4.28 of D2.1)");
        System.out.println(dynamicDAG.toString());

        /**
         * 1. We now create the Bayesian network from the previous DAG.
         *
         * 2. The BN object is created from the DAG. It automatically looks at the distribution type
         * of each variable and their parents to initialize the Distributions objects that are stored
         * inside (i.e. Multinomial, Normal, CLG, etc). The parameters defining these distributions are
         * properly initialized.
         *
         * 3. The network is printed and we can have look at the kind of distributions stored in the BN object. Similarly
         * to dynamic DAG, it is printed in two layers. The model for time 0 and the model from time t.
         */
        DynamicBayesianNetwork dbn = DynamicBayesianNetwork.newDynamicBayesianNetwork(dynamicDAG);
        System.out.println(dbn.toString());

        DynamicBayesianNetworkWriter.saveToFile(dbn,"networks/HuginVerdandeIOSKF.ser");

    }

    /**
     * In this example we show how to create an input-output KF with Gaussian mixtures (as in Figure 4.29 of Deliverable 2.1).
     */
    public static void VerdandeInputOutputKFwithMG() throws IOException {

        /**
         * 1. Our data is on disk and does not fit in memory. So, we use a DataOnDisk object.
         * 2. Our data is dynamic and is on file, so we create the DataOnDisk using a DynamicDataOnDiskFromFile object.
         * 3. Our data is in Weka format, so we use a WekaDataFileReader.
         */
        DataOnDisk data = new DynamicDataOnDiskFromFile(new ARFFDataReader("datasets/syntheticDataVerdandeScenario2.arff"));

        /**
         * 1. Once the data is loaded, we create random dynamic variables for some of the attributes (i.e. data columns)
         * in our data. In this case, we use the method "addObservedDynamicVariable" of the Dynamic Variable class.
         *
         * Here dynamic variable has the same type than static variables. However, there are two main differences
         *      - If we called to the method "isDynamic" of Variable class, it will return true.
         *      - They have a temporal clone (similarly to Hugin). This temporal clone is another variable object. It is
         *      automatically created for each dynamic variable. It can be queried by the method "getTemporalClone" of the
         *      DynamicVariable class.
         *
         * 2. DynamicVariables is the class for doing that. It takes a list of Attributes and internally creates
         * all the variables. We create the variables using DynamicVariables class to guarantee that each variable
         * has a different ID number and make it transparent for the user.
         *
         * 3. We can extract the Variable objects by using the method getVariableByName();
         *
         * 4. The created variables are dynamic. Here in AMIDST toolbox, it implies they have a temporal clone (similarly to Hugin).
         * This temporal clone is automatically created for each dynamic variable.
         */

        //***************************************** Network structure **************************************************
        /* Figure 4.29 in D2.1 */

        /* Control variables */
        Attribute attWOB = data.getAttributes().getAttributeByName("WOB");
        Attribute attRPM = data.getAttributes().getAttributeByName("RPMB");
        Attribute attMFI = data.getAttributes().getAttributeByName("MFI");

        /* Response variables */
        Attribute attTRQ = data.getAttributes().getAttributeByName("TRQ");
        Attribute attROP = data.getAttributes().getAttributeByName("ROP");
        Attribute attPRESSURE = data.getAttributes().getAttributeByName("PRESSURE");

        DynamicVariables dynamicVariables = new DynamicVariables();

        Variable observedWOB = dynamicVariables.addObservedDynamicVariable(attWOB);
        Variable observedRPMB = dynamicVariables.addObservedDynamicVariable(attRPM);
        Variable observedMFI = dynamicVariables.addObservedDynamicVariable(attMFI);
        Variable observedTRQ = dynamicVariables.addObservedDynamicVariable(attTRQ);
        Variable observedROP = dynamicVariables.addObservedDynamicVariable(attROP);
        Variable observedPRESSURE = dynamicVariables.addObservedDynamicVariable(attPRESSURE);

        /**
         * 1. We now create the hidden variables. If a hidden variable can be created from an real observed Variable
         * we use addRealDynamicVariable directly. Otherwise, we make use of the class VariableBuilder. When
         * a variable is created from an Attribute object, it contains all the information we need (e.g.
         * the name, the type, etc). But hidden variables does not have an associated attribute
         * and, for this reason, we use now this VariableBuilder to provide this information to
         * DynamicVariables object.
         *
         * 2. Using VariableBuilder, we define the hidden variables and we explicitly indicate if the are Multinomial,
         * Gaussian or Multinomial_Logistic (i.e. a multinomial variable with continuous parents).
         *
         * 3. We finally create the hidden variable using the method "addHiddenDynamicVariable".
         */

        /* In Figure 4.29, these 3 variables are part of the Continuous subnetwork */
        Variable realTRQ = dynamicVariables.addRealDynamicVariable(observedTRQ);
        Variable realROP = dynamicVariables.addRealDynamicVariable(observedROP);
        Variable realPRESSURE = dynamicVariables.addRealDynamicVariable(observedPRESSURE);

        VariableBuilder variableBuilder = new VariableBuilder();
        variableBuilder.setName("HiddenVar");
        variableBuilder.setObservable(false);
        variableBuilder.setStateSpace(new RealStateSpace());
        variableBuilder.setDistributionType(DistType.GAUSSIAN);
        Variable hidden = dynamicVariables.addHiddenDynamicVariable(variableBuilder);

        variableBuilder = new VariableBuilder();
        variableBuilder.setName("Mixture");
        variableBuilder.setObservable(false);
        variableBuilder.setStateSpace(new FiniteStateSpace(2));
        variableBuilder.setDistributionType(DistType.MULTINOMIAL_LOGISTIC);
        Variable mixture = dynamicVariables.addHiddenDynamicVariable(variableBuilder);

        /**
         * 1. Once we have defined your DynamicVariables object the next step is to create a dynamic DAG
         * structure over this set of variables.
         *
         * 2. To create a dynamic DAG, we just define the transition graph.
         *
         * 4. To add parents to each dynamic variable, we first recover the ParentSet object by the method
         * getParentSetTimeT(Variable var) and then call the method addParent(Variable var).
         *
         * 4. We can define temporal dependencies by referring to the temporal clones of the variables.
         *
         */
        DynamicDAG dynamicDAG = new DynamicDAG(dynamicVariables);

        dynamicDAG.getParentSetTimeT(observedTRQ).addParent(observedWOB);
        dynamicDAG.getParentSetTimeT(observedTRQ).addParent(observedRPMB);
        dynamicDAG.getParentSetTimeT(observedTRQ).addParent(observedMFI);
        dynamicDAG.getParentSetTimeT(observedTRQ).addParent(realTRQ);
        dynamicDAG.getParentSetTimeT(observedTRQ).addParent(hidden);
        dynamicDAG.getParentSetTimeT(observedTRQ).addParent(mixture);

        dynamicDAG.getParentSetTimeT(observedROP).addParent(observedWOB);
        dynamicDAG.getParentSetTimeT(observedROP).addParent(observedRPMB);
        dynamicDAG.getParentSetTimeT(observedROP).addParent(observedMFI);
        dynamicDAG.getParentSetTimeT(observedROP).addParent(realROP);
        dynamicDAG.getParentSetTimeT(observedROP).addParent(hidden);
        dynamicDAG.getParentSetTimeT(observedROP).addParent(mixture);

        dynamicDAG.getParentSetTimeT(observedPRESSURE).addParent(observedWOB);
        dynamicDAG.getParentSetTimeT(observedPRESSURE).addParent(observedRPMB);
        dynamicDAG.getParentSetTimeT(observedPRESSURE).addParent(observedMFI);
        dynamicDAG.getParentSetTimeT(observedPRESSURE).addParent(realPRESSURE);
        dynamicDAG.getParentSetTimeT(observedPRESSURE).addParent(hidden);
        dynamicDAG.getParentSetTimeT(observedPRESSURE).addParent(mixture);


        dynamicDAG.getParentSetTimeT(realTRQ).addParent(dynamicVariables.getTemporalClone(realTRQ));
        dynamicDAG.getParentSetTimeT(realTRQ).addParent(observedWOB);
        dynamicDAG.getParentSetTimeT(realTRQ).addParent(observedRPMB);
        dynamicDAG.getParentSetTimeT(realTRQ).addParent(observedMFI);

        dynamicDAG.getParentSetTimeT(realROP).addParent(dynamicVariables.getTemporalClone(realROP));
        dynamicDAG.getParentSetTimeT(realROP).addParent(observedWOB);
        dynamicDAG.getParentSetTimeT(realROP).addParent(observedRPMB);
        dynamicDAG.getParentSetTimeT(realROP).addParent(observedMFI);

        dynamicDAG.getParentSetTimeT(realPRESSURE).addParent(dynamicVariables.getTemporalClone(realTRQ));
        dynamicDAG.getParentSetTimeT(realPRESSURE).addParent(observedWOB);
        dynamicDAG.getParentSetTimeT(realPRESSURE).addParent(observedRPMB);
        dynamicDAG.getParentSetTimeT(realPRESSURE).addParent(observedMFI);

        dynamicDAG.getParentSetTimeT(hidden).addParent(dynamicVariables.getTemporalClone(hidden));
        dynamicDAG.getParentSetTimeT(hidden).addParent(observedWOB);
        dynamicDAG.getParentSetTimeT(hidden).addParent(observedRPMB);
        dynamicDAG.getParentSetTimeT(hidden).addParent(observedMFI);

        dynamicDAG.getParentSetTimeT(mixture).addParent(observedWOB);
        dynamicDAG.getParentSetTimeT(mixture).addParent(observedRPMB);
        dynamicDAG.getParentSetTimeT(mixture).addParent(observedMFI);




        /**
         * 1. We print the graph to see if it is properly created.
         *
         * 2. The printed graph is structured in two layers. We first display the graph structure for time 0 (no
         * temporal dependencies) and, the, we time t (with temporal dependencies).
         */
        System.out.println("-------------------------------------\n");
        System.out.println("Input-output KF (Figure 4.29 of D2.1)\n");
        System.out.println(dynamicDAG.toString());

        /**
         * 1. We now create the Bayesian network from the previous DAG.
         *
         * 2. The BN object is created from the DAG. It automatically looks at the distribution type
         * of each variable and their parents to initialize the Distributions objects that are stored
         * inside (i.e. Multinomial, Normal, CLG, etc). The parameters defining these distributions are
         * properly initialized.
         *
         * 3. The network is printed and we can have look at the kind of distributions stored in the BN object. Similarly
         * to dynamic DAG, it is printed in two layers. The model for time 0 and the model from time t.
         */

        DynamicBayesianNetwork dbn = DynamicBayesianNetwork.newDynamicBayesianNetwork(dynamicDAG);
        System.out.println(dbn.toString());

        DynamicBayesianNetworkWriter.saveToFile(dbn,"networks/HuginVerdandeIOSKFwithMG.ser");

    }

    /**
     * In this example we show how to create an input-output KF with Gaussian mixtures (as in Figure 4.29 of Deliverable 2.1).
     */
    public static void VerdandeInputOutputHMM() throws IOException {
        DataOnDisk data = new DynamicDataOnDiskFromFile(new ARFFDataReader("datasets/syntheticDataVerdandeScenario3.arff"));

        Attribute attDepth = data.getAttributes().getAttributeByName("depth");
        Attribute attGammaDiff = data.getAttributes().getAttributeByName("gammaDiff");

        DynamicVariables dynamicVariables = new DynamicVariables();

        Variable observedDepth = dynamicVariables.addObservedDynamicVariable(attDepth);
        Variable observedGammaDiff = dynamicVariables.addObservedDynamicVariable(attGammaDiff);

        VariableBuilder variableBuilder = new VariableBuilder();
        variableBuilder.setName("FormationNo");
        variableBuilder.setObservable(false);
        variableBuilder.setStateSpace(new FiniteStateSpace(2));
        variableBuilder.setDistributionType(DistType.MULTINOMIAL_LOGISTIC);
        Variable formationNo = dynamicVariables.addHiddenDynamicVariable(variableBuilder);

        variableBuilder = new VariableBuilder();
        variableBuilder.setName("Shift");
        variableBuilder.setObservable(false);
        variableBuilder.setStateSpace(new FiniteStateSpace(2));
        variableBuilder.setDistributionType(DistType.MULTINOMIAL);
        Variable shift = dynamicVariables.addHiddenDynamicVariable(variableBuilder);


        DynamicDAG dynamicDAG = new DynamicDAG(dynamicVariables);

        dynamicDAG.getParentSetTimeT(formationNo).addParent(observedDepth);
        dynamicDAG.getParentSetTimeT(formationNo).addParent(dynamicVariables.getTemporalClone(formationNo));

        //TODO Error trying to add a duplicate parent. A -> B <- Aclone. We are considering A and AClone the same variables? Is that right?
        dynamicDAG.getParentSetTimeT(shift).addParent(formationNo);
        //dynamicDAG.getParentSetTimeT(shift).addParent(dynamicVariables.getTemporalClone(formationNo));
        dynamicDAG.getParentSetTimeT(shift).addParent(dynamicVariables.getTemporalClone(shift));

        dynamicDAG.getParentSetTimeT(observedGammaDiff).addParent(shift);

        System.out.println("-------------------------------------\n");
        System.out.println("Input-output HMM (Figure 4.31 of D2.1)\n");
        System.out.println(dynamicDAG.toString());


        DynamicBayesianNetwork dbn = DynamicBayesianNetwork.newDynamicBayesianNetwork(dynamicDAG);
        System.out.println(dbn.toString());


        DynamicBayesianNetworkWriter.saveToFile(dbn, "networks/HuginVerdandeIOHMM.ser");

    }

    public static void main(String[] args) throws IOException {
        VerdandeModels.VerdandeInputOutputSKF();
        VerdandeModels.VerdandeInputOutputKFwithMG();
        VerdandeModels.VerdandeInputOutputHMM();
    }
}
