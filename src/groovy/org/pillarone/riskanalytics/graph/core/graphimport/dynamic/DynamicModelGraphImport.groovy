package org.pillarone.riskanalytics.graph.core.graphimport.dynamic

import org.apache.commons.lang.StringUtils
import org.pillarone.riskanalytics.core.components.Component
import org.pillarone.riskanalytics.core.model.Model
import org.pillarone.riskanalytics.core.output.NoOutput
import org.pillarone.riskanalytics.core.parameterization.ParameterApplicator
import org.pillarone.riskanalytics.core.parameterization.ParameterizationHelper
import org.pillarone.riskanalytics.core.simulation.engine.SimulationConfiguration
import org.pillarone.riskanalytics.core.simulation.engine.SimulationRunner
import org.pillarone.riskanalytics.core.simulation.engine.grid.SimulationBlock
import org.pillarone.riskanalytics.core.simulation.item.Parameterization
import org.pillarone.riskanalytics.core.simulation.item.ResultConfiguration
import org.pillarone.riskanalytics.core.simulation.item.Simulation
import org.pillarone.riskanalytics.core.simulation.item.VersionNumber
import org.pillarone.riskanalytics.core.util.PropertiesUtils
import org.pillarone.riskanalytics.core.wiring.IPacketListener
import org.pillarone.riskanalytics.core.wiring.Transmitter
import org.pillarone.riskanalytics.graph.core.graph.model.Connection
import org.pillarone.riskanalytics.graph.core.graph.model.ModelGraphModel
import org.pillarone.riskanalytics.graph.core.graphimport.TraceImport

/**
 * Import of a graphmodel from a running model
 */
public class DynamicModelGraphImport extends AbstractDynamicGraphImport {

    @Deprecated
    public ModelGraphModel importDynamicModel(String content) {
        String fileContent = getFileContent(content)
        ConfigObject data = new ConfigSlurper().parse(fileContent);
        spreadRanges(data)
        Parameterization params = ParameterizationHelper.createParameterizationFromConfigObject(data, "params1");
        println params;
        Model m = data.model.newInstance();
        m.init();
        m.injectComponentNames();
        ParameterApplicator pa = new ParameterApplicator(model: m, parameterization: params);
        pa.init();
        pa.applyParameterForPeriod(0);
        m.wire();
        ModelGraphModel mg = createFromWired(m);
        return mg;
    }

    /** *
     * Create a graph model out of a parameter file's content
     * @param parameter file content
     * @return
     */
    public ModelGraphModel importFromTracing(String content) {

        String fileContent = getFileContent(content)
        ConfigObject data = new ConfigSlurper().parse(fileContent);
        spreadRanges(data)
        Parameterization params = ParameterizationHelper.createParameterizationFromConfigObject(data, "params1");
        IPacketListener ipl = new TraceImport();
        Simulation run = new Simulation("Core_${new Date().toString()}")

        run.parameterization = params
        run.template = new ResultConfiguration("test")
        run.modelClass = data.model
        run.modelVersionNumber = new VersionNumber("1")
        run.periodCount = 1
        run.numberOfIterations = 1
        run.structure = null

        SimulationConfiguration simulationConfiguration = new SimulationConfiguration(
                simulation: run, outputStrategy: new NoOutput(),
                simulationBlocks: [new SimulationBlock(0, run.numberOfIterations, 0)]
        )
        //Add PacketListener, so packets are traced while running a simulation
        simulationConfiguration.packetListener = ipl;

        SimulationRunner runner = SimulationRunner.createRunner();
        runner.simulationConfiguration = simulationConfiguration

        runner.start()
        ipl.resolveConnections();

        return ipl.mgm;
    }

    @Deprecated
    public ModelGraphModel createFromWired(Model m) {
        graph = new ModelGraphModel(m.getClass().getSimpleName(), m.getClass().getPackage().name);
        for (Component c: m.allComponents) {
            if (!resolveInterior(c))
                addComponentNode(m, c);
            for (Transmitter t: c.allInputTransmitter) {
                wireComponents(m, m, t);
            }
        }
        return graph;
    }

    private Properties getProperties(List lines) {
        Properties properties = new Properties()
        String appVersion = new PropertiesUtils().getProperties("/version.properties").getProperty("version", "N/A")
        String pVersion = getVersion(lines)
        if (pVersion) {
            properties = new PropertiesUtils().getProperties("/parameterization_${pVersion}_${appVersion}.properties")
        } else {
            properties = new PropertiesUtils().getProperties("/parameterization_${DEFAULT_VERSION}_${appVersion}.properties")
        }
        return properties
    }

    private String getFileContent(String fileContent) {
        List<String> lines = new StringReader(fileContent).readLines();
        Properties properties = getProperties(lines)
        properties.propertyNames().each {String old ->
            fileContent = fileContent.replaceAll(old, properties.get(old))
        }
        return fileContent
    }

    private void spreadRanges(ConfigObject config) {
        def rangeKeys = [:]
        List ranges = []
        config.each {key, value ->
            if (value instanceof ConfigObject) {
                spreadRanges(value)
            }
            if (key instanceof Range) {
                ranges << key
                key.each {
                    rangeKeys[it] = value
                }
            }
        }
        config.putAll(rangeKeys)
        ranges.each {
            config.remove(it)
        }
    }


    private String getVersion(List lines) {
        for (String str: lines) {
            if (StringUtils.isNotEmpty(str) && str.indexOf("applicationVersion") != -1) {
                return str.substring(str.indexOf("=") + 1).trim().replaceAll("'", "")
            }
        }
        return null
    }
}