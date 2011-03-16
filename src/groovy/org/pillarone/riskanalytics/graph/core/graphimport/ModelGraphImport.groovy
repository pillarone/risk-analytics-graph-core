package org.pillarone.riskanalytics.graph.core.graphimport

import org.pillarone.riskanalytics.core.components.Component
import org.pillarone.riskanalytics.core.model.Model
import org.pillarone.riskanalytics.graph.core.graph.model.AbstractGraphModel
import org.pillarone.riskanalytics.graph.core.graph.model.ModelGraphModel
import org.pillarone.riskanalytics.graph.core.graph.model.ComponentNode

public class ModelGraphImport extends AbstractGraphImport {

    @Override
    public AbstractGraphModel importGraph(Class clazz, String comments) {

        try {
            commentImport = new CommentImport(comments);
            Model m = (Model) clazz.newInstance();

            m.init();

            ModelGraphModel graph = new ModelGraphModel(m.getClass().getSimpleName(), m.getClass().getPackage().name);
            HashMap<Component, ComponentNode> components = getComponents(m, graph);

            addStartComponents(graph, components, m);

            m.wire();

            addConnections(graph, components);
            return graph;

        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            return null;
        }
    }


    private void addStartComponents(ModelGraphModel graph, HashMap<Component, ComponentNode> components, Model graphClass) {

        for (Component c: graphClass.startComponents) {
            ComponentNode n = components.get(c);
            graph.startComponents.add(n);
        }
    }
}
