/*
 *
 *
 *    Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.
 *    See the NOTICE file distributed with this work for additional information regarding copyright ownership.
 *    The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use
 *    this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *            http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software distributed under the License is
 *    distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and limitations under the License.
 *
 *
 */

package eu.amidst.core.io;

import eu.amidst.core.Main;
import eu.amidst.core.datastream.Attribute;
import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.utils.DataSetGenerator;
import junit.framework.TestCase;

import java.io.IOException;

/**
 * Created by andresmasegosa on 14/10/15.
 */
public class DataStreamLoaderTest extends TestCase {

    public static void test1() throws IOException{

        DataStreamWriter.writeDataToFile(DataSetGenerator.generate(1234,50, 5, 5), "../datasets/simulated/dataTest.arff");

        DataStream<DataInstance> dataTest = DataStreamLoader.open("../datasets/simulated/dataTest.arff");
        for (Attribute attribute : dataTest.getAttributes().getFullListOfAttributes()) {
            if (Main.VERBOSE) System.out.println(attribute.getName() +", "+attribute.getIndex());
        }
        assertEquals(dataTest.getAttributes().getNumberOfAttributes(),10);
        assertEquals(dataTest.stream().count(), 50);
    }
}