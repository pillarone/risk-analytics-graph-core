package org.pillarone.riskanalytics.graph.core.graph.model.filters;

import org.pillarone.riskanalytics.graph.core.graph.model.AbstractGraphModel;
import org.pillarone.riskanalytics.graph.core.graph.model.ComponentNode;
import org.pillarone.riskanalytics.graph.core.graph.model.Connection;
import org.pillarone.riskanalytics.graph.core.graph.model.Port;

import java.util.List;

/**
 *
 */
public class NoneComponentNodeFilter implements IComponentNodeFilter {

    public List<ComponentNode> filterNodesList(List<ComponentNode> nodes) {
        return nodes;
    }

    public List<Connection> filterConnectionsList(List<Connection> connections) {
        return connections;
    }

    public boolean isSelected(ComponentNode node) {
        return true;
    }

    public boolean isSelected(Port port) {
        return true;
    }

    public boolean isSelected(Connection connection) {
        return true;
    }
}
