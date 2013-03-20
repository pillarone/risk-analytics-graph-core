package org.pillarone.riskanalytics.graph.core.graph.model

import org.pillarone.riskanalytics.graph.core.graph.util.UIUtils

/**
 */
class GraphElement {
    String name

    String getDisplayName() {
        return UIUtils.formatDisplayName(name)
    }
}
