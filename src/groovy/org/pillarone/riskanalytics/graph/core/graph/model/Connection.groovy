package org.pillarone.riskanalytics.graph.core.graph.model


class Connection extends GraphElement {

    Port from
    Port to
    String comment

    public Connection(Port from, Port to) {
        this.from = from
        this.to = to
    }

    boolean isReplicatingConnection() {
        return from.class == to.class
    }

}
