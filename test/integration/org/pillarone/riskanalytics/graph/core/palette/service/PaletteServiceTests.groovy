package org.pillarone.riskanalytics.graph.core.palette.service

import org.pillarone.riskanalytics.core.example.component.TestComponent
import org.pillarone.riskanalytics.graph.core.palette.model.ComponentDefinition
import org.pillarone.riskanalytics.graph.core.palette.service.filter.CategoryFilter
import org.pillarone.riskanalytics.graph.core.palette.service.filter.NoFilter

class PaletteServiceTests extends GroovyTestCase {
    String ccFile = """
package model;
import org.pillarone.riskanalytics.core.components.ComposedComponent;
import org.pillarone.riskanalytics.core.example.component.ExampleInputOutputComponent;
import org.pillarone.riskanalytics.core.packets.Packet;
import org.pillarone.riskanalytics.core.packets.PacketList;
import org.pillarone.riskanalytics.core.components.ComponentCategory;

@ComponentCategory(categories=["CAT1","CAT2"])
public class TestCC
    extends ComposedComponent
{
    PacketList<Packet> inEventSeverities = new PacketList(Packet.class);
    PacketList<Packet> outClaims = new PacketList(Packet.class);

    ExampleInputOutputComponent subClaimsGenerator = new ExampleInputOutputComponent();
    ExampleInputOutputComponent subSeverityExtractor = new ExampleInputOutputComponent();
    /**
     * Component:subSeverityExtractor2
     * empty
     */
    ExampleInputOutputComponent subSeverityExtractor2 = new ExampleInputOutputComponent();
    public void wire() {
        org.pillarone.riskanalytics.core.wiring.WiringUtils.use(org.pillarone.riskanalytics.core.wiring.PortReplicatorCategory) {
        /**
         * Replication:subClaimsGenerator.outValue->outClaims
         * empty
         */
        this.outClaims = subClaimsGenerator.outValue;
        subSeverityExtractor.inValue = this.inEventSeverities;
        subSeverityExtractor2 .inValue = this.inEventSeverities;
        }
        org.pillarone.riskanalytics.core.wiring.WiringUtils.use(org.pillarone.riskanalytics.core.wiring.WireCategory) {
        /**
         * Connection:subSeverityExtractor.outValue->subClaimsGenerator.inValue
         * empty
         */
        subClaimsGenerator.inValue = subSeverityExtractor.outValue;
        }
    }
}        """

    @Override
    protected void setUp() {
        PaletteService.instance.reset()
    }

    void testFilter() {
        PaletteService service = PaletteService.getInstance()
        assertTrue(service.paletteFilter instanceof CategoryFilter) //test if filters are taken from Config
        List<ComponentDefinition> definitions = service.getAllComponentDefinitions()

        assertTrue definitions.size() > 0

        service.paletteFilter = new CategoryFilter("myCategory")
        service.reset()

        definitions = service.getAllComponentDefinitions()

        assertTrue definitions.empty

        service.paletteFilter = new NoFilter()
        service.reset()

        definitions = service.getAllComponentDefinitions()

        assertTrue definitions.size() > 0
    }

    void testGetComponents() {
        PaletteService service = PaletteService.getInstance()
        List<ComponentDefinition> definitions = service.getAllComponentDefinitions()

        assertTrue definitions.size() > 0
        assertTrue service.getDefinitionsFromCategory(PaletteService.CAT_OTHER).size() > 0

        Class clazz = TestComponent

        ComponentDefinition definition = service.getComponentDefinition(clazz)
        assertNotNull definition
        assertTrue definitions.contains(definition)

        ComponentDefinition definition2 = service.getComponentDefinition(clazz.name)
        assertNotNull definition2
        assertSame definition, definition2
        assertTrue definitions.contains(definition2)
    }

    void testAddCategory() {
        PaletteService service = PaletteService.getInstance()
        GroovyClassLoader gcl = new GroovyClassLoader();
        Class clazz = gcl.parseClass(ccFile);
        service.addToCategoryInternal(new ComponentDefinition(typeClass: clazz));
        assertTrue service.getCategoriesFromDefinition(new ComponentDefinition(typeClass: clazz)).size() > 0;
        assertTrue service.getDefinitionsFromCategory("CAT1").size() > 0;
        assertTrue service.getDefinitionsFromCategory("CAT2").size() > 0;
    }

    void testAddComponent() {
        final PaletteService service = PaletteService.getInstance()
        ComponentDefinition definition = new ComponentDefinition(typeClass: String)
        service.addComponentDefinition(definition)
        final List<ComponentDefinition> definitions = service.getDefinitionsFromCategory(PaletteService.CAT_OTHER)
        assertTrue(definitions*.typeClass.contains(String))
        assertNotNull(service.getComponentDefinition(String.name))
    }
}
