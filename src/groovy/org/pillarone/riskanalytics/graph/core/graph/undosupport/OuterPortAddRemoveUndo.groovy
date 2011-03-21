package org.pillarone.riskanalytics.graph.core.graph.undosupport

import org.pillarone.riskanalytics.graph.core.graph.model.Connection
import org.pillarone.riskanalytics.graph.core.graph.model.Port
import javax.swing.undo.AbstractUndoableEdit
import org.pillarone.riskanalytics.graph.core.graph.model.ComposedComponentGraphModel

public class OuterPortAddRemoveUndo extends AbstractUndoableEdit {

    private ComposedComponentGraphModel model;
    private Port port;
    private List<Connection> connections;
    private UndoOperation undoOperation;


    OuterPortAddRemoveUndo(ComposedComponentGraphModel model, Port port, List<Connection> connections, UndoOperation undoOperation) {
        this.model = model
        this.port = port
        this.connections = connections
        this.undoOperation = undoOperation
    }

    @Override
    void undo() {
        super.undo()
        switch (undoOperation) {
            case UndoOperation.ADD: model.removeOuterPort(port); break;
            case UndoOperation.REMOVE: addOuterPort(); break;
        }
    }

    @Override
    void redo() {
        super.redo()
        switch (undoOperation) {
            case UndoOperation.ADD: addOuterPort(); break;
            case UndoOperation.REMOVE: model.removeOuterPort(port); break;
        }
    }

    private void addOuterPort() {
        model.addOuterPort(port);
        if (connections != null) {
            for (Connection c: connections) {
                model.createConnection(c.from, c.to);
            }
        }
    }


}
