package org.pillarone.riskanalytics.graph.core.graph.model.filters

import org.pillarone.riskanalytics.graph.core.graph.model.ComponentNode
import org.pillarone.riskanalytics.graph.core.graph.model.Connection
import org.pillarone.riskanalytics.graph.core.graph.model.AbstractGraphModel
import org.pillarone.riskanalytics.graph.core.graph.model.Port

interface IComponentNodeFilter {

    boolean isSelected(ComponentNode node)

    boolean isSelected(Port port)

    boolean isSelected(Connection connection)

    List<ComponentNode> filterNodesList(List<ComponentNode> nodes)

    List<Connection> filterConnectionsList(List<Connection> connections)
}
