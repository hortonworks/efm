/*
 * Copyright (c) 2018-2019 Cloudera, Inc. All rights reserved.
 *
 * This code is provided to you pursuant to your written agreement with Cloudera, which may be the terms of the
 * Affero General Public License version 3 (AGPLv3), or pursuant to a written agreement with a third party authorized
 * to distribute this code.  If you do not have a written agreement with Cloudera or with an authorized and
 * properly licensed third party, you do not have any rights to this code.
 *
 * If this code is provided to you under the terms of the AGPLv3:
 *  (A) CLOUDERA PROVIDES THIS CODE TO YOU WITHOUT WARRANTIES OF ANY KIND;
 *  (B) CLOUDERA DISCLAIMS ANY AND ALL EXPRESS AND IMPLIED WARRANTIES WITH RESPECT TO THIS CODE, INCLUDING BUT NOT
 *      LIMITED TO IMPLIED WARRANTIES OF TITLE, NON-INFRINGEMENT, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE;
 *  (C) CLOUDERA IS NOT LIABLE TO YOU, AND WILL NOT DEFEND, INDEMNIFY, OR HOLD YOU HARMLESS FOR ANY CLAIMS ARISING
 *      FROM OR RELATED TO THE CODE; AND
 *  (D) WITH RESPECT TO YOUR EXERCISE OF ANY RIGHTS GRANTED TO YOU FOR THE CODE, CLOUDERA IS NOT LIABLE FOR ANY
 *      DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, PUNITIVE OR CONSEQUENTIAL DAMAGES INCLUDING, BUT NOT LIMITED
 *      TO, DAMAGES RELATED TO LOST REVENUE, LOST PROFITS, LOSS OF INCOME, LOSS OF BUSINESS ADVANTAGE OR
 *      UNAVAILABILITY, OR LOSS OR CORRUPTION OF DATA.
 */
package com.cloudera.cem.efm.service.operation;

import com.google.common.collect.Lists;
import com.google.common.graph.ElementOrder;
import com.google.common.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility methods for working with a {@code com.google.common.graph.Graph} instance.
 */
class GraphUtil {

    private static final Logger logger = LoggerFactory.getLogger(StandardOperationService.class);


    // TODO, consider contributing these sorting operations to Guava's com.google.common.graph package.
    // There is an open feature request for topological sort, Guava GitHub Issue #2641:
    //     https://github.com/google/guava/issues/2641

    /**
     * For a given directed, acyclic {@link Graph} (DAG), this method
     * returns a list of nodes in the graph in linear order such that
     * for every edge (A->B), node A occurs before B in the ordering.
     *
     * <p>Furthermore, this method takes into account a feature of the
     * {@code com.google.common.graph.Graph} class, which is node ordering,
     * so that when {@code Graph.nodeOrder().type() != ElementOrder.Type#UNORDERED}
     * the graph's node order is used in any situation where node order
     * would otherwise be ambiguous. Because of this, the result
     * of this method is deterministic (there is exactly one result),
     * for graphs with a deterministic node ordering, which is more
     * strict than a general topological sort algorithm for a DAG.
     *
     * @param directedAcyclicGraph the {@link Graph} instance to sort,
     *                             must be not null and must be a
     *                             directed, acyclic graph (DAG).
     * @return a list of nodes topologically sorted.
     *
     * @see #reverseTopologicalSort(Graph)
     */
    static <N> List<N> topologicalSort(Graph<N> directedAcyclicGraph) {
        return topologicalSort(directedAcyclicGraph, false);
    }

