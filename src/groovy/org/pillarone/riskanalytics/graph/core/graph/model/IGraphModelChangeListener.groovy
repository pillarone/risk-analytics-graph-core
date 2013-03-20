package org.pillarone.riskanalytics.graph.core.graph.model

import org.pillarone.riskanalytics.graph.core.graph.model.filters.IComponentNodeFilter


interface IGraphModelChangeListener {

    void connectionAdded(Connection c)

    void connectionRemoved(Connection c)

    void nodeAdded(ComponentNode node)

    void nodeRemoved(ComponentNode node)

    void outerPortAdded(Port p)

    void outerPortRemoved(Port p)

    void nodePropertyChanged(ComponentNode node, String propertyName, Object oldValue, Object newValue)

}
