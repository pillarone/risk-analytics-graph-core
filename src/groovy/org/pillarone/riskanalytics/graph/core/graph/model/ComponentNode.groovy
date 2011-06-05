package org.pillarone.riskanalytics.graph.core.graph.model

import org.pillarone.riskanalytics.graph.core.palette.model.ComponentDefinition
import org.pillarone.riskanalytics.core.components.ComposedComponent


class ComponentNode extends GraphElement {

    ComponentDefinition type
    List<InPort> inPorts
    List<OutPort> outPorts
    String comment;

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

    boolean hasPorts() {
        return inPorts?.size()>0 || outPorts?.size()> 0
    }

}
