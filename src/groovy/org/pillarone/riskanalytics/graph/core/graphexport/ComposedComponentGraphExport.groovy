package org.pillarone.riskanalytics.graph.core.graphexport

import com.sun.codemodel.JCodeModel
import com.sun.codemodel.JDefinedClass

import com.sun.codemodel.JFieldVar
import com.sun.codemodel.JClass

import com.sun.codemodel.JMod
import com.sun.codemodel.JExpr
import com.sun.codemodel.JMethod
import com.sun.codemodel.JBlock
import com.sun.codemodel.writer.SingleStreamCodeWriter
import org.pillarone.riskanalytics.core.components.ComposedComponent
import org.pillarone.riskanalytics.core.packets.PacketList
import org.pillarone.riskanalytics.graph.core.graph.model.AbstractGraphModel
import org.pillarone.riskanalytics.graph.core.graph.model.ComposedComponentGraphModel
import org.pillarone.riskanalytics.graph.core.graph.model.Port
import org.pillarone.riskanalytics.graph.core.graph.model.Connection
import org.pillarone.riskanalytics.graph.core.graph.model.ComponentNode

public class ComposedComponentGraphExport extends AbstractGraphExport {

    private HashMap<String, JFieldVar> outerPorts = new HashMap<String, JFieldVar>();
    private static String WIREUTILS = "org.pillarone.riskanalytics.core.wiring.WiringUtils";
    private static String WIRECAT = "org.pillarone.riskanalytics.core.wiring.WireCategory";
    private static String PORTREPCAT = "org.pillarone.riskanalytics.core.wiring.PortReplicatorCategory";

    public String exportGraph(AbstractGraphModel graph) {
        JCodeModel codeModel = new JCodeModel();
        JDefinedClass ccClass = codeModel._class(graph.packageName + "." + graph.name)._extends(ComposedComponent.class);
        initFields(graph, codeModel, ccClass);
        wireCC(graph, ccClass);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        codeModel.build(new SingleStreamCodeWriter(out));
        return removeHeader(out.toString());
    }

    @Override
    protected void initFields(AbstractGraphModel graph, JCodeModel codeModel, JDefinedClass graphClass) {
        initOuterPorts(graph, codeModel, graphClass);
        super.initFields(graph, codeModel, graphClass)
        for (ComponentNode c: fields.keySet()) {
            JFieldVar field= fields.get(c);
            field.init(JExpr._new(field.type()));
        }
    }



    private void initOuterPorts(ComposedComponentGraphModel graph, JCodeModel codeModel, JDefinedClass ccClass) {
        for (Port p: (graph.getOuterInPorts() + graph.getOuterOutPorts())) {
            JClass rawClass = codeModel.ref(PacketList.class);
            JClass detailClass = codeModel.ref(p.packetType);
            JClass packetList = rawClass.narrow(detailClass);
            JFieldVar field = ccClass.field(JMod.NONE, packetList, p.getName());
            outerPorts.put(p.getName(), field);
            field.init(JExpr._new(rawClass).arg(JExpr.dotclass(detailClass)));
        }
    }

    private void wireCC(ComposedComponentGraphModel graph, JDefinedClass ccClass) {
        JMethod wireComps = ccClass.method(com.sun.codemodel.JMod.PUBLIC, Void.TYPE, "wire");
        JBlock block = wireComps.body();
        block.directStatement(WIREUTILS + ".use(" + PORTREPCAT + ") {");

        for (Connection c: graph.getAllConnections()) {
            if (c.from.composedComponentOuterPort) {
                JFieldVar fromField = outerPorts.get(c.from.name);
                JFieldVar toField;
                if (c.to.componentNode != null && (toField = fields.get(c.to.componentNode)) != null) {
                    block.add(toField.ref(c.to.name).assign(JExpr._this().ref(fromField)));
                    CommentCreator.setReplicationComment(c,block,false);
                }
            } else if (c.to.composedComponentOuterPort) {
                JFieldVar toField = outerPorts.get(c.to.name);
                JFieldVar fromField;
                if (c.from.componentNode != null && (fromField = fields.get(c.from.componentNode)) != null) {
                    block.add(JExpr._this().ref(toField).assign(fromField.ref(c.from.name)));
                    CommentCreator.setReplicationComment(c,block,true);
                }
            }
        }
        block.directStatement("}")
        block.directStatement(WIREUTILS + ".use(" + WIRECAT + ") {");
        wireFields(graph, block);
        block.directStatement("}");

    }


}
