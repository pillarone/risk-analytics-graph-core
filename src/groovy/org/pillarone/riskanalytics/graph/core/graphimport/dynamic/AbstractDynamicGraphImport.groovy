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
    protected List<DynamicComposedComponent> dynamicWiringResolved = new ArrayList<DynamicComposedComponent>();


    protected Connection wireComponents(Object componentContainer, Component src, Component dst, Transmitter transmitter) {
        addComponentNode(componentContainer, src);
        addComponentNode(componentContainer, dst);
        wireConcreteComponents(src, dst, transmitter.source, transmitter.target);
    }

    protected Connection wireComponents(Object componentContainer, DynamicComposedComponent src, Component dst, Transmitter transmitter) {
        addComponentNode(componentContainer, dst);
        addDynamicSubComponents(src);
        for (ComponentPortTuple tuple: getReplicatedOutputs(src, transmitter.source)) {
            addComponentNode(src, tuple.component);
            wireConcreteComponents(tuple.component, dst, tuple.port, transmitter.target)
        }
    }

    protected Connection wireComponents(Object componentContainer, Component src, DynamicComposedComponent dst, Transmitter transmitter) {
        addComponentNode(componentContainer, src);
        addDynamicSubComponents(dst);
        for (ComponentPortTuple tuple: getReplicatedInputs(dst, transmitter.target)) {
            addComponentNode(dst, tuple.component);
            wireConcreteComponents(src, tuple.component, transmitter.source, tuple.port)
        }
    }

    protected Connection wireComponents(Object componentContainer, DynamicComposedComponent src, DynamicComposedComponent dst, Transmitter transmitter) {
        addDynamicSubComponents(src);
        addDynamicSubComponents(dst);
        List<ComponentPortTuple> outTuples = getReplicatedOutputs(src, transmitter.source);
        List<ComponentPortTuple> inTuples = getReplicatedInputs(dst, transmitter.target);
        for (ComponentPortTuple outTuple: outTuples) {
            for (ComponentPortTuple inTuple: inTuples) {
                addComponentNode(src, outTuple.component);
                addComponentNode(src, inTuple.component);
                new ParameterConstraints().isWired(outTuple.component, inTuple.component);
                wireConcreteComponents(outTuple.component, inTuple.component, outTuple.port, inTuple.port);
            }
        }
    }

    protected List<ComponentPortTuple> getReplicatedInputs(DynamicComposedComponent dst, PacketList dstPort) {
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

    protected List<ComponentPortTuple> getReplicatedOutputs(DynamicComposedComponent src, PacketList srcPort) {
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

    protected void addDynamicSubComponents(DynamicComposedComponent cc) {
        if (!dynamicWiringResolved.contains(cc)) {
            dynamicWiringResolved.add(cc);
            for (Component c: cc.allSubComponents()) {
                for (Transmitter t: c.allInputTransmitter) {
                    if (t.sender != cc) {
                        wireComponents(cc, t.sender, t.receiver, t);
                    }
                }
                for (Transmitter t: c.allOutputTransmitter) {
                    if (t.receiver != cc) {
                        wireComponents(cc, t.sender, t.receiver, t);
                    }
                }
            }
        }
    }

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
