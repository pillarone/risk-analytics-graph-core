package org.pillarone.riskanalytics.graph.core.graph.util

import org.pillarone.riskanalytics.core.util.ResourceBundleRegistry
import java.text.MessageFormat

/**
 * @author fouad.jaada@intuitive-collaboration.com
 */

//todo this class will be deleted
// at the moment, it belongs to RA.
// for common using of this methods, a plugins with RA utilities will be created
//now it s just for test
class UIUtils {

    public static String formatDisplayName(String name) {
        if (name == null) {
            name = ""
        }

        if (name.startsWith("sub")) {
            return formatSubComponentName(name.substring(3))
        }
        if (name.startsWith("parm")) {
            name = name.substring(4)
        }
        if (name.startsWith("out")) {
            name = name.substring(3)
        }
        if (name.startsWith("in")) {
            name = name.substring(2)
        }

        return formatComponentName(name)
    }

    private static String formatComponentName(String value) {
        StringBuffer displayNameBuffer = new StringBuffer()
        value.eachWithIndex {String it, int index ->
            char c = -1
            if (index + 1 < value.length())
                c = value.charAt(index + 1)
            if (!it.equals(it.toLowerCase()) && c != -1 && c.equals(c.toLowerCase())) {
                if (index > 0 && index < value.length() - 1) {
                    displayNameBuffer << " " + it.toLowerCase()
                } else {
                    displayNameBuffer << ((index < value.length() - 1) ? it.toLowerCase() : it)
                }
            } else {
                displayNameBuffer << it
            }
        }
        return displayNameBuffer.toString()
    }

    private static String formatSubComponentName(String value) {
        StringBuffer displayNameBuffer = new StringBuffer()
        value = value.replaceAll("_", " ")
        value.getChars().eachWithIndex {Character it, int index ->
            char c = -1
            if (index + 1 < value.length())
                c = value.charAt(index + 1)
            if (it.isUpperCase() && c != -1 && c.isLowerCase()) {
                if (index > 0 && index < value.length() - 1) {
                    displayNameBuffer << " " + it
                } else {
                    displayNameBuffer << it
                }
            } else {
                displayNameBuffer << it
            }
        }
        value = displayNameBuffer.toString()
        return value.replaceAll("  ", " ")
    }

    static Locale getLocale() {
        return new Locale("en", "US")
    }

    static Set getBundles(String key) {
        def resourceBundle = []
        def resources = ResourceBundleRegistry.getBundles(key)
        for (String bundleName in resources) {
            resourceBundle << ResourceBundle.getBundle(bundleName, getLocale())
        }
        return resourceBundle
    }

    public static String getPropertyValue(Set bundles, String bundleKey, String arg) {
        if (!bundles)
            bundles = getBundles(bundleKey)
        return getTextByResourceBundles(bundles, arg)
    }

    private static String getTextByResourceBundles(Set bundles, String argument) {
        def keys = null
        String text = ""
        try {
            argument = argument.replaceAll("\n", "")
            for (ResourceBundle resourceBundle: bundles) {
                keys = (List) new GroovyShell().evaluate(argument)
                try {
                    text = resourceBundle.getString(keys[0])
                } catch (Exception ex) {}
                if (text) {
                    List args = []
                    keys.eachWithIndex {String key, int index ->
                        if (index > 0) {
                            args << key
                        }
                    }
                    if (args.size() > 0)
                        text = MessageFormat.format(text, args.toArray())
                }
            }
        } catch (Exception ex) { /*ignore the exception*/}
        return text;
    }
}
