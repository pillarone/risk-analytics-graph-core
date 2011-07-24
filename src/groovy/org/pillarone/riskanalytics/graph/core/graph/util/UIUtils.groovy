package org.pillarone.riskanalytics.graph.core.graph.util

/**
 * @author fouad.jaada@intuitive-collaboration.com
 */

//todo fja moving this class to core
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
}
