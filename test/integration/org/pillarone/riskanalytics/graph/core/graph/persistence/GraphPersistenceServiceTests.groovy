package org.pillarone.riskanalytics.graph.core.graph.persistence

import org.pillarone.riskanalytics.graph.core.graph.model.Connection
import org.pillarone.riskanalytics.core.example.component.TestComponent
import org.pillarone.riskanalytics.graph.core.palette.service.PaletteService
import org.pillarone.riskanalytics.graph.core.graph.model.ComponentNode
import org.pillarone.riskanalytics.graph.core.graph.model.ModelGraphModel


class GraphPersistenceServiceTests extends GroovyTestCase {

    GraphPersistenceService graphPersistenceService

    void testSave() {
        ModelGraphModel model = new ModelGraphModel("name", "package")
        ComponentNode node = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "name")
        ComponentNode node2 = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "name2")

        model.createConnection(node.getPort("input3"), node2.getPort("outClaims"))

        graphPersistenceService.save(model)

        assertEquals 1, GraphModel.list().size()
    }
}
