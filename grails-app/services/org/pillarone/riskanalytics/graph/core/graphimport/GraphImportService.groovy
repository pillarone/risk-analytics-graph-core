package org.pillarone.riskanalytics.graph.core.graphimport

import org.pillarone.riskanalytics.graph.core.graph.model.AbstractGraphModel
import org.pillarone.riskanalytics.core.model.Model
import org.pillarone.riskanalytics.core.components.ComposedComponent

class GraphImportService {

    AbstractGraphModel importGraph(String content) {
        try {
            GroovyClassLoader gcl = new GroovyClassLoader(this.getClass().getClassLoader());
            Class clazz = gcl.parseClass(content);

            if (Model.isAssignableFrom(clazz)) {
                return new ModelGraphImport().importGraph(clazz,content);
            }
            if (ComposedComponent.isAssignableFrom(clazz)) {
                return new ComposedComponentGraphImport().importGraph(clazz,content);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
