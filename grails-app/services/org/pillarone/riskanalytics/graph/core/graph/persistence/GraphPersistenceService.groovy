package org.pillarone.riskanalytics.graph.core.graph.persistence

import org.pillarone.riskanalytics.graph.core.graph.model.AbstractGraphModel
import org.pillarone.riskanalytics.graph.core.graph.model.ComponentNode
import org.pillarone.riskanalytics.graph.core.graph.model.Connection
import org.pillarone.riskanalytics.graph.core.graph.model.Port
import org.springframework.dao.DataAccessException
import org.pillarone.riskanalytics.graph.core.graph.model.ComposedComponentGraphModel
import org.pillarone.riskanalytics.graph.core.graph.model.ModelGraphModel
import org.pillarone.riskanalytics.graph.core.palette.service.PaletteService

import org.pillarone.riskanalytics.core.model.registry.ModelRegistry
import org.pillarone.riskanalytics.graph.core.layout.GraphLayout
import org.pillarone.riskanalytics.core.user.UserManagement
import org.pillarone.riskanalytics.graph.core.layout.ComponentLayout
import org.pillarone.riskanalytics.graph.core.layout.EdgeLayout
import java.awt.Point
import org.pillarone.riskanalytics.graph.core.layout.ControlPoint
import java.awt.Rectangle

class GraphPersistenceService {

    protected PaletteService paletteService = PaletteService.getInstance()

    void save(AbstractGraphModel graphModel) {
        final GraphModel model = findOrCreateGraphModel(graphModel)
        final GraphLayout layout = findOrCreateGraphLayout(model)
        removeExistingEntities(model, layout)

        for (ComponentNode componentNode in graphModel.allComponentNodes) {
            final Node node = createNode(componentNode)
            model.addToNodes(node)
            addLayoutInfo(layout, componentNode, node)
        }

        for (Connection connection in graphModel.allConnections) {
            final Edge edge = new Edge(
                    from: "${connection.from.componentNode.name}.${connection.from.name}",
                    to: "${connection.to.componentNode.name}.${connection.to.name}"
            )
            model.addToEdges(edge)

            addLayoutInfo(layout, connection, edge)
        }
        doTypeSpecificMapping(graphModel, model)
        try {
            if (!model.save(flush: true)) {
                throw new GraphPersistenceException(model.errors.toString())
            }
            if (!layout.save(flush: true)) {
                throw new GraphPersistenceException(layout.errors.toString())
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

        }
        model.typeClass = graphModel.class.name
        return model
    }

    protected GraphLayout findOrCreateGraphLayout(GraphModel model) {
        GraphLayout layout = null

        if (model.id != null) {
            layout = GraphLayout.findByPersonAndGraphModel(org.pillarone.riskanalytics.core.user.UserManagement.currentUser, model)
        }

        if (layout == null) {
            layout = new GraphLayout(graphModel: model, person: UserManagement.currentUser)
        }

        return layout
    }

    protected removeExistingEntities(GraphModel model, GraphLayout layout) {
        if (layout.components != null) {
            Set<ComponentLayout> allNodes = new HashSet<ComponentLayout>(layout.components)
            for (ComponentLayout toRemove in allNodes) {
                layout.removeFromComponents(toRemove)
                toRemove.delete()
            }
        }
        if (layout.edges != null) {
            Set<EdgeLayout> allNodes = new HashSet<EdgeLayout>(layout.edges)
            for (EdgeLayout toRemove in allNodes) {
                layout.removeFromEdges(toRemove)
                toRemove.delete()
            }
        }

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

    protected void addLayoutInfo(GraphLayout layout, ComponentNode componentNode, Node node) {
        layout.addToComponents(new ComponentLayout(
                node: node,
                x: componentNode?.rectangle?.x,
                y: componentNode?.rectangle?.y,
                width: componentNode?.rectangle?.width,
                height: componentNode?.rectangle?.height
        ))
    }

    protected void addLayoutInfo(GraphLayout layout, Connection connection, Edge edge) {
        EdgeLayout edgeLayout = new EdgeLayout(edge: edge)
        if (connection.controlPoints != null) {
            for (Point point in connection.controlPoints) {
                edgeLayout.addToPoints(new ControlPoint(x: point.x, y: point.y))
            }
        }

        layout.addToEdges(edgeLayout)

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
            GraphLayout.findAllByGraphModel(model)*.delete(flush: true)
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

    AbstractGraphModel load(String name, String packageName) {
        GraphModel model = GraphModel.findByNameAndPackageName(name, packageName)
        if (model == null) {
            throw new GraphPersistenceException("No model found with name $name in package $packageName")
        }

        return load(model)
    }

    List<AbstractGraphModel> loadAll() {
        return GraphModel.list().collect { load(it) }
    }

    protected AbstractGraphModel load(GraphModel model) {
        List<ComponentNode> createdNodes = []
        final GraphLayout layout = findOrCreateGraphLayout(model)

        AbstractGraphModel graphModel = getClass().getClassLoader().loadClass(model.typeClass).newInstance()
        graphModel.name = model.name
        graphModel.packageName = model.packageName
        graphModel.id = model.id

        for (Node node in model.nodes) {
            final ComponentNode componentNode = graphModel.createComponentNode(paletteService.getComponentDefinition(node.className), node.name)
            ComponentLayout componentLayout = layout.components.find { it.node.id == node.id }
            if (componentLayout != null) {
                componentNode.setRectangle(new Rectangle(componentLayout.x, componentLayout.y, componentLayout.width, componentLayout.height))
            }
            createdNodes << componentNode
        }
        for (Edge edge in model.edges) {
            ComponentNode fromNode = createdNodes.find { it.name == edge.from.split("\\.")[0] }
            ComponentNode toNode = createdNodes.find { it.name == edge.to.split("\\.")[0] }
            final Connection connection = graphModel.createConnection(fromNode.getPort(edge.from.split("\\.")[1]), toNode.getPort(edge.to.split("\\.")[1]))
            EdgeLayout edgeLayout = layout.edges.find { it.edge.id == edge.id }
            if (edgeLayout != null) {
                List<Point> points = []
                for (ControlPoint controlPoint in edgeLayout.points) {
                    points << new Point(controlPoint.x, controlPoint.y)
                }
                connection.setControlPoints(points)
            }
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

    //TODO: probably belongs somewhere else
    void deployClass(Class modelClass) {
        ModelRegistry.instance.addModel(modelClass)
    }
}
