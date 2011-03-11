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
import org.pillarone.riskanalytics.graph.core.graph.model.InPort
import org.pillarone.riskanalytics.graph.core.graph.util.IntegerRange
import com.sun.codemodel.JAnnotationUse
import org.pillarone.riskanalytics.graph.core.palette.annotations.WiringValidation
import com.sun.codemodel.JAnnotationArrayMember
import org.pillarone.riskanalytics.graph.core.graph.util.WiringValidationUtil
import java.util.regex.Matcher
import java.util.regex.Pattern

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
        return replaceAnnotationListBrackets(removeHeader(out.toString()));
    }

    @Override
    protected void initFields(AbstractGraphModel graph, JCodeModel codeModel, JDefinedClass graphClass) {
        initOuterPorts(graph, codeModel, graphClass);
        super.initFields(graph, codeModel, graphClass)
        for (ComponentNode c: fields.keySet()) {
            JFieldVar field = fields.get(c);
            field.init(JExpr._new(field.type()));
        }
    }



    private void initOuterPorts(ComposedComponentGraphModel graph, JCodeModel codeModel, JDefinedClass ccClass) {
        for (Port p: (graph.getOuterInPorts() + graph.getOuterOutPorts())) {
            JClass rawClass = codeModel.ref(PacketList.class);
            JClass detailClass = codeModel.ref(p.packetType);
            JClass packetList = rawClass.narrow(detailClass);
            JFieldVar field = ccClass.field(JMod.NONE, packetList, p.getName());
            if (p instanceof InPort)
                setWiringAnnotation(graph, field, p);
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
                    CommentCreator.setReplicationComment(c, block, false);
                    block.add(toField.ref(c.to.name).assign(JExpr._this().ref(fromField)));
                }
            } else if (c.to.composedComponentOuterPort) {
                JFieldVar toField = outerPorts.get(c.to.name);
                JFieldVar fromField;
                if (c.from.componentNode != null && (fromField = fields.get(c.from.componentNode)) != null) {
                    CommentCreator.setReplicationComment(c, block, true);
                    block.add(JExpr._this().ref(toField).assign(fromField.ref(c.from.name)));
                }
            }
        }
        block.directStatement("}")
        block.directStatement(WIREUTILS + ".use(" + WIRECAT + ") {");
        wireFields(graph, block);
        block.directStatement("}");

    }

    private void setWiringAnnotation(ComposedComponentGraphModel graph, JFieldVar field, InPort port) {
        List<Connection> replicateDst = graph.getAllConnections().findAll {it.from.equals(port)};

        if (!replicateDst.empty) {
            List<IntegerRange> connectionRange = new ArrayList<IntegerRange>();
            List<IntegerRange> packetRange = new ArrayList<IntegerRange>();
            for (Connection c: replicateDst) {
                InPort p = c.to;
                if (p.connectionCardinality != null)
                    connectionRange.add(p.connectionCardinality);
                if (p.packetCardinality != null) {
                    packetRange.add(p.packetCardinality);
                }
            }
            IntegerRange cRange = WiringValidationUtil.getEnclosingRange(connectionRange);
            IntegerRange pRange = WiringValidationUtil.getEnclosingRange(packetRange);

            if (cRange == null && pRange == null)
                return;

            JAnnotationUse wiringAt = field.annotate(WiringValidation.class);


            def ranges = [connections: cRange, packets: pRange]

            for (String key: ranges.keySet()) {
                if (ranges[key] != null) {
                    JAnnotationArrayMember cardinality = wiringAt.paramArray(key);
                    cardinality.param(ranges[key].from);
                    cardinality.param(ranges[key].to);
                }
            }


        }

    }

    private String replaceAnnotationListBrackets(String content) {
        Pattern p = Pattern.compile('\\= \\{(.+?)\\}', Pattern.DOTALL);
        Matcher mc = p.matcher(content);

        String output = mc.replaceAll('=[$1]');
        mc = p.matcher(output);

        return mc.replaceAll('=[$1]');
    }


}
