package org.pillarone.riskanalytics.graph.core.graph.undosupport

import javax.swing.undo.AbstractUndoableEdit
import org.pillarone.riskanalytics.graph.core.graph.model.ComponentNode
import org.pillarone.riskanalytics.graph.core.graph.model.Connection
import org.pillarone.riskanalytics.graph.core.graph.model.AbstractGraphModel

public class NodeAddRemoveUndo extends AbstractUndoableEdit {
    private ComponentNode componentNode;
    private List<Connection> connections;
    private UndoOperation undoOperation;
    private AbstractGraphModel model;

    public NodeAddRemoveUndo(AbstractGraphModel model,ComponentNode componentNode, List<Connection> connections, UndoOperation undoOperation) {
        this.model=model;
        this.componentNode = componentNode;
        this.connections = connections;
        this.undoOperation = undoOperation;
    }

    @Override
    void redo() {
        super.redo()
        switch (undoOperation) {
            case UndoOperation.ADD: addNode(); break;
            case UndoOperation.REMOVE: model.removeComponentNode(componentNode);; break;
        }
    }

    @Override
    void undo() {
        super.undo()
        switch (undoOperation) {
            case UndoOperation.ADD: model.removeComponentNode(componentNode);; break;
            case UndoOperation.REMOVE: addNode(); break;
        }
    }

    private void addNode() {
        model.addComponentNode(componentNode);
        if (connections!=null){
            for (Connection c:connections){
                model.createConnection(c.from,c.to);
            }
        }
    }

}
