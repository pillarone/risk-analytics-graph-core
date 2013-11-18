package org.pillarone.riskanalytics.graph.core.graphimport

import org.codehaus.groovy.runtime.DefaultGroovyMethods
import org.pillarone.riskanalytics.core.components.Component
import org.pillarone.riskanalytics.core.components.ComposedComponent
import org.pillarone.riskanalytics.core.packets.PacketList
import org.pillarone.riskanalytics.core.wiring.ITransmitter
import org.pillarone.riskanalytics.core.wiring.WiringUtils
import org.pillarone.riskanalytics.graph.core.graph.model.*

public class ComposedComponentGraphImport extends AbstractGraphImport {

    @Override
    public AbstractGraphModel importGraph(Class clazz, String comments) {

        ComposedComponent cc = (ComposedComponent) clazz.newInstance();

        cc.wire();

        if (comments != null) addGraphImportListener(new CommentImport(comments));
        return createFromWiredComponent(cc);
    }


    
    public ComposedComponentGraphModel createFromWiredComponent(ComposedComponent cc) {
        ComposedComponentGraphModel graph = new ComposedComponentGraphModel(cc.getClass().getSimpleName(), cc.getClass().getPackage().name);
        HashMap<Component, ComponentNode> components = getComponents(cc.allSubComponents(),cc, graph);
        addOuterPorts(graph, cc);
        addConnections(graph, components, cc);
        return graph;
    }

    protected void addConnections(ComposedComponentGraphModel graph, HashMap<Component, ComponentNode> components, Object graphClass) {
        super.addConnections(graph, components);
        ComposedComponent c = (ComposedComponent) graphClass;

        for (ITransmitter t: c.getAllOutputReplicationTransmitter()) {
            String outPort = WiringUtils.getSenderChannelName(t.getReceiver(), t.getTarget());
            String fromPort = WiringUtils.getSenderChannelName(t.getSender(), t.getSource());
            ComponentNode fromComp = components.get(t.getSender());
            if (fromComp == null)
                continue;
            Port fromP = fromComp.outPorts.find {it.name.equals(fromPort)};
            Port toP = graph.getOuterOutPorts().find {it.name.equals(outPort)};
            Connection connection = graph.createConnection(fromP, toP);
            importListeners.each{it.connectionImported(connection)};
        }

        for (ITransmitter t: c.getAllInputReplicationTransmitter()) {
            String inPort = WiringUtils.getSenderChannelName(t.getSender(), t.getSource());
            String toPort = WiringUtils.getSenderChannelName(t.getReceiver(), t.getTarget());
            ComponentNode toComp = components.get(t.getReceiver());
            if (toComp == null)
                continue;
            Port toP = toComp.inPorts.find {it.name.equals(toPort)};
            Port fromP = graph.getOuterInPorts().find {it.name.equals(inPort)};
            Connection connection = graph.createConnection(fromP, toP);
            importListeners.each{it.connectionImported(connection)};
        }
    }


    private void addOuterPorts(ComposedComponentGraphModel graph, ComposedComponent graphClass) {
        for (MetaProperty mp: graphClass.metaClass.getProperties()) {
            if (mp.name.startsWith("in") && !mp.name.equals('interceptor')) {
                PacketList p = DefaultGroovyMethods.getAt(graphClass, mp.name);
                graph.createOuterInPort(p.type, mp.name);
            } else if (mp.name.startsWith("out")) {
                PacketList p = DefaultGroovyMethods.getAt(graphClass, mp.name);
                graph.createOuterOutPort(p.type, mp.name);
            }
        }
    }

    @Override protected ComposedComponentGraphModel getComposedComponentGraph(ComposedComponent cc) {
        cc.wire();
        return new ComposedComponentGraphImport().createFromWiredComponent(cc);
    }
}
