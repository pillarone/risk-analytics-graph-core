package org.pillarone.riskanalytics.graph.core.graph.persistence

import org.pillarone.riskanalytics.core.example.component.TestComponent
import org.pillarone.riskanalytics.graph.core.graph.model.ComponentNode
import org.pillarone.riskanalytics.graph.core.graph.model.ModelGraphModel
import org.pillarone.riskanalytics.graph.core.palette.service.PaletteService
import org.pillarone.riskanalytics.graph.core.graph.model.ComposedComponentGraphModel
import org.pillarone.riskanalytics.core.example.packet.TestPacket
import java.awt.Rectangle
import org.pillarone.riskanalytics.graph.core.layout.GraphLayout
import org.pillarone.riskanalytics.graph.core.layout.ComponentLayout
import org.pillarone.riskanalytics.graph.core.graph.model.Connection
import java.awt.Point
import org.pillarone.riskanalytics.graph.core.layout.EdgeLayout
import org.pillarone.riskanalytics.graph.core.layout.ControlPoint

class GraphPersistenceServiceTests extends GroovyTestCase {

    GraphPersistenceService graphPersistenceService

    void testSaveLoad() {
        ModelGraphModel model = new ModelGraphModel("name", "package")
        ComponentNode node = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "name")
        node.rectangle = new Rectangle(10, 10, 100, 100)
        ComponentNode node2 = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "name2")

        final Connection connection = model.createConnection(node.getPort("input3"), node2.getPort("outClaims"))
        connection.controlPoints = [new Point(50, 100), new Point(100, 200)]

        model.startComponents << node2

        graphPersistenceService.save(model)

        assertEquals 1, GraphModel.count()

        long id = model.id
        GraphModel persistentModel = GraphModel.get(id)
        assertNotNull persistentModel

        assertEquals "name", persistentModel.name
        assertEquals "package", persistentModel.packageName

        assertEquals 2, persistentModel.nodes.size()
        assertEquals 1, persistentModel.edges.size()

        Node name = persistentModel.nodes.find { it.name == "name" }
        assertNotNull name

        assertFalse name.startComponent
        assertEquals TestComponent.name, name.className

        NodePort name_input3 = name.ports.find { it.name == "input3" }
        assertNotNull name_input3

        Node name2 = persistentModel.nodes.find { it.name == "name2" }
        assertTrue name2.startComponent
        assertEquals TestComponent.name, name2.className

        NodePort name2_outClaims = name2.ports.find { it.name == "outClaims" }
        assertNotNull name2_outClaims

        Edge edge = persistentModel.edges.toList()[0]
        assertEquals "${name.name}.${name_input3.name}", edge.from
        assertEquals "${name2.name}.${name2_outClaims.name}", edge.to

        GraphLayout layout = GraphLayout.findByGraphModel(persistentModel)
        final ComponentLayout nodeLayout = layout.components.find { it.node.name == "name" }
        assertEquals(10, nodeLayout.x)
        assertEquals(10, nodeLayout.y)
        assertEquals(100, nodeLayout.width)
        assertEquals(100, nodeLayout.height)

        final EdgeLayout edgeLayout = layout.edges.toList()[0]
        assertEquals(2, edgeLayout.points.size())
        assertNotNull(edgeLayout.points.find { it.x == 50 && it.y == 100})
        assertNotNull(edgeLayout.points.find { it.x == 100 && it.y == 200})

        persistentModel.discard()
        ModelGraphModel reloaded = graphPersistenceService.load(id)

        assertEquals "name", reloaded.name
        assertEquals "package", reloaded.packageName

        assertEquals 2, reloaded.allComponentNodes.size()
        final ComponentNode reloadedNode = reloaded.allComponentNodes.find { it.name == "name"}
        assertEquals(10, reloadedNode.getRectangle().getX())
        assertEquals(10, reloadedNode.getRectangle().getY())
        assertEquals(100, reloadedNode.getRectangle().getWidth())
        assertEquals(100, reloadedNode.getRectangle().getHeight())

        assertEquals 1, reloaded.allConnections.size()
        final Connection reloadedConnection = reloaded.allConnections[0]
        assertEquals(2, reloadedConnection.controlPoints.size())
        assertNotNull(reloadedConnection.controlPoints.find { it.x == 50 && it.y == 100})
        assertNotNull(reloadedConnection.controlPoints.find { it.x == 100 && it.y == 200})

        assertEquals 1, reloaded.startComponents.size()
    }

    void testSaveLoadComposedComponent() {
        ComposedComponentGraphModel model = new ComposedComponentGraphModel("name", "package")
        ComponentNode node = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "name")
        ComponentNode node2 = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "name2")

        model.createConnection(node.getPort("input3"), node2.getPort("outClaims"))

        model.createOuterInPort(TestPacket, "inOuter")
        model.createOuterOutPort(TestPacket, "outOuter")
        model.createConnection(model.getOuterPort("inOuter"), node.getPort("input3"))
        model.createConnection(node.getPort("outClaims"), model.getOuterPort("outOuter"))

        graphPersistenceService.save(model)

        assertEquals 1, GraphModel.count()

        long id = model.id
        GraphModel persistentModel = GraphModel.get(id)
        assertNotNull persistentModel

        assertEquals "name", persistentModel.name
        assertEquals "package", persistentModel.packageName

        assertEquals 2, persistentModel.nodes.size()
        assertEquals 3, persistentModel.edges.size()

        Node name = persistentModel.nodes.find { it.name == "name" }
        assertNotNull name

        assertFalse name.startComponent
        assertEquals TestComponent.name, name.className

        NodePort name_input3 = name.ports.find { it.name == "input3" }
        assertNotNull name_input3

        Node name2 = persistentModel.nodes.find { it.name == "name2" }
        assertFalse name2.startComponent
        assertEquals TestComponent.name, name2.className

        NodePort name2_outClaims = name2.ports.find { it.name == "outClaims" }
        assertNotNull name2_outClaims

        String innerEdgeFromName = "${name.name}.${name_input3.name}"
        for (Edge e : persistentModel.edges.toList()) {
            if (e.from.equals(innerEdgeFromName)) {
                assertEquals "${name2.name}.${name2_outClaims.name}", e.to
            } else if (e.from.equals("inOuter")) {
                assertEquals "${name.name}.${name_input3.name}", e.to
            } else if (e.to.equals("outOuter")) {
                assertEquals "${name.name}.${name2_outClaims.name}", e.from
            } else {
                fail()
            }
        }

        assertEquals 2, persistentModel.ports.size()

        ComposedComponentGraphModel reloaded = graphPersistenceService.load(id)

        assertEquals "name", reloaded.name
        assertEquals "package", reloaded.packageName

        assertEquals 2, reloaded.allComponentNodes.size()
        assertEquals 3, reloaded.allConnections.size()
        assertEquals 1, reloaded.outerInPorts.size()
        assertEquals 1, reloaded.outerOutPorts.size()
    }

    void testDelete() {
        ModelGraphModel model = new ModelGraphModel("name", "package")
        ComponentNode node = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "name")
        ComponentNode node2 = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "name2")

        model.createConnection(node.getPort("input3"), node2.getPort("outClaims"))

        graphPersistenceService.save(model)

        assertEquals 1, GraphModel.count()
        assertEquals 1, GraphLayout.count()

        assertNotNull model.id

        graphPersistenceService.delete(model)

        assertNull model.id

        assertEquals 0, GraphModel.count()
        assertEquals 0, Node.count()
        assertEquals 0, Edge.count()
        assertEquals 0, NodePort.count()
        assertEquals 0, GraphLayout.count()
        assertEquals 0, ControlPoint.count()
        assertEquals 0, ComponentLayout.count()
        assertEquals 0, EdgeLayout.count()
    }

    void testUpdate() {

        ModelGraphModel model = new ModelGraphModel("name", "package")
        ComponentNode node = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "name")
        node.rectangle = new Rectangle(10, 10, 100, 100)
        ComponentNode node2 = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "name2")

        final Connection connection = model.createConnection(node.getPort("input3"), node2.getPort("outClaims"))
        connection.controlPoints = [new Point(50, 100), new Point(100, 200)]

        graphPersistenceService.save(model)

        assertEquals 1, GraphModel.count()

        assertNotNull model.id
        long id = model.id

        model = new ModelGraphModel("name2", "package2")
        model.id = id
        node = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "newName")
        node.rectangle = new Rectangle(20, 20, 200, 200)
        node2 = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "newName2")

        connection = model.createConnection(node2.getPort("input3"), node.getPort("outClaims"))
        connection.controlPoints = [new Point(10, 20), new Point(20, 30)]

        graphPersistenceService.save(model)

        assertEquals 1, GraphModel.count()

        GraphModel persistentModel = GraphModel.get(model.id)
        assertNotNull persistentModel

        assertEquals "name2", persistentModel.name
        assertEquals "package2", persistentModel.packageName

        assertEquals 2, persistentModel.nodes.size()
        assertEquals 1, persistentModel.edges.size()

        Node name = persistentModel.nodes.find { it.name == "newName" }
        assertNotNull name

        assertEquals TestComponent.name, name.className

        NodePort name_outClaims = name.ports.find { it.name == "outClaims" }
        assertNotNull name_outClaims

        Node name2 = persistentModel.nodes.find { it.name == "newName2" }
        assertEquals TestComponent.name, name2.className

        NodePort name2_input3 = name2.ports.find { it.name == "input3" }
        assertNotNull name2_input3

        Edge edge = persistentModel.edges.toList()[0]
        assertEquals "${name2.name}.${name2_input3.name}", edge.from
        assertEquals "${name.name}.${name_outClaims.name}", edge.to

        GraphLayout layout = GraphLayout.findByGraphModel(persistentModel)
        final ComponentLayout nodeLayout = layout.components.find { it.node.name == "newName" }
        assertEquals(20, nodeLayout.x)
        assertEquals(20, nodeLayout.y)
        assertEquals(200, nodeLayout.width)
        assertEquals(200, nodeLayout.height)

        final EdgeLayout edgeLayout = layout.edges.toList()[0]
        assertEquals(2, edgeLayout.points.size())
        assertNotNull(edgeLayout.points.find { it.x == 10 && it.y == 20})
        assertNotNull(edgeLayout.points.find { it.x == 20 && it.y == 30})

    }
}
