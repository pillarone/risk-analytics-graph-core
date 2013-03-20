package org.pillarone.riskanalytics.graph.core.palette.model

import java.lang.reflect.Field
import java.lang.reflect.Type
import java.lang.reflect.ParameterizedType
import org.pillarone.riskanalytics.core.components.Component
import org.pillarone.riskanalytics.core.packets.Packet
import org.pillarone.riskanalytics.core.packets.PacketList

class ComponentDefinition {

    Class typeClass

    @Override
    String toString() {
        typeClass?.name
    }

    String getSimpleName() {
        typeClass?.simpleName
    }

    public static Comparator<ComponentDefinition> getComparator() {
        return new ComponentDefinitionComparator<ComponentDefinition>()
    }

    /**
     * compare ComponentDefinition by class simpleName
     * @param < T >
     */
    private static class ComponentDefinitionComparator<T extends ComponentDefinition> implements Comparator {

        int compare(Object o1, Object o2) {
            return compare((ComponentDefinition) o1, (ComponentDefinition) o2)
        }

        int compare(ComponentDefinition o1, ComponentDefinition o2) {
            o1.getSimpleName().compareToIgnoreCase(o2.getSimpleName())
        }

    }

    public static Map<Field, Class> getPortDefinitions(ComponentDefinition definition, String prefix) {
        Map<Field, Class> result = [:]
        Class currentClass = definition.typeClass
        while (currentClass != Component.class) {
            for (Field field in currentClass.declaredFields) {
                if (field.name.startsWith(prefix) && PacketList.isAssignableFrom(field.type)) {
                    Class packetType = Packet
                    Type genericType = field.genericType
                    if (genericType instanceof ParameterizedType) {
                        packetType = genericType.actualTypeArguments[0]
                    }
                    result.put(field, packetType)
                }
            }
            currentClass = currentClass.superclass
        }
        return result
    }

}
