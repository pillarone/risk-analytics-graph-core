package org.pillarone.riskanalytics.graph.core.palette.model

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

    private static class ComponentDefinitionComparator<T extends ComponentDefinition> implements Comparator {

        int compare(Object o1, Object o2) {
            return compare((ComponentDefinition) o1, (ComponentDefinition) o2);
        }

        int compare(ComponentDefinition o1, ComponentDefinition o2) {
            o1.getSimpleName().compareToIgnoreCase(o2.getSimpleName());
        }

    }


}
