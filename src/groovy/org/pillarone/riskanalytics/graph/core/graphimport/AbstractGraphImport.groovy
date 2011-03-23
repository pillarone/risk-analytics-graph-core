package org.pillarone.riskanalytics.graph.core.graphimport

import org.pillarone.riskanalytics.core.components.Component
import org.pillarone.riskanalytics.core.wiring.ITransmitter
import org.pillarone.riskanalytics.core.wiring.WiringUtils
import org.pillarone.riskanalytics.graph.core.graph.model.AbstractGraphModel
import org.pillarone.riskanalytics.graph.core.graph.model.ComponentNode
import org.pillarone.riskanalytics.graph.core.palette.service.PaletteService
import org.pillarone.riskanalytics.graph.core.graph.model.Port
import java.util.regex.Pattern
import java.util.regex.Matcher
import org.pillarone.riskanalytics.graph.core.graph.model.Connection
import org.pillarone.riskanalytics.core.components.ComposedComponent
import org.pillarone.riskanalytics.graph.core.graph.model.ComposedComponentNode
import org.pillarone.riskanalytics.graph.core.graph.model.ComposedComponentGraphModel
import org.pillarone.riskanalytics.graph.core.graph.model.InPort
import org.pillarone.riskanalytics.graph.core.graph.model.OutPort

public abstract class AbstractGraphImport {

    protected CommentImport commentImport;

    public abstract AbstractGraphModel importGraph(Class clazz, String comments)


    protected ComposedComponentNode createComposedComponentNode(ComposedComponent cc, String name) {
        ComposedComponentNode cn = new ComposedComponentNode(name: name, type: PaletteService.getInstance().getComponentDefinition(cc.class));
        ComposedComponentGraphModel graph = new ComposedComponentGraphImport().createFromWiredComponent(cc);

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

    protected HashMap<Component, ComponentNode> getComponents(List<Component> subComponents,Object componentContainer, AbstractGraphModel graph) {
        HashMap<Component, ComponentNode> components = new HashMap<Component, ComponentNode>();
        for (Component c: subComponents) {
            String name = componentContainer.properties.find {Map.Entry entry -> entry.value.is(c)}.key;
            ComponentNode n;
            if (ComposedComponent.isAssignableFrom(c.class)) {
                n = createComposedComponentNode(c, name);
                graph.addComponentNode(n);
            } else {
                n = graph.createComponentNode(PaletteService.getInstance().getComponentDefinition(c.class), name);
            }
            n.comment = commentImport.getComponentComment(n);
            components.put(c, n);
        }
        return components;
    }


    protected void addConnections(AbstractGraphModel graph, HashMap<Component, ComponentNode> components) {
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

                //if ComposedComponent port, no Component found
                if (toComp == null || fromComp == null) {
                    continue;
                }

                String key = fromComp.name + "." + fromPort + "=" + toComp.name + "." + toPort;

                Port fromP = fromComp.outPorts.find {it.name.equals(fromPort)};
                Port toP = toComp.inPorts.find {it.name.equals(toPort)};

                if (fromP != null && toP != null) {
                    if (!visited.get(key)) {
                        Connection connection = graph.createConnection(fromP, toP);
                        connection.comment = commentImport.getConnectionComment(connection);
                        visited.put(key, true);
                    }
                }

            }
        }
    }


}

class CommentImport {
    private HashMap<String, String> comments = new HashMap<String, String>();

    public CommentImport(String content) {
        if (content != null)
            structureComments(content);
    }

    public String getComponentComment(ComponentNode componentNode) {
        return comments.get("Component:" + componentNode.name);
    }

    public String getConnectionComment(Connection connection) {
        return comments.get("Connection:" + connection.from.componentNode.name + "." + connection.from.name +
                "->" + connection.to.componentNode.name + "." + connection.to.name);
    }

    public String getReplicationComment(Connection connection, boolean direction) {
        if (direction)
            return comments.get("Replication:" + connection.from.name +
                    "->" + connection.to.componentNode.name + "." + connection.to.name);
        else
            return comments.get("Replication:" + connection.from.componentNode.name + "." + connection.from.name +
                    "->" + connection.to.name);
    }

    private void structureComments(String content) {
        Pattern p = Pattern.compile('/\\*+(.*?)\\*+/', Pattern.DOTALL);
        Matcher m = p.matcher(content);
        while (m.find()) {
            String plain = m.group(1).replaceAll('\\*', '').trim();
            try {
                StringReader sr = new StringReader(plain);
                String header = sr.readLine().trim();
                String comment = sr.readLine().trim();
                comments.put(header, comment);
            } catch (Exception e) {
                continue;
            }
        }
    }
}
