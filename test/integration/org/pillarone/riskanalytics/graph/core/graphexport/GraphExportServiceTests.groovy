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

    String ccFile = """package model;
import org.pillarone.riskanalytics.core.components.ComposedComponent;
import org.pillarone.riskanalytics.core.packets.PacketList;
import org.pillarone.riskanalytics.domain.pc.claims.Claim;
import org.pillarone.riskanalytics.domain.pc.generators.claims.EventClaimsGenerator;
import org.pillarone.riskanalytics.domain.pc.generators.copulas.EventDependenceStream;
import org.pillarone.riskanalytics.domain.pc.severities.EventSeverityExtractor;
public class TestCC
    extends ComposedComponent
{
    PacketList<EventDependenceStream> inEventSeverities = new PacketList(EventDependenceStream.class);

    PacketList<Claim> outClaims = new PacketList(Claim.class);
    /**
     * Component:subClaimsGenerator
     * empty comment
     *
     */
    EventClaimsGenerator subClaimsGenerator = new EventClaimsGenerator();
    /**
     * Component:subSeverityExtractor
     * empty comment
     *
     */
    EventSeverityExtractor subSeverityExtractor = new EventSeverityExtractor();
    EventSeverityExtractor subSeverityExtractor2 = new EventSeverityExtractor();
    public void wire() {
        org.pillarone.riskanalytics.core.wiring.WiringUtils.use(org.pillarone.riskanalytics.core.wiring.PortReplicatorCategory) {
        this.outClaims = subClaimsGenerator.outClaims;
        /**
         * Replication:subClaimsGenerator.outClaims->outClaims
         * empty comment
         */
        subSeverityExtractor.inSeverities = this.inEventSeverities;
        subSeverityExtractor2.inSeverities= this.inEventSeverities;
        /**
         * Replication:inEventSeverities->subSeverityExtractor.inSeverities
         * empty comment
         */
        }
        org.pillarone.riskanalytics.core.wiring.WiringUtils.use(org.pillarone.riskanalytics.core.wiring.WireCategory) {
        /**
         * Connection:subSeverityExtractor.outSeverities->subClaimsGenerator.inSeverities
         * empty comment
         */
        subClaimsGenerator.inSeverities = subSeverityExtractor.outSeverities;
        }
    }
}
"""

    String modelFile = """package model;
import org.pillarone.riskanalytics.core.model.StochasticModel;
import org.pillarone.riskanalytics.domain.pc.generators.claims.DynamicDevelopedClaimsGenerators;
import org.pillarone.riskanalytics.domain.pc.generators.copulas.DynamicDependencies;
import org.pillarone.riskanalytics.domain.pc.generators.copulas.DynamicMultipleDependencies;
import org.pillarone.riskanalytics.domain.pc.lob.DynamicConfigurableLobsWithReserves;
import org.pillarone.riskanalytics.domain.pc.reserves.fasttrack.DynamicReservesGeneratorLean;
import org.pillarone.riskanalytics.domain.pc.underwriting.DynamicUnderwritingSegments;
public class TestModel
    extends StochasticModel
{
    DynamicUnderwritingSegments underwritingSegments = new DynamicUnderwritingSegments();
    DynamicDevelopedClaimsGenerators claimsGenerators = new DynamicDevelopedClaimsGenerators();
    DynamicConfigurableLobsWithReserves linesOfBusiness = new DynamicConfigurableLobsWithReserves();
    /**
     * Component:reserveGenerators
     * empty comment
     *
     */
    DynamicReservesGeneratorLean reserveGenerators = new DynamicReservesGeneratorLean();
    DynamicDependencies dependencies = new DynamicDependencies();
    /**
     * Component:eventGenerators
     * empty comment
     *
     */
    DynamicMultipleDependencies eventGenerators = new DynamicMultipleDependencies();
    public void initComponents() {
        addStartComponent underwritingSegments
        addStartComponent dependencies
        addStartComponent eventGenerators
    }
    public void wireComponents() {
        claimsGenerators.inProbabilities = dependencies.outProbabilities;
        linesOfBusiness.inUnderwritingInfoGross = underwritingSegments.outUnderwritingInfo;
        linesOfBusiness.inClaimsGross = claimsGenerators.outClaims;
        linesOfBusiness.inClaimsGross = reserveGenerators.outClaimsDevelopment;
       /**
         * Connection:claimsGenerators.outClaims->reserveGenerators.inClaims
         * empty comment
         */
        reserveGenerators.inClaims = claimsGenerators.outClaims;
       /**
         * Connection:underwritingSegments.outUnderwritingInfo->claimsGenerators.inUnderwritingInfo
         * empty comment
         */
        claimsGenerators.inUnderwritingInfo = underwritingSegments.outUnderwritingInfo;
        claimsGenerators.inEventSeverities = eventGenerators.outEventSeverities;
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
        InPort p = c.getInPorts().find {it.name.equals("inSeverities")};
        p.connectionCardinality = testCRange1
        p.packetCardinality = testPRange1

        c = graph.getAllComponentNodes().find {it.name.equals("subSeverityExtractor2")};
        InPort p2 = c.getInPorts().find {it.name.equals("inSeverities")};
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
