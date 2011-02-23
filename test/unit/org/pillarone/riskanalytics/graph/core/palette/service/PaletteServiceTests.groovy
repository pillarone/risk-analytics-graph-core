package org.pillarone.riskanalytics.graph.core.palette.service

import org.pillarone.riskanalytics.graph.core.palette.model.ComponentDefinition
import org.pillarone.riskanalytics.core.example.component.TestComponent


class PaletteServiceTests extends GroovyTestCase {

    void testGetComponents() {
        PaletteService service = PaletteService.getInstance()
        List<ComponentDefinition> definitions = service.getAllComponentDefinitions()

        assertTrue definitions.size() > 0

        Class clazz = TestComponent

        ComponentDefinition definition = service.getComponentDefinition(clazz)
        assertNotNull definition
        assertTrue definitions.contains(definition)

        ComponentDefinition definition2 = service.getComponentDefinition(clazz.name)
        assertNotNull definition2
        assertSame definition, definition2
        assertTrue definitions.contains(definition2)
    }
}
