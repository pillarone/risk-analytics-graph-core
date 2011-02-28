package org.pillarone.riskanalytics.graph.core.graphexport

import com.sun.codemodel.JCodeModel
import com.sun.codemodel.JDefinedClass
import com.sun.codemodel.JMethod
import com.sun.codemodel.JMod
import com.sun.codemodel.JBlock
import com.sun.codemodel.writer.SingleStreamCodeWriter

import org.pillarone.riskanalytics.core.model.StochasticModel
import org.pillarone.riskanalytics.graph.core.graph.model.AbstractGraphModel
import org.pillarone.riskanalytics.graph.core.graph.model.ModelGraphModel
import org.pillarone.riskanalytics.graph.core.graph.model.ComponentNode

public class ModelGraphExport extends AbstractGraphExport {

    @Override
    public String exportGraph(AbstractGraphModel graph) {

        JCodeModel codeModel = new JCodeModel();
        JDefinedClass modelClass = codeModel._class(graph.packageName+"."+graph.name)._extends(StochasticModel.class);
        initFields(graph, codeModel, modelClass);
        addStartComponents(graph, modelClass);
        wireModel(graph, modelClass);
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        codeModel.build(new SingleStreamCodeWriter(out));
        return removeHeader(out.toString());
    }

    private void addStartComponents(ModelGraphModel graph, JDefinedClass modelClass) {
        JMethod addStart = modelClass.method(JMod.PUBLIC, Void.TYPE, "initComponents");
        JBlock block = addStart.body();
        for (ComponentNode n: graph.startComponents) {
            block.directStatement("addStartComponent " + n.getName());
        }
    }

    private void wireModel(ModelGraphModel graph, JDefinedClass modelClass) {
        JMethod wireComps = modelClass.method(JMod.PUBLIC, Void.TYPE, "wireComponents");
        JBlock block = wireComps.body();
        wireFields(graph, block);
    }
}
