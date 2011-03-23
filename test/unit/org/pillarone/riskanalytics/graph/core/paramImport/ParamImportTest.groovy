package org.pillarone.riskanalytics.graph.core.paramImport

import org.pillarone.riskanalytics.core.util.PropertiesUtils
import org.apache.commons.lang.StringUtils
import org.pillarone.riskanalytics.core.simulation.item.Parameterization
import org.pillarone.riskanalytics.core.parameterization.ParameterizationHelper
import org.pillarone.riskanalytics.core.parameterization.ParameterApplicator
import org.pillarone.riskanalytics.core.model.Model
import org.pillarone.riskanalytics.graph.core.graphimport.ModelGraphImport

class ParamImportTest extends GroovyTestCase {

    public void testParamImport() {
        List lines = new File("C:\\Users\\MW\\projects\\PrjPillarOne\\paramset.groovy").readLines()
        String fileContent = getFileContent(lines)
        ConfigObject data = new ConfigSlurper().parse(fileContent);
        spreadRanges (data)
        Parameterization params=ParameterizationHelper.createParameterizationFromConfigObject(data,"params1");
        println params;
        Model m=data.model.newInstance();
        m.init();
        ParameterApplicator pa=new ParameterApplicator(model: m,parameterization: params);
        pa.init();
        pa.applyParameterForPeriod(0);
        m.wire();
        new ModelGraphImport().createFromWiredModel(m);
        //new ComposedComponentGraphImport().importGraph(ReinsuranceWithBouquetCommissionProgram.class,null);

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

    private String getFileContent(List lines) {
        String fileContent = lines.join("\n")
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
