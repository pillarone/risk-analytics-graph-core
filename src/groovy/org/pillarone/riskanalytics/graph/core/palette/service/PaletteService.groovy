package org.pillarone.riskanalytics.graph.core.palette.service

import org.pillarone.riskanalytics.graph.core.palette.model.ComponentDefinition
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AssignableTypeFilter
import org.pillarone.riskanalytics.core.components.Component
import org.pillarone.riskanalytics.graph.core.palette.Palette
import org.pillarone.riskanalytics.graph.core.palette.PaletteEntry
import org.pillarone.riskanalytics.core.components.ComponentCategory
import org.pillarone.riskanalytics.graph.core.loader.ClassRepository
import org.pillarone.riskanalytics.graph.core.loader.ClassType
import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.Log
import org.pillarone.riskanalytics.graph.core.palette.service.filter.IPaletteFilter
import org.pillarone.riskanalytics.graph.core.palette.service.filter.NoFilter
import org.codehaus.groovy.grails.commons.ConfigurationHolder


class PaletteService {

    private static Log LOG = LogFactory.getLog(PaletteService)
    private static IPaletteServiceAccessor serviceAccessor

    private List<ComponentDefinition> cache
    private Map<String, List<ComponentDefinition>> categoryCache = new HashMap<String, List<ComponentDefinition>>();
    public static final String CAT_OTHER = "Others";

    private List<IPaletteServiceListener> listeners = []

    IPaletteFilter paletteFilter = new NoFilter()

    PaletteService() {
        initCache();
        if(ConfigurationHolder.config.containsKey("paletteFilter")) {
            paletteFilter = ConfigurationHolder.config.get("paletteFilter")
        }
    }

    public static PaletteService getInstance() {
        if (serviceAccessor == null) {
            ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false)
            provider.addIncludeFilter(new AssignableTypeFilter(org.pillarone.riskanalytics.graph.core.palette.service.IPaletteServiceAccessor))
            List<Class> classes = provider.findCandidateComponents("org.pillarone")*.beanClassName.collect {
                Thread.currentThread().getContextClassLoader().loadClass(it)
            }
            if (classes.empty) {
                throw new IllegalStateException("No IPaletteServiceAccessor found on classpath.")
            }

            Class accessorClass = classes.find { !it.name.startsWith("org.pillarone.riskanalytics.graph.core") }
            if (accessorClass == null) {
                accessorClass = classes[0]
            }

            serviceAccessor = accessorClass.newInstance()
        }

        return serviceAccessor.obtainService()
    }

    List<ComponentDefinition> getAllComponentDefinitions() {
        return Collections.unmodifiableList(cache)
    }

    private void initCache() {
        List<ComponentDefinition> cache
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false)
        provider.addIncludeFilter(new AssignableTypeFilter(Component))
        cache = provider.findCandidateComponents("org.pillarone")*.beanClassName.collect {
            new ComponentDefinition(typeClass: Thread.currentThread().getContextClassLoader().loadClass(it))
        }.findAll { paletteFilter.accept(it) }
        cache.addAll(ClassRepository.findAllByClassType(ClassType.COMPONENT).collect {
            new ComponentDefinition(typeClass: Thread.currentThread().getContextClassLoader().loadClass(it.name))
        }.findAll { paletteFilter.accept(it) })
        for (ComponentDefinition definition: cache) {
            addToCategoryInternal(definition);
        }
        Collections.sort(cache, ComponentDefinition.getComparator())
        this.cache = cache
    }

    ComponentDefinition getComponentDefinition(Class clazz) {
        return getAllComponentDefinitions().find { it.typeClass == clazz}
    }

    ComponentDefinition getComponentDefinition(String className) {
        return getComponentDefinition(Thread.currentThread().getContextClassLoader().loadClass(className))
    }

    protected void addToCategoryInternal(ComponentDefinition definition) {
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

    public void reset() {
        cache.clear()
        categoryCache.clear();
        initCache()
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

    void addComponentDefinition(ComponentDefinition definition) {
        if (paletteFilter.accept(definition)) {
            cache.add(definition)
            Collections.sort(cache, org.pillarone.riskanalytics.graph.core.palette.model.ComponentDefinition.getComparator())
            addToCategoryInternal(definition)
            fireComponentAdded(definition)
        }
    }

    void addPaletteServiceListener(IPaletteServiceListener listener) {
        listeners << listener
    }

    void removePaletteServiceListener(IPaletteServiceListener listener) {
        listeners.remove(listener)
    }

    protected void fireComponentAdded(ComponentDefinition definition) {
        listeners*.componentDefinitionAdded(definition)
    }
}
