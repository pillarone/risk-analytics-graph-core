package org.pillarone.riskanalytics.graph.core.graph.model

import org.pillarone.riskanalytics.core.packets.Packet


class ComposedComponentGraphModel extends AbstractGraphModel {

    private List<InPort> outerInPorts = []
    private List<OutPort> outerOutPorts = []

    InPort createOuterInPort(Class<? extends Packet> packetClass, String name) {
        InPort inPort = new InPort(packetType: packetClass, name: name, composedComponentOuterPort: true)
        outerInPorts << inPort
        return inPort
    }

    OutPort createOuterOutPort(Class<? extends Packet> packetClass, String name) {
        OutPort outPort = new OutPort(packetType: packetClass, name: name, composedComponentOuterPort: true)
        outerOutPorts << outPort
        return outPort
    }

    @Override
    List<Port> getAvailablePorts(Port portToConnect) {
        List<Port> innerPorts = super.getAvailablePorts(portToConnect)
        for (Port port in outerInPorts + outerOutPorts) {
            if (port.allowedToConnectTo(portToConnect)) {
                innerPorts << port
            }
        }

        return innerPorts
    }

    public List<InPort> getOuterInPorts() {
        Collections.unmodifiableList(outerInPorts)
    }

    public List<OutPort> getOuterOutPorts() {
        Collections.unmodifiableList(outerOutPorts)
    }

}
