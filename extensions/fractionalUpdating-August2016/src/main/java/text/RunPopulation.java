/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and limitations under the License.
 *
 */

package text;

import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataOnMemory;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.learning.parametric.bayesian.PopulationVI;
import eu.amidst.core.variables.Variable;
import eu.amidst.lda.core.BatchSpliteratorByID;
import eu.amidst.lda.core.PlateauLDA;

import java.io.FileWriter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Created by andresmasegosa on 4/5/16.
 */
public class RunPopulation {

    public static void main(String[] args) throws Exception{

        String[] years = {"90","91","92","93","94","95","96","97","98","99","00","01","02","03"};

        String model = "TEXT";
        String dataPath = "/Users/andresmasegosa/Dropbox/Amidst/datasets/NFSAbstracts/abstractByYear/";
        int docsPerBatch = 10000;


/*        String model = "BCC1";
        String dataPath = "/Users/andresmasegosa/Dropbox/Amidst/datasets/cajamarData/IDA2015Data/splittedByMonths/dataWeka/";
        int docsPerBatch = 35000;
*/
        int ntopics = 2;
        int niter = 100;
        double threshold = 0.1;
        int setSIZE = 84000;
        double learningRate = 0.1;
        boolean fixedLearningRate = true;

        if (args.length>1){
            int cont  = 0;
            model = args[cont++];

            dataPath=args[cont++];
            ntopics= Integer.parseInt(args[cont++]);
            niter = Integer.parseInt(args[cont++]);
            threshold = Double.parseDouble(args[cont++]);
            docsPerBatch = Integer.parseInt(args[cont++]);
            setSIZE = Integer.parseInt(args[cont++]);
            learningRate = Double.parseDouble(args[cont++]);
            fixedLearningRate = Boolean.parseBoolean(args[cont++]);

            args[1]="";
        }


        PopulationVI svb = new PopulationVI();


        DataStream<DataInstance> dataInstances = DataStreamLoader.open(dataPath+"abstract_"+years[0]+".arff");

        Attribute wordCountAtt = dataInstances.getAttributes().getAttributeByName("count");
        PlateauLDA plateauLDA = new PlateauLDA(dataInstances.getAttributes(), "word", "count");
        plateauLDA.setNTopics(ntopics);
        plateauLDA.getVMP().setTestELBO(true);
        plateauLDA.getVMP().setMaxIter(niter);
        plateauLDA.getVMP().setOutput(true);
        plateauLDA.getVMP().setThreshold(threshold);

        svb.setPlateuStructure(plateauLDA);
        svb.setOutput(true);


        svb.setLocalThreshold(threshold);
        svb.setOutput(true);
        svb.setMaximumLocalIterations(niter);
        svb.setBatchSize(docsPerBatch);
        svb.setDataSetSize(setSIZE);
        svb.setLearningFactor(learningRate);

        svb.setFixedStepSize(fixedLearningRate);
        svb.initLearning();


        svb.getSVB().randomInitialize();

        svb.setBatchSize(docsPerBatch);
        svb.initLearning();

        svb.randomInitialize();

        //svb.setLowerInterval(0.5);

        FileWriter fw = new FileWriter(dataPath+"Population_Output_"+Arrays.toString(args)+"_.txt");


        fw.write("\t\t\t\t");
        for (Variable var : svb.getSVB().getPlateuStructure().getNonReplicatedVariables()) {
            fw.write(var.getName() + "\t");
        }
        fw.write("\n");

        final String path = dataPath;
        final int finalDocsPerBatch = docsPerBatch;

        int count=0;



        Random random = new Random(1);

        double totalLog = 0;

        for (int year = 0; year < years.length; year++) {

            DataStream<DataInstance> batch= DataStreamLoader.open(dataPath+"abstract_"+years[year]+".arff");


            List<DataOnMemory<DataInstance>> trainTest =  Utils.splitTrainTest(batch,1);

            DataOnMemory<DataInstance> train = trainTest.get(0);
            DataOnMemory<DataInstance> test = trainTest.get(1);


            Iterator<DataOnMemory<DataInstance>> iteratorInner = BatchSpliteratorByID.iterableOverDocuments(train, finalDocsPerBatch).iterator();

            int n = 0;
            double[][] vals = null;
            while (iteratorInner.hasNext()){
                svb.updateModel(iteratorInner.next());
            }

            int wordCount = 0;
            double log = 0;
           iteratorInner = BatchSpliteratorByID.iterableOverDocuments(test, finalDocsPerBatch).iterator();
            while (iteratorInner.hasNext()) {
                DataOnMemory<DataInstance> batchTest = iteratorInner.next();
                log+=svb.predictedLogLikelihood(batchTest);

                wordCount+=batchTest.stream().mapToDouble(d -> d.getValue(wordCountAtt)).sum();
            }

            System.out.println("OUT"+(count)+"\t"+log/wordCount+"\t"+wordCount+"\n");

            fw.write((count++)+"\t"+log/wordCount+"\t"+wordCount);
            fw.write("\n");

            fw.flush();

            totalLog+=log/wordCount;

//            System.out.println(svb.getLearntBayesianNetwork());

        }
        fw.close();

        System.out.println("TOTAL LOG: " + totalLog);

    }
}
