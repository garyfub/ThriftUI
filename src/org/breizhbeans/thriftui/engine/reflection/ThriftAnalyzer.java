package org.breizhbeans.thriftui.engine.reflection;

import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Give it a jar of compiled thrift classes, and maybe you'll get :
 * - the namespace
 * - the structures
 * - the services
 * - the exceptions
 * <p/>
 * Created with IntelliJ IDEA.
 * User: Pascal.Lombard
 * Date: 18/11/12
 * Time: 15:43
 * To change this template use File | Settings | File Templates.
 */
public class ThriftAnalyzer {

    /**
     * As the name implies, return an object with every type of thrift member
     *
     * @param jar
     * @return
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     */
    public static ParsedThrift findClassesInJar(JarFile jar) throws ClassNotFoundException, NoSuchMethodException {
        ParsedThrift parsedThrift = new ParsedThrift();

        // open jar, get every directory (package)
        // which is *not* META-INF
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            // retain only pure POJO generated by thrift (structs, services, exceptions)
            if (name.endsWith(".class") && !name.contains("$")) {
                name = name.substring(0, name.length() - 6);
                name = name.replaceAll("/", ".");

                Class<?> classToTest = Class.forName(name);

                if (parsedThrift.namespace == null) {
                    parsedThrift.namespace = classToTest.getPackage().getName();
                }
                // structures -> has no getStackTrace() method and a validate() method
                // exceptions -> has a getStackTrace() method and a validate() method
                // services -> has no validate() method
                try {
                    classToTest.getMethod("validate", (Class<?>[]) null);
                } catch (NoSuchMethodException nsme) {
                    // -> service class
                    parsedThrift.services.put(classToTest.getSimpleName(), classToTest);
                    continue;
                }
                try {
                    classToTest.getMethod("getStackTrace", (Class<?>[]) null);
                    parsedThrift.exceptions.put(classToTest.getSimpleName(), classToTest);
                } catch (NoSuchMethodException nsme) {
                    parsedThrift.structures.put(classToTest.getSimpleName(), classToTest);
                }
            }
        }

        return parsedThrift;
    }

}
