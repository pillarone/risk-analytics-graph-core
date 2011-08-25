package org.pillarone.riskanalytics.graph.core.palette

import org.pillarone.riskanalytics.graph.core.palette.service.PaletteService
import org.pillarone.riskanalytics.graph.core.palette.model.ComponentDefinition
import org.pillarone.riskanalytics.core.example.component.TestComponent
import org.pillarone.riskanalytics.core.example.component.TestComposedComponent

class PalettePersistenceTests extends GroovyTestCase {

    void testStoreLoad() {
        PaletteService ps = PaletteService.getInstance();
        ps.addToCategory("p1", new ComponentDefinition(typeClass: TestComponent.class));
        ps.addToCategory("p1", new ComponentDefinition(typeClass: TestComposedComponent.class));
        ps.addToCategory("p2", new ComponentDefinition(typeClass: TestComponent.class));
        ps.storeUserCategory("p1", 1);
        ps.storeUserCategory("p2", 1);
        ps.reset();
        ps.loadUserCategories(1);
        assertEquals 2, ps.getDefinitionsFromCategory("p1").findAll {it.typeClass == TestComponent.class || it.typeClass == TestComposedComponent.class}.size()
        assertEquals 1, ps.getDefinitionsFromCategory("p2").findAll {it.typeClass == TestComponent.class}.size();

    }
}
