package org.pillarone.riskanalytics.graph.core.layout

import org.pillarone.riskanalytics.graph.core.graph.model.AbstractGraphModel
import org.pillarone.riskanalytics.graph.core.graph.model.ComponentNode
import org.pillarone.riskanalytics.graph.core.graph.model.ComposedComponentNode

class GraphLayoutService {

    public boolean findLayout(long userId, String layoutName, String graphName) {
        return (loadLayout(userId, layoutName, graphName) != null);
    }

    public void saveLayout(long userId, String layoutName, String graphName, List<ComponentLayout> components) {
        GraphLayout layout;

        if ((layout = loadLayout(userId, layoutName, graphName)) != null) {
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
        Set<ComponentLayout> s = new HashSet<ComponentLayout>();
        for (ComponentLayout cl: layout.components) {
            s.add(new ComponentLayout(name:cl.name,type:cl.type,x:cl.x,y:cl.y,h:cl.h,w:cl.w,unfolded: cl.unfolded));
        }
        return s;
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
