package org.pillarone.riskanalytics.graph.core.palette.annotations

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

@Retention(RetentionPolicy.RUNTIME)
public @interface ComponentCategory {
    String [] categories();
}
