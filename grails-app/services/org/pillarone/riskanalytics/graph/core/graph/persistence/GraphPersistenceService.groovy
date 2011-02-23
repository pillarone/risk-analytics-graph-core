package org.pillarone.riskanalytics.graph.core.graph.persistence

import org.pillarone.riskanalytics.graph.core.graph.model.AbstractGraphModel
import org.pillarone.riskanalytics.graph.core.graph.model.ComponentNode
import org.pillarone.riskanalytics.graph.core.graph.model.Connection
import org.pillarone.riskanalytics.graph.core.graph.model.Port

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
        model.save(flush: true)
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
}
