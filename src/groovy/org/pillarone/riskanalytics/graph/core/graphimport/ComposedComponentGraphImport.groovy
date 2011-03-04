package org.pillarone.riskanalytics.graph.core.graphimport

import org.codehaus.groovy.runtime.DefaultGroovyMethods
import org.pillarone.riskanalytics.core.components.Component
import org.pillarone.riskanalytics.core.components.ComposedComponent
import org.pillarone.riskanalytics.core.packets.PacketList
import org.pillarone.riskanalytics.core.wiring.ITransmitter
import org.pillarone.riskanalytics.core.wiring.WiringUtils
import org.pillarone.riskanalytics.graph.core.graph.model.AbstractGraphModel
import org.pillarone.riskanalytics.graph.core.graph.model.ComponentNode
import org.pillarone.riskanalytics.graph.core.graph.model.ComposedComponentGraphModel
import org.pillarone.riskanalytics.graph.core.graph.model.Port

public class ComposedComponentGraphImport extends AbstractGraphImport {

    @Override
    public AbstractGraphModel importGraph(Class clazz) {
        try {
            ComposedComponent m = (ComposedComponent) clazz.newInstance();

            ComposedComponentGraphModel graph = new ComposedComponentGraphModel(m.getClass().getSimpleName(), m.getClass().getPackage().name);
            HashMap<Component, ComponentNode> components = getComponents(m, graph);


            addOuterPorts(graph, m);

            m.wire();

            addConnections(graph, components, m);
            return graph;

        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            return null;
        }
    }



    protected void addConnections(AbstractGraphModel graph, HashMap<Component, ComponentNode> components, Object graphClass) {
        super.addConnections(graph, components);
        ComposedComponent c = (ComposedComponent) graphClass;

        for (ITransmitter t: c.getAllOutputReplicationTransmitter()) {
            String outPort = WiringUtils.getSenderChannelName(t.getReceiver(), t.getTarget());
            String fromPort = WiringUtils.getSenderChannelName(t.getSender(), t.getSource());
            ComponentNode fromComp = components.get(t.getSender());
            if (fromComp == null)
                continue;
            Port fromP = fromComp.outPorts.find {it.name.equals(fromPort)};
            Port toP = ((ComposedComponentGraphModel) graph).getOuterOutPorts().find {it.name.equals(outPort)};
            graph.createConnection(fromP, toP);
        }

        for (ITransmitter t: c.getAllInputReplicationTransmitter()) {
            String inPort = WiringUtils.getSenderChannelName(t.getSender(), t.getSource());
            String toPort = WiringUtils.getSenderChannelName(t.getReceiver(), t.getTarget());
            ComponentNode toComp = components.get(t.getReceiver());
            if (toComp == null)
                continue;
            Port toP = toComp.inPorts.find {it.name.equals(toPort)};
            Port fromP = ((ComposedComponentGraphModel) graph).getOuterInPorts().find {it.name.equals(inPort)};
            graph.createConnection(fromP, toP);
        }
    }


    private void addOuterPorts(ComposedComponentGraphModel graph, ComposedComponent graphClass) {
        for (MetaProperty mp: graphClass.metaClass.getProperties()) {
            if (mp.name.startsWith("in")) {
                PacketList p = DefaultGroovyMethods.getAt(graphClass, mp.name);
                graph.createOuterInPort(p.type, mp.name);
            } else if (mp.name.startsWith("out")) {
                PacketList p = DefaultGroovyMethods.getAt(graphClass, mp.name);
                graph.createOuterOutPort(p.type, mp.name);
            }
        }
    }
}
