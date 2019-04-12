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
package com.cloudera.cem.efm.service.operation

import com.google.common.graph.ElementOrder
import com.google.common.graph.Graph
import com.google.common.graph.GraphBuilder
import spock.lang.Specification

class GraphUtilSpec extends Specification {

    def "test cyclic graph [reverse]topologicalSort"() {

        setup:
        //
        //  nodes: [A, B]
        // edges: [<A -> B>, <B -> A>]
        //
        Graph<String> graph = GraphBuilder
                .directed()
                .allowsSelfLoops(false)
                .nodeOrder(ElementOrder.natural())
                .build()
        graph.putEdge("A", "B")
        graph.putEdge("B", "A")

        when:
        GraphUtil.topologicalSort(graph)

        then: "exception is thrown"
        thrown RuntimeException

    }

    def "test unconnected [reverse]topologicalSort"() {

        setup:
        //
        //  nodes: [A, B, C]
        //
        Graph<String> graph = GraphBuilder
                .directed()
                .allowsSelfLoops(false)
                .nodeOrder(ElementOrder.natural())
                .build()
        graph.addNode("C")
        graph.addNode("A")
        graph.addNode("B")

        when:
        List<String> sorted = GraphUtil.topologicalSort(graph)

        then: "graph order is maintained"
        sorted == ["A", "B", "C"]

        when:
        List<String> reverseSorted = GraphUtil.reverseTopologicalSort(graph)

        then: "graph order is maintained"
        reverseSorted == ["A", "B", "C"]

    }

    def "test simple deep [reverse]topologicalSort"() {

        setup:
        //
        // C ---> A ---> B
        //
        // nodes: [A, B, C], edges: [<A -> B>, <C -> A>]
        //
        Graph<String> graph = GraphBuilder
                .directed()
                .allowsSelfLoops(false)
                .nodeOrder(ElementOrder.natural())
                .build()
        graph.putEdge("A", "B")
        graph.putEdge("C", "A")

        when:
        List<String> sorted = GraphUtil.topologicalSort(graph)

        then:
        sorted == ["C", "A", "B"]

        when:
        List<String> reverseSorted = GraphUtil.reverseTopologicalSort(graph)

        then:
        reverseSorted == ["B", "A", "C"]

    }

    def "test simple wide [reverse]topologicalSort"() {

        setup:
        //
        // A ---> [B, C, D, E, F, G]
        //
        Graph<String> graph = GraphBuilder
                .directed()
                .allowsSelfLoops(false)
                .nodeOrder(ElementOrder.natural())
                .build()
        graph.putEdge("A", "B")
        graph.putEdge("A", "G")
        graph.putEdge("A", "C")
        graph.putEdge("A", "F")
        graph.putEdge("A", "E")
        graph.putEdge("A", "D")

        when:
        List<String> sorted = GraphUtil.topologicalSort(graph)

        then:
        sorted == ["A", "B", "C", "D", "E", "F", "G"]

        when:
        List<String> reverseSorted = GraphUtil.reverseTopologicalSort(graph)

        then:
        reverseSorted == ["B", "C", "D", "E", "F", "G", "A"]

    }

    def "test simple forest [reverse]topologicalSort"() {

        setup:
        //
        //  A
        //  C ---> B
        //
        Graph<String> graph = GraphBuilder
                .directed()
                .allowsSelfLoops(false)
                .nodeOrder(ElementOrder.natural())
                .build()
        graph.addNode("A")
        graph.putEdge("C", "B")

        when:
        List<String> sorted = GraphUtil.topologicalSort(graph)

        then:
        sorted == ["A", "C", "B"]

        when:
        List<String> reverseSorted = GraphUtil.reverseTopologicalSort(graph)

        then:
        reverseSorted == ["A", "B", "C"]

    }

    def "test forest [reverse]topologicalSort"() {

        setup:
        //
        //  A ---> B
        //  D ---> C
        //
        Graph<String> graph = GraphBuilder
                .directed()
                .allowsSelfLoops(false)
                .nodeOrder(ElementOrder.natural())
                .build()
        graph.putEdge("A", "B")
        graph.putEdge("D", "C")

        when:
        List<String> sorted = GraphUtil.topologicalSort(graph)

        then:
        sorted == ["A", "B", "D", "C"]

        when:
        List<String> reverseSorted = GraphUtil.reverseTopologicalSort(graph)

        then:
        reverseSorted == ["B", "A", "C", "D"]

    }

    def "test complex [reverse]topologicalSort"() {

        setup:
        //
        // nodes: [A, B, C, D, E, F]
        // edges: [<C -> A>, <D -> B>, <E -> C>, <E -> D>]
        //
        Graph<String> graph = GraphBuilder
                .directed()
                .allowsSelfLoops(false)
                .nodeOrder(ElementOrder.natural())
                .build()
        graph.putEdge("C", "A")
        graph.putEdge("D", "B")
        graph.putEdge("E", "C")
        graph.putEdge("E", "D")
        graph.addNode("F")

        when:
        List<String> sorted = GraphUtil.topologicalSort(graph)

        then:
        sorted == ["E", "C", "A", "D", "B", "F"]

        when:
        List<String> reverseSorted = GraphUtil.reverseTopologicalSort(graph)

        then:
        reverseSorted == ["A", "B", "C", "D", "E", "F"]

    }

    def "test even more complex [reverse]topologicalSort"() {

        setup:
        //
        // nodes: [A, B, C, D, E, F]
        // edges: [<C -> A>, <D -> B>, <E -> C>, <E -> D>, <B -> A>]
        //
        Graph<String> graph = GraphBuilder
                .directed()
                .allowsSelfLoops(false)
                .nodeOrder(ElementOrder.natural())
                .build()
        graph.putEdge("C", "A")
        graph.putEdge("D", "B")
        graph.putEdge("E", "C")
        graph.putEdge("E", "D")
        graph.putEdge("B", "A")
        graph.addNode("F")

        when:
        List<String> sorted = GraphUtil.topologicalSort(graph)

        then:
        sorted == ["E", "C", "D", "B", "A", "F"]

        when:
        List<String> reverseSorted = GraphUtil.reverseTopologicalSort(graph)

        then:
        reverseSorted == ["A", "B", "C", "D", "E", "F"]

    }

}
