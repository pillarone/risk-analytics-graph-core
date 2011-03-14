package org.pillarone.riskanalytics.graph.core.graphexport

import org.pillarone.riskanalytics.core.FileConstants
import org.pillarone.riskanalytics.graph.core.graph.model.ComponentNode
import org.pillarone.riskanalytics.graph.core.graph.model.ComposedComponentGraphModel
import org.pillarone.riskanalytics.graph.core.graph.model.InPort
import org.pillarone.riskanalytics.graph.core.graph.model.ModelGraphModel
import org.pillarone.riskanalytics.graph.core.graph.util.IntegerRange
import org.pillarone.riskanalytics.graph.core.graphimport.GraphImportService
import org.pillarone.riskanalytics.graph.core.palette.model.ComponentDefinition


class GraphExportServiceTests extends GroovyTestCase {

    GraphImportService graphImportService;
    GraphExportService graphExportService;

    String ccFile =  """
package model;
import org.pillarone.riskanalytics.core.components.ComposedComponent;
import org.pillarone.riskanalytics.core.example.component.ExampleInputOutputComponent;
import org.pillarone.riskanalytics.core.packets.Packet;
import org.pillarone.riskanalytics.core.packets.PacketList;
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

    String modelFile = """
package model;
import org.pillarone.riskanalytics.core.example.component.ExampleInputOutputComponent;
import org.pillarone.riskanalytics.core.model.StochasticModel;
public class TestModel
    extends StochasticModel
{
    /**
     * Component:underwritingSegments
     * empty
     */
    ExampleInputOutputComponent underwritingSegments;
    /**
     * Component:claimsGenerators
     * empty
     */
    ExampleInputOutputComponent claimsGenerators;
    ExampleInputOutputComponent linesOfBusiness;
    public void initComponents() {
        claimsGenerators = new ExampleInputOutputComponent();
        linesOfBusiness = new ExampleInputOutputComponent();
        underwritingSegments = new ExampleInputOutputComponent();
        addStartComponent underwritingSegments
        addStartComponent claimsGenerators
    }
    public void wireComponents() {
        /**
         * Connection:underwritingSegments.outValue->claimsGenerators.inValue
         * empty
         */
        claimsGenerators.inValue = underwritingSegments.outValue;
        /**
         * Connection:underwritingSegments.outValue->linesOfBusiness.inValue
         * empty
         */
        linesOfBusiness.inValue = underwritingSegments.outValue;
        linesOfBusiness.inValue = claimsGenerators.outValue;
    }
}
"""



    protected void setUp() {
        super.setUp()
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testCCExport() {
        ComposedComponentGraphModel graph = graphImportService.importGraph(ccFile);
        String ret = graphExportService.exportGraph(graph);
        println ret;
        ComposedComponentGraphModel graph2 = graphImportService.importGraph(ret);

        assertEquals graph.allComponentNodes.size(), graph2.allComponentNodes.size()
        assertEquals graph.allConnections.size(), graph2.allConnections.size()
        assertEquals graph.outerInPorts.size(), graph2.outerInPorts.size()
        assertEquals graph.outerOutPorts.size(), graph2.outerOutPorts.size()

    }

    void testModelExport() {
        ModelGraphModel graph = graphImportService.importGraph(modelFile);
        String ret = graphExportService.exportGraph(graph);
        println(ret)
        ModelGraphModel graph2 = graphImportService.importGraph(ret);

        assertEquals graph.allComponentNodes.size(), graph2.allComponentNodes.size()
        assertEquals graph.allConnections.size(), graph2.allConnections.size()
        assertEquals graph.startComponents.size(), graph2.startComponents.size()

    }

    void testCCToBinary() {
        ComposedComponentGraphModel graph = graphImportService.importGraph(ccFile);
        Map<String, byte[]> classes = graphExportService.exportGraphToBinary(graph);
        assertTrue classes.keySet().size() > 0
        GroovyClassLoader gcl = new GroovyClassLoader();
        for (String name: classes.keySet()) {
            gcl.defineClass(name, classes.get(name));
        }
    }

    void testModelToBinary() {
        ModelGraphModel graph = graphImportService.importGraph(modelFile);
        Map<String, byte[]> classes = graphExportService.exportGraphToBinary(graph)
        assertTrue classes.keySet().size() > 0
        GroovyClassLoader gcl = new GroovyClassLoader();
        for (String name: classes.keySet()) {
            gcl.defineClass(name, classes.get(name));
        };
    }

    void testCCToJAR() {
        ComposedComponentGraphModel graph = graphImportService.importGraph(ccFile);
        byte[] content = graphExportService.exportGraphToJAR(graph);
        assertNotNull content
        File f = new File(FileConstants.TEMP_FILE_DIRECTORY + File.separator + graph.name + ".jar");
        f.delete();
        FileOutputStream fout = new FileOutputStream(f);
        fout.write(content);
        fout.close();
        GroovyClassLoader gcl = new GroovyClassLoader();
        gcl.addClasspath(f.path);
        gcl.loadClass(graph.packageName + "." + graph.name);
    }



    void testModelToJAR() {
        ModelGraphModel graph = graphImportService.importGraph(modelFile);
        byte[] content = graphExportService.exportGraphToJAR(graph);
        assertNotNull content
        File f = new File(FileConstants.TEMP_FILE_DIRECTORY + File.separator + graph.name + ".jar");
        f.delete();
        FileOutputStream fout = new FileOutputStream(f);
        fout.write(content);
        fout.close();
        GroovyClassLoader gcl = new GroovyClassLoader();
        gcl.addClasspath(f.path);
        gcl.loadClass(graph.packageName + "." + graph.name);
    }



    void testCCAnnotations() {
        IntegerRange testCRange1 = new IntegerRange(from: 0, to: 2);
        IntegerRange testCRange2 = new IntegerRange(from: 1, to: 3);
        IntegerRange testPRange1 = new IntegerRange(from: 1, to: 2);
        IntegerRange testPRange2 = new IntegerRange(from: 0, to: 1);

        ComposedComponentGraphModel graph = graphImportService.importGraph(ccFile);
        ComponentNode c = graph.getAllComponentNodes().find {it.name.equals("subSeverityExtractor")};
        InPort p = c.getInPorts().find {it.name.equals("inValue")};
        p.connectionCardinality = testCRange1
        p.packetCardinality = testPRange1

        c = graph.getAllComponentNodes().find {it.name.equals("subSeverityExtractor2")};
        InPort p2 = c.getInPorts().find {it.name.equals("inValue")};
        p2.connectionCardinality = testCRange2
        p2.packetCardinality = testPRange2

        String ret = graphExportService.exportGraph(graph);

        GroovyClassLoader gcl = new GroovyClassLoader();
        Class clazz = gcl.parseClass(ret);
        ComponentNode node = new ComposedComponentGraphModel().createComponentNode(new ComponentDefinition(typeClass: clazz), "name");
        p = node.inPorts.find {it.name.equals("inEventSeverities")}

        assertTrue((p.connectionCardinality.from == Math.max(testCRange1.from, testCRange2.from)) &&
                (p.connectionCardinality.to == Math.min(testCRange1.to, testCRange2.to)))

        assertTrue((p.packetCardinality.from == Math.max(testPRange1.from, testPRange2.from)) &&
                (p.packetCardinality.to == Math.min(testPRange1.to, testPRange2.to)))


    }


}
