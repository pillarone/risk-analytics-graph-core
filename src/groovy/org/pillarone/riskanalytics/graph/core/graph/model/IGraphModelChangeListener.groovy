package org.pillarone.riskanalytics.graph.core.graph.model


interface IGraphModelChangeListener {

    void connectionAdded(Connection c)

    void connectionRemoved(Connection c)

    void nodeAdded(ComponentNode node)

    void nodeRemoved(ComponentNode node)

    void nodesSelected(List<ComponentNode> nodes)

    void connectionsSelected(List<Connection> connections)

    void selectionCleared()

    void filtersApplied()

    void nodePropertyChanged(ComponentNode node, String propertyName, Object oldValue, Object newValue)
}
