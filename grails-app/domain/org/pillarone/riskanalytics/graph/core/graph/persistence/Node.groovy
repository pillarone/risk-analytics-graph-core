package org.pillarone.riskanalytics.graph.core.graph.persistence


class Node {

    static belongsTo = [model: GraphModel]

    String name
    String className

    static hasMany = [ports: NodePort]

    boolean startComponent

    String toString() {
        name
    }
}
