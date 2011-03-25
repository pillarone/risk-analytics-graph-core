package org.pillarone.riskanalytics.graph.core.loader

class ClassRepository {

    String name
    byte[] data

    String toString() {
        name
    }

    static constraints = {
        data(maxSize: 20000)
    }
}
