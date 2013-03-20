package org.pillarone.riskanalytics.graph.core.graphimport

import grails.test.*
import org.pillarone.riskanalytics.graph.core.graph.model.AbstractGraphModel
import org.pillarone.riskanalytics.graph.core.graph.model.ModelGraphModel
import org.pillarone.riskanalytics.graph.core.graph.model.ComposedComponentGraphModel
import org.pillarone.riskanalytics.graph.core.graphexport.GraphExportService

class GraphImportServiceTests extends GroovyTestCase {

    GraphImportService graphImportService;


    void testCCImport() {
        String ccFile = """
package model;
import org.pillarone.riskanalytics.core.components.ComposedComponent;
import org.pillarone.riskanalytics.core.packets.PacketList;
import org.pillarone.riskanalytics.core.packets.Packet;
import org.pillarone.riskanalytics.core.example.component.ExampleInputOutputComponent
import org.pillarone.riskanalytics.core.example.component.TestComposedComponent

public class TestCC
    extends ComposedComponent
{
    PacketList<Packet> inEventSeverities = new PacketList(Packet.class);
    PacketList<Packet> outClaims = new PacketList(Packet.class);
    TestComposedComponent subSeverityExtractor = new TestComposedComponent();
    ExampleInputOutputComponent subClaimsGenerator = new ExampleInputOutputComponent();
    public void wire() {
        org.pillarone.riskanalytics.core.wiring.WiringUtils.use(org.pillarone.riskanalytics.core.wiring.PortReplicatorCategory) {
        this.outClaims = subClaimsGenerator.outValue;
        subSeverityExtractor.input1 = this.inEventSeverities;
        }
        org.pillarone.riskanalytics.core.wiring.WiringUtils.use(org.pillarone.riskanalytics.core.wiring.WireCategory) {
        subClaimsGenerator.inValue = subSeverityExtractor.outValue1;
        }
    }
}
        """
        AbstractGraphModel graph = graphImportService.importGraph(ccFile);

        assertEquals 2, graph.allComponentNodes.size()
        assertEquals 3, graph.allConnections.size()
        assertEquals 1, ((ComposedComponentGraphModel) graph).outerInPorts.size()
        assertEquals 1, ((ComposedComponentGraphModel) graph).outerOutPorts.size()


    }

    void testModelImport() {

        String modelFile = """
package model

import org.pillarone.riskanalytics.core.model.StochasticModel
import org.pillarone.riskanalytics.core.example.component.ExampleInputOutputComponent

class TestModel extends StochasticModel {

    ExampleInputOutputComponent underwritingSegments
    ExampleInputOutputComponent claimsGenerators
    ExampleInputOutputComponent linesOfBusiness


    void initComponents() {
        underwritingSegments = new ExampleInputOutputComponent()
        claimsGenerators = new ExampleInputOutputComponent()
        linesOfBusiness = new ExampleInputOutputComponent()

        addStartComponent underwritingSegments
        addStartComponent claimsGenerators
    }

    void wireComponents() {
        claimsGenerators.inValue = underwritingSegments.outValue

        linesOfBusiness.inValue = claimsGenerators.outValue
        linesOfBusiness.inValue = underwritingSegments.outValue
    }
}

"""

        AbstractGraphModel graph = graphImportService.importGraph(modelFile);

        assertEquals 3, graph.allComponentNodes.size()
        assertEquals 3, graph.allConnections.size()
        assertEquals 2, ((ModelGraphModel) graph).startComponents.size()
    }
}
