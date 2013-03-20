package org.pillarone.riskanalytics.graph.core.graph.util

public class IntegerRange {

    int from, to;

    public boolean inRange(int n) {
        return (n >= from && n <= to);
    }
}
