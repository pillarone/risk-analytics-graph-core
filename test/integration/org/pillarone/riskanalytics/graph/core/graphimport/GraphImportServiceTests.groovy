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
import org.pillarone.riskanalytics.domain.pc.claims.Claim;
import org.pillarone.riskanalytics.domain.pc.generators.claims.EventClaimsGenerator;
import org.pillarone.riskanalytics.domain.pc.generators.copulas.EventDependenceStream;
import org.pillarone.riskanalytics.domain.pc.severities.EventSeverityExtractor;
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
import org.pillarone.riskanalytics.domain.pc.aggregators.AlmResultAggregator
import org.pillarone.riskanalytics.domain.pc.assetLiabilityMismatch.DynamicAssetLiabilityMismatchGenerator
import org.pillarone.riskanalytics.domain.pc.generators.claims.DynamicDevelopedClaimsGenerators
import org.pillarone.riskanalytics.domain.pc.generators.copulas.DynamicDependencies
import org.pillarone.riskanalytics.domain.pc.generators.copulas.DynamicMultipleDependencies
import org.pillarone.riskanalytics.domain.pc.lob.DynamicConfigurableLobsWithReserves
import org.pillarone.riskanalytics.domain.pc.reinsurance.programs.ReinsuranceWithBouquetCommissionProgram
import org.pillarone.riskanalytics.domain.pc.reserves.fasttrack.DynamicReservesGeneratorLean
import org.pillarone.riskanalytics.domain.pc.underwriting.DynamicUnderwritingSegments
import org.pillarone.riskanalytics.domain.pc.filter.DynamicSegmentFilters

class TestModel extends StochasticModel {

    DynamicUnderwritingSegments underwritingSegments
    DynamicDevelopedClaimsGenerators claimsGenerators
    DynamicReservesGeneratorLean reserveGenerators
    DynamicDependencies dependencies
    DynamicMultipleDependencies eventGenerators
    DynamicConfigurableLobsWithReserves linesOfBusiness


    void initComponents() {
        underwritingSegments = new DynamicUnderwritingSegments()
        claimsGenerators = new DynamicDevelopedClaimsGenerators()
        reserveGenerators = new DynamicReservesGeneratorLean()
        dependencies = new DynamicDependencies()
        eventGenerators = new DynamicMultipleDependencies()
        linesOfBusiness = new DynamicConfigurableLobsWithReserves()

        addStartComponent underwritingSegments
        addStartComponent dependencies
        addStartComponent eventGenerators
    }

    void wireComponents() {
        claimsGenerators.inUnderwritingInfo = underwritingSegments.outUnderwritingInfo
        claimsGenerators.inProbabilities = dependencies.outProbabilities
        claimsGenerators.inEventSeverities = eventGenerators.outEventSeverities
        reserveGenerators.inClaims = claimsGenerators.outClaims

        linesOfBusiness.inUnderwritingInfoGross = underwritingSegments.outUnderwritingInfo
        linesOfBusiness.inClaimsGross = claimsGenerators.outClaims
        linesOfBusiness.inClaimsGross = reserveGenerators.outClaimsDevelopment
    }
}

"""

        AbstractGraphModel graph = graphImportService.importGraph(modelFile);

        assertEquals 6, graph.allComponentNodes.size()
        assertEquals 7, graph.allConnections.size()
        assertEquals 3, ((ModelGraphModel) graph).startComponents.size()
    }
}
