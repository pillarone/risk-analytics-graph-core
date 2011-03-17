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

abstract class AbstractGraphModel {

    Long id

    String name
    String packageName

    private List<ComponentNode> componentNodes = []
    private List<Connection> connections = []

    private List<IGraphModelChangeListener> graphModelChangeListeners = []

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
        ComponentNode newNode = new ComponentNode(type: definition, name: name)
        newNode.inPorts = Collections.unmodifiableList(obtainInPorts(newNode))
        newNode.outPorts = Collections.unmodifiableList(obtainOutPorts(newNode))
        componentNodes << newNode

        graphModelChangeListeners*.nodeAdded(newNode)

        return newNode
    }

    void removeComponentNode(ComponentNode toRemove) {
        componentNodes.remove(toRemove)

        graphModelChangeListeners*.nodeRemoved(toRemove)

        Iterator<Connection> iterator = connections.iterator()
        List<Connection> toRemoveList = new ArrayList<Connection>()
        while (iterator.hasNext()) {
            Connection connection = iterator.next()
            if (connection.from.componentNode == toRemove || connection.to.componentNode == toRemove) {
                toRemoveList.add(connection);
            }
        }
        for (Connection c: toRemoveList)
            removeConnection(c)
    }

    Connection createConnection(Port from, Port to) {
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
}
