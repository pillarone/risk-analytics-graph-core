package org.pillarone.riskanalytics.graph.core.loader


abstract class GroovyHelper {

    public static List<String> getAllClassNames() {
        ClassRepository.list()*.name
    }

    public static byte[] getClassDefinition(String name) {
        ClassRepository.findByName(name).data
    }
}
