package org.pillarone.riskanalytics.graph.core.palette.service.filter

import org.pillarone.riskanalytics.graph.core.palette.model.ComponentDefinition
import org.pillarone.riskanalytics.core.components.ComponentCategory
import org.pillarone.riskanalytics.graph.core.palette.service.PaletteService


class CategoryFilter implements IPaletteFilter {

    List<String> categories = []

    CategoryFilter(String... categories) {
        this.categories.addAll(categories.toList())
    }

    boolean accept(ComponentDefinition definition) {
        List<String> availableCategories = extractCategories(definition)
        for(String category in categories) {
            if(availableCategories.contains(category)) {
                return true
            }
        }
        if(availableCategories.empty && categories.contains(PaletteService.CAT_OTHER)) {
            return true
        }
        return false
    }

    List<String> extractCategories(ComponentDefinition definition) {
        ComponentCategory cc = definition.typeClass.getAnnotation(ComponentCategory);
        List<String> result = []
        if(cc != null) {
            result.addAll(cc.categories().toList())
        }

        return result
    }

}
