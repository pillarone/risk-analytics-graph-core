package org.pillarone.riskanalytics.graph.core.layout

import org.pillarone.riskanalytics.graph.core.graph.persistence.Edge


class EdgeLayout {

    Edge edge

    static belongsTo = [graphLayout: GraphLayout]

    static hasMany = [points: ControlPoint]
}
