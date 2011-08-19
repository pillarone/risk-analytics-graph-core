package org.pillarone.riskanalytics.graph.core.loader

class ClassRepository {

    String name
    ClassType classType
    byte[] data

    String toString() {
        name
    }

    static constraints = {
        name(unique: true)
        data(maxSize: 20000)
    }
}
