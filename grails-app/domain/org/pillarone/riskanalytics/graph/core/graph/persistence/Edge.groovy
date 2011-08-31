package org.pillarone.riskanalytics.graph.core.graph.persistence


class Edge {

    static belongsTo = [model: GraphModel]

    //TODO: using NodePort results in Hibernate problems..
    String from
    String to

    static mapping = {
        from(column: '_from')
        to(column: '_to')
    }

    String toString() {
        "$from -> $to"
    }
}
