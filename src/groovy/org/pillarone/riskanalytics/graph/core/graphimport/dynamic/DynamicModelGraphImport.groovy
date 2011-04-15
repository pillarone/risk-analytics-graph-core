package org.pillarone.riskanalytics.graph.core.graphimport.dynamic

import org.pillarone.riskanalytics.graph.core.graph.model.ModelGraphModel
import org.pillarone.riskanalytics.core.model.Model
import org.pillarone.riskanalytics.core.components.Component
import org.pillarone.riskanalytics.core.wiring.Transmitter
import org.pillarone.riskanalytics.core.components.DynamicComposedComponent
import org.pillarone.riskanalytics.core.util.PropertiesUtils
import org.apache.commons.lang.StringUtils
import org.pillarone.riskanalytics.core.simulation.item.Parameterization
import org.pillarone.riskanalytics.core.parameterization.ParameterizationHelper
import org.pillarone.riskanalytics.core.parameterization.ParameterApplicator

public class DynamicModelGraphImport extends AbstractDynamicGraphImport {

    public ModelGraphModel importDynamicModel(String content) {
        String fileContent = getFileContent(content)
        ConfigObject data = new ConfigSlurper().parse(fileContent);
        spreadRanges(data)
        Parameterization params = ParameterizationHelper.createParameterizationFromConfigObject(data, "params1");
        println params;
        Model m = data.model.newInstance();
        m.init();
        ParameterApplicator pa = new ParameterApplicator(model: m, parameterization: params);
        pa.init();
        pa.applyParameterForPeriod(0); 
        m.wire();
        ModelGraphModel mg= createFromWired(m);
        return mg;
    }

    public ModelGraphModel createFromWired(Model m) {
        graph = new ModelGraphModel(m.getClass().getSimpleName(), m.getClass().getPackage().name);
        for (Component c: m.allComponents) {
            if (!resolveInterior(c))
                addComponentNode(m, c);
            for (Transmitter t: c.allInputTransmitter) {
                wireComponents(m,m, t);
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
        List<String> lines=new StringReader(fileContent).readLines();
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
