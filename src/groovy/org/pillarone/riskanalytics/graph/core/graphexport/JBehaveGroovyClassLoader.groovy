package org.pillarone.riskanalytics.graph.core.graphexport

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.SourceUnit;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

/**
 * Groovy does not cache the bytecode sequences for generated classes.
 * BytecodeReadingParanamer needs these to get paramater names from classes
 * The Groovy compiler does create the debug tables, and they are the same as the
 * ones made for a native Java class, so this derived GroovyClassLoader fills in
 * for the missing functionality from the base GroovyClassLoader.
 *
 * There is a mechanism to set a system property that would force Groovy's internals
 * to write out bytecode to a (temp) directory, but who want's to have to do that,
 * and clean up temp directories after a build run. Assuming that was not slower
 * anyway.
 */
public class JBehaveGroovyClassLoader extends GroovyClassLoader {

    private Map<String, byte[]> classBytes = new HashMap<String, byte[]>();

    @Override
    public InputStream getResourceAsStream(String name) {
        String nosuffix=name.replaceAll('.class$',"");
        if (classBytes.containsKey(nosuffix)) {
            return new ByteArrayInputStream(classBytes.get(name));
        }
        return super.getResourceAsStream(name);
    }

    public Map<String, byte[]> getClassBytes() {
        return classBytes;
    }

    @Override
    protected GroovyClassLoader.ClassCollector createCollector(CompilationUnit unit, SourceUnit su) {
        // These six lines copied from Groovy itself, with the intention to return a subclass
        GroovyClassLoader.InnerLoader loader = AccessController.doPrivileged(new PrivilegedAction<GroovyClassLoader.InnerLoader>() {
            public GroovyClassLoader.InnerLoader run() {
                return new GroovyClassLoader.InnerLoader(JBehaveGroovyClassLoader.this);
            }
        });
        return new JBehaveClassCollector(classBytes, loader, unit, su);
    }

    public static class JBehaveClassCollector extends GroovyClassLoader.ClassCollector {
        private final Map<String, byte[]> classBytes;

        public JBehaveClassCollector(Map<String, byte[]> classBytes, GroovyClassLoader.InnerLoader loader, CompilationUnit unit, SourceUnit su) {
            super(loader, unit, su);
            this.classBytes = classBytes;
        }

        /*@Override
        protected Class onClassNode(ClassWriter classWriter, ClassNode classNode) {
            classBytes.put(classNode.getName() + ".class", classWriter.toByteArray());
            return super.onClassNode(classWriter, classNode);
        }*/

        protected Class createClass(byte[] code, ClassNode classNode) {
            classBytes.put(classNode.getName(), code);
            return super.createClass(code, classNode)
        }
    }

}
