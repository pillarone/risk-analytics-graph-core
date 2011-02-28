package org.pillarone.riskanalytics.graph.core.graphexport

import org.pillarone.riskanalytics.graph.core.graph.model.AbstractGraphModel
import org.pillarone.riskanalytics.graph.core.graph.model.ModelGraphModel
import org.pillarone.riskanalytics.graph.core.graph.model.ComposedComponentGraphModel

class GraphExportService {

    String exportGraph(AbstractGraphModel graph) {
        return exportGraphIn(graph);
    }

    Class exportGraphToClass (AbstractGraphModel graph){
        String content=exportGraphIn(graph);
        GroovyClassLoader gcl = new GroovyClassLoader(this.getClass().getClassLoader());
        return gcl.parseClass(content);
    }

    private String exportGraphIn(ModelGraphModel graph) {
        return new ModelAbstractGraphExport().exportGraph(graph);
    }

    private String exportGraphIn(ComposedComponentGraphModel graph) {
        return new ComposedComponentAbstractGraphExport().exportGraph(graph);
    }
}
