package org.pillarone.riskanalytics.graph.core.graphexport

import org.pillarone.riskanalytics.core.FileConstants
import org.pillarone.riskanalytics.graph.core.graph.model.ComposedComponentGraphModel
import org.pillarone.riskanalytics.graph.core.graph.model.ModelGraphModel
import org.pillarone.riskanalytics.graph.core.graphimport.GraphImportService

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
    EventClaimsGenerator subClaimsGenerator = new EventClaimsGenerator();
    EventSeverityExtractor subSeverityExtractor = new EventSeverityExtractor();

    public void wire() {
        org.pillarone.riskanalytics.core.wiring.WiringUtils.use(org.pillarone.riskanalytics.core.wiring.PortReplicatorCategory) {
        this.outClaims = subClaimsGenerator.outClaims;
        subSeverityExtractor.inSeverities = this.inEventSeverities;
        }
        org.pillarone.riskanalytics.core.wiring.WiringUtils.use(org.pillarone.riskanalytics.core.wiring.WireCategory) {
        subClaimsGenerator.inSeverities = subSeverityExtractor.outSeverities;
        }
    }

}"""

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
    DynamicReservesGeneratorLean reserveGenerators = new DynamicReservesGeneratorLean();
    DynamicDependencies dependencies = new DynamicDependencies();
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
        reserveGenerators.inClaims = claimsGenerators.outClaims;
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
        assertTrue classes.keySet().size()>0
        GroovyClassLoader gcl = new GroovyClassLoader();
        for (String name: classes.keySet()) {
            gcl.defineClass(name, classes.get(name));
        }
    }

    void testModelToBinary() {
        ModelGraphModel graph = graphImportService.importGraph(modelFile);
        Map<String, byte[]> classes = graphExportService.exportGraphToBinary(graph)
        assertTrue classes.keySet().size()>0
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


}
