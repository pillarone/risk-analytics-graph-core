package org.pillarone.riskanalytics.graph.core.graph.persistence

import org.pillarone.riskanalytics.core.example.component.TestComponent
import org.pillarone.riskanalytics.graph.core.graph.model.ComponentNode
import org.pillarone.riskanalytics.graph.core.graph.model.ModelGraphModel
import org.pillarone.riskanalytics.graph.core.palette.service.PaletteService

class GraphPersistenceServiceTests extends GroovyTestCase {

    GraphPersistenceService graphPersistenceService

    void testSave() {
        ModelGraphModel model = new ModelGraphModel("name", "package")
        ComponentNode node = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "name")
        ComponentNode node2 = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "name2")

        model.createConnection(node.getPort("input3"), node2.getPort("outClaims"))

        graphPersistenceService.save(model)

        assertEquals 1, GraphModel.count()

        GraphModel persistentModel = GraphModel.get(model.id)
        assertNotNull persistentModel

        assertEquals "name", persistentModel.name
        assertEquals "package", persistentModel.packageName

        assertEquals 2, persistentModel.nodes.size()
        assertEquals 1, persistentModel.edges.size()

        Node name = persistentModel.nodes.find { it.name == "name" }
        assertNotNull name

        assertEquals TestComponent.name, name.className

        NodePort name_input3 = name.ports.find { it.name == "input3" }
        assertNotNull name_input3

        Node name2 = persistentModel.nodes.find { it.name == "name2" }
        assertEquals TestComponent.name, name2.className

        NodePort name2_outClaims = name2.ports.find { it.name == "outClaims" }
        assertNotNull name2_outClaims

        Edge edge = persistentModel.edges.toList()[0]
        assertSame name_input3, edge.from
        assertSame name2_outClaims, edge.to
    }

    void testDelete() {
        ModelGraphModel model = new ModelGraphModel("name", "package")
        ComponentNode node = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "name")
        ComponentNode node2 = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "name2")

        model.createConnection(node.getPort("input3"), node2.getPort("outClaims"))

        graphPersistenceService.save(model)

        assertEquals 1, GraphModel.count()

        assertNotNull model.id

        graphPersistenceService.delete(model)

        assertNull model.id

        assertEquals 0, GraphModel.count()
        assertEquals 0, Node.count()
        assertEquals 0, Edge.count()
        assertEquals 0, NodePort.count()
    }

    void testUpdate() {

        ModelGraphModel model = new ModelGraphModel("name", "package")
        ComponentNode node = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "name")
        ComponentNode node2 = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "name2")

        model.createConnection(node.getPort("input3"), node2.getPort("outClaims"))

        graphPersistenceService.save(model)

        assertEquals 1, GraphModel.count()

        assertNotNull model.id
        long id = model.id

        model = new ModelGraphModel("name2", "package2")
        model.id = id
        node = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "newName")
        node2 = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "newName2")

        model.createConnection(node2.getPort("input3"), node.getPort("outClaims"))

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
        assertSame name2_input3, edge.from
        assertSame name_outClaims, edge.to

    }
}
