package eu.amidst.examples;

import COM.hugin.HAPI.Domain;
import COM.hugin.HAPI.ExceptionHugin;
import eu.amidst.core.database.Attribute;
import eu.amidst.core.database.DataOnDisk;
import eu.amidst.core.database.filereaders.DynamicDataOnDiskFromFile;
import eu.amidst.core.database.filereaders.arffFileReader.ARFFDataReader;
import eu.amidst.core.huginlink.ConverterToHugin;
import eu.amidst.core.huginlink.Utils;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DynamicBayesianNetwork;
import eu.amidst.core.models.DynamicBayesianNetworkWriter;
import eu.amidst.core.models.DynamicDAG;
import eu.amidst.core.variables.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class contains examples about how we can create Daimler's dynamic models using the AMIDST Toolbox.
 * It shows how to create 2T-DBNs over multinomial and Gassuian variables.
 *
 * The models here included can be found on Figures 4.14 of Deliverable 2.1.
 *
 * Created by ana@cs.aau.dk on 25/11/14.
 */
public final class DaimlerModels {

    private DaimlerModels(){
        //Not called
    }

    /**
     * In this example we show how to create an OOBN fragment for the LE hypothesis with a hidden node for acceleration
     * (as in Figure 4.14 of D2.1).
     */
    public static void Daimler_LE_acceleration() throws ExceptionHugin, IOException {
        /**
         * 1. Our data is on disk and does not fit in memory. So, we use a DataOnDisk object.
         * 2. Our data is dynamic and is on file, so we create the DataOnDisk using a DynamicDataOnDiskFromFile object.
         * 3. Our data is in Weka format, so we use a WekaDataFileReader.
         */
        DataOnDisk data = new DynamicDataOnDiskFromFile(new ARFFDataReader("datasets/syntheticDataDaimler.arff"));

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

        Attribute attVLATSIGMA = data.getAttributes().getAttributeByName("V_LAT_SIGMA");
        Attribute attVLATMEAS  = data.getAttributes().getAttributeByName("V_LAT_MEAS");
        Attribute attOLATSIGMA = data.getAttributes().getAttributeByName("O_LAT_SIGMA");
        Attribute attOLATMEAS  = data.getAttributes().getAttributeByName("O_LAT_MEAS");

        List<Attribute> attributeList = new ArrayList();
        attributeList.add(attVLATSIGMA);
        attributeList.add(attVLATMEAS);
        attributeList.add(attOLATSIGMA);
        attributeList.add(attOLATMEAS);

        DynamicVariables dynamicVariables = new DynamicVariables();

        Variable vlatSIGMA = dynamicVariables.addObservedDynamicVariable(attVLATSIGMA);
        Variable vlatMEAS = dynamicVariables.addObservedDynamicVariable(attVLATMEAS);
        Variable olatSIGMA = dynamicVariables.addObservedDynamicVariable(attOLATSIGMA);
        Variable olatMEAS = dynamicVariables.addObservedDynamicVariable(attOLATMEAS);

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
         */

        Variable vlatREAL = dynamicVariables.addRealDynamicVariable(vlatMEAS);
        Variable olatREAL = dynamicVariables.addRealDynamicVariable(olatMEAS);

        VariableBuilder variableBuilder = new VariableBuilder();
        variableBuilder.setName("A_LAT");
        variableBuilder.setObservable(false);
        variableBuilder.setStateSpace(new RealStateSpace());
        variableBuilder.setDistributionType(DistType.GAUSSIAN);
        Variable aLAT = dynamicVariables.addHiddenDynamicVariable(variableBuilder);

        variableBuilder = new VariableBuilder();
        variableBuilder.setName("LE");
        variableBuilder.setObservable(false);
        variableBuilder.setStateSpace(new FiniteStateSpace(Arrays.asList("Yes", "No")));
        variableBuilder.setDistributionType(DistType.MULTINOMIAL_LOGISTIC);
        Variable latEv = dynamicVariables.addHiddenDynamicVariable(variableBuilder);


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

        dynamicDAG.getParentSetTimeT(vlatMEAS).addParent(vlatSIGMA);
        dynamicDAG.getParentSetTimeT(vlatMEAS).addParent(vlatREAL);


        dynamicDAG.getParentSetTimeT(olatMEAS).addParent(olatSIGMA);
        dynamicDAG.getParentSetTimeT(olatMEAS).addParent(olatREAL);

        dynamicDAG.getParentSetTimeT(aLAT).addParent(dynamicVariables.getTemporalClone(aLAT));

        dynamicDAG.getParentSetTimeT(vlatREAL).addParent(aLAT);
        dynamicDAG.getParentSetTimeT(vlatREAL).addParent(dynamicVariables.getTemporalClone(vlatREAL));

        dynamicDAG.getParentSetTimeT(olatREAL).addParent(dynamicVariables.getTemporalClone(olatREAL));
        dynamicDAG.getParentSetTimeT(olatREAL).addParent(dynamicVariables.getTemporalClone(vlatREAL));

        dynamicDAG.getParentSetTimeT(latEv).addParent(vlatREAL);
        dynamicDAG.getParentSetTimeT(latEv).addParent(olatREAL);

        /**
         * 1. We print the graph to see if it is properly created.
         *
         * 2. The printed graph is structured in two layers. We first display the graph structure for time 0 (no
         * temporal dependencies) and, the, we time t (with temporal dependencies).
         */
        System.out.println("OOBN fragment for the LE hypothesis with acceleration (as in Figure 4.14 of D2.1)");
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
        DynamicBayesianNetwork dynamicBayesianNetwork = DynamicBayesianNetwork.newDynamicBayesianNetwork(dynamicDAG);
        System.out.println(dynamicBayesianNetwork.toString());

        DynamicBayesianNetworkWriter.saveToFile(dynamicBayesianNetwork,"networks/HuginDaimlerLEAcceleration.ser");
    }



    public static void main(String[] args) throws ExceptionHugin, IOException {
        DaimlerModels.Daimler_LE_acceleration();
    }
}
