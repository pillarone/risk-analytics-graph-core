package org.pillarone.riskanalytics.graph.core.graphexport

import com.sun.codemodel.JCodeModel
import com.sun.codemodel.JDefinedClass
import com.sun.codemodel.JBlock
import com.sun.codemodel.JClass
import com.sun.codemodel.JFieldVar
import com.sun.codemodel.JMod
import com.sun.codemodel.JExpr
import org.pillarone.riskanalytics.graph.core.graph.model.AbstractGraphModel
import org.pillarone.riskanalytics.graph.core.graph.model.ComponentNode
import org.pillarone.riskanalytics.graph.core.graph.model.Connection


public abstract class AbstractGraphExport {

    protected HashMap<String,JFieldVar> fields=new HashMap<String,JFieldVar>();

    public abstract String exportGraph (AbstractGraphModel graph);

    protected void initFields (AbstractGraphModel graph,JCodeModel codeModel,JDefinedClass graphClass){
        for (ComponentNode n:graph.allComponentNodes){
            JClass component=codeModel.ref(n.getType().typeClass);
            JFieldVar field=graphClass.field(JMod.NONE,component,n.getName());
            fields.put(n.getName(),field);
            field.init(JExpr._new(component));
        }
    }

    protected void wireFields (AbstractGraphModel graph, JBlock block){
        for (Connection c:graph.allConnections){
            ComponentNode fromComp=c.from.componentNode;
            String fromPortStr=c.from.name;
            ComponentNode toComp=c.to.componentNode;
            String toPortStr=c.to.name;

            if (c.from.composedComponentOuterPort||c.to.composedComponentOuterPort)
                continue;

            JFieldVar fromField=fields.get(fromComp.name);
            JFieldVar toField=fields.get(toComp.name);

            if (fromField==null || toField==null)
                continue;

            block.add(toField.ref(toPortStr).assign(fromField.ref(fromPortStr)));
        }
    }

    protected String removeHeader (String content){
        StringReader sr=new StringReader(content);
        BufferedReader br=new BufferedReader(sr);
        br.readLine();
        br.readLine();
        StringBuilder output=new StringBuilder();
        String s;
        while ((s=br.readLine())!=null){
            output.append(s+System.getProperty("line.separator"));
        }
        return output.toString();
    }
}
