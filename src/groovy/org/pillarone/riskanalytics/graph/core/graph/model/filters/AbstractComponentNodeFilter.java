package org.pillarone.riskanalytics.graph.core.graph.model.filters;

import org.pillarone.riskanalytics.graph.core.graph.model.AbstractGraphModel;
import org.pillarone.riskanalytics.graph.core.graph.model.ComponentNode;
import org.pillarone.riskanalytics.graph.core.graph.model.Connection;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 */
public abstract class AbstractComponentNodeFilter implements IComponentNodeFilter {

    public List<ComponentNode> filterNodesList(List<ComponentNode> nodes) {
        if (nodes==null) {
            return null;
        } else {
            List<ComponentNode> selectedNodes = new ArrayList<ComponentNode>();
            for (ComponentNode node : nodes) {
                if (isSelected(node)) {
                    selectedNodes.add(node);
                }
            }
            return selectedNodes;
        }
    }

    public List<Connection> filterConnectionsList(List<Connection> connections) {
        if (connections==null) {
            return null;
        } else {
            List<Connection> selectedConnections = new ArrayList<Connection>();
            for (Connection connection : connections) {
                if (isSelected(connection)) {
                    selectedConnections.add(connection);
                }
            }
            return selectedConnections;
        }
    }

    public boolean isSelected(Connection connection) {
        return isSelected(connection.getFrom().getComponentNode())
                        && isSelected(connection.getTo().getComponentNode());
    }
}
