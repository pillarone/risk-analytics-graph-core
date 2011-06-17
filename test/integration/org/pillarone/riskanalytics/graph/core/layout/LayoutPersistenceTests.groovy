package org.pillarone.riskanalytics.graph.core.layout

import org.pillarone.riskanalytics.graph.core.graph.model.ModelGraphModel
import org.pillarone.riskanalytics.graph.core.graph.model.ComponentNode
import org.pillarone.riskanalytics.graph.core.palette.service.PaletteService
import org.pillarone.riskanalytics.core.example.component.TestComponent
import org.pillarone.riskanalytics.core.example.component.TestComposedComponent
import org.pillarone.riskanalytics.graph.core.graph.model.AbstractGraphModel
import org.pillarone.riskanalytics.graph.core.graph.model.ComposedComponentNode

class LayoutPersistenceTests extends GroovyTestCase {

    void testStoreLoad() {
        ModelGraphModel model = new ModelGraphModel("name", "package")
        ComponentNode node = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "name")
        ComponentNode node2 = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComposedComponent), "name2")

        List<ComponentLayout> componentLayouts = new ArrayList<ComponentLayout>()
        int i = 0
        for (ComponentNode n: model.allComponentNodes) {
            componentLayouts.add(new ComponentLayout(name: n.name, type: n.type.typeClass, x: i, y: i));
            i++
        }
        GraphLayoutService.getInstance().saveLayout(0, "test", model.packageName + "." + model.name, componentLayouts)

        Set<ComponentLayout> result = GraphLayoutService.getInstance().loadLayout(0, "test", model.packageName + "." + model.name);
        assertEquals model.allComponentNodes.size(), result.size()
        for (i = 0; i < result.size(); i++) {
            assertNotNull result.find {it.x == i}
            assertNotNull result.find {it.y == i}
        }
        GraphLayoutService.getInstance().deleteLayout(0, model.packageName + "." + model.name);
        assertEquals false, GraphLayoutService.getInstance().findLayout(0, "test", model.packageName + "." + model.name)

        GraphLayoutService.getInstance().saveLayout(0, "test", model.packageName + "." + model.name, createLayouts(model));
        result = GraphLayoutService.getInstance().loadLayout(0, "test", model.packageName + "." + model.name);

        Map<ComponentNode, ComponentLayout> m = GraphLayoutService.getInstance().resolveGraphModel(model, result);

        assertEquals 4, m.values().size();

    }

    List<ComponentLayout> createLayouts(ModelGraphModel m) {
        List<ComponentLayout> s = new ArrayList<ComponentLayout>();
        createLayoutsRec(s, m, null);
        return s;
    }

    void createLayoutsRec(List<ComponentLayout> layouts, AbstractGraphModel m, String path) {
        for (ComponentNode n: m.allComponentNodes) {
            String name = (path == null ? "" : path + ".") + n.name;
            layouts.add(new ComponentLayout(name: name, type: n.type.typeClass, x: 1, y: 1));
            if (n instanceof ComposedComponentNode)
                createLayoutsRec(layouts, n.componentGraph, name);
        }
    }
}
