package org.pillarone.riskanalytics.graph.core.graph.util

import javax.swing.undo.UndoManager
import org.pillarone.riskanalytics.core.example.component.TestComponent
import org.pillarone.riskanalytics.core.packets.Packet
import org.pillarone.riskanalytics.graph.core.graph.model.ComponentNode
import org.pillarone.riskanalytics.graph.core.graph.model.ComposedComponentGraphModel
import org.pillarone.riskanalytics.graph.core.graph.model.Connection
import org.pillarone.riskanalytics.graph.core.graph.model.Port
import org.pillarone.riskanalytics.graph.core.graph.undosupport.NodeAddRemoveUndo
import org.pillarone.riskanalytics.graph.core.graph.undosupport.OuterPortAddRemoveUndo
import org.pillarone.riskanalytics.graph.core.graph.undosupport.UndoOperation
import org.pillarone.riskanalytics.graph.core.palette.service.PaletteService
import org.pillarone.riskanalytics.graph.core.graph.model.InPort
import org.pillarone.riskanalytics.graph.core.graph.model.OutPort

class UIUtilsTest extends GroovyTestCase {

    public void testFormatTechnicalName() {
        String techName = UIUtils.formatTechnicalName("a new component", ComponentNode.class, true)
        assertTrue(techName.equals("subANewComponent"))
        techName = UIUtils.formatTechnicalName("a new component", ComponentNode.class, false)
        assertTrue(techName.equals("aNewComponent"))

        techName = UIUtils.formatTechnicalName("the Port", InPort.class, true)
        assertTrue(techName.equals("inThePort"))
        techName = UIUtils.formatTechnicalName("the Port", InPort.class, false)
        assertTrue(techName.equals("inThePort"))
        techName = UIUtils.formatTechnicalName("the Port", OutPort.class, false)
        assertTrue(techName.equals("outThePort"))
    }

    public void testFormatTechnicalPortName() {
        String techName = UIUtils.formatTechnicalPortName("the Port", true)
        assertTrue(techName.equals("inThePort"))
        techName = UIUtils.formatTechnicalPortName("the Port", false)
        assertTrue(techName.equals("outThePort"))

    }

    public void testGetDisplayName() {
        String displayName = UIUtils.formatDisplayName("thePort")
        assertTrue(displayName.equals("the port"))

        displayName = UIUtils.formatDisplayName("inThePort")
        assertTrue(displayName.equals("the port"))

        displayName = UIUtils.formatDisplayName("outThePort")
        assertTrue(displayName.equals("the port"))

        displayName = UIUtils.formatDisplayName("subThisComponent")
        assertTrue(displayName.equals("this component"))

        displayName = UIUtils.formatDisplayName("parmThisComponent")
        assertTrue(displayName.equals("this component"))
    }
}

