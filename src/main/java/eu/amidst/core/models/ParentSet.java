
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package eu.amidst.core.models;

import eu.amidst.core.variables.Variable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Created by afa on 02/07/14.
 */

public interface ParentSet extends Iterable<Variable>{

    Variable getMainVar();

    void addParent(Variable var);

    void removeParent(Variable var);

    List<Variable> getParents();

    int getNumberOfParents();

    String toString();

    void blockParents();

    boolean contains(Variable var);

    @Override
    boolean equals(Object o);

    @Override
    default Iterator<Variable> iterator(){
        return this.getParents().iterator();
    }

}
