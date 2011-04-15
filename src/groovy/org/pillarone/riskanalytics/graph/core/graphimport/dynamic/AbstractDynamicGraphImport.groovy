package org.pillarone.riskanalytics.graph.core.graphimport.dynamic

import org.pillarone.riskanalytics.graph.core.graph.model.AbstractGraphModel
import org.pillarone.riskanalytics.graph.core.graph.model.ComponentNode
import org.pillarone.riskanalytics.core.components.Component
import org.pillarone.riskanalytics.core.wiring.Transmitter
import org.pillarone.riskanalytics.core.components.DynamicComposedComponent
import org.pillarone.riskanalytics.graph.core.graph.model.ComposedComponentNode
import org.pillarone.riskanalytics.core.components.ComposedComponent
import org.pillarone.riskanalytics.graph.core.palette.service.PaletteService
import org.pillarone.riskanalytics.graph.core.graph.model.ComposedComponentGraphModel
import org.pillarone.riskanalytics.graph.core.graph.model.InPort
import org.pillarone.riskanalytics.graph.core.graph.model.OutPort
import org.pillarone.riskanalytics.graph.core.graph.model.Port
import org.pillarone.riskanalytics.core.packets.PacketList
import org.pillarone.riskanalytics.core.wiring.WiringUtils
import org.pillarone.riskanalytics.graph.core.graph.model.Connection
import org.pillarone.riskanalytics.core.parameterization.TableMultiDimensionalParameter
import org.pillarone.riskanalytics.core.parameterization.ComboBoxTableMultiDimensionalParameter
import org.pillarone.riskanalytics.core.parameterization.ConstrainedMultiDimensionalParameter

public abstract class AbstractDynamicGraphImport {

    protected AbstractGraphModel graph;
    protected Map<Component, ComponentNode> components = new HashMap<Component, ComponentNode>();
    protected List<DynamicComposedComponent> subWiringResolved = new ArrayList<DynamicComposedComponent>();


    protected void wireComponents(Object parentSrc, Object parentDst, Transmitter transmitter) {
        boolean senderResolved = resolveInterior(transmitter.sender);
        boolean receiverResolved = resolveInterior(transmitter.receiver);
        if (senderResolved && receiverResolved) {
            addSubComponents(transmitter.sender)
            addSubComponents(transmitter.receiver)
            List<ComponentPortTuple> outTuples = getReplicatedOutputs(transmitter.sender, transmitter.source);
            List<ComponentPortTuple> inTuples = getReplicatedInputs(transmitter.receiver, transmitter.target);
            for (ComponentPortTuple outTuple: outTuples) {
                for (ComponentPortTuple inTuple: inTuples) {
                    wireComponents(transmitter.sender, transmitter.receiver, new Transmitter(outTuple.component, outTuple.port, inTuple.component, inTuple.port));
                }
            }

        } else if (senderResolved && !receiverResolved) {
            addSubComponents(transmitter.sender)
            for (ComponentPortTuple tuple: getReplicatedOutputs(transmitter.sender, transmitter.source)) {
                wireComponents(transmitter.sender, parentDst, new Transmitter(tuple.component, tuple.port, transmitter.receiver, transmitter.target))
            }

        } else if (!senderResolved && receiverResolved) {
            addSubComponents(transmitter.receiver)
            for (ComponentPortTuple tuple: getReplicatedInputs(transmitter.receiver, transmitter.target)) {
                wireComponents(parentSrc, transmitter.receiver, new Transmitter(transmitter.sender, transmitter.source, tuple.component, tuple.port))
            }
        } else {
            addComponentNode(parentSrc, transmitter.sender);
            addComponentNode(parentDst, transmitter.receiver);
            if (ComposedComponent.isAssignableFrom(parentDst.class))
                new ParameterConstraints().isWired(transmitter.sender,transmitter.receiver);
            wireConcreteComponents(transmitter.sender, transmitter.receiver, transmitter.source, transmitter.target);
        }
    }

    protected boolean resolveInterior(Component c) {
        return ComposedComponent.isAssignableFrom(c.class);
    }

    protected List<ComponentPortTuple> getReplicatedInputs(ComposedComponent dst, PacketList dstPort) {
        List<ComponentPortTuple> components = new ArrayList<ComponentPortTuple>();
        String receivingName = WiringUtils.getSenderChannelName(dst, dstPort);
        for (Transmitter t: dst.allInputReplicationTransmitter) {
            String sendingName = WiringUtils.getSenderChannelName(t.sender, t.source);
            if (receivingName.equals(sendingName)) {
                components.add(new ComponentPortTuple(component: t.receiver, port: t.target));
            }
        }
        return components;
    }

    protected List<ComponentPortTuple> getReplicatedOutputs(ComposedComponent src, PacketList srcPort) {
        List<ComponentPortTuple> components = new ArrayList<ComponentPortTuple>();
        String sendingName = WiringUtils.getSenderChannelName(src, srcPort);
        for (Transmitter t: src.allOutputReplicationTransmitter) {
            String receivingName = WiringUtils.getSenderChannelName(t.receiver, t.target);
            if (sendingName.equals(receivingName)) {
                components.add(new ComponentPortTuple(component: t.sender, port: t.source));
            }
        }
        return components;
    }

