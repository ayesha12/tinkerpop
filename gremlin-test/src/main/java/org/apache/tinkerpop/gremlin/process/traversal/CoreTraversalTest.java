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
package org.apache.tinkerpop.gremlin.process.traversal;

import org.apache.tinkerpop.gremlin.ExceptionCoverage;
import org.apache.tinkerpop.gremlin.FeatureRequirement;
import org.apache.tinkerpop.gremlin.LoadGraphWith;
import org.apache.tinkerpop.gremlin.process.AbstractGremlinProcessTest;
import org.apache.tinkerpop.gremlin.process.Traversal;
import org.apache.tinkerpop.gremlin.process.TraversalInterruptedException;
import org.apache.tinkerpop.gremlin.process.graph.traversal.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.util.BulkSet;
import org.apache.tinkerpop.gremlin.structure.Contains;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.tinkerpop.gremlin.LoadGraphWith.GraphData.GRATEFUL;
import static org.apache.tinkerpop.gremlin.LoadGraphWith.GraphData.MODERN;
import static org.apache.tinkerpop.gremlin.process.graph.traversal.__.*;
import static org.apache.tinkerpop.gremlin.structure.Graph.Features.GraphFeatures.FEATURE_TRANSACTIONS;
import static org.junit.Assert.*;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
@ExceptionCoverage(exceptionClass = Traversal.Exceptions.class, methods = {
        "traversalIsLocked"
})
public class CoreTraversalTest extends AbstractGremlinProcessTest {

    @Test
    @LoadGraphWith(MODERN)
    public void shouldHaveNextAndToCollectionWorkProperly() {
        final Traversal<Vertex, Vertex> traversal = g.V();
        assertTrue(traversal.next() instanceof Vertex);
        assertEquals(4, traversal.next(4).size());
        assertTrue(traversal.hasNext());
        assertTrue(traversal.tryNext().isPresent());
        assertFalse(traversal.hasNext());
        assertFalse(traversal.tryNext().isPresent());
        assertFalse(traversal.hasNext());

        Traversal<Integer, Integer> intTraversal = inject(7, 7, 2, 3, 6);
        assertTrue(intTraversal.hasNext());
        final List<Integer> list = intTraversal.toList();
        assertFalse(intTraversal.hasNext());
        assertEquals(5, list.size());
        assertEquals(7, list.get(0).intValue());
        assertEquals(7, list.get(1).intValue());
        assertEquals(2, list.get(2).intValue());
        assertEquals(3, list.get(3).intValue());
        assertEquals(6, list.get(4).intValue());
        assertFalse(intTraversal.hasNext());
        assertFalse(intTraversal.tryNext().isPresent());

        intTraversal = inject(7, 7, 2, 3, 6);
        assertTrue(intTraversal.hasNext());
        final Set<Integer> set = intTraversal.toSet();
        assertFalse(intTraversal.hasNext());
        assertEquals(4, set.size());
        assertTrue(set.contains(7));
        assertTrue(set.contains(2));
        assertTrue(set.contains(3));
        assertTrue(set.contains(6));
        assertFalse(intTraversal.hasNext());
        assertFalse(intTraversal.tryNext().isPresent());

        intTraversal = inject(7, 7, 2, 3, 6);
        assertTrue(intTraversal.hasNext());
        final BulkSet<Integer> bulkSet = intTraversal.toBulkSet();
        assertFalse(intTraversal.hasNext());
        assertEquals(4, bulkSet.uniqueSize());
        assertEquals(5, bulkSet.longSize());
        assertEquals(5, bulkSet.size());
        assertTrue(bulkSet.contains(7));
        assertTrue(bulkSet.contains(2));
        assertTrue(bulkSet.contains(3));
        assertTrue(bulkSet.contains(6));
        assertEquals(2, bulkSet.get(7));
        assertFalse(intTraversal.hasNext());
        assertFalse(intTraversal.tryNext().isPresent());
    }

