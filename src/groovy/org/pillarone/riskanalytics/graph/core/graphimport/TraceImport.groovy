package org.pillarone.riskanalytics.graph.core.graphimport

import org.pillarone.riskanalytics.core.wiring.IPacketListener
import org.pillarone.riskanalytics.core.wiring.Transmitter
import org.pillarone.riskanalytics.core.components.Component
import org.pillarone.riskanalytics.core.packets.PacketList
import org.pillarone.riskanalytics.core.wiring.WiringUtils
import org.pillarone.riskanalytics.core.components.DynamicComposedComponent
import org.pillarone.riskanalytics.graph.core.graph.model.ComponentNode
import org.pillarone.riskanalytics.core.components.ComposedComponent
import org.pillarone.riskanalytics.graph.core.graph.model.ComposedComponentGraphModel
import org.codehaus.groovy.runtime.DefaultGroovyMethods
import org.pillarone.riskanalytics.graph.core.graph.model.AbstractGraphModel
import org.pillarone.riskanalytics.graph.core.palette.service.PaletteService
import org.pillarone.riskanalytics.graph.core.graph.model.Port
import org.pillarone.riskanalytics.graph.core.graph.model.ComposedComponentNode
import org.pillarone.riskanalytics.graph.core.graph.model.InPort
import org.pillarone.riskanalytics.graph.core.graph.model.OutPort
import org.pillarone.riskanalytics.core.model.Model
import org.pillarone.riskanalytics.graph.core.graph.model.ModelGraphModel
import org.pillarone.riskanalytics.domain.pc.lob.DynamicConfigurableLobsWithReserves

public class TraceImport implements IPacketListener {

    private HashMap<Component, HashMap<ListAsKey, List<ComponentPort>>> connections = new HashMap<Component, HashMap<ListAsKey, List<ComponentPort>>>();

    HashMap<Component, ComponentNodeParent> componentCache = new HashMap<Component, ComponentNodeParent>();

    void packetSent(Transmitter t) {
        List<ComponentPort> dst = fillConnections(t.sender, t.source);
        Component origin = null;
        ComponentPort cp = new ComponentPort(component: t.receiver, packetList: t.target, origin: ComposedComponent.isAssignableFrom(t.receiver.class) ? null : origin);
        if (DynamicConfigurableLobsWithReserves.class.isAssignableFrom(t.sender.class)) {
            println "";
        }
        if (dst.find {it.component == cp.component && it.packetList.is(cp.packetList) && it.origin == cp.origin} == null) {
            dst.add(cp);
        }
    }

    private List<ComponentPort> fillConnections(Component c, PacketList p) {
        HashMap<ListAsKey, List<ComponentPort>> ports = connections.get(c);
        if (ports == null) {
            ports = new HashMap<ListAsKey, List<ComponentPort>>();
            ports.put(new ListAsKey(p), new ArrayList<ComponentPort>());
            connections.put(c, ports);
        }
        else if (ports.get(new ListAsKey(p)) == null) {
            ports.put(new ListAsKey(p), new ArrayList<ComponentPort>());
        }
        return ports.get(new ListAsKey(p));
    }


    public void resolveConnections() {
        for (Component c: connections.keySet()) {
            if (!resolveComponent(c)) {
                HashMap<ListAsKey, List<ComponentPort>> ports = connections.get(c);
                for (ListAsKey p: ports.keySet()) {
                    List<ComponentPort> destinations = new ArrayList<ComponentPort>();
                    String s = WiringUtils.getSenderChannelName(c, p.packetList);
                    getDestinations(c, p.packetList, c, destinations);
                    s = WiringUtils.getSenderChannelName(destinations[0].component, destinations[0].packetList);
                    println()
                }
            }
        }
    }

    private void getDestinations(Component sender, PacketList source, Component origin, List<ComponentPort> destinations) {
        HashMap<ListAsKey, List<ComponentPort>> ports = connections.get(sender);
        List<ComponentPort> dst;
        dst = ports.get(new ListAsKey(source));
        if (dst == null) {
            return;
        }
        for (ComponentPort cp: dst) {
            if (resolveComponent(cp.component)) {
                getDestinations(cp.component, cp.packetList, origin, destinations);
            } else {
                if (cp.origin == null || cp.origin == origin)
                    destinations.add(cp);
            }
        }
    }


    private boolean resolveComponent(Component c) {
        return DynamicComposedComponent.isAssignableFrom(c.class);
    }

    public void initComponentCache(Model m) {
        ModelGraphModel mgm = new ModelGraphModel(m.class.simpleName, m.class.package.name);
        for (Component c: m.allComponents) {
            addComponentToModel(c, mgm)
        }
        int x = 0;
    }

