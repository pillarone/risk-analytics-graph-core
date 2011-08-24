package org.pillarone.riskanalytics.graph.core.graph.model.filters

import org.pillarone.riskanalytics.graph.core.graph.model.AbstractGraphModel
import org.pillarone.riskanalytics.graph.core.graph.model.ComponentNode
import org.pillarone.riskanalytics.graph.core.graph.model.Connection
import org.pillarone.riskanalytics.graph.core.palette.model.ComponentDefinition
import org.pillarone.riskanalytics.graph.core.palette.service.PaletteService
import org.pillarone.riskanalytics.graph.core.graph.model.Port

/**
 *
 */
class CategoryFilter extends AbstractComponentNodeFilter {

    String fPattern
    PaletteService fPalette

    CategoryFilter(String expr) {
        if (!expr.startsWith('.*')) {
            expr = '.*' + expr
        }
        if (!expr.endsWith('.*')) {
            expr = expr + '.*'
        }
        fPattern = expr

        fPalette = PaletteService.instance
    }

    @Override
    boolean isSelected(ComponentNode node) {
        if (node == null) return false;
        List<String> categories = fPalette.getCategoriesFromDefinition(node.getType())
        for (String category: categories) {
            if (category.matches(fPattern)) {
                return true
            }
        }
        return false
    }

    boolean isSelected(Port port) {
        if (port == null) return false
        return !port.isComposedComponentOuterPort()
    }


}
