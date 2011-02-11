package org.pillarone.riskanalytice.graph.core.graph.model

import org.pillarone.riskanalytice.graph.core.palette.model.ComponentDefinition
import org.pillarone.riskanalytics.core.components.ComposedComponent


class ComponentNode {

    String name
    ComponentDefinition type
    List<InPort> inPorts
    List<OutPort> outPorts

    private Boolean composedComponent = null

    boolean isComposedComponent() {
        if (composedComponent == null) {
            composedComponent = ComposedComponent.isAssignableFrom(type.typeClass)
        }

        return composedComponent
    }

    Port getPort(String name) {
        return (inPorts + outPorts).find { it.name == name}
    }
}
