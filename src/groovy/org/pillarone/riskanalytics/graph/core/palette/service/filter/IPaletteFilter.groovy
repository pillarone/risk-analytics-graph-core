package org.pillarone.riskanalytics.graph.core.palette.service.filter

import org.pillarone.riskanalytics.graph.core.palette.model.ComponentDefinition


interface IPaletteFilter {
    boolean accept(ComponentDefinition definition)
}
