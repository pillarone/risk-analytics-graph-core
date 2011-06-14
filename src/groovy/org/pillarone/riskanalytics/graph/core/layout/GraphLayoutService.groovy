package org.pillarone.riskanalytics.graph.core.layout

class GraphLayoutService {

    private static GraphLayoutService graphLayoutService;

    private GraphLayoutService() {

    }

    public static GraphLayoutService getInstance() {
        if (graphLayoutService == null)
            graphLayoutService = new GraphLayoutService()
        return graphLayoutService;
    }

    public void saveLayout(long userId, String graphName, Set<ComponentLayout> components) {
        GraphLayout layout;
        if ((layout = GraphLayout.findByGraphModelNameAndUserId(graphName, userId)) != null) {
            layout.components.clear();
        } else {
            layout = new GraphLayout(graphModelName: graphName, userId: userId);
        }
        for (ComponentLayout component: components) {
            layout.addToComponents(component);
        }
        layout.save();
    }

    public Set<ComponentLayout> loadLayout(long userId, String graphName) {
        GraphLayout layout;
        if ((layout = GraphLayout.findByGraphModelNameAndUserId(graphName, userId)) == null)
            return null;
        return layout.components;
    }
}
