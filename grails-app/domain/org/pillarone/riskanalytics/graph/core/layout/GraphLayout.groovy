package org.pillarone.riskanalytics.graph.core.layout

class GraphLayout {

    static hasMany = [components: ComponentLayout]
    long userId
    String graphModelName
    String layoutName
}
