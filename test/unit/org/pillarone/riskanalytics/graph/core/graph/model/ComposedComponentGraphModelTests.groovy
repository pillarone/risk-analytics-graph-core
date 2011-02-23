package org.pillarone.riskanalytics.graph.core.graph.model

import org.pillarone.riskanalytics.core.example.packet.TestPacket

import org.pillarone.riskanalytics.graph.core.palette.service.PaletteService
import org.pillarone.riskanalytics.core.example.component.TestComponent

class ComposedComponentGraphModelTests extends GroovyTestCase {

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

}
