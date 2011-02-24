package org.pillarone.riskanalytics.graph.core.graph.persistence


class ComponentPort {

    String name
    String packetClass

    static belongsTo = [graphModel: GraphModel]

    static constraints = {
        name(unique: 'graphModel')
    }

    String toString() {
        name
    }

}
