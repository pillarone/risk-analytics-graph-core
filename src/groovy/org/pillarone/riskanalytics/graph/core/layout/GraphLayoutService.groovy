package org.pillarone.riskanalytics.graph.core.layout

import org.pillarone.riskanalytics.graph.core.graph.model.ComponentNode
import org.pillarone.riskanalytics.graph.core.graph.model.AbstractGraphModel
import org.pillarone.riskanalytics.graph.core.graph.model.ComposedComponentNode

class GraphLayoutService {

    private static GraphLayoutService graphLayoutService;

    private GraphLayoutService() {

    }

    public static GraphLayoutService getInstance() {
        if (graphLayoutService == null)
            graphLayoutService = new GraphLayoutService()
        return graphLayoutService;
    }

    public boolean findLayout(long userId, String layoutName, String graphName) {
        return (GraphLayout.find("from GraphLayout where userId=? and layoutName=? and graphModelName=?", [userId, layoutName, graphName]) != null);
    }

    public void saveLayout(long userId, String layoutName, String graphName, List<ComponentLayout> components) {
        GraphLayout layout;

        if ((layout = GraphLayout.find("from GraphLayout where userId=? and layoutName=? and graphModelName=?", [userId, layoutName, graphName])) != null) {
            layout.components.clear();
        } else {
            layout = new GraphLayout(userId: userId, layoutName: layoutName, graphModelName: graphName);
        }
        for (ComponentLayout component: components) {
            layout.addToComponents(component);
        }
        layout.save();
    }

    public void deleteLayout(long userId, String graphName) {
        GraphLayout layout;
        if ((layout = GraphLayout.findByGraphModelNameAndUserId(graphName, userId)) != null) {
            layout.delete();
        }
    }

    public List<String> getLayoutsOfGraph(long userId, String graphName) {
        List<GraphLayout> layouts;
        List<String> ret = new ArrayList<String>();
        if ((layouts = GraphLayout.findAllByGraphModelNameAndUserId(graphName, userId)) != null) {
            for (GraphLayout layout: layouts) {
                ret.add(layout.layoutName);
            }
        }
        return ret;
    }

    public Set<ComponentLayout> loadLayout(long userId, String layoutName, String graphName) {
        GraphLayout layout;
        if ((layout = GraphLayout.find("from GraphLayout where userId=? and layoutName=? and graphModelName=?", [userId, layoutName, graphName])) == null)
            return null;
        return layout.components;
    }

    public Map<ComponentNode, ComponentLayout> resolveGraphModel(AbstractGraphModel m, Set<ComponentLayout> layouts) {
        Map<ComponentNode, ComponentLayout> resolved = new HashMap<ComponentNode, ComponentLayout>();
        resolveComponentRec(layouts, m, null, resolved)
        return resolved;
    }

    private void resolveComponentRec(Set<ComponentLayout> layouts, AbstractGraphModel m, String path, Map<ComponentNode, ComponentLayout> resolved) {
        for (ComponentNode n: m.allComponentNodes) {
            ComponentLayout l;
            String name = (path == null ? "" : path + ".") + n.name;
            if ((l = layouts.find {it.name.equals(name)}) != null) {
                resolved.put(n, l);
                if (n instanceof ComposedComponentNode)
                    resolveComponentRec(layouts, n.componentGraph, name, resolved);
            }
        }
    }
}
