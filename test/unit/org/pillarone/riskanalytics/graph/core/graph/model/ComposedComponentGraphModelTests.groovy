package org.pillarone.riskanalytics.graph.core.graph.model

import org.pillarone.riskanalytics.core.example.packet.TestPacket

import org.pillarone.riskanalytics.graph.core.palette.service.PaletteService
import org.pillarone.riskanalytics.core.example.component.TestComponent
import org.pillarone.riskanalytics.graph.core.palette.model.ComponentDefinition

class ComposedComponentGraphModelTests extends GroovyTestCase {


    String ccFile = """
   package model;
   import org.pillarone.riskanalytics.core.components.ComposedComponent;
   import org.pillarone.riskanalytics.core.packets.PacketList;
   import org.pillarone.riskanalytics.domain.pc.claims.Claim;
   import org.pillarone.riskanalytics.domain.pc.generators.claims.EventClaimsGenerator;
   import org.pillarone.riskanalytics.domain.pc.generators.copulas.EventDependenceStream;
   import org.pillarone.riskanalytics.domain.pc.severities.EventSeverityExtractor;
   import org.pillarone.riskanalytics.graph.core.palette.annotations.ComponentCategory;
   import org.pillarone.riskanalytics.graph.core.palette.annotations.WiringValidation

   @ComponentCategory(categories=["CAT1","CAT2"])
   public class TestCC
       extends ComposedComponent
   {
       @WiringValidation(connections=[1,1],packets=[1,10])
       PacketList<EventDependenceStream> inEventSeverities = new PacketList(EventDependenceStream.class);
       @WiringValidation(connections=[0,1],packets=[0,1])
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

    void testCreateOuterPorts() {
        ComposedComponentGraphModel model = new ComposedComponentGraphModel()
        InPort inPort = model.createOuterInPort(TestPacket, "inTest")
        OutPort outPort = model.createOuterOutPort(TestPacket, "outTest")

        assertTrue model.outerInPorts.contains(inPort)
        assertTrue model.outerOutPorts.contains(outPort)
    }

    void testAvailablePorts() {
        ComposedComponentGraphModel model = new ComposedComponentGraphModel()
        InPort inPort = model.createOuterInPort(TestPacket, "inTest")
        OutPort outPort = model.createOuterOutPort(TestPacket, "outTest")

        ComponentNode node = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "name")
        ComponentNode node2 = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "name2")

        List<Port> ports = model.getAvailablePorts(node.getPort("input3"))

        assertEquals 2, ports.size()
        assertTrue ports.contains(inPort)
        assertTrue ports.contains(node2.getPort("outClaims"))

        ports = model.getAvailablePorts(node.getPort("outClaims"))

        assertEquals 2, ports.size()
        assertTrue ports.contains(outPort)
        assertTrue ports.contains(node2.getPort("input3"))

        ports = model.getAvailablePorts(inPort)

        assertEquals 2, ports.size()
        assertTrue ports.contains(node.getPort("input3"))
        assertTrue ports.contains(node2.getPort("input3"))

        ports = model.getAvailablePorts(outPort)

        assertEquals 2, ports.size()
        assertTrue ports.contains(node.getPort("outClaims"))
        assertTrue ports.contains(node2.getPort("outClaims"))

    }

    void testWiringValidation() {
        ComposedComponentGraphModel model = new ComposedComponentGraphModel()
        GroovyClassLoader gcl = new GroovyClassLoader();
        Class clazz = gcl.parseClass(ccFile);
        ComponentNode node = model.createComponentNode(new ComponentDefinition(typeClass: clazz), "name");
        assertTrue node.getPort("inEventSeverities").connectionCardinality.from == 1
        assertTrue node.getPort("outClaims").connectionCardinality.to == 1
        assertTrue node.getPort("inEventSeverities").packetCardinality.to == 10
        assertTrue node.getPort("outClaims").packetCardinality.from == 0
    }

}
