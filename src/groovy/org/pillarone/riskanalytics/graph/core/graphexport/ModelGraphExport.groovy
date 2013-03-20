package org.pillarone.riskanalytics.graph.core.graphexport

import com.sun.codemodel.writer.SingleStreamCodeWriter
import org.pillarone.riskanalytics.core.model.StochasticModel
import org.pillarone.riskanalytics.graph.core.graph.model.AbstractGraphModel
import org.pillarone.riskanalytics.graph.core.graph.model.ComponentNode
import org.pillarone.riskanalytics.graph.core.graph.model.ModelGraphModel
import com.sun.codemodel.*

public class ModelGraphExport extends AbstractGraphExport {

    @Override
    public String exportGraph(AbstractGraphModel graph) {

        JCodeModel codeModel = new JCodeModel();
        JDefinedClass modelClass = codeModel._class(graph.packageName + "." + graph.name)._extends(StochasticModel.class);
        initFields(graph, codeModel, modelClass);
        wireModel(graph, modelClass);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        codeModel.build(new SingleStreamCodeWriter(out));
        return removeHeader(out.toString());
    }

    @Override
    protected void initFields(AbstractGraphModel graph, JCodeModel codeModel, JDefinedClass graphClass) {
        super.initFields(graph, codeModel, graphClass);
        initStartComponents(graph, codeModel, graphClass);
    }


    private void initStartComponents(ModelGraphModel graph, JCodeModel codeModel, JDefinedClass modelClass) {
        JMethod addStart = modelClass.method(JMod.PUBLIC, Void.TYPE, "initComponents");
        JBlock block = addStart.body();
        for (ComponentNode c: fields.keySet()) {
            JFieldVar field = fields.get(c);
            block.assign(field, JExpr._new(field.type()));
        }
        for (ComponentNode n: graph.resolveStartComponents()) {
            block.directStatement("addStartComponent " + n.getName());
        }
    }

    private void wireModel(ModelGraphModel graph, JDefinedClass modelClass) {
        JMethod wireComps = modelClass.method(JMod.PUBLIC, Void.TYPE, "wireComponents");
        JBlock block = wireComps.body();
        wireFields(graph, block);
    }


}
