package org.pillarone.riskanalytics.graph.core.graph.model

class ComposedComponentGraphModel extends AbstractGraphModel {

    private List<InPort> outerInPorts = []
    private List<OutPort> outerOutPorts = []

    ComposedComponentGraphModel() {
    }

    ComposedComponentGraphModel(String name, String packageName) {
        super(name, packageName)
    }

    InPort createOuterInPort(Class packetClass, String name) {
        InPort inPort = new InPort(packetType: packetClass, name: name, composedComponentOuterPort: true)
        outerInPorts << inPort
        return inPort
    }

    OutPort createOuterOutPort(Class packetClass, String name) {
        OutPort outPort = new OutPort(packetType: packetClass, name: name, composedComponentOuterPort: true)
        outerOutPorts << outPort
        return outPort
    }

    void addOuterPort(Port port) {
        if (port instanceof InPort) {
            outerInPorts << port;
        } else {
            outerOutPorts << port;
        }
    }

    public void removeOuterPort(Port port) {
        if (port instanceof InPort) {
            outerInPorts.remove(port);
        } else {
            outerOutPorts.remove(port);
        }

        List<Connection> toRemoveList = new ArrayList<Connection>()
        for (Connection c: connections) {
            if (c.from == port || c.to == port) {
                toRemoveList.add(c);
            }
        }

        for (Connection c: toRemoveList) {
            removeConnection(c);
        }

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
