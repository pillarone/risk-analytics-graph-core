package org.pillarone.riskanalytics.graph.core.loader;

import java.util.List;

public class DatabaseClassLoader extends ClassLoader {

    private List<String> availableClasses;

    public DatabaseClassLoader(ClassLoader parent) {
        super(parent);
    }

    @Override
    protected synchronized Class<?> findClass(String name) throws ClassNotFoundException {
        if (availableClasses == null) {
            try {
                refresh();
            } catch (Throwable e) {
                throw new ClassNotFoundException(name, e);
            }
        }
        if (!availableClasses.contains(name)) {
            throw new ClassNotFoundException(name);
        }

        String packageName = name.substring(0, name.lastIndexOf("."));
        if (getPackage(packageName) == null) {
            definePackage(packageName, null, null, null, null, null, null, null);
        }
        byte[] data = GroovyHelper.getClassDefinition(name);
        return defineClass(name, data, 0, data.length);

    }

    public synchronized void refresh() {
        availableClasses = GroovyHelper.getAllClassNames();
    }


}
