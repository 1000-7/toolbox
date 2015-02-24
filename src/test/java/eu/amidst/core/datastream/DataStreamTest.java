package eu.amidst.core.datastream;

import eu.amidst.core.exponentialfamily.EF_BayesianNetwork;
import eu.amidst.core.exponentialfamily.SufficientStatistics;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.utils.BayesianNetworkGenerator;
import eu.amidst.core.utils.BayesianNetworkSampler;
import junit.framework.TestCase;
import org.apache.hadoop.hdfs.server.protocol.BlockMetaDataInfo;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class DataStreamTest extends TestCase {

    @Test
    public void test1() {

        BayesianNetworkGenerator.setNumberOfContinuousVars(10);
        BayesianNetworkGenerator.setNumberOfDiscreteVars(1);
        BayesianNetworkGenerator.setNumberOfStates(2);
        BayesianNetworkGenerator.setSeed(0);
        final BayesianNetwork naiveBayes = BayesianNetworkGenerator.generateNaiveBayes(2);

        //Sampling
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(naiveBayes);
        sampler.setSeed(0);
        sampler.setParallelMode(true);

        assertTrue(sampler.sampleToDataBase(100).streamOfBatches(2).count()==50);

        sampler.sampleToDataBase(100).streamOfBatches(2).forEach( batch -> assertTrue(batch.getNumberOfDataInstances()==2));

        assertTrue(sampler.sampleToDataBase(100).parallelStreamOfBatches(2).count()==50);

        sampler.sampleToDataBase(100).parallelStreamOfBatches(2).forEach( batch -> assertTrue(batch.getNumberOfDataInstances()==2));

    }

    @Test
    public void test2() {


        BayesianNetworkGenerator.setNumberOfContinuousVars(10);
        BayesianNetworkGenerator.setNumberOfDiscreteVars(1);
        BayesianNetworkGenerator.setNumberOfStates(2);
        BayesianNetworkGenerator.setSeed(0);
        final BayesianNetwork naiveBayes = BayesianNetworkGenerator.generateNaiveBayes(2);

        //Sampling
        BayesianNetworkSampler sampler = new BayesianNetworkSampler(naiveBayes);
        sampler.setSeed(0);
        sampler.setParallelMode(true);


        /*******************************************************************************/

        EF_BayesianNetwork efBayesianNetwork = new EF_BayesianNetwork(naiveBayes.getDAG());

        AtomicInteger dataInstanceCount = new AtomicInteger(0);

        SufficientStatistics sumSS = sampler.sampleToDataBase(1000).stream()
                .peek(w -> {
                    dataInstanceCount.getAndIncrement();
                })
                .map(efBayesianNetwork::getSufficientStatistics)
                .reduce(SufficientStatistics::sumVector).get();

        //Normalize the sufficient statistics
        sumSS.divideBy(dataInstanceCount.get());

        efBayesianNetwork.setMomentParameters(sumSS);
        BayesianNetwork bn1 = efBayesianNetwork.toBayesianNetwork(naiveBayes.getDAG());

        /*******************************************************************************/

        efBayesianNetwork = new EF_BayesianNetwork(naiveBayes.getDAG());


        sumSS = sampler.sampleToDataBase(1000).streamOfBatches(10)
                .map( batch -> {
                    EF_BayesianNetwork efBayesianNetworkLocal = new EF_BayesianNetwork(naiveBayes.getDAG());
                    return batch.stream().map(efBayesianNetworkLocal::getSufficientStatistics).reduce(SufficientStatistics::sumVector).get();
                })
                .reduce(SufficientStatistics::sumVector).get();

        //Normalize the sufficient statistics
        sumSS.divideBy(dataInstanceCount.get());

        efBayesianNetwork.setMomentParameters(sumSS);
        BayesianNetwork bn2 = efBayesianNetwork.toBayesianNetwork(naiveBayes.getDAG());

        /*******************************************************************************/


        efBayesianNetwork = new EF_BayesianNetwork(naiveBayes.getDAG());


        sumSS = sampler.sampleToDataBase(1000).parallelStreamOfBatches(10)
                .map( batch -> {
                    EF_BayesianNetwork efBayesianNetworkLocal = new EF_BayesianNetwork(naiveBayes.getDAG());
                    return batch.stream().map(efBayesianNetworkLocal::getSufficientStatistics).reduce(SufficientStatistics::sumVector).get();
                })
                .reduce(SufficientStatistics::sumVector).get();

        //Normalize the sufficient statistics
        sumSS.divideBy(dataInstanceCount.get());

        efBayesianNetwork.setMomentParameters(sumSS);
        BayesianNetwork bn3 = efBayesianNetwork.toBayesianNetwork(naiveBayes.getDAG());

        /*******************************************************************************/

        assertTrue(bn1.equalBNs(bn2, 0.01));
        assertTrue(bn1.equalBNs(bn3, 0.01));

    }


}