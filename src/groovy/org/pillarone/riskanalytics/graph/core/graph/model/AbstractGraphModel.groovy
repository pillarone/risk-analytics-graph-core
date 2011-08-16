package org.pillarone.riskanalytics.graph.core.graph.model

import java.lang.reflect.Field
import org.pillarone.riskanalytics.graph.core.palette.model.ComponentDefinition
import org.pillarone.riskanalytics.core.packets.PacketList
import org.pillarone.riskanalytics.core.packets.Packet
import java.lang.reflect.Type
import java.lang.reflect.ParameterizedType
import org.pillarone.riskanalytics.core.components.Component
import java.util.Map.Entry
import org.pillarone.riskanalytics.graph.core.graph.wiringvalidation.WiringValidationUtil
import org.pillarone.riskanalytics.core.components.ComposedComponent
import org.pillarone.riskanalytics.graph.core.graph.model.filters.IComponentNodeFilter
import org.pillarone.riskanalytics.core.components.MultiPhaseComponent

abstract class AbstractGraphModel extends GraphElement {

    Long id
    String packageName

    protected List<ComponentNode> componentNodes = []
    protected List<Connection> connections = []

    protected List<IGraphModelChangeListener> graphModelChangeListeners = []

    AbstractGraphModel() {
    }

    AbstractGraphModel(String name, String packageName) {
        this.name = name
        this.packageName = packageName
    }

    void addGraphModelChangeListener(IGraphModelChangeListener listener) {
        graphModelChangeListeners << listener
    }

    void removeGraphModelChangeListener(IGraphModelChangeListener listener) {
        graphModelChangeListeners.remove(listener)
    }

    ComponentNode createComponentNode(ComponentDefinition definition, String name) {
        ComponentNode newNode
        if (ComposedComponent.isAssignableFrom(definition.typeClass)) {
            newNode = new ComposedComponentNode(type: definition, name: name)
        } else {
            newNode = new ComponentNode(type: definition, name: name)
        }
        newNode.inPorts = Collections.unmodifiableList(obtainInPorts(newNode))
        newNode.outPorts = Collections.unmodifiableList(obtainOutPorts(newNode))
        componentNodes << newNode

        graphModelChangeListeners*.nodeAdded(newNode)

        return newNode
    }

    void addComponentNode(ComponentNode componentNode) {
        if (componentNodes.find {it.equals(componentNode)} != null) {
            throw new IllegalStateException("ComponentNode " + componentNode + " already exists")
        }
        componentNodes.add(componentNode);
        graphModelChangeListeners*.nodeAdded(componentNode)
    }

    void removeComponentNode(ComponentNode toRemove) {
        Iterator<Connection> iterator = connections.iterator()
        List<Connection> toRemoveList = new ArrayList<Connection>()
        while (iterator.hasNext()) {
            Connection connection = iterator.next()
            if (connection.from.componentNode == toRemove || connection.to.componentNode == toRemove) {
                toRemoveList.add(connection);
            }
        }
        for (Connection c: toRemoveList) {
            removeConnection(c)
        }

        componentNodes.remove(toRemove)

        graphModelChangeListeners*.nodeRemoved(toRemove)
    }

    Connection createConnection(Port from, Port to) {
        if (connections.find {it.from == from && it.to == to} != null) {
            throw new IllegalStateException("Connection " + from.componentNode + "." + from + "->" + to.componentNode + "." + to + " already exists")
        }

        if (!from.allowedToConnectTo(to)) {
            throw new IllegalStateException("Cannot connect ${from.packetType.simpleName} to ${to.packetType.simpleName}")
        }
        Connection newConnection = new Connection(from, to)
        if (!to.composedComponentOuterPort) {
            if (to instanceof InPort)
                ((InPort) to).connectionCount++;
        }
        connections << newConnection

        graphModelChangeListeners*.connectionAdded(newConnection)

        return newConnection
    }

    void removeConnection(Connection connection) {
        connections.remove(connection)
        if (!connection.to.composedComponentOuterPort) {
            if (connection.to instanceof InPort)
                ((InPort) (connection.to)).connectionCount--;
        }

        graphModelChangeListeners*.connectionRemoved(connection)
    }

    void changeNodeProperty(ComponentNode node, String propertyName, Object oldValue, Object newValue) {
        if (oldValue != newValue && node.properties.containsKey(propertyName)) {
            node."$propertyName" = newValue
            graphModelChangeListeners*.nodePropertyChanged(node, propertyName, oldValue, newValue)
        }
    }

    List<Port> getAvailablePorts(Port portToConnect) {

        List<Port> result = []

        for (ComponentNode node in componentNodes) {
            for (Port port in node.inPorts + node.outPorts) {
                if (port.allowedToConnectTo(portToConnect)) {
                    result << port
                }
            }
        }

        return result
    }

    List<ComponentNode> getAllComponentNodes() {
        return Collections.unmodifiableList(componentNodes)
    }

    List<Connection> getAllConnections() {
        return Collections.unmodifiableList(connections)
    }

    protected List<InPort> obtainInPorts(ComponentNode node) {
        List<InPort> result = []
        for (Entry<Field, Class> entry in obtainPorts(node.type, Port.IN_PORT_PREFIX)) {
            result << new InPort(name: entry.key.name, packetType: entry.value, componentNode: node,
                    connectionCardinality: WiringValidationUtil.getConnectionCardinality(entry.key),
                    packetCardinality: WiringValidationUtil.getPacketCardinality(entry.key));
        }

        return result
    }

