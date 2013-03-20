package org.pillarone.riskanalytics.graph.core.graph.model

class ComposedComponentGraphModel extends AbstractGraphModel {

    private List<InPort> outerInPorts = []
    private List<OutPort> outerOutPorts = []

    ComposedComponentGraphModel() {
        super()
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
        graphModelChangeListeners.each {it -> it.outerPortAdded(port) }
    }

    public void removeOuterPort(Port port) {
        List<Connection> toRemoveList = new ArrayList<Connection>()
        for (Connection c: connections) {
            if (c.from == port || c.to == port) {
                toRemoveList.add(c);
            }
        }

        for (Connection c: toRemoveList) {
            removeConnection(c);
        }

        if (port instanceof InPort) {
            outerInPorts.remove(port);
        } else {
            outerOutPorts.remove(port);
        }
        graphModelChangeListeners.each {it -> it.outerPortRemoved(port) }
    }

    boolean isReplicated(Port p) {
        boolean isOutPort = p instanceof OutPort
        for (Connection c : this.getEmergingConnections(p)) {
            if (isOutPort) {
                if (c.getFrom()==p && c.getTo().isComposedComponentOuterPort()) {
                    return true
                }
            } else {
                if (c.getTo()==p && c.getFrom().isComposedComponentOuterPort()) {
                    return true
                }
            }
        }
        return false
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

    Port getOuterPort(String name) {
        name = name.toLowerCase()
        return (outerInPorts + outerOutPorts).find { it.name.toLowerCase() == name}
    }
}
