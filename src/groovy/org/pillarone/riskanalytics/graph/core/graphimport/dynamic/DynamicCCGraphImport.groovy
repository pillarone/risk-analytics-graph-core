package org.pillarone.riskanalytics.graph.core.graphimport.dynamic

import org.codehaus.groovy.runtime.DefaultGroovyMethods
import org.pillarone.riskanalytics.core.components.Component
import org.pillarone.riskanalytics.core.components.ComposedComponent
import org.pillarone.riskanalytics.core.components.DynamicComposedComponent
import org.pillarone.riskanalytics.core.packets.PacketList
import org.pillarone.riskanalytics.core.wiring.Transmitter
import org.pillarone.riskanalytics.core.wiring.WiringUtils
import org.pillarone.riskanalytics.graph.core.graph.model.ComponentNode
import org.pillarone.riskanalytics.graph.core.graph.model.ComposedComponentGraphModel
import org.pillarone.riskanalytics.graph.core.graph.model.Connection
import org.pillarone.riskanalytics.graph.core.graph.model.Port

public class DynamicCCGraphImport extends AbstractDynamicGraphImport {

    public ComposedComponentGraphModel createFromWired(ComposedComponent cc) {
        graph = new ComposedComponentGraphModel(cc.getClass().getSimpleName(), cc.getClass().getPackage().name);
        addOuterPorts(graph, cc);
        for (Component c: cc.allSubComponents()) {
            if (!DynamicComposedComponent.isAssignableFrom(c.class))
                addComponentNode(cc, c);
            for (Transmitter t: c.allInputTransmitter) {
                if (t.sender == cc)
                    continue;
                wireComponents(cc, cc, t);
            }
        }

        for (Transmitter t: cc.allInputReplicationTransmitter) {
            if (DynamicComposedComponent.isAssignableFrom(t.receiver.class)) {
                for (ComponentPortTuple tuple: getReplicatedInputs(t.receiver, t.target)) {
                    addComponentNode(t.receiver, tuple.component);
                    wireInReplication(cc, tuple.component, t.source, tuple.port);
                }
            } else {
                addComponentNode(cc, t.receiver);
                new ParameterConstraints().isWired(cc,t.receiver);
                wireInReplication(cc, t.receiver, t.source, t.target);
            }
        }

        for (Transmitter t: cc.allOutputReplicationTransmitter) {
            if (DynamicComposedComponent.isAssignableFrom(t.sender.class)) {

                for (ComponentPortTuple tuple: getReplicatedOutputs(t.sender, t.source)) {
                    addComponentNode(t.sender, tuple.component);
                    wireOutReplication(tuple.component, cc, tuple.port, t.target);
                }
            } else {
                addComponentNode(cc, t.sender);
                new ParameterConstraints().isWired(t.sender,cc);
                wireOutReplication(t.sender, cc, t.source, t.target);
            }
        }

        return graph;
    }

    private void wireInReplication(Component src, Component dst, PacketList srcPort, PacketList dstPort) {
        String toPort = WiringUtils.getSenderChannelName(dst, dstPort);
        String fromPort = WiringUtils.getSenderChannelName(src, srcPort);
        ComponentNode toComp = components.get(dst);
        Port toP = toComp.inPorts.find {it.name.equals(toPort)};
        Port fromP = ((ComposedComponentGraphModel) graph).getOuterInPorts().find {it.name.equals(fromPort)};
        Connection connection = graph.createConnection(fromP, toP);
        println(src.class.simpleName + "." + fromP + " -> " + toComp.name + "." + toP);
    }

    private void wireOutReplication(Component src, Component dst, PacketList srcPort, PacketList dstPort) {
        String toPort = WiringUtils.getSenderChannelName(dst, dstPort);
        String fromPort = WiringUtils.getSenderChannelName(src, srcPort);
        ComponentNode fromComp = components.get(src);
        Port fromP = fromComp.outPorts.find {it.name.equals(fromPort)};
        Port toP = ((ComposedComponentGraphModel) graph).getOuterOutPorts().find {it.name.equals(toPort)};
        Connection connection = graph.createConnection(fromP, toP);
        println(fromComp.name + "." + fromP + " -> " + dst.class.simpleName + "." + toP);
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
