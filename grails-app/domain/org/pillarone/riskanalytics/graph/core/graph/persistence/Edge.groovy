package org.pillarone.riskanalytics.graph.core.graph.persistence


class Edge {

    static belongsTo = [model: GraphModel]

    NodePort from
    NodePort to

    static mapping = {
        from(insertable: false, updateable: false)
        to(insertable: false, updateable: false)
    }

    String toString() {
        "$from -> $to"
    }
}