    private ComponentNode createCCNode(ComposedComponent cc) {

        ComposedComponentNode cn = new ComposedComponentNode(name: cc.name, type: PaletteService.getInstance().getComponentDefinition(cc.class));
        ComposedComponentGraphModel graph = new ComposedComponentGraphModel(cc.getClass().getSimpleName(), cc.getClass().getPackage().name);
        for (Component c: cc.allSubComponents()) {
            addComponentToModel(c, graph);
        }
        addOuterPorts(graph, cc);

        List<PacketList> inProcessed = new ArrayList<PacketList>();
        for (Transmitter t: cc.allInputReplicationTransmitter) {
            if (inProcessed.find {it.is(t.source)} == null) {
                inProcessed.add(t.source);
                List<ComponentPort> tmp = new ArrayList<ComponentPort>();
                getReplicatedInputs(t.sender, t.source, tmp);
                for (ComponentPort cp: tmp) {
                    ComponentNodeParent dst = componentCache.get(cp.component);
                    if (dst == null) {
                        continue;
                    }
                    Port dstPort = dst.componentNode.getPort(WiringUtils.getSenderChannelName(cp.component, cp.packetList));
                    Port srcPort = graph.getOuterInPorts().find {return it.name.equals(WiringUtils.getSenderChannelName(cc, t.source))}
                    graph.createConnection(srcPort, dstPort);
                }
            }
        }

        List<PacketList> outProcessed = new ArrayList<PacketList>();
        for (Transmitter t: cc.allOutputReplicationTransmitter) {
            if (outProcessed.find {it.is(t.target)} == null) {
                outProcessed.add(t.target);
                List<ComponentPort> tmp = new ArrayList<ComponentPort>();
                getReplicatedOutputs(t.receiver, t.target, tmp);
                for (ComponentPort cp: tmp) {
                    ComponentNodeParent src = componentCache.get(cp.component);
                    if (src == null) {
                        continue;
                    }
                    Port dstPort = graph.getOuterOutPorts().find {it.name.equals(WiringUtils.getSenderChannelName(cc, t.target))}
                    Port srcPort = src.componentNode.getPort(WiringUtils.getSenderChannelName(cp.component, cp.packetList));
                    graph.createConnection(srcPort, dstPort);
                }
            }
        }

        cn.inPorts = new ArrayList<InPort>();
        cn.outPorts = new ArrayList<OutPort>();
        for (Port p: graph.outerInPorts) {
            cn.inPorts << p;
            p.componentNode = cn;
            p.composedComponentOuterPort = false;
        }
        for (Port p: graph.outerOutPorts) {
            cn.outPorts << p;
            p.componentNode = cn;
            p.composedComponentOuterPort = false;
        }
        cn.componentGraph = graph;
        return cn;
    }

    private void getReplicatedOutputs(ComposedComponent src, PacketList srcPort, List<ComponentPort> components) {
        String sendingName = WiringUtils.getSenderChannelName(src, srcPort);
        for (Transmitter t: src.allOutputReplicationTransmitter) {
            String receivingName = WiringUtils.getSenderChannelName(t.receiver, t.target);
            if (receivingName.equals(sendingName)) {
                if (resolveComponent(t.sender)) {
                    getReplicatedOutputs(t.sender, t.source, components);
                } else {
                    components.add(new ComponentPort(component: t.sender, packetList: t.source));
                }
            }
        }
    }

    private void getReplicatedInputs(ComposedComponent dst, PacketList dstPort, List<ComponentPort> components) {
        String receivingName = WiringUtils.getSenderChannelName(dst, dstPort);
        for (Transmitter t: dst.allInputReplicationTransmitter) {
            String sendingName = WiringUtils.getSenderChannelName(t.sender, t.source);
            if (sendingName.equals(receivingName)) {
                if (resolveComponent(t.receiver)) {
                    getReplicatedInputs(t.receiver, t.target, components);
                } else {
                    components.add(new ComponentPort(component: t.receiver, packetList: t.target));
                }
            }
        }
    }


    private void addComponentToModel(Component c, AbstractGraphModel m) {

        if (resolveComponent(c)) {
            for (Component sub: ((ComposedComponent) c).allSubComponents())
                addComponentToModel(sub, m);
        } else {
            ComponentNode cn;
            if (ComposedComponent.isAssignableFrom(c.class)) {
                cn = createCCNode(c);
                m.addComponentNode(cn);
            } else {
                cn = m.createComponentNode(PaletteService.getInstance().getComponentDefinition(c.class), c.name);
            }
            componentCache.put(c, new ComponentNodeParent(componentNode: cn, parent: m));
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

    public String toString() {
        StringBuilder sb = new StringBuilder("");
        for (Component c: connections.keySet()) {
            HashMap<ListAsKey, List<ComponentPort>> ports = connections.get(c);
            for (ListAsKey p: ports.keySet()) {
                List<ComponentPort> dst = ports.get(p);
                for (ComponentPort cp: dst) {
                    sb.append(c.class.simpleName + ":" + c.name + "." + WiringUtils.getSenderChannelName(c, p.packetList) + "->" +
                            cp.component.class.simpleName + ":" + cp.component.name + "." + WiringUtils.getSenderChannelName(cp.component, cp.packetList));
                    sb.append("\n");
                }
            }
        }
        return sb.toString();
    }

}

class ListAsKey {
    PacketList packetList;

    public ListAsKey(PacketList p) {
        packetList = p;
    }

    public boolean equals(Object o) {
        if (o instanceof ListAsKey) {
            return packetList.is(((ListAsKey) o).packetList);
        }
        return false;
    }

    public int hashCode() {
        return packetList.hashCode();
    }
}
class ComponentPort {
    Component component;
    PacketList packetList;
    Component origin;
}

class ComponentNodeParent {
    ComponentNode componentNode;
    AbstractGraphModel parent;
}
