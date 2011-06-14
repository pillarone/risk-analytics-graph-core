package org.pillarone.riskanalytics.graph.core.layout

import org.pillarone.riskanalytics.graph.core.graph.model.ModelGraphModel
import org.pillarone.riskanalytics.graph.core.graph.model.ComponentNode
import org.pillarone.riskanalytics.graph.core.palette.service.PaletteService
import org.pillarone.riskanalytics.core.example.component.TestComponent

class LayoutPersistenceTests extends GroovyTestCase {

    void testStoreLoad() {
        ModelGraphModel model = new ModelGraphModel("name", "package")
        ComponentNode node = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "name")
        ComponentNode node2 = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "name2")

        Set<ComponentLayout> componentLayouts = new ArrayList<ComponentLayout>()
        int i = 0
        for (ComponentNode n: model.allComponentNodes) {
            componentLayouts.add(new ComponentLayout(name: n.name, type: n.type.typeClass, x: i, y: i));
            i++
        }
        GraphLayoutService.getInstance().saveLayout(0, model.packageName + "." + model.name, componentLayouts)

        componentLayouts = GraphLayoutService.getInstance().loadLayout(0, model.packageName + "." + model.name);
        assertEquals model.allComponentNodes.size(), componentLayouts.size()
        for (i=0;i<componentLayouts.size();i++) {
            assertNotNull componentLayouts.find{it.x==i}
            assertNotNull componentLayouts.find{it.y==i}
        }
    }
}