    /**
     * For a given directed, acyclic {@code Graph} (DAG), this method
     * returns a list of nodes in the graph in linear order such that
     * for every edge (A->B), node B occurs before A in the ordering.
     *
     * <p>This is useful for one common use case of DAGs, which  is
     * representing dependency relationships in the directed form:
     * {@code dependent -> dependency}
     *
     * <p>Furthermore, this method takes into account a feature of the
     * {@code com.google.common.graph.Graph} class, which is node ordering,
     * so that when {@code Graph.nodeOrder().type() != ElementOrder.Type#UNORDERED}
     * the graph's node order is used in any situation where node order
     * would otherwise be ambiguous. Because of this, the result
     * of this method is deterministic (there is exactly one result),
     * for graphs with a deterministic node ordering, which is more
     * strict than a general topological sort algorithm for a DAG.
     *
     * <p>Note that this method is NOT the equivalent of reversing the
     * result of {@link #topologicalSort(Graph)}. That is because
     * the reverse order is only used to interpret the linear order
     * of nodes connected by a directed edge. In cases where there
     * is no directed edge between nodes (e.g., a forest), or where
     * there are multiple, non-deterministic orderings based on graph
     * traversal, then the Graph's nodeOrder is used (not the reverse nodeOrder)
     *
     * <p>This means that, for example, an input without any edges (that is,
     * a forest of unconnected nodes), this method is actually equivalent
     * to {@link #topologicalSort(Graph)}, as the graph's node order
     * determines the entire linear order.
     *
     * @param directedAcyclicGraph the {@code Graph} instance to sort,
     *                             must be not null and must be a
     *                             directed, acyclic graph (DAG).
     * @return a list of nodes topologically sorted.
     *
     * @see #topologicalSort(Graph)
     */
    static <N> List<N> reverseTopologicalSort(Graph<N> directedAcyclicGraph) {
        return topologicalSort(directedAcyclicGraph, true);
    }

    private static <N> List<N> topologicalSort(Graph<N> directedAcyclicGraph, boolean reverse) {

        if (!directedAcyclicGraph.isDirected() || directedAcyclicGraph.allowsSelfLoops()) {
            throw new IllegalArgumentException("Input graph is not a DAG.");
        }

        final int nodeCount = directedAcyclicGraph.nodes().size();
        final HashSet<N> visitedNodes = new HashSet<>(nodeCount);
        final List<N> topologicallySortedNodes = new ArrayList<>(nodeCount);

        // graph node order needs to be reversed as the resulting order is based on FILO stack order
        List<N> allNodes = new ArrayList<>(directedAcyclicGraph.nodes());
        allNodes = Lists.reverse(allNodes);

        // This loop ensures we visit every node at least once
        for (final N node : allNodes) {
            if (!visitedNodes.contains(node)) {
                // DFS traversal from this node, when a node is finished, it gets added to the front of topologicallySortedNodes
                topologicalSortRecursionHelper(directedAcyclicGraph, node, visitedNodes, new HashSet<>(), topologicallySortedNodes, reverse);
            }
        }

        return topologicallySortedNodes;
    }

    private static <N> void topologicalSortRecursionHelper(Graph<N> dag, N currentNode, Set<N> visitedNodes, Set<N> nodesVisitedOnCurrentPath, List<N> sortedNodes, boolean reverse) {

        // Mark current node as visited
        nodesVisitedOnCurrentPath.add(currentNode);
        visitedNodes.add(currentNode);

        // Determine the order to visit neighbors, which depends on the direction we are traversing directed edges and the graph's node order
        Collection<N> neighbors = reverse ? dag.predecessors(currentNode) : dag.successors(currentNode);
        if (dag.nodeOrder().type() != ElementOrder.Type.UNORDERED) {
            try {
                // nodeOrder comparator needs to be reversed as the resulting order is based on FILO stack order
                final Comparator<N> graphNodeOrderComparator = dag.nodeOrder().comparator().reversed();
                List<N> orderedNeighbors = new ArrayList<>(neighbors);
                orderedNeighbors.sort(graphNodeOrderComparator);
                neighbors = orderedNeighbors;
            } catch (Exception e) {
                // Do nothing.
                logger.debug("Encountered exception '" + e + "'. Unable to determine neighboring node order and continuing graph traversal using unordered neighbors.", e);
            }
        }

        // Visit each neighbor in DFS traversal, checking for cycles
        for (N neighbor : neighbors) {
            if (nodesVisitedOnCurrentPath.contains(neighbor)) {
                throw new RuntimeException("Input graph contains a cycle.");
            }
            if (!visitedNodes.contains(neighbor)) {
                topologicalSortRecursionHelper(dag, neighbor, visitedNodes, nodesVisitedOnCurrentPath, sortedNodes, reverse);
            }
        }

        // Add the current, "finished" (no more neighbors) node in our DFS to the top of the "stack" that represents the resulting order.
        sortedNodes.add(0, currentNode);
        nodesVisitedOnCurrentPath.remove(currentNode);
    }

}
