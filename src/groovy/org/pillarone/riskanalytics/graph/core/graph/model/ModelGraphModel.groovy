package org.pillarone.riskanalytics.graph.core.graph.model


class ModelGraphModel extends AbstractGraphModel {

    List<ComponentNode> startComponents = []


    ModelGraphModel() {
    }

    ModelGraphModel(String name, String packageName) {
        super(name, packageName)
    }


    public List<ComponentNode> resolveStartComponents() {
        List<ComponentNode> startComponents = new ArrayList<ComponentNode>();
        for (ComponentNode c: allComponentNodes) {
            boolean inConnected;
            for (InPort inport: c.inPorts) {
                if (allConnections.find {it.to == inport} != null) {
                    inConnected = true;
                }
            }
            if (!inConnected) {
                startComponents.add(c);
            }
        }
        return startComponents;
    }
}
