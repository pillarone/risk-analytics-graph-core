package org.pillarone.riskanalytics.graph.core.graph.persistence

import org.pillarone.riskanalytics.graph.core.graph.model.AbstractGraphModel
import org.pillarone.riskanalytics.graph.core.graph.model.ComponentNode
import org.pillarone.riskanalytics.graph.core.graph.model.Connection
import org.pillarone.riskanalytics.graph.core.graph.model.Port
import org.springframework.dao.DataAccessException
import org.pillarone.riskanalytics.graph.core.graph.model.ComposedComponentGraphModel
import org.pillarone.riskanalytics.graph.core.graph.model.ModelGraphModel
import org.pillarone.riskanalytics.graph.core.palette.service.PaletteService

class GraphPersistenceService {

    protected PaletteService paletteService = PaletteService.getInstance()

    void save(AbstractGraphModel graphModel) {
        GraphModel model = findOrCreateGraphModel(graphModel)

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
        doTypeSpecificMapping(graphModel, model)
        try {
            if (!model.save(flush: true)) {
                throw new GraphPersistenceException(model.errors.toString())
            }
            graphModel.id = model.id
        } catch (DataAccessException e) {
            throw new GraphPersistenceException(e.message, e)
        }
    }

    protected GraphModel findOrCreateGraphModel(AbstractGraphModel graphModel) {
        GraphModel model
        if (graphModel.id == null) {
            model = new GraphModel(name: graphModel.name, packageName: graphModel.packageName)
        } else {
            model = GraphModel.get(graphModel.id)
            model.name = graphModel.name
            model.packageName = graphModel.packageName

            if (model.nodes != null) {
                Set<Node> allNodes = new HashSet<Node>(model.nodes)
                for (Node toRemove in allNodes) {
                    model.removeFromNodes(toRemove)
                    toRemove.delete()
                }
            }

            if (model.edges != null) {
                Set<Edge> allEdges = new HashSet<Edge>(model.edges)
                for (Edge toRemove in allEdges) {
                    model.removeFromEdges(toRemove)
                    toRemove.delete()
                }
            }

            if (model.ports != null) {
                Set<ComponentPort> allPorts = new HashSet<ComponentPort>(model.ports)
                for (ComponentPort toRemove in allPorts) {
                    model.removeFromPorts(toRemove)
                    toRemove.delete()
                }
            }

        }
        model.typeClass = graphModel.class.name
        return model
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

    protected void doTypeSpecificMapping(AbstractGraphModel graphModel, GraphModel model) { }

    protected void doTypeSpecificMapping(ComposedComponentGraphModel graphModel, GraphModel model) {
        for (Port port in graphModel.outerInPorts) {
            model.addToPorts(new ComponentPort(name: port.name, packetClass: port.packetType.name))
        }

        for (Port port in graphModel.outerOutPorts) {
            model.addToPorts(new ComponentPort(name: port.name, packetClass: port.packetType.name))
        }
    }

    protected void doTypeSpecificMapping(ModelGraphModel graphModel, GraphModel model) {
        for (ComponentNode startComponent in graphModel.startComponents) {
            model.nodes.find { it.name == startComponent.name }.startComponent = true
        }
    }

    AbstractGraphModel load(long id) {
        GraphModel model = GraphModel.get(id)
        if (model == null) {
            throw new GraphPersistenceException("No model found with id $id")
        }

        return load(model)
    }

    List<AbstractGraphModel> loadAll() {
        return GraphModel.list().collect { load(it) }
    }

    protected AbstractGraphModel load(GraphModel model) {
        List<ComponentNode> createdNodes = []

        AbstractGraphModel graphModel = getClass().getClassLoader().loadClass(model.typeClass).newInstance()
        graphModel.name = model.name
        graphModel.packageName = model.packageName
        graphModel.id = model.id

        for (Node node in model.nodes) {
            createdNodes << graphModel.createComponentNode(paletteService.getComponentDefinition(node.className), node.name)
        }
        for (Edge edge in model.edges) {
            ComponentNode fromNode = createdNodes.find { it.name == edge.from.node.name }
            ComponentNode toNode = createdNodes.find { it.name == edge.to.node.name }
            graphModel.createConnection(fromNode.getPort(edge.from.name), toNode.getPort(edge.to.name))
        }
        doTypeSpecificLoading(graphModel, model)
        return graphModel
    }

    protected void doTypeSpecificLoading(AbstractGraphModel graphModel, GraphModel model) {}

    protected void doTypeSpecificLoading(ModelGraphModel graphModel, GraphModel model) {
        for (Node node in model.nodes) {
            if (node.startComponent) {
                graphModel.startComponents << graphModel.allComponentNodes.find { it.name == node.name }
            }
        }
    }

    protected void doTypeSpecificLoading(ComposedComponentGraphModel graphModel, GraphModel model) {
        for (ComponentPort port in model.ports) {
            String portName = port.name
            if (portName.startsWith("in")) {
                graphModel.createOuterInPort(getClass().getClassLoader().loadClass(port.packetClass), portName)
            } else if (portName.startsWith("out")) {
                graphModel.createOuterOutPort(getClass().getClassLoader().loadClass(port.packetClass), portName)
            } else {
                throw new GraphPersistenceException("Channels must start with 'in' or 'out' - Found ${portName}")
            }
        }
    }
}
