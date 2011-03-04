package org.pillarone.riskanalytics.graph.core.graphexport

import org.pillarone.riskanalytics.graph.core.graph.model.AbstractGraphModel
import org.pillarone.riskanalytics.graph.core.graph.model.ModelGraphModel
import org.pillarone.riskanalytics.graph.core.graph.model.ComposedComponentGraphModel
//
class GraphExportService {

    String exportGraph(AbstractGraphModel graph) {
        return exportGraphIn(graph);
    }

    Class exportGraphToClass(AbstractGraphModel graph) {
        String content = exportGraphIn(graph);
        GroovyClassLoader gcl = new GroovyClassLoader(this.getClass().getClassLoader());
        return gcl.parseClass(content);
    }

    public Map<String, byte[]> exportGraphToBinary(AbstractGraphModel graph) {
        String content = exportGraph(graph);
        JBehaveGroovyClassLoader jgl = new JBehaveGroovyClassLoader();
        jgl.parseClass(content);
        return jgl.getClassBytes();
    }

    private String exportGraphIn(ModelGraphModel graph) {
        return new ModelGraphExport().exportGraph(graph);
    }

    private String exportGraphIn(ComposedComponentGraphModel graph) {
        return new ComposedComponentGraphExport().exportGraph(graph);
    }
}
