package org.pillarone.riskanalytics.graph.core.graph.model.filters;

import org.pillarone.riskanalytics.graph.core.graph.model.ComponentNode;
import org.pillarone.riskanalytics.graph.core.graph.model.Port;

/**
 *
 */
public class NamePatternFilter extends AbstractComponentNodeFilter {

    private String fExpression;

    public NamePatternFilter(String expr) {
        if (!expr.startsWith(".*")) {
            expr = ".*" + expr;
        }
        if (!expr.endsWith(".*")) {
            expr = expr + ".*";
        }
        fExpression = expr;
    }

    public boolean isSelected(ComponentNode node) {
        if (node == null) return false;
        String name = node.getName();
        return name.matches(fExpression);
    }

    public boolean isSelected(Port port) {
        if (port == null) return false;
        if (port.isComposedComponentOuterPort())
            return port.getName().matches(fExpression);
        return true;
    }
}
