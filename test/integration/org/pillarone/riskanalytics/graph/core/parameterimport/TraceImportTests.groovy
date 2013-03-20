package org.pillarone.riskanalytics.graph.core.parameterimport

import org.pillarone.riskanalytics.graph.core.graphimport.dynamic.DynamicModelGraphImport
import org.pillarone.riskanalytics.graph.core.graph.model.ModelGraphModel
import org.pillarone.riskanalytics.graph.core.graph.model.ComposedComponentNode

class TraceImportTests extends GroovyTestCase {

    String params = """
package models.core

model=org.pillarone.riskanalytics.core.example.model.TracingTestModel
applicationVersion='1.4-ALPHA-1.3-kti'
components {
	dynamicCC {
		subSubcomponent {
			parmValue[0]=new Double(1)
		}
	}
}

    """

    void testTraceImport() {
        ModelGraphModel mgm = new DynamicModelGraphImport().importFromTracing(params);
        println(mgm);
        assertEquals 4, mgm.allComponentNodes.size();
        assertTrue mgm.allConnections.size()>=2;
        assertTrue mgm.allComponentNodes.find {it instanceof ComposedComponentNode}.componentGraph.allConnections.size()>=2

    }
}
