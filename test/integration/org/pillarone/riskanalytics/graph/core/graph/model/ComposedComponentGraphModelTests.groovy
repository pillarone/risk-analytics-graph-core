package org.pillarone.riskanalytics.graph.core.graph.model

import org.pillarone.riskanalytics.core.example.packet.TestPacket

import org.pillarone.riskanalytics.graph.core.palette.service.PaletteService
import org.pillarone.riskanalytics.core.example.component.TestComponent
import org.pillarone.riskanalytics.graph.core.palette.model.ComponentDefinition
import org.pillarone.riskanalytics.core.example.component.TestComposedComponent

class ComposedComponentGraphModelTests extends GroovyTestCase {


    String ccFile = """
package model;
import org.pillarone.riskanalytics.core.components.ComposedComponent;
import org.pillarone.riskanalytics.core.example.component.ExampleInputOutputComponent;
import org.pillarone.riskanalytics.core.packets.Packet;
import org.pillarone.riskanalytics.core.packets.PacketList;
import org.pillarone.riskanalytics.core.wiring.WiringValidation
public class TestCC
    extends ComposedComponent
{
    @WiringValidation(connections=[0,1],packets=[1,2])
    PacketList<Packet> inEventSeverities = new PacketList(Packet.class);
    @WiringValidation(connections=[1,2],packets=[0,2])
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
        assertTrue node.getPort("inEventSeverities").connectionCardinality.from == 0
        assertTrue node.getPort("outClaims").connectionCardinality.to == 2
        assertTrue node.getPort("inEventSeverities").packetCardinality.to == 2
        assertTrue node.getPort("outClaims").packetCardinality.from == 0
    }

    void testComposedComponent(){
        ComposedComponentGraphModel model = new ComposedComponentGraphModel()
        ComposedComponentNode n=model.createComponentNode(PaletteService.getInstance().getComponentDefinition(TestComposedComponent.class),"name");
        assertEquals 1,n.getReplicatedInPorts(n.inPorts.first()).size();
        assertEquals 1,n.getReplicatedOutPorts(n.outPorts.first()).size();
    }

}
