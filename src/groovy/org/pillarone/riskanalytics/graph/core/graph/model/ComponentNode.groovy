package org.pillarone.riskanalytics.graph.core.graph.model

import org.pillarone.riskanalytics.graph.core.palette.model.ComponentDefinition
import org.pillarone.riskanalytics.core.components.ComposedComponent
import java.lang.reflect.Field
import java.util.HashMap.Entry
import org.pillarone.riskanalytics.graph.core.graph.wiringvalidation.WiringValidationUtil
import java.awt.Rectangle


class ComponentNode extends GraphElement {

    ComponentDefinition type
    List<InPort> inPorts
    List<OutPort> outPorts
    String comment;
    Rectangle rectangle

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

    public static ComponentNode createInstance(ComponentDefinition definition, String name) {
        ComponentNode node = new ComponentNode(type: definition, name: name)
        addPorts(definition, node)
        return node
    }

    protected static void addPorts(ComponentDefinition definition, ComponentNode node) {
        List<InPort> inPorts = []
        for (Entry<Field, Class> entry in ComponentDefinition.getPortDefinitions(definition, Port.IN_PORT_PREFIX)) {
            inPorts << new InPort(name: entry.key.name, packetType: entry.value, componentNode: node,
                    connectionCardinality: WiringValidationUtil.getConnectionCardinality(entry.key),
                    packetCardinality: WiringValidationUtil.getPacketCardinality(entry.key));
        }
        node.inPorts = Collections.unmodifiableList(inPorts)

        List<OutPort> outPorts = []
        for (Entry<Field, Class> entry in ComponentDefinition.getPortDefinitions(definition, Port.OUT_PORT_PREFIX)) {
            outPorts << new OutPort(name: entry.key.name, packetType: entry.value, componentNode: node,
                    connectionCardinality: WiringValidationUtil.getConnectionCardinality(entry.key),
                    packetCardinality: WiringValidationUtil.getPacketCardinality(entry.key));
        }
        node.outPorts = Collections.unmodifiableList(outPorts)
    }
}
