package org.pillarone.riskanalytics.graph.core.graph.persistence


class GraphModel {

    String name
    String packageName

    static hasMany = [nodes: Node, edges: Edge]

    static constraints = {
        name(blank: false)
        packageName(blank: false)
    }

    String toString() {
        name
    }

}
