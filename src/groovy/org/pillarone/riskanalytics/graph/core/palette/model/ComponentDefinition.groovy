package org.pillarone.riskanalytics.graph.core.palette.model

class ComponentDefinition {

    Class typeClass

    @Override
    String toString() {
        typeClass?.name
    }


}
