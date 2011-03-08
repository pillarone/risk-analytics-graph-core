package org.pillarone.riskanalytics.graph.core.graphexport

import com.sun.codemodel.JCodeModel
import com.sun.codemodel.JDefinedClass
import com.sun.codemodel.JBlock
import com.sun.codemodel.JClass
import com.sun.codemodel.JFieldVar
import com.sun.codemodel.JMod

import org.pillarone.riskanalytics.graph.core.graph.model.AbstractGraphModel
import org.pillarone.riskanalytics.graph.core.graph.model.ComponentNode
import org.pillarone.riskanalytics.graph.core.graph.model.Connection
import com.sun.codemodel.JDocComment


public abstract class AbstractGraphExport {

    protected HashMap<String, JFieldVar> fields = new HashMap<ComponentNode, JFieldVar>();

    public abstract String exportGraph(AbstractGraphModel graph)

    ;

    protected void initFields(AbstractGraphModel graph, JCodeModel codeModel, JDefinedClass graphClass) {
        for (ComponentNode n: graph.allComponentNodes) {
            JClass component = codeModel.ref(n.getType().typeClass);
            JFieldVar field = graphClass.field(JMod.NONE, component, n.getName());
            CommentCreator.setComponentComment(n, field.javadoc());
            fields.put(n, field);
        }
    }

    protected void wireFields(AbstractGraphModel graph, JBlock block) {
        for (Connection c: graph.allConnections) {
            ComponentNode fromComp = c.from.componentNode;
            String fromPortStr = c.from.name;
            ComponentNode toComp = c.to.componentNode;
            String toPortStr = c.to.name;

            if (c.from.composedComponentOuterPort || c.to.composedComponentOuterPort)
                continue;

            JFieldVar fromField = fields.get(fromComp);
            JFieldVar toField = fields.get(toComp);

            if (fromField == null || toField == null)
                continue;
            CommentCreator.setConnectionComment(c, block);
            block.add(toField.ref(toPortStr).assign(fromField.ref(fromPortStr)));
        }
    }

    protected String removeHeader(String content) {
        StringReader sr = new StringReader(content);
        BufferedReader br = new BufferedReader(sr);
        br.readLine();
        br.readLine();
        StringBuilder output = new StringBuilder();
        String s;
        while ((s = br.readLine()) != null) {
            output.append(s + System.getProperty("line.separator"));
        }
        return output.toString();
    }
}

class CommentCreator {
    private static String LINESEP = System.getProperty("line.separator");

    public static setComponentComment(ComponentNode componentNode, JDocComment doc) {
        doc.append("Component:" + componentNode.name + LINESEP + "empty comment");
    }

    public static setConnectionComment(Connection connection, JBlock block) {
        block.directStatement("/**" + LINESEP);
        block.directStatement(" * Connection:" + connection.from.componentNode.name + "." + connection.from.name + "->"
                + connection.to.componentNode.name + "." + connection.to.name + LINESEP);
        block.directStatement(" * empty comment" + LINESEP);
        block.directStatement(" */");
    }

    public static setReplicationComment(Connection connection, JBlock block, boolean direction) {
        block.directStatement("/**" + LINESEP);
        if (direction) {
            block.directStatement(" * Replication:" + connection.from.componentNode.name + "." + connection.from.name + "->"
                    + connection.to.name + LINESEP);
        } else {
            block.directStatement(" * Replication:" + connection.from.name + "->"
                    + connection.to.componentNode.name + "." + connection.to.name + LINESEP);
        }
        block.directStatement(" * empty comment" + LINESEP);
        block.directStatement(" */");

    }
}
