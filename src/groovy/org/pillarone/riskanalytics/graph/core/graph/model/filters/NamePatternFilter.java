package org.pillarone.riskanalytics.graph.core.graph.model.filters;

import org.pillarone.riskanalytics.graph.core.graph.model.ComponentNode;

/**
 * 
 */
public class NamePatternFilter extends AbstractComponentNodeFilter {

    private String fExpression;

    public NamePatternFilter(String expr) {
        if (!expr.startsWith(".*")){
            expr = ".*"+expr;
        }
        if (!expr.endsWith(".*")){
            expr = expr+".*";
        }
        fExpression = expr;
    }

    public boolean isSelected(ComponentNode node) {
        String name = node.getName();
        return name.matches(fExpression);
    }
}
