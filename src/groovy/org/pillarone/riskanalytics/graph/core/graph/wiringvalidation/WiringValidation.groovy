package org.pillarone.riskanalytics.graph.core.graph.wiringvalidation

import org.pillarone.riskanalytics.graph.core.graph.model.InPort
import org.pillarone.riskanalytics.graph.core.graph.model.AbstractGraphModel
import org.pillarone.riskanalytics.graph.core.graph.model.ComponentNode


public class WiringValidation {
    private List<WiringValidationRule> rules = new ArrayList<WiringValidationRule>();

    public WiringValidation() {
        rules.add(new WiringValidationRule() {
            boolean isValid(InPort port) {
                return port.connectionCardinality == null ? true :
                    (port.connectionCount >= port.connectionCardinality.from && port.connectionCount <= port.connectionCardinality.to);
            }

        });

    }

    public List<InPort> validateWiring(AbstractGraphModel graph) {
        List<InPort> notValidPorts = new ArrayList<InPort>();
        for (ComponentNode c: graph.getAllComponentNodes()) {
            for (InPort p: c.inPorts) {
                boolean valid = true;
                for (WiringValidationRule wvr: rules) {
                    valid = valid & wvr.isValid(p);
                }
                if (!valid) {
                    notValidPorts.add(p);
                }
            }
        }
        return notValidPorts;
    }
}

interface WiringValidationRule {
    public boolean isValid(InPort port)
}
