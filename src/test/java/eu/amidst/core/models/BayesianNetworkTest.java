package eu.amidst.core.models;

import eu.amidst.core.database.DataInstance;
import eu.amidst.core.database.DataOnDisk;
import eu.amidst.core.database.filereaders.StaticDataOnDiskFromFile;
import eu.amidst.core.database.filereaders.arffWekaReader.WekaDataFileReader;
import eu.amidst.core.huginlink.ConverterToHugin;
import eu.amidst.core.variables.*;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;

/**
 * Created by Hanen on 24/11/14.
 */
public class BayesianNetworkTest {

    DataOnDisk data = new StaticDataOnDiskFromFile(new WekaDataFileReader(new String("datasets/syntheticData.arff")));

    @Test
    public void testingBN(){

        StaticVariables variables = new StaticVariables(data.getAttributes());

        Variable A = variables.getVariableByName("A");
        Variable B = variables.getVariableByName("B");
        Variable C = variables.getVariableByName("C");
        Variable D = variables.getVariableByName("D");
        Variable E = variables.getVariableByName("E");
        Variable G = variables.getVariableByName("G");
        Variable H = variables.getVariableByName("H");
        Variable I = variables.getVariableByName("I");

        DAG dag = new DAG(variables);

        dag.getParentSet(E).addParent(A);
        dag.getParentSet(E).addParent(B);

        dag.getParentSet(H).addParent(A);
        dag.getParentSet(H).addParent(B);

        dag.getParentSet(I).addParent(A);
        dag.getParentSet(I).addParent(B);
        dag.getParentSet(I).addParent(C);
        dag.getParentSet(I).addParent(D);

        dag.getParentSet(G).addParent(C);
        dag.getParentSet(G).addParent(D);

        System.out.println(dag.toString());

        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);

        System.out.println(bn.toString());

        /* testing number of variables*/
        assertEquals(8, bn.getNumberOfVars());

        /*testing acyclic structure */
        assertFalse(bn.getDAG().containCycles());

        double logProb = 0;
        for (DataInstance instance : data) {
            logProb += bn.getLogProbabiltyOfFullAssignment(instance);
        }

        System.out.println(logProb);

        /* testing adding duplicate parents */
        try {
            dag.getParentSet(E).addParent(A);
            fail("Should throw an IllegalArgumentException because A is already a parent of E!");
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), "Trying to add a duplicated parent");
        }

        /* testing adding a Gaussian variable as a parent to a Multinomial variable */

        try {
            dag.getParentSet(E).addParent(D);
            fail("Should throw an IllegalArgumentException because No Gaussian Parent is allowed as parent of a Multinomial variable!");
        } catch (IllegalArgumentException e) {
            assertEquals(e.getMessage(), "Adding a Gaussian variable as parent of a Multinomial variable");
        }
    }

}