    protected List<OutPort> obtainOutPorts(ComponentNode node) {
        List<OutPort> result = []
        for (Entry<Field, Class> entry in obtainPorts(node.type, Port.OUT_PORT_PREFIX)) {
            result << new OutPort(name: entry.key.name, packetType: entry.value, componentNode: node,
                    connectionCardinality: WiringValidationUtil.getConnectionCardinality(entry.key),
                    packetCardinality: WiringValidationUtil.getPacketCardinality(entry.key));
        }

        return result
    }

    protected Map<Field, Class> obtainPorts(ComponentDefinition definition, String prefix) {
        Map<String, Class> result = [:]
        Class currentClass = definition.typeClass
        while (currentClass != Component.class) {
            for (Field field in currentClass.declaredFields) {
                if (field.name.startsWith(prefix) && PacketList.isAssignableFrom(field.type)) {
                    Class packetType = Packet
                    Type genericType = field.genericType
                    if (genericType instanceof ParameterizedType) {
                        packetType = genericType.actualTypeArguments[0]
                    }
                    result.put(field, packetType)
                }
            }
            currentClass = currentClass.superclass
        }
        return result
    }

    List<Connection> getEmergingConnections(List<ComponentNode> nodes) {
        List<Connection> emergingConnections = new ArrayList<Connection>()
        for (ComponentNode node: nodes) {
            for (Connection c: getEmergingConnections(node)) {
                if (!emergingConnections.contains(c)) {
                    emergingConnections.add(c)
                }
            }
        }
        return emergingConnections;
    }

    List<Connection> getEmergingConnections(ComponentNode node) {
        List<Connection> emergingConnections = new ArrayList<Connection>()
        for (Connection c: connections) {
            if (c.getFrom().getComponentNode() == node || c.getTo().getComponentNode() == node) {
                emergingConnections.add(c)
            }
        }
        return emergingConnections
    }

    List<Connection> getEmergingConnections(Port p) {
        List<Connection> emergingConnections = new ArrayList<Connection>()
        for (Connection c: getEmergingConnections(p.getComponentNode())) {
            if (c.getFrom() == p || c.getTo() == p) {
                emergingConnections.add(c)
            }
        }
        return emergingConnections
    }


    List<ComponentNode> getAttachedNodes(List<Connection> connections) {
        List<ComponentNode> connectedNodes = new ArrayList<ComponentNode>()
        for (Connection c: connections) {
            ComponentNode node = c.getFrom().getComponentNode()
            if (!connectedNodes.contains(node)) {
                connectedNodes.add(node)
            }
            node = c.getTo().getComponentNode()
            if (!connectedNodes.contains(node)) {
                connectedNodes.add(node)
            }
        }
        return connectedNodes
    }

    List<ComponentNode> getConnectedNodes(List<ComponentNode> nodes) {
        List<ComponentNode> connectedNodes = new ArrayList<ComponentNode>(nodes)
        for (ComponentNode node: getAttachedNodes(getEmergingConnections(nodes))) {
            if (!connectedNodes.contains(node)) {
                connectedNodes.add(node)
            }
        }
        return connectedNodes
    }

    /**
     * Find a component node with given name in graph model. Return null if not found.
     *
     * @param model model to search the component node in.
     * @param name name to search for.
     * @return
     */
    ComponentNode findNodeByName(String name) {
        for (ComponentNode node: componentNodes) {
            if (node.getName().equals(name)) {
                return node
            }
        }
        return null
    }

    /**
     * Check whether there are connections to or from the given component node in the given model.
     *
     * @param node
     * @return
     */
    boolean hasConnections(ComponentNode node) {
        for (Port p: node.getInPorts()) {
            for (Connection c: connections) {
                if (c.getTo() == p) return true
            }
        }
        for (Port p: node.getOutPorts()) {
            for (Connection c: connections) {
                if (c.getFrom() == p) return true;
            }
        }
        return false;
    }

    /**
     * Check whether there are connections to or from the given component node in the given model.
     *
     * @param node
     * @return
     */
    boolean hasConnections(List<ComponentNode> nodes) {
        for (ComponentNode node : nodes) {
            if (this.hasConnections(node)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check whether there is a connection between two given ports:
     *
     * @param port1
     * @param port2
     * @return
     */
    boolean isConnected(Port port1, Port port2) {
        for (Connection c: connections) {
            if ((c.getTo() == port1 && c.getFrom() == port2)
                    || (c.getTo() == port2 && c.getFrom() == port1)) {
                return true
            }
        }
        return false;
    }


    public boolean hasLoop(ComponentNode src, ComponentNode dst) {
        return findLoop(src, dst);
    }

    private boolean findLoop(ComponentNode n, ComponentNode visited) {
        if (n == null || visited == null)
            return false;
        boolean hasLoop = false;
        for (InPort inPort: n.inPorts) {
            Collection<Connection> connected = connections.findAll {it.to == inPort}
            if (connected != null) {
                for (Connection c: connected) {
                    if (c.from.componentNode == visited) {
                        return !(MultiPhaseComponent.isAssignableFrom(c.from.componentNode.type.typeClass));
                    }
                    else {
                        hasLoop = hasLoop | findLoop(c.from.componentNode, visited);
                    }
                }
            }
        }
        return hasLoop
    }

}
