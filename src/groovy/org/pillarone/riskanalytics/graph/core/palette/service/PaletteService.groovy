package org.pillarone.riskanalytics.graph.core.palette.service

import org.pillarone.riskanalytics.graph.core.palette.model.ComponentDefinition
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AssignableTypeFilter
import org.pillarone.riskanalytics.core.components.Component


class PaletteService {

    private static PaletteService service
    private static List<ComponentDefinition> cache

    public static PaletteService getInstance() {
        if (service == null) {
            service = new PaletteService()
        }

        return service
    }

    List<ComponentDefinition> getAllComponentDefinitions() {
        if (cache == null) {
            ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false)
            provider.addIncludeFilter(new AssignableTypeFilter(Component))
            cache = provider.findCandidateComponents("org.pillarone")*.beanClassName.collect {
                new ComponentDefinition(typeClass: getClass().getClassLoader().loadClass(it))
            }
        }
        return cache
    }

    ComponentDefinition getComponentDefinition(Class clazz) {
        return getAllComponentDefinitions().find { it.typeClass == clazz}
    }

    ComponentDefinition getComponentDefinition(String className) {
        return getComponentDefinition(getClass().getClassLoader().loadClass(className))
    }
}
