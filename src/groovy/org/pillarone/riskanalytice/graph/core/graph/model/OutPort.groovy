package org.pillarone.riskanalytice.graph.core.graph.model

import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.Log


class OutPort extends Port {

    private static Log LOG = LogFactory.getLog(InPort)

    @Override
    protected boolean internalAllowedToConnectTo(Port port) {
        return doInternalAllowedToConnectTo(port)
    }

    private boolean doInternalAllowedToConnectTo(OutPort outPort) {
        return this.composedComponentOuterPort ? !outPort.composedComponentOuterPort : outPort.composedComponentOuterPort
    }

    private boolean doInternalAllowedToConnectTo(InPort inPort) {
        return this.composedComponentOuterPort ? false : !inPort.composedComponentOuterPort
    }

    private boolean doInternalAllowedToConnectTo(Port port) {
        LOG.warn("Unknown port found: ${port}")
        return false
    }

}
