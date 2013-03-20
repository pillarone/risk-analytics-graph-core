package org.pillarone.riskanalytics.graph.core.graph.persistence


class GraphModel {

    String name
    String packageName
    String typeClass

    static hasMany = [nodes: Node, edges: Edge, ports: ComponentPort]

    static constraints = {
        name(blank: false)
        packageName(blank: false)
    }

    String toString() {
        name
    }

}
