package org.pillarone.riskanalytics.graph.core.graphimport

import org.codehaus.groovy.runtime.DefaultGroovyMethods
import org.pillarone.riskanalytics.core.components.Component
import org.pillarone.riskanalytics.core.components.ComposedComponent
import org.pillarone.riskanalytics.core.components.DynamicComposedComponent
import org.pillarone.riskanalytics.core.model.Model
import org.pillarone.riskanalytics.core.packets.Packet
import org.pillarone.riskanalytics.core.packets.PacketList
import org.pillarone.riskanalytics.core.wiring.IPacketListener
import org.pillarone.riskanalytics.core.wiring.Transmitter
import org.pillarone.riskanalytics.core.wiring.WiringUtils
import org.pillarone.riskanalytics.graph.core.palette.service.PaletteService
import org.pillarone.riskanalytics.graph.core.graph.model.*

public class TraceImport implements IPacketListener {

    private HashMap<Component, HashMap<ListAsKey, List<ComponentPort>>> connections = new HashMap<Component, HashMap<ListAsKey, List<ComponentPort>>>();

    HashMap<Component, ComponentNodeParent> componentCache = new HashMap<Component, ComponentNodeParent>();

    ModelGraphModel mgm;

    void packetSent(Transmitter t, PacketList packets) {

        List<ComponentPort> dst = fillConnections(t.sender, t.source);
        ComponentPort cp = dst.find {it.component == t.receiver && it.packetList.is(t.target)};

        if (cp == null) {
            cp = new ComponentPort(component: t.receiver, packetList: t.target);
            dst.add(cp);
        }
        for (Packet p: packets) {
            cp.ids.add(p.id);
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
                ComponentNodeParent src = componentCache.get(c);
                for (ListAsKey p: ports.keySet()) {
                    List<ComponentPort> destinations = new ArrayList<ComponentPort>();

                    String srcPort = WiringUtils.getSenderChannelName(c, p.packetList);
                    getDestinations(c, p.packetList, null, destinations);
                    if (destinations.find {it.component.name.contains("subWxl")} != null) {
                        int x = 0;
                    }
                    for (ComponentPort cp: destinations) {
                        ComponentNodeParent dst = componentCache.get(cp.component);
                        String dstPort = WiringUtils.getSenderChannelName(cp.component, cp.packetList);
                        if (src == null || dst == null)
                            continue;
                        if (src.parent != dst.parent) {
                            // If replicated connections should be added lazy (so only connections created which are used in a running model, no static wired)
                            /*if (src.componentNode instanceof ComposedComponentNode) {
                                if (((ComposedComponentNode) src.componentNode).componentGraph == dst.parent) {
                                    dst.parent.createConnection(((ComposedComponentGraphModel) dst.parent).getOuterInPorts().find
                                            {it.name.equals(srcPort)}, dst.componentNode.getPort(dstPort));
                                }
                            } else if (dst.componentNode instanceof ComposedComponentNode) {
                                if (((ComposedComponentNode) dst.componentNode).componentGraph == src.parent) {
                                    src.parent.createConnection(src.componentNode.getPort(srcPort),
                                            ((ComposedComponentGraphModel) src.parent).getOuterOutPorts().find {it.name.equals(dstPort)});
                                }
                            }
                            else*/
                            continue;
                        }
                        try {
                            src.parent.createConnection(src.componentNode.getPort(srcPort), dst.componentNode.getPort(dstPort));
                        }
                        catch (Exception e) {}
                    }
                }
            }
        }
    }

    private void getDestinations(Component sender, PacketList source, Set<UUID> ids, List<ComponentPort> destinations) {
        HashMap<ListAsKey, List<ComponentPort>> ports = connections.get(sender);
        List<ComponentPort> dst;
        dst = ports.get(new ListAsKey(source));

        if (dst == null) {
            return;
        }
        for (ComponentPort cp: dst) {
            if (ids == null || contained(ids, cp.ids)) {
                if (resolveComponent(cp.component)) {
                    getDestinations(cp.component, cp.packetList, ids == null ? cp.ids : ids, destinations);
                } else {
                    //check if any inner component is connected
                    /*if (ComposedComponent.isAssignableFrom(cp.component.class)) {
                        if (isComposedWired(cp.component, cp.packetList, ids == null ? cp.ids : ids)) {
                        destinations.add(cp);
                            }
                        } else
                        destinations.add(cp);*/
                    destinations.add(cp);
                }

            }
        }
    }

    //check if receiving composedcomponent needs to be wired (by checking all inner components)

    private boolean isComposedWired(ComposedComponent receiver, PacketList source, Set<UUID> ids) {
        List<ComponentPort> dst = new ArrayList<ComponentPort>();
        boolean ret = false;
        getDestinations(receiver, source, ids, dst);
        for (ComponentPort cp: dst) {
            if (ComposedComponent.isAssignableFrom(cp.component)) {
                ret = (ret | isComposedWired(cp.component, cp.packetList, ids));
            } else {
                return true;
            }
        }
        return ret;
    }

    private boolean contained(Set<UUID> a, Set<UUID> b) {
        for (UUID u: a) {
            for (UUID u2: b) {
                if (u.compareTo(u2) == 0) return true;
            }
        }
        return false;
    }


    private boolean resolveComponent(Component c) {
        if (DynamicComposedComponent.isAssignableFrom(c.class))
            return true;
        if (ComposedComponent.isAssignableFrom(c.class)) {
            for (Component sub: ((ComposedComponent) c).allSubComponents()) {
                if (DynamicComposedComponent.isAssignableFrom(sub.class)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void initComponentCache(Model m) {
        mgm = new ModelGraphModel(m.class.simpleName, m.class.package.name);
        for (Component c: m.allComponents) {
            addComponentToModel(c, mgm)
        }
        setUniqueNames(mgm);
    }

    private String setUniqueNames(AbstractGraphModel g) {

        HashMap<String, List<ComponentNode>> uniqueNames = new HashMap<String, List<ComponentNode>>();
        for (ComponentNode c: g.allComponentNodes) {
            ArrayList<ComponentNode> n
            if ((n = uniqueNames.get(c.name)) == null) {
                n = new ArrayList<ComponentNode>();
                uniqueNames.put(c.name, n);
            }
            n.add(c);
        }
        for (Map.Entry<String, List<ComponentNode>> entry: uniqueNames) {
            if (entry.value.size() > 1) {
                HashMap<String, Integer> u = new HashMap<String, Integer>();
                for (ComponentNode c: entry.value) {
                    String cat = PaletteService.getInstance().getCategoriesFromDefinition(c.type)[0];
                    u.put(cat, u.get(cat) == null ? 0 : u.get(cat) + 1);
                    c.name = c.name + cat + u.get(cat);

                }
            }
        }
    }

    private ComponentNode createCCNode(ComposedComponent cc, AbstractGraphModel model) {

        ComposedComponentNode cn = model.createComponentNode(PaletteService.getInstance().getComponentDefinition(cc.class), cc.name);
        ComposedComponentGraphModel graph = new ComposedComponentGraphModel(cc.getClass().getSimpleName(), cc.getClass().getPackage().name);
        for (Component c: getSubComponents(cc)) {
            addComponentToModel(c, graph);
        }
        addOuterPorts(graph, cc);

        //add replicated connections based on static inforumation (not on running model)
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

        //add replicated connections based on static inforumation (not on running model)
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

        cn.componentGraph = graph;
        return cn;
    }


    private Set<Component> getSubComponents(ComposedComponent cc) {
        Set<Component> cset = new HashSet<Component>();
        for (Component c: cc.allSubComponents()) {
            cset.add(c);
        }
        for (Component c: cc.getListedComponents()) {
            cset.add(c);
        }
        for (prop in cc.properties) {
            if (prop.value instanceof Component) {
                cset.add(prop.value);
            }
        }
        return cset;
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

            for (Component sub: getSubComponents(c))
                addComponentToModel(sub, m);
        } else {
            ComponentNode cn;
            if (ComposedComponent.isAssignableFrom(c.class)) {
                cn = createCCNode(c, m);
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
        return 0;
    }
}
class ComponentPort {
    Component component;
    PacketList packetList;
    Set<UUID> ids = new HashSet<UUID>();
}

class ComponentNodeParent {
    ComponentNode componentNode;
    AbstractGraphModel parent;
}
