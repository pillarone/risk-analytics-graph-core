package org.pillarone.riskanalytics.graph.core.graph.undosupport

import javax.swing.undo.AbstractUndoableEdit
import org.pillarone.riskanalytics.graph.core.graph.model.AbstractGraphModel
import org.pillarone.riskanalytics.graph.core.graph.model.Connection

public class ConnectionAddRemoveUndo extends AbstractUndoableEdit {

    private AbstractGraphModel model;
    private Connection connection;
    private UndoOperation undoOperation;


    ConnectionAddRemoveUndo(AbstractGraphModel model, Connection connection, UndoOperation undoOperation) {
        this.model = model
        this.connection = connection
        this.undoOperation = undoOperation
    }

    @Override
    void redo() {
        super.redo()
        switch (undoOperation) {
            case UndoOperation.ADD: model.createConnection(connection.from, connection.to); break;
            case UndoOperation.REMOVE: model.removeConnection(connection); break;
        }
    }

    @Override
    void undo() {
        super.undo()
        switch (undoOperation) {
            case UndoOperation.ADD: model.removeConnection(connection); break;
            case UndoOperation.REMOVE: model.createConnection(connection.from, connection.to); break;
        }
    }
}
