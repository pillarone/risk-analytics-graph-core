package org.pillarone.riskanalytics.graph.core.loader

import org.codehaus.groovy.control.CompilerConfiguration
import org.pillarone.riskanalytics.core.FileConstants
import org.codehaus.groovy.tools.Compiler
import org.pillarone.riskanalytics.core.model.Model
import org.pillarone.riskanalytics.graph.core.graph.persistence.GraphPersistenceService
import org.pillarone.riskanalytics.core.model.registry.ModelRegistry

class DatabaseClassLoaderTests extends GroovyTestCase {

    GraphPersistenceService graphPersistenceService


    String modelName = "dynamically loaded model!"
    String someObjectName = "some object"

    String objectCode = """
package some.packagename

class SomeObject {

    @Override
    String toString() { "${someObjectName}" }
}
"""

    String modelCode = """
package some.packagename

import org.pillarone.riskanalytics.core.model.Model
import org.pillarone.riskanalytics.core.simulation.IPeriodCounter
import org.joda.time.DateTime

class TestModel extends Model {

    SomeObject obj = new SomeObject()

    @Override
    void initComponents() { }

    @Override
    void wireComponents() { }

    @Override
    IPeriodCounter createPeriodCounter(DateTime beginOfFirstPeriod) { return null }

    @Override
    boolean requiresStartDate() { return false }

    @Override
    String getName() { "${modelName}" + obj.toString()}
}
"""

    void setUp() {
        File input = new File(FileConstants.TEMP_FILE_DIRECTORY + "/src/some/packagename")
        input.deleteDir()
        input.mkdirs()

        File objectFile = new File(input, "SomeObject.groovy")
        BufferedWriter writer = objectFile.newWriter("UTF-8")
        writer.write(objectCode)
        writer.close()

        File modelFile = new File(input, "TestModel.groovy")
        writer = modelFile.newWriter("UTF-8")
        writer.write(modelCode)
        writer.close()


        File output = new File(FileConstants.TEMP_FILE_DIRECTORY + "/compiler")
        output.deleteDir()
        output.mkdirs()

        CompilerConfiguration compilerConfiguration = CompilerConfiguration.DEFAULT
        compilerConfiguration.targetDirectory = output
        Compiler compiler = new Compiler(compilerConfiguration)
        compiler.compile([objectFile, modelFile] as File[])

        File model = new File(output, "some/packagename/TestModel.class")
        assertNotNull new ClassRepository(name: "some.packagename.TestModel", data: model.bytes).save()

        File object = new File(output, "some/packagename/SomeObject.class")
        assertNotNull new ClassRepository(name: "some.packagename.SomeObject", data: object.bytes).save()
    }

    void testLoadClass() {
        ClassLoader currentLoader = Thread.currentThread().contextClassLoader

        shouldFail {
            currentLoader.loadClass("some.packagename.TestModel")
        }

        ClassLoader dbLoader = new DatabaseClassLoader(currentLoader)
//        Thread.currentThread().setContextClassLoader(dbLoader)


        Class c = dbLoader.loadClass("some.packagename.TestModel")
        Model model = c.newInstance()
        model.init()

        assertEquals modelName + someObjectName, model.name
    }

    void testDeploy() {
        ClassLoader dbLoader = new DatabaseClassLoader(Thread.currentThread().contextClassLoader)
        Class c = dbLoader.loadClass("some.packagename.TestModel")
        graphPersistenceService.deployClass(c)
        Set<Class> classes = ModelRegistry.instance.allModelClasses
        assertEquals "some.packagename.TestModel", classes.toList()[0].name
    }

}
