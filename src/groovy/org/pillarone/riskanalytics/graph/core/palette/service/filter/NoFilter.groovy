package org.pillarone.riskanalytics.graph.core.palette.service.filter

import org.pillarone.riskanalytics.graph.core.palette.model.ComponentDefinition

class NoFilter implements IPaletteFilter {

    boolean accept(ComponentDefinition definition) {
        return true
    }


}
