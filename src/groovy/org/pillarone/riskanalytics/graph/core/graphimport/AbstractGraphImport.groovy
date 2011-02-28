package org.pillarone.riskanalytics.graph.core.graphimport

import org.codehaus.groovy.runtime.DefaultGroovyMethods
import org.pillarone.riskanalytics.core.components.Component
import org.pillarone.riskanalytics.core.wiring.ITransmitter
import org.pillarone.riskanalytics.core.wiring.WiringUtils
import org.pillarone.riskanalytics.graph.core.graph.model.AbstractGraphModel
import org.pillarone.riskanalytics.graph.core.graph.model.ComponentNode
import org.pillarone.riskanalytics.graph.core.palette.service.PaletteService
import org.pillarone.riskanalytics.graph.core.graph.model.Port

public abstract class AbstractGraphImport {


    public abstract AbstractGraphModel importGraph (Class clazz);

    protected HashMap<Component, ComponentNode> getComponents(Object o,AbstractGraphModel graph) {

        HashMap<Component, ComponentNode> components = new HashMap<Component, ComponentNode>();
        for (MetaProperty mp: o.metaClass.getProperties()) {
            if (Component.isAssignableFrom(mp.type)) {
                ComponentNode n=graph.createComponentNode(PaletteService.getInstance().getComponentDefinition(mp.type),mp.name);
                components.put(DefaultGroovyMethods.getAt(o, mp.name), n);
            }
        }
        return components;
    }


    protected void addConnections(AbstractGraphModel graph, HashMap<Component, ComponentNode> components, Object graphClass) {
        HashMap<String, Boolean> visited = new HashMap<String, Boolean>();

        for (Component c: components.keySet()) {
            List allTransmitter = new ArrayList();
            allTransmitter.addAll(c.getAllOutputTransmitter());
            allTransmitter.addAll(c.getAllInputTransmitter());
            for (ITransmitter t: allTransmitter) {
                String toPort = WiringUtils.getSenderChannelName(t.getReceiver(), t.getTarget());
                String fromPort = WiringUtils.getSenderChannelName(t.getSender(), t.getSource());
                ComponentNode toComp = components.get(t.getReceiver());
                ComponentNode fromComp = components.get(t.getSender());

                if (toComp == null || fromComp == null) {
                    continue;
                }

                String key = fromComp.name + "." + fromPort + "=" + toComp.name + "." + toPort;

                Port fromP = fromComp.outPorts.find{it.name.equals(fromPort)};
                Port toP = toComp.inPorts.find{it.name.equals(toPort)};

                if (fromP != null && toP != null) {
                    if (!visited.get(key)) {
                        graph.createConnection(fromP,toP);
                        visited.put(key, true);
                    }
                }

            }
        }
    }


}
