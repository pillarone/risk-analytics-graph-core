package org.pillarone.riskanalytics.graph.core.palette.service

import org.pillarone.riskanalytics.graph.core.palette.model.ComponentDefinition
import org.pillarone.riskanalytics.core.example.component.TestComponent

class PaletteServiceTests extends GroovyTestCase {
    String ccFile = """
package model;
import org.pillarone.riskanalytics.core.components.ComposedComponent;
import org.pillarone.riskanalytics.core.packets.PacketList;
import org.pillarone.riskanalytics.domain.pc.claims.Claim;
import org.pillarone.riskanalytics.domain.pc.generators.claims.EventClaimsGenerator;
import org.pillarone.riskanalytics.domain.pc.generators.copulas.EventDependenceStream;
import org.pillarone.riskanalytics.domain.pc.severities.EventSeverityExtractor;
import org.pillarone.riskanalytics.graph.core.palette.annotations.ComponentCategory;

@ComponentCategory(categories=["CAT1","CAT2"])
public class TestCC
    extends ComposedComponent
{
    PacketList<EventDependenceStream> inEventSeverities = new PacketList(EventDependenceStream.class);
    PacketList<Claim> outClaims = new PacketList(Claim.class);
    EventSeverityExtractor subSeverityExtractor = new EventSeverityExtractor();
    EventClaimsGenerator subClaimsGenerator = new EventClaimsGenerator();
    public void wire() {
        org.pillarone.riskanalytics.core.wiring.WiringUtils.use(org.pillarone.riskanalytics.core.wiring.PortReplicatorCategory) {
        this.outClaims = subClaimsGenerator.outClaims;
        subSeverityExtractor.inSeverities = this.inEventSeverities;
        }
        org.pillarone.riskanalytics.core.wiring.WiringUtils.use(org.pillarone.riskanalytics.core.wiring.WireCategory) {
        subClaimsGenerator.inSeverities = subSeverityExtractor.outSeverities;
        }
    }
}
        """

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

    void testAddCategory(){
        PaletteService service = PaletteService.getInstance()
        GroovyClassLoader gcl=new GroovyClassLoader();
        Class clazz=gcl.parseClass(ccFile);
        service.addToCategoryInternal(new ComponentDefinition(typeClass:clazz));
        assertTrue service.getDefinitionsFromCategory("CAT1").size()>0;
        assertTrue service.getDefinitionsFromCategory("CAT2").size()>0;
    }
}
