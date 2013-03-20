package org.pillarone.riskanalytics.graph.core.graphexport.util

import java.util.jar.JarOutputStream
import java.util.jar.JarEntry

public class JarCreatorUtil {

    public static byte[] createJar(Map<String, byte[]> classes) {
        String fname = org.pillarone.riskanalytics.core.FileConstants.TEMP_FILE_DIRECTORY + File.separator + System.currentTimeMillis() + ".jar";
        JarOutputStream jarOutput = new JarOutputStream(new FileOutputStream(fname));

        for (String name: classes.keySet()) {
            jarOutput.putNextEntry(new JarEntry(name.replace('.', '/') + ".class"));
            jarOutput.write(classes.get(name));
            jarOutput.closeEntry();
        }
        jarOutput.close();

        FileInputStream fin = new FileInputStream(fname);
        byte[] content = new byte[fin.available()];
        fin.read(content);
        fin.close();
        new File(fname).delete();
        return content;
    }
}