    @Test
    @LoadGraphWith(MODERN)
    public void shouldHavePropertyForEachRemainingBehaviorEvenWithStrategyRewrite() {
        final GraphTraversal<Vertex, Vertex> traversal = g.V().out().groupCount();
        traversal.forEachRemaining(Map.class, map -> assertTrue(map instanceof Map));
    }

    @Test
    @LoadGraphWith(MODERN)
    public void shouldNotAlterTraversalAfterTraversalBecomesLocked() {
        final GraphTraversal<Vertex, Vertex> traversal = this.g.V();
        assertTrue(traversal.hasNext());
        try {
            traversal.count().next();
            fail("Should throw: " + Traversal.Exceptions.traversalIsLocked());
        } catch (IllegalStateException e) {
            assertEquals(Traversal.Exceptions.traversalIsLocked().getMessage(), e.getMessage());
        } catch (Exception e) {
            fail("Should throw: " + Traversal.Exceptions.traversalIsLocked() + " not " + e + ":" + e.getMessage());
        }
        traversal.iterate();
        assertFalse(traversal.hasNext());
    }

    @Test
    @LoadGraphWith(MODERN)
    public void shouldAddStartsProperly() {
        final Traversal<Object, Vertex> traversal = out().out();
        assertFalse(traversal.hasNext());
        traversal.asAdmin().addStarts(traversal.asAdmin().getTraverserGenerator().generateIterator(g.V(), traversal.asAdmin().getSteps().get(0), 1l));
        assertTrue(traversal.hasNext());
        assertEquals(2, IteratorUtils.count(traversal));

        traversal.asAdmin().addStarts(traversal.asAdmin().getTraverserGenerator().generateIterator(g.V(), traversal.asAdmin().getSteps().get(0), 1l));
        traversal.asAdmin().addStarts(traversal.asAdmin().getTraverserGenerator().generateIterator(g.V(), traversal.asAdmin().getSteps().get(0), 1l));
        assertEquals(4, IteratorUtils.count(traversal));
        assertFalse(traversal.hasNext());
    }

    @Test
    @LoadGraphWith(MODERN)
    public void shouldTraversalResetProperly() {
        final Traversal<Object, Vertex> traversal = as("a").out().out().has("name", Contains.within, Arrays.asList("ripple", "lop")).as("b");
        if (new Random().nextBoolean()) traversal.asAdmin().reset();
        assertFalse(traversal.hasNext());
        traversal.asAdmin().addStarts(traversal.asAdmin().getTraverserGenerator().generateIterator(g.V(), traversal.asAdmin().getSteps().get(0), 1l));
        assertTrue(traversal.hasNext());
        assertEquals(2, IteratorUtils.count(traversal));

        if (new Random().nextBoolean()) traversal.asAdmin().reset();
        traversal.asAdmin().addStarts(traversal.asAdmin().getTraverserGenerator().generateIterator(g.V(), traversal.asAdmin().getSteps().get(0), 1l));
        assertTrue(traversal.hasNext());
        traversal.next();
        assertTrue(traversal.hasNext());
        traversal.asAdmin().reset();
        assertFalse(traversal.hasNext());

        traversal.asAdmin().addStarts(traversal.asAdmin().getTraverserGenerator().generateIterator(g.V(), traversal.asAdmin().getSteps().get(0), 1l));
        assertEquals(2, IteratorUtils.count(traversal));

        assertFalse(traversal.hasNext());
        if (new Random().nextBoolean()) traversal.asAdmin().reset();
        assertFalse(traversal.hasNext());
    }

