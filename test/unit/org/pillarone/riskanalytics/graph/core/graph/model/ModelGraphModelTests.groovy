package org.pillarone.riskanalytics.graph.core.graph.model

import org.pillarone.riskanalytics.graph.core.palette.service.PaletteService
import org.pillarone.riskanalytics.core.example.component.TestComponent

class ModelGraphModelTests extends GroovyTestCase {

    void testCreateComponentNode() {
        ModelGraphModel model = new ModelGraphModel()
        ComponentNode node = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "name")

        assertEquals 3, node.inPorts.size()
        assertEquals 3, node.outPorts.size()

        assertTrue model.allComponentNodes.contains(node)
    }

    void testCreateConnection() {
        ModelGraphModel model = new ModelGraphModel()
        ComponentNode node = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "name")
        ComponentNode node2 = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "name2")

        Connection connection = model.createConnection(node2.getPort("outClaims"),node.getPort("input3"))

        assertTrue model.allConnections.contains(connection)

    }

    void testRemoveConnection() {
        ModelGraphModel model = new ModelGraphModel()
        ComponentNode node = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "name")
        ComponentNode node2 = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "name2")

        Connection connection = model.createConnection(node2.getPort("outClaims"),node.getPort("input3"))

        assertTrue model.allConnections.contains(connection)

        model.removeConnection(connection)

        assertTrue model.allConnections.empty
    }

    void testRemoveComponentNode() {
        ModelGraphModel model = new ModelGraphModel()
        ComponentNode node = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "name")
        ComponentNode node2 = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "name2")

        Connection connection = model.createConnection(node2.getPort("outClaims"),node.getPort("input3"))

        assertTrue model.allConnections.contains(connection)

        model.removeComponentNode(node2)

        assertFalse model.allConnections.contains(connection)
        assertFalse model.allComponentNodes.contains(node2)
        assertTrue model.allComponentNodes.contains(node)

    }

    void testGetAvailablePorts() {
        ModelGraphModel model = new ModelGraphModel()
        ComponentNode node = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "name")
        ComponentNode node2 = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "name2")

        Port from = node.getPort("input3")
        List<Port> ports = model.getAvailablePorts(from)

        assertEquals 1, ports.size()
        assertSame node2, ports[0].componentNode

    }

    void testGetEmergingConnections() {
        ModelGraphModel model = new ModelGraphModel()
        ComponentNode node1 = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "node1")
        ComponentNode node2 = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "node2")
        ComponentNode node3 = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "node3")

        Connection conn1 = model.createConnection(node2.getPort("outClaims"),node1.getPort("input3"))
        Connection conn2 = model.createConnection(node2.getPort("outValue1"),node1.getPort("input1"))
        Connection conn3 = model.createConnection(node3.getPort("outValue2"), node2.getPort("input2"))
        Connection conn4 = model.createConnection(node3.getPort("outValue2"), node1.getPort("input2"))

     
        List<Connection> ec = model.getEmergingConnections(node2)
        assertEquals 3, ec.size()
        assertTrue ec.contains(conn1)
        assertTrue ec.contains(conn2)
        assertTrue ec.contains(conn3)
        assertFalse ec.contains(conn4)

        ec = model.getEmergingConnections([node1,node2])
        assertEquals 4, ec.size()
        assertTrue ec.contains(conn1)
        assertTrue ec.contains(conn2)
        assertTrue ec.contains(conn3)
        assertTrue ec.contains(conn4)
    }

    void testGetAttachedNodes() {
        ModelGraphModel model = new ModelGraphModel()
        ComponentNode node1 = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "node1")
        ComponentNode node2 = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "node2")
        ComponentNode node3 = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "node3")
        ComponentNode node4 = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "node4")

        Connection conn1 = model.createConnection(node2.getPort("outClaims"),node1.getPort("input3"))
        Connection conn2 = model.createConnection(node3.getPort("outValue2"), node2.getPort("input2"))

        List<ComponentNode> nodes = model.getAttachedNodes([conn1, conn2])
        assertEquals 3, nodes.size()
        assertTrue nodes.contains(node1)
        assertTrue nodes.contains(node2)
        assertTrue nodes.contains(node3)
        assertFalse nodes.contains(node4)
    }

    void testGetConnectedNodes() {
        ModelGraphModel model = new ModelGraphModel()
        ComponentNode node1 = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "node1")
        ComponentNode node2 = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "node2")
        ComponentNode node3 = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "node3")
        ComponentNode node4 = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "node4")

        Connection conn1 = model.createConnection(node2.getPort("outClaims"),node1.getPort("input3"))
        Connection conn2 = model.createConnection(node3.getPort("outValue2"), node2.getPort("input2"))

        List<ComponentNode> nodes = model.getConnectedNodes([node1])
        assertEquals 2, nodes.size()
        assertTrue nodes.contains(node1)
        assertTrue nodes.contains(node2)
        assertFalse nodes.contains(node3)
        assertFalse nodes.contains(node4)

        nodes = model.getConnectedNodes([node2])
        assertEquals 3, nodes.size()
        assertTrue nodes.contains(node1)
        assertTrue nodes.contains(node2)
        assertTrue nodes.contains(node3)
        assertFalse nodes.contains(node4)
    }
}
