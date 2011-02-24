package org.pillarone.riskanalytics.graph.core.graph.persistence

import org.pillarone.riskanalytics.graph.core.graph.model.AbstractGraphModel
import org.pillarone.riskanalytics.graph.core.graph.model.ComponentNode
import org.pillarone.riskanalytics.graph.core.graph.model.Connection
import org.pillarone.riskanalytics.graph.core.graph.model.Port
import org.springframework.dao.DataAccessException

class GraphPersistenceService {

    void save(AbstractGraphModel graphModel) {
        GraphModel model = new GraphModel(name: graphModel.name, packageName: graphModel.packageName)

        for (ComponentNode node in graphModel.allComponentNodes) {
            model.addToNodes(createNode(node))
        }

        for (Connection connection in graphModel.allConnections) {
            model.addToEdges(
                    new Edge(
                            from: find(model, connection.from.componentNode.name, connection.from.name),
                            to: find(model, connection.to.componentNode.name, connection.to.name)
                    )
            )
        }
        try {
            if (!model.save(flush: true)) {
                throw new GraphPersistenceException(model.errors.toString())
            }
            graphModel.id = model.id
        } catch (DataAccessException e) {
            throw new GraphPersistenceException(e.message, e)
        }
    }

    protected Node createNode(ComponentNode componentNode) {
        Node node = new Node(name: componentNode.name, className: componentNode.type.typeClass.name)
        for (Port port in componentNode.inPorts) {
            node.addToPorts(new NodePort(name: port.name, packetClass: port.packetType.name))
        }

        for (Port port in componentNode.outPorts) {
            node.addToPorts(new NodePort(name: port.name, packetClass: port.packetType.name))
        }
        return node
    }

    protected NodePort find(GraphModel model, String nodeName, String portName) {
        Node node = model.nodes.find {it.name == nodeName}
        return node.ports.find {it.name == portName}
    }

    void delete(AbstractGraphModel graphModel) {
        Long id = graphModel.id
        if (id == null) {
            throw new GraphPersistenceException("Delete failed. Model ${graphModel} is not persisted.")
        }
        GraphModel model = GraphModel.get(id)
        if (model == null) {
            throw new GraphPersistenceException("Delete failed. No entity with id ${id} found.")
        }
        try {
            model.delete(flush: true)
            graphModel.id = null
        } catch (DataAccessException e) {
            throw new GraphPersistenceException(e.message, e)
        }
    }
}
