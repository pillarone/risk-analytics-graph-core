package org.pillarone.riskanalytics.graph.core.graphimport

import org.pillarone.riskanalytics.core.components.Component
import org.pillarone.riskanalytics.core.model.Model
import org.pillarone.riskanalytics.graph.core.graph.model.AbstractGraphModel
import org.pillarone.riskanalytics.graph.core.graph.model.ModelGraphModel
import org.pillarone.riskanalytics.graph.core.graph.model.ComponentNode

public class ModelGraphImport extends AbstractGraphImport {

    @Override
    public AbstractGraphModel importGraph(Class clazz, String comments) {

        commentImport = new CommentImport(comments);
        Model m = (Model) clazz.newInstance();
        m.init();
        m.wire();

        return createFromWiredModel(m);

    }


    public ModelGraphModel createFromWiredModel(Model m) {
        commentImport = new CommentImport(null);
        ModelGraphModel graph = new ModelGraphModel(m.getClass().getSimpleName(), m.getClass().getPackage().name);
        HashMap<Component, ComponentNode> components = getComponents(m.allComponents,m, graph);
        addStartComponents(graph, components, m);
        addConnections(graph, components);
        return graph;
    }


    private void addStartComponents(ModelGraphModel graph, HashMap<Component, ComponentNode> components, Model graphClass) {

        for (Component c: graphClass.startComponents) {
            ComponentNode n = components.get(c);
            graph.startComponents.add(n);
        }
    }
}