    @Test
    @LoadGraphWith(MODERN)
    @FeatureRequirement(featureClass = Graph.Features.GraphFeatures.class, feature = FEATURE_TRANSACTIONS)
    public void shouldTraverseIfAutoTxEnabledAndOriginalTxIsClosed() {
        // this should be the default, but manually set in just in case the implementation has other ideas
        g.tx().onReadWrite(Transaction.READ_WRITE_BEHAVIOR.AUTO);

        // close down the current transaction
        final Traversal t = g.V().has("name", "marko");
        g.tx().rollback();

        // the traversal should still work since there are auto transactions
        assertEquals(1, IteratorUtils.count(t));
    }

    @Test
    @LoadGraphWith(MODERN)
    @FeatureRequirement(featureClass = Graph.Features.GraphFeatures.class, feature = FEATURE_TRANSACTIONS)
    public void shouldTraverseIfManualTxEnabledAndOriginalTxIsClosed() {
        // auto should be the default, so force manual
        g.tx().onReadWrite(Transaction.READ_WRITE_BEHAVIOR.MANUAL);

        // close down the current transaction and fire up a fresh one
        g.tx().open();
        final Traversal t = g.V().has("name", "marko");
        g.tx().rollback();

        // the traversal should still work since there are auto transactions
        g.tx().open();
        assertEquals(1, IteratorUtils.count(t));
        g.tx().rollback();
    }

    @Test
    @LoadGraphWith(GRATEFUL)
    public void shouldTimeoutOnTraversalWhereIterateHasStarted() throws Exception {
        final AtomicBoolean interrupted = new AtomicBoolean(false);

        final Thread t = new Thread(() -> {
            try {
                g.V().out().out().out().out().out().out().out().out().out().out().out().iterate();
                fail("No way this should have completed in any reasonable time");
            } catch (Exception ex) {
                interrupted.set(ex.getClass().equals(TraversalInterruptedException.class));
            }
        });

        t.start();

        Thread.sleep(500);
        t.interrupt();
        t.join();

        assertTrue(interrupted.get());
    }

    @Test
    @LoadGraphWith(GRATEFUL)
    public void shouldTimeoutOnTraversalWhereForEachRemainingHasStarted() throws Exception {
        final AtomicBoolean interrupted = new AtomicBoolean(false);

        final Thread t = new Thread(() -> {
            try {
                g.V().out().out().out().out().out().out().out().out().out().out().out().forEachRemaining(Object::toString);
                fail("No way this should have completed in any reasonable time");
            } catch (Exception ex) {
                interrupted.set(ex.getClass().equals(TraversalInterruptedException.class));
            }
        });

        t.start();

        Thread.sleep(500);
        t.interrupt();
        t.join();

        assertTrue(interrupted.get());
    }

    @Test
    @LoadGraphWith(GRATEFUL)
    public void shouldTimeoutOnTraversalWhereNextingStarted() throws Exception {
        final AtomicBoolean interrupted = new AtomicBoolean(false);

        final Thread t = new Thread(() -> {
            try {
                g.V().out().out().out().out().out().out().out().out().out().out().out().next(Integer.MAX_VALUE);
                fail("No way this should have completed in any reasonable time");
            } catch (Exception ex) {
                ex.printStackTrace();
                interrupted.set(ex.getClass().equals(TraversalInterruptedException.class));
            }
        });

        t.start();

        Thread.sleep(500);
        t.interrupt();
        t.join();

        assertTrue(interrupted.get());
    }

    @Test
    @LoadGraphWith(GRATEFUL)
    public void shouldTimeoutOnTraversalWhereFillStarted() throws Exception {
        final AtomicBoolean interrupted = new AtomicBoolean(false);

        final Thread t = new Thread(() -> {
            try {
                g.V().out().out().out().out().out().out().out().out().out().out().out().fill(new ArrayList<>());
                fail("No way this should have completed in any reasonable time");
            } catch (Exception ex) {
                interrupted.set(ex.getClass().equals(TraversalInterruptedException.class));
            }
        });

        t.start();

        Thread.sleep(500);
        t.interrupt();
        t.join();

        assertTrue(interrupted.get());
    }
}