import org.pillarone.riskanalytics.graph.core.loader.DatabaseClassLoader

class GraphCoreBootStrap {

    def init = {servletContext ->
        Thread.currentThread().contextClassLoader = new DatabaseClassLoader(Thread.currentThread().contextClassLoader)
    }

    def destroy = {
    }
}