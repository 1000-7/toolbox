package eu.amidst.examples;

import COM.hugin.HAPI.ExceptionHugin;
import eu.amidst.core.database.Attribute;
import eu.amidst.core.database.DataInstance;
import eu.amidst.core.database.DataOnDisk;
import eu.amidst.core.database.filereaders.DynamicDataOnDiskFromFile;
import eu.amidst.core.database.filereaders.arffWekaReader.WekaDataFileReader;
import eu.amidst.core.huginlink.ConverterToHugin;
import eu.amidst.core.huginlink.Utils;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DynamicBayesianNetwork;
import eu.amidst.core.models.DynamicDAG;
import eu.amidst.core.variables.*;

import java.util.ArrayList;
import java.util.List;

/**
 * This class contains examples about how we can create CajaMar's dynamic models using the AMIDST Toolbox.
 * It show how to create 2T-DBNs over multinomial, Gassuian and Indicator variables.
 *
 * Created by andresmasegosa on 22/11/14.
 */
public class CajaMarModels {

    /**
     * In this example, we create the proposed dynamic model for making predictions about the defaulting
     * behaviour of a client. We took some fake data with some fake attributes.
     *
     * We show how to create indicator variables and use it in the model.
     *
     * We finally compute the log-likelihood of the data according to the created model (i.e. the probabilty distributions
     * are randomly initialized, there is no parametric learning). The data is a single long temporal sequence.
     */
    public static void CajaMarDefaulterPredictor() throws ExceptionHugin {

        /**
         * 1. Our data is on disk and does not fit in memory. So, we use a DataOnDisk object.
         * 2. Our data is dynamic and is on file, so we create the DataOnDisk using a DynamicDataOnDiskFromFile object.
         * 3. Our data is in Weka format, so we use a WekaDataFileReader.
         */
        DataOnDisk data = new DynamicDataOnDiskFromFile(new WekaDataFileReader(new String("datasets/syntheticDataCajaMar.arff")));


        /**
         * 1. Once the data is loaded, we create a random dynamic variable for each of the attributes (i.e. data columns)
         * in our data. Here dynamic variable has the same type than static variables. However, there are two main differences
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
        DynamicVariables dynamicVariables = new DynamicVariables(data.getAttributes());

        Variable defaulter = dynamicVariables.getVariable("DEFAULTER");
        Variable sex = dynamicVariables.getVariable("SEX");
        Variable creditCard = dynamicVariables.getVariable("CREDITCARD");
        Variable balance = dynamicVariables.getVariable("BALANCE");
        Variable withDraw = dynamicVariables.getVariable("WITHDRAW");
        Variable salary = dynamicVariables.getVariable("SALARY");
        Variable monthlyBalance = dynamicVariables.getVariable("MONTHLYBALANCE");
        Variable movements = dynamicVariables.getVariable("MOVEMENTS");


        /**
         * We can create indicator variables. For doing that, we just simply call the method "addIndicatorDynamicVariable"
         * of DynamicVariables object.
         */
        Variable indicatorSalary = dynamicVariables.addIndicatorDynamicVariable(salary);


        /**
         * 1. Once we have defined your DynamicVariables object, including the indicator variable,
         * the next step is to create a dynamic DAG structure over this set of variables.
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

        dynamicDAG.getParentSetTimeT(defaulter).addParent(dynamicVariables.getTemporalClone(defaulter));

        dynamicDAG.getParentSetTimeT(sex).addParent(defaulter);

        dynamicDAG.getParentSetTimeT(creditCard).addParent(defaulter);
        dynamicDAG.getParentSetTimeT(creditCard).addParent(dynamicVariables.getTemporalClone(creditCard));


        dynamicDAG.getParentSetTimeT(balance).addParent(defaulter);
        dynamicDAG.getParentSetTimeT(balance).addParent(dynamicVariables.getTemporalClone(balance));

        dynamicDAG.getParentSetTimeT(withDraw).addParent(defaulter);
        dynamicDAG.getParentSetTimeT(withDraw).addParent(dynamicVariables.getTemporalClone(withDraw));

        dynamicDAG.getParentSetTimeT(salary).addParent(defaulter);
        dynamicDAG.getParentSetTimeT(salary).addParent(dynamicVariables.getTemporalClone(salary));
        dynamicDAG.getParentSetTimeT(salary).addParent(indicatorSalary);



        dynamicDAG.getParentSetTimeT(monthlyBalance).addParent(defaulter);
        dynamicDAG.getParentSetTimeT(monthlyBalance).addParent(dynamicVariables.getTemporalClone(balance));
        dynamicDAG.getParentSetTimeT(monthlyBalance).addParent(dynamicVariables.getTemporalClone(monthlyBalance));


        dynamicDAG.getParentSetTimeT(movements).addParent(defaulter);
        dynamicDAG.getParentSetTimeT(movements).addParent(creditCard);


        /**
         * 1. We print the graph to see if it is properly created.
         *
         * 2. The printed graph is structured in two layers. We first display the graph structure for time 0 (no
         * temporal dependencies) and, the, we time t (with temporal dependencies).
         */
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


        /**
         * 1. Now each DataInstance object is composed by two samples (x_{t},x_{t-1}). For the time 0, the data instance
         * object is just (x_0).
         *
         * 2. We compute the following log probs:
         *      - if t=0, we compute log p(x_0)
         *      - it t>0, we comptue log (x_t|x_{t-1)}
         *
         * 3. We accumulate these log-probs and obtain the log-probability that this dynamic model assigns to the provided
         * data sequence. .
         */
        double logProb = 0;

        for (DataInstance dataInstance: data){
            if (dataInstance.getTimeID()==0) {
                logProb += dynamicBayesianNetwork.getLogProbabiltyOfFullAssignmentTime0(dataInstance);
            }else{
                logProb += dynamicBayesianNetwork.getLogProbabiltyOfFullAssignmentTimeT(dataInstance);
            }
        }

        System.out.println(logProb);


        /**
         * 1. The DBN is now converted to Hugin format and stored on a file.
         *
         * 2. We can open HUGIN and visually inspect the BN created with the AMIDST toolbox.
         */
        BayesianNetwork bayesianNetwork = Utils.DBNToBN(dynamicBayesianNetwork);

        ConverterToHugin converterToHugin = new ConverterToHugin(bayesianNetwork);
        converterToHugin.convertToHuginBN();
        String outFile = new String("networks/HuginCajaMarDefaulterPredictor.net");
        converterToHugin.getHuginNetwork().saveAsNet(new String(outFile));


    }
    public static void main(String[] args) throws ExceptionHugin {
        CajaMarModels.CajaMarDefaulterPredictor();
    }
}
