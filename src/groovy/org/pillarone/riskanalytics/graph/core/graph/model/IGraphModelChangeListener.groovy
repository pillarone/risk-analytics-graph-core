package org.pillarone.riskanalytics.graph.core.graph.model


interface IGraphModelChangeListener {

    void connectionAdded(Connection c)

    void connectionRemoved(Connection c)

    void nodeAdded(ComponentNode node)

    void nodeRemoved(ComponentNode node)

}