    protected void addSubComponents(ComposedComponent cc) {
        if (!subWiringResolved.contains(cc)) {
            subWiringResolved.add(cc);
            for (Component c: cc.allSubComponents()) {
                for (Transmitter t: c.allInputTransmitter) {
                    if (t.sender != cc) {
                        wireComponents(cc, cc, t);
                    }
                }
                for (Transmitter t: c.allOutputTransmitter) {
                    if (t.receiver != cc) {
                        wireComponents(cc, cc, t);
                    }
                }
            }
        }
    }

    /*protected boolean isComposedWired(Component src, ComposedComponent dst, PacketList dstPort) {
        boolean isWired = false;
        for (ComponentPortTuple cpt: getReplicatedInputs(dst, dstPort)) {
            if (ComposedComponent.isAssignableFrom(cpt.component.class)) {
                isWired = isWired | isComposedWired(src, cpt.component, cpt.port);
            } else {
                isWired = isWired | new ParameterConstraints().isWired(src, cpt.component);
            }
        }
        return isWired;
    }*/

    protected ComponentNode addComponentNode(Object componentContainer, Component c) {
        ComponentNode componentNode;

        if ((componentNode = components.get(c)) != null)
            return componentNode;

        Map.Entry entry = componentContainer.properties.find {Map.Entry entry -> entry.value.is(c)};
        String name = c.class.simpleName + "_" + c.name;
        if (entry != null)
            name = entry.key;

        if (ComposedComponent.isAssignableFrom(c.class)) {
            componentNode = createComposedComponentNode(c, name);
            graph.addComponentNode(componentNode);
        }
        else {
            componentNode = graph.createComponentNode(PaletteService.getInstance().getComponentDefinition(c.class), name);
        }
        components.put(c, componentNode);
        return componentNode;

    }

    protected ComposedComponentNode createComposedComponentNode(ComposedComponent cc, String name) {
        ComposedComponentNode cn = new ComposedComponentNode(name: name, type: PaletteService.getInstance().getComponentDefinition(cc.class));
        ComposedComponentGraphModel graph = new DynamicCCGraphImport().createFromWired(cc);

        //setting ports of componentNode as outerport of composedcomponentgraph
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

    protected Connection wireConcreteComponents(Component src, Component dst, PacketList srcPort, PacketList dstPort) {
        String toPort = WiringUtils.getSenderChannelName(dst, dstPort);
        String fromPort = WiringUtils.getSenderChannelName(src, srcPort);

        ComponentNode toComp = components.get(dst);
        ComponentNode fromComp = components.get(src);

        Port fromP = fromComp.outPorts.find {it.name.equals(fromPort)};
        Port toP = toComp.inPorts.find {it.name.equals(toPort)};

        Connection connection;
        try {
            connection = graph.createConnection(fromP, toP);
        } catch (Exception e) {}
        return connection;

    }
}

class ComponentPortTuple {
    Component component;
    PacketList port;
}

class ParameterConstraints {

    public boolean isWired(Component src, Component dst) {
        List<TableMultiDimensionalParameter> inParams = new ArrayList<TableMultiDimensionalParameter>();
        List<TableMultiDimensionalParameter> outParams = new ArrayList<TableMultiDimensionalParameter>();
        getMarkerParameter(dst, inParams);
        getMarkerParameter(src, outParams);

        boolean wired = true;
        if (inParams.size() > 0) {
            TableMultiDimensionalParameter inParam;
            for (TableMultiDimensionalParameter tmp: inParams) {
                if (getMarker(tmp).isAssignableFrom(src.class)) {
                    inParam = tmp;
                    break;
                }
            }
            if (inParam != null) {
                List values = getValues(inParam);
                for (Object o: values) {
                    if (o == src) {
                        return true;
                    }
                }
                wired = false;
            }
            for (TableMultiDimensionalParameter tmp: inParams) {

                for (TableMultiDimensionalParameter tmpout: outParams) {
                    if (getMarker(tmpout) != null && getMarker(tmp) == getMarker(tmpout)) {
                        List values = getValues(tmp);
                        for (Object o: values) {
                            if (getValues(tmpout).contains(o)) {
                                return true;
                            }
                        }
                        wired = false;
                    }
                }

            }
        }
        return wired;
    }

    private void getMarkerParameter(Object o, List<TableMultiDimensionalParameter> results) {
        if (TableMultiDimensionalParameter.isAssignableFrom(o.class)) {
            results.add(o);
            return;
        }
        for (Map.Entry entry: o.properties) {
            if (entry.value == null) continue;
            if (((String) entry.key).startsWith("parm") || (entry.value != null && entry.value.class != null &&
                    TableMultiDimensionalParameter.isAssignableFrom(entry.value.class))) {
                getMarkerParameter(entry.value, results)
            } /*else {
                if (ComposedComponent.isAssignableFrom(o.class) && !DynamicComposedComponent.isAssignableFrom(o.class)) {
                    for (Component n: ((ComposedComponent) o).allSubComponents()) {
                        getMarkerParameter(n, results)
                    }
                }
            }*/
        }
    }

    private Class getMarker(ComboBoxTableMultiDimensionalParameter cmd) {
        return cmd.getMarkerClass();
    }

    private Class getMarker(ConstrainedMultiDimensionalParameter cmd) {
        return cmd.constraints.getColumnType(0);
    }

    private Class getMarker(TableMultiDimensionalParameter cmd) {
        return null;
    }

    private List getValues(ComboBoxTableMultiDimensionalParameter cmd) {
        return cmd.getValuesAsObjects()[0];
    }

    private List getValues(ConstrainedMultiDimensionalParameter cmd) {
        return cmd.getValuesAsObjects(0);
    }
}
