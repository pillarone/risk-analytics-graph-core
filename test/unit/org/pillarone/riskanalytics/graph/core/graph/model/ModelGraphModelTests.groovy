package org.pillarone.riskanalytics.graph.core.graph.model

import org.pillarone.riskanalytice.graph.core.graph.model.ModelGraphModel
import org.pillarone.riskanalytice.graph.core.palette.service.PaletteService
import org.pillarone.riskanalytics.core.example.component.TestComponent
import org.pillarone.riskanalytice.graph.core.graph.model.ComponentNode
import org.pillarone.riskanalytice.graph.core.graph.model.Connection
import org.pillarone.riskanalytice.graph.core.graph.model.Port

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

        Connection connection = model.createConnection(node.getPort("input3"), node2.getPort("outClaims"))

        assertTrue model.allConnections.contains(connection)

    }

    void testRemoveConnection() {
        ModelGraphModel model = new ModelGraphModel()
        ComponentNode node = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "name")
        ComponentNode node2 = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "name2")

        Connection connection = model.createConnection(node.getPort("input3"), node2.getPort("outClaims"))

        assertTrue model.allConnections.contains(connection)

        model.removeConnection(connection)

        assertTrue model.allConnections.empty
    }

    void testRemoveComponentNode() {
        ModelGraphModel model = new ModelGraphModel()
        ComponentNode node = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "name")
        ComponentNode node2 = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "name2")

        Connection connection = model.createConnection(node.getPort("input3"), node2.getPort("outClaims"))

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
}
