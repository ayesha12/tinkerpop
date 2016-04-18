/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.tinkerpop.gremlin.process.traversal.step.sideEffect;

import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.Traverser;
import org.apache.tinkerpop.gremlin.structure.util.StringFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Ted Wilmes (http://twilmes.org)
 */
public final class PrunePathStep<S> extends SideEffectStep<S> {

    final Set<String> dropLabels;
    final boolean dropPath;

    public PrunePathStep(final Traversal.Admin traversal, final String... dropLabels) {
        super(traversal);
        this.dropLabels = new HashSet<>(Arrays.asList(dropLabels));
        this.dropPath = false;
    }

    @Override
    protected void sideEffect(Traverser.Admin<S> traverser) {
        final Traverser<S> start = this.starts.next();
        if(this.dropPath) start.asAdmin().dropPath();
        else start.asAdmin().dropLabels(labels);
    }

    @Override
    public String toString() {
        return StringFactory.stepString(this, this.dropLabels);
    }
}
