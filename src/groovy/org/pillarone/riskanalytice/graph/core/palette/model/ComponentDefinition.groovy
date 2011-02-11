package org.pillarone.riskanalytice.graph.core.palette.model

import org.pillarone.riskanalytics.core.components.Component


class ComponentDefinition {

    Class<? extends Component> typeClass

    @Override
    String toString() {
        typeClass?.name
    }


}
