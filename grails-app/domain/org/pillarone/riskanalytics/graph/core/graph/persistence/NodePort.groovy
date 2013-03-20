package org.pillarone.riskanalytics.graph.core.graph.persistence


class NodePort {

    String name
    String packetClass

    static belongsTo = [node: Node]

    static constraints = {
        name(unique: 'node')
    }

    String toString() {
        name
    }
}
