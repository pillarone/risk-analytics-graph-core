package org.pillarone.riskanalytics.graph.core.graph.model

import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.Log


class InPort extends Port {

    private static Log LOG = LogFactory.getLog(InPort)
    int connectionCount=0;

    @Override
    protected boolean internalAllowedToConnectTo(Port port) {
        return doInternalAllowedToConnectTo(port)
    }

    private boolean doInternalAllowedToConnectTo(OutPort outPort) {
        return this.composedComponentOuterPort ? false : !outPort.composedComponentOuterPort
    }

    private boolean doInternalAllowedToConnectTo(InPort inPort) {
        return this.composedComponentOuterPort ? !inPort.composedComponentOuterPort : inPort.composedComponentOuterPort
    }

    private boolean doInternalAllowedToConnectTo(Port port) {
        LOG.warn("Unknown port found: ${port}")
        return false
    }


}
