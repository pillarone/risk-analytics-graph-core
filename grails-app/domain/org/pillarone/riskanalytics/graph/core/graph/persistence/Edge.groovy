package org.pillarone.riskanalytics.graph.core.graph.persistence


class Edge {

    static belongsTo = [model: GraphModel]

    NodePort from
    NodePort to

    String toString() {
        "$from -> $to"
    }
}
