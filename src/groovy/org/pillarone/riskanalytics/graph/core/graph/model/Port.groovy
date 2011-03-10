package org.pillarone.riskanalytics.graph.core.graph.model

import org.pillarone.riskanalytics.core.packets.Packet
import org.pillarone.riskanalytics.graph.core.graph.util.IntegerRange


abstract class Port {

    public static final String IN_PORT_PREFIX = "in"
    public static final String OUT_PORT_PREFIX = "out"

    String name
    Class<? extends Packet> packetType
    ComponentNode componentNode
    boolean composedComponentOuterPort = false
    IntegerRange connectionCardinality
    IntegerRange packetCardinality

    final boolean allowedToConnectTo(Port port) {
        if (packetType == port.packetType && componentNode != port.componentNode) {
            return internalAllowedToConnectTo(port)
        }
        return false
    }

    abstract protected boolean internalAllowedToConnectTo(Port port)

    @Override
    String toString() {
        return "$name (${packetType.simpleName})"
    }


}
