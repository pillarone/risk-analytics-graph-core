package org.pillarone.riskanalytics.graph.core.graphimport

import org.pillarone.riskanalytics.graph.core.graph.model.ComponentNode
import org.pillarone.riskanalytics.graph.core.graph.model.Connection

public interface GraphImportListener {

    public void nodeImported(ComponentNode node);
    public void connectionImported(Connection conenction);

}