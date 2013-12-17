package org.pillarone.riskanalytics.graph.core

import org.pillarone.riskanalytics.graph.core.loader.DatabaseClassLoader
import org.springframework.beans.BeansException
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory

class ClassLoaderInitializer implements BeanFactoryPostProcessor {

    @Override
    void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        Thread.currentThread().contextClassLoader = new DatabaseClassLoader(Thread.currentThread().contextClassLoader)
    }
}
