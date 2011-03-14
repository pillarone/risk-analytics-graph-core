package org.pillarone.riskanalytics.graph.core.graph.model


class Connection {

    public Connection(Port from, Port to) {
        this.from = from
        this.to = to
    }

    Port from
    Port to
    String comment

    boolean isReplicatingConnection() {
        return from.class == to.class
    }

}
