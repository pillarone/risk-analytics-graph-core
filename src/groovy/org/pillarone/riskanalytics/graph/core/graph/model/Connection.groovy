package org.pillarone.riskanalytics.graph.core.graph.model

import java.awt.Point


class Connection extends GraphElement {

    Port from
    Port to
    String comment
    List<Point> controlPoints

    public Connection(Port from, Port to) {
        this.from = from
        this.to = to
    }

    boolean isReplicatingConnection() {
        return from.class == to.class
    }

}
