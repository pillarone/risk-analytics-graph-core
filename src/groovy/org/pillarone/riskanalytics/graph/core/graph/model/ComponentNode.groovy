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

    /**
     *
     * @param name is the portType(OUT or IN) + port name(name-portType)
     * @return
     */
    Port getPort(String name) {
        name = name.toLowerCase()
        return (inPorts + outPorts).find { it.name.toLowerCase() == name}
    }

    boolean hasPorts() {
        return inPorts?.size() > 0 || outPorts?.size() > 0
    }

}
