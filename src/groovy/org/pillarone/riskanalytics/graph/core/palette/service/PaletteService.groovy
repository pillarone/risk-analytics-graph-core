package org.pillarone.riskanalytics.graph.core.palette.service

import org.pillarone.riskanalytics.graph.core.palette.model.ComponentDefinition
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AssignableTypeFilter
import org.pillarone.riskanalytics.core.components.Component
import org.pillarone.riskanalytics.graph.core.palette.Palette
import org.pillarone.riskanalytics.graph.core.palette.PaletteEntry
import org.pillarone.riskanalytics.core.components.ComponentCategory


class PaletteService {

    private static PaletteService service
    private static List<ComponentDefinition> cache
    private Map<String, List<ComponentDefinition>> categoryCache = new HashMap<String, List<ComponentDefinition>>();
    public static String CAT_OTHER = "Others";

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
            for (ComponentDefinition definition: cache) {
                addToCategoryInternal(definition);
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

    public void addToCategoryInternal(ComponentDefinition definition) {
        ComponentCategory cc = definition.typeClass.getAnnotation(ComponentCategory);
        List<String> categories = new ArrayList<String>();

        if (cc != null && cc.categories() != null)
            categories.addAll(cc.categories());
        else
            categories.add(CAT_OTHER);

        for (String category: categories) {
            List<ComponentDefinition> definitions = categoryCache.get(category);
            if (definitions == null) {
                definitions = new ArrayList<ComponentDefinition>()
                categoryCache.put(category, definitions)
            }
            definitions.add(definition);
        }
    }

    public void addToCategory(String category, ComponentDefinition definition) {
        List<ComponentDefinition> definitions = categoryCache.get(category);
        if (definitions == null) {
            definitions = new ArrayList<ComponentDefinition>();
            categoryCache.put(category, definitions);
        }

        if (!definitions.find {it.typeClass.equals(definition.typeClass)}) {
            definitions.add(definition);
        }
    }

    public void clearCache(){
        cache=null;
        categoryCache.clear();
    }

    public List<ComponentDefinition> getDefinitionsFromCategory(String category) {
        return categoryCache.get(category);
    }

    public List<String> getCategoriesFromDefinition(ComponentDefinition definition) {
        private List<String> categories = new ArrayList<String>();
        for (String category: categoryCache.keySet()) {
            List<ComponentDefinition> definitions = categoryCache.get(category);
            if (definitions.find {it.typeClass.equals(definition.typeClass)}) {
                categories.add(category);
            }
        }
        return categories;
    }

    public void storeUserCategory(String name, long uid) {
        Palette palette;
        List<ComponentDefinition> definitions;
        if ((definitions = categoryCache.get(name)) == null)
            return;

        if ((palette = Palette.findByNameAndUserId(name, uid)) != null) {
            palette.entries.clear();
        } else {
            palette = new Palette(name: name, userId: uid);
        }

        for (ComponentDefinition cd: definitions) {
            palette.addToEntries(new PaletteEntry(type: cd.typeClass));
        }
        palette.save();
    }

    public void loadUserCategories(long uid) {
        List<Palette> palettes = Palette.findAllByUserId(uid);
        for (Palette p: palettes) {
            for (PaletteEntry pe: p.entries) {
                addToCategory(p.name, new ComponentDefinition(typeClass: pe.type));
            }
        }
    }

}
