package org.pillarone.riskanalytics.graph.core.graph.model

import org.pillarone.riskanalytics.core.example.packet.TestPacket

import org.pillarone.riskanalytics.graph.core.palette.service.PaletteService
import org.pillarone.riskanalytics.core.example.component.TestComponent
import org.pillarone.riskanalytics.graph.core.palette.model.ComponentDefinition
import javax.swing.undo.UndoManager
import org.pillarone.riskanalytics.graph.core.graph.undosupport.NodeAddRemoveUndo
import org.pillarone.riskanalytics.graph.core.graph.undosupport.UndoOperation
import org.pillarone.riskanalytics.graph.core.graph.undosupport.OuterPortAddRemoveUndo
import org.pillarone.riskanalytics.core.packets.Packet

class GraphUndoRedoTest extends GroovyTestCase {


    void testAvailablePorts() {
        UndoManager undoManager = new UndoManager();
        ComposedComponentGraphModel model = new ComposedComponentGraphModel()

        ComponentNode node = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "name")
        undoManager.addEdit(new NodeAddRemoveUndo(model, node, null, UndoOperation.ADD));
        ComponentNode node2 = model.createComponentNode(PaletteService.instance.getComponentDefinition(TestComponent), "name2")

        undoManager.undo();

        assertNull model.getAllComponentNodes().find {it.equals(node)};

        undoManager.redo();

        assertNotNull model.getAllComponentNodes().find {it.equals(node)};

        Connection c = model.createConnection(node2.getPort("outValue1"), node.getPort("input1"));

        undoManager.addEdit(new NodeAddRemoveUndo(model, node, model.getAllConnections().findAll {
            it.from.componentNode.equals(node) ||
                    it.to.componentNode.equals(node)
        }, UndoOperation.REMOVE));
        model.removeComponentNode(node);

        undoManager.undo();
        assertEquals node, model.getAllConnections()[0].to.componentNode;

        undoManager.redo();
        assertEquals 0, model.getAllConnections().size();

        Port p = model.createOuterInPort(Packet, "inTest1");
        undoManager.addEdit(new OuterPortAddRemoveUndo(model, p, null, UndoOperation.ADD));

        undoManager.undo();
        assertEquals 0, model.outerInPorts.size();

        undoManager.redo();

        assertEquals p, model.outerInPorts[0]

        model.createConnection(model.getOuterInPorts()[0], node.getPort("input1"));

        undoManager.addEdit(new OuterPortAddRemoveUndo(model, p, model.getAllConnections().findAll {it.from.equals(p) || it.to.equals(p)}, UndoOperation.REMOVE));
        model.removeOuterPort(p);

        undoManager.undo();
        assertEquals p, model.getAllConnections()[0].from;


        undoManager.redo();
        assertEquals 0, model.getAllConnections().size();

    }


}

