package org.pillarone.riskanalytics.graph.core.item

import org.pillarone.riskanalytics.core.simulation.item.Parameterization
import org.pillarone.riskanalytics.graph.core.graph.persistence.GraphParameterizationDAO
import org.pillarone.riskanalytics.core.simulation.item.VersionNumber
import org.pillarone.riskanalytics.core.ModelDAO
import org.pillarone.riskanalytics.core.simulation.item.parameter.ParameterHolder
import org.pillarone.riskanalytics.core.parameter.Parameter

/**
 * @author fouad.jaada@intuitive-collaboration.com
 */
class GraphParameterization extends Parameterization {

    private GraphParameterizationDAO graphParameterizationDAO

    public GraphParameterization(Map params) {
        super(params)
    }

    public GraphParameterization(String name) {
        super(name)
    }

    public GraphParameterization(String name, Class modelClass) {
        super(name, modelClass)
    }


    protected void mapToDao(Object dao) {
        dao = dao as GraphParameterizationDAO
        dao.itemVersion = versionNumber.toString()
        dao.name = name
        dao.periodCount = periodCount
        List periodDates = obtainPeriodLabelsFromParameters()
        if (periodDates != null) {
            periodLabels = periodDates
        }
        dao.periodLabels = periodLabels != null && !periodLabels.empty ? periodLabels.join(";") : null
        dao.creationDate = creationDate
        dao.modificationDate = modificationDate
        dao.valid = valid
        dao.creator = creator
        dao.lastUpdater = lastUpdater
        dao.comment = comment
        dao.status = status
        dao.dealId = dealId
        dao.valuationDate = valuationDate
        saveParameters(parameterHolders, dao.parameters, dao)
    }

    @Override
    public Object getDaoClass() {
        return GraphParameterizationDAO
    }

    @Override
    protected Object createDao() {
        return new GraphParameterizationDAO()
    }

    @Override
    protected void addToDao(Parameter parameter, Object dao) {
        dao = dao as GraphParameterizationDAO
        dao.addToParameters(parameter)
    }

    @Override
    public void setDao(def newDao) {
        graphParameterizationDAO = newDao
    }

    @Override
    public getDao() {
        if (graphParameterizationDAO?.id == null) {
            graphParameterizationDAO = createDao()
            return graphParameterizationDAO
        } else {
            return getDaoClass().get(graphParameterizationDAO.id)
        }
    }

    public static GraphParameterization toGraphParameterization(Parameterization parameterization) {
        GraphParameterization graphParameterization = new GraphParameterization(parameterization.name)
        return (GraphParameterization) copy(parameterization, graphParameterization)
    }

    public static Parameterization toParameterization(GraphParameterization graphParameterization) {
        Parameterization parameterization = new Parameterization(graphParameterization.name)
        return copy(graphParameterization, parameterization)
    }

    private static Parameterization copy(Parameterization source, Parameterization target) {
        target.versionNumber = source.versionNumber
        target.periodCount = source.periodCount
        target.periodLabels = source.periodLabels
        target.creationDate = source.creationDate
        target.modificationDate = source.modificationDate
        target.valid = source.valid
        target.creator = source.creator
        target.lastUpdater = source.lastUpdater
        target.status = source.status
        target.dealId = source.dealId
        target.comment = source.comment

        for (ParameterHolder parameterHolder: source.getParameterHolders()) {
            target.addParameter((ParameterHolder) parameterHolder.clone())
        }
        return target
    }


}
