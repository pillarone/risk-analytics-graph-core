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

abstract class AbstractGraphModel extends GraphElement {

    Long id
    String packageName

    protected List<ComponentNode> componentNodes = []
    protected List<Connection> connections = []

    protected List<ComponentNode> selectedNodes = []
    protected List<Connection> selectedConnections = []

    private List<IGraphModelChangeListener> graphModelChangeListeners = []

    protected List<IComponentNodeFilter> nodeFilters = []
    protected List<ComponentNode> filteredNodesList = []
    protected List<Connection> filteredConnectionsList = []

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

    List<ComponentNode> getSelectedNodes() {
        return selectedNodes;
    }

    void setSelectedNodes(List<ComponentNode> nodes, Object originator) {
        selectedNodes = nodes
        graphModelChangeListeners.each {
            if (it!=originator) {
                it.nodesSelected(selectedNodes)
            }
        }
    }

    List<Connection> getSelectedConnections() {
        return selectedConnections;
    }
    
    void setSelectedConnections(List<Connection> connections, Object originator) {
        selectedConnections = connections
        graphModelChangeListeners.each {
            if (it!=originator) {
                it.connectionsSelected(selectedConnections)
            }
        }
    }

    void clearSelections() {
        selectedNodes = []
        selectedConnections = []
        graphModelChangeListeners*.selectionCleared()
    }

    void removeComponentNode(ComponentNode toRemove) {
        componentNodes.remove(toRemove)
        if (selectedNodes.contains(toRemove)) {
            selectedNodes.remove(toRemove)
        }
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

        if (selectedConnections.contains(connection)) {
            selectedConnections.remove(connection)
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
        for (ComponentNode node : nodes) {
            for (Connection c : getEmergingConnections(node)) {
                if (!emergingConnections.contains(c)) {
                    emergingConnections.add(c)
                }
            }
        }
        return emergingConnections;
    }

    List<Connection> getEmergingConnections(ComponentNode node) {
        List<Connection> emergingConnections = new ArrayList<Connection>()
        for (Connection c : connections) {
            if (c.getFrom().getComponentNode()==node || c.getTo().getComponentNode()==node) {
                emergingConnections.add(c)
            }
        }
        return emergingConnections
    }

    List<ComponentNode> getAttachedNodes(List<Connection> connections) {
        List<ComponentNode> connectedNodes = new ArrayList<ComponentNode>()
        for (Connection c : connections) {
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
        for (ComponentNode node : getAttachedNodes(getEmergingConnections(nodes))) {
            if (!connectedNodes.contains(node)) {
                connectedNodes.add(node)
            }
        }
        return connectedNodes
    }

    void addNodeFilter(IComponentNodeFilter filter) {
        if (!nodeFilters.contains(filter)) {
            nodeFilters << filter
        }
        filteredNodesList = null
        filteredConnectionsList = null
        graphModelChangeListeners*.filtersApplied()
    }

    void clearNodeFilters() {
        nodeFilters = []
        filteredNodesList = null
        filteredConnectionsList = null
        graphModelChangeListeners*.filtersApplied()
    }

    List<ComponentNode> getFilteredComponentsList() {
        if (filteredNodesList) return filteredNodesList
        List<ComponentNode> filteredList = componentNodes
        for (IComponentNodeFilter filter : nodeFilters) {
            filteredList = filter.filterNodesList(filteredList)
        }
        return filteredList
    }

    List<Connection> getFilteredConnectionsList() {
        if (filteredConnectionsList) return filteredConnectionsList
        List<Connection> filteredList = connections
        for (IComponentNodeFilter filter : nodeFilters) {
            filteredList = filter.filterConnectionsList(filteredList)
        }
        return filteredList
    }

    /**
     * Find a component node with given name in graph model. Return null if not found.
     *
     * @param model model to search the component node in.
     * @param name  name to search for.
     * @return
     */
    ComponentNode findNodeByName(String name) {
        for (ComponentNode node : componentNodes()) {
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
     * @param model
     * @return
     */
    boolean isConnected(ComponentNode node) {
        for (Port p : node.getInPorts()) {
            for (Connection c : connections) {
                if (c.getTo() == p) return true
            }
        }
        for (Port p : node.getOutPorts()) {
            for (Connection c : connections) {
                if (c.getFrom() == p) return true;
            }
        }
        return false;
    }

}
