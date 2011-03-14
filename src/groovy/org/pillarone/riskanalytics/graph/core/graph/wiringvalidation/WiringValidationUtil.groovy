package org.pillarone.riskanalytics.graph.core.graph.wiringvalidation

import java.lang.reflect.Field
import org.pillarone.riskanalytics.graph.core.palette.annotations.WiringValidation
import org.pillarone.riskanalytics.graph.core.graph.util.IntegerRange
import org.pillarone.riskanalytics.graph.core.graph.model.InPort
import org.pillarone.riskanalytics.graph.core.graph.model.AbstractGraphModel
import org.pillarone.riskanalytics.graph.core.graph.model.ComponentNode

public class WiringValidationUtil {

    private List<WiringValidationRule> rules = new ArrayList<WiringValidationRule>();

    public static IntegerRange getConnectionCardinality(Field field) {
        IntegerRange ir;
        WiringValidation wv = field.getAnnotation(WiringValidation);
        if (wv != null && wv.connections() != null) {
            ir = new IntegerRange(from: wv.connections()[0], to: wv.connections()[1])
        }
        return ir;
    }

    public static IntegerRange getPacketCardinality(Field field) {
        IntegerRange ir;
        WiringValidation wv = field.getAnnotation(WiringValidation);
        if (wv != null && wv.packets() != null) {
            ir = new IntegerRange(from: wv.packets()[0], to: wv.packets()[1])
        }
        return ir;
    }

    public static IntegerRange getEnclosingRange(List<IntegerRange> ranges) {
        if (ranges.empty)
            return null;
        IntegerRange ir = new IntegerRange(from: 0, to: Integer.MAX_VALUE);
        for (IntegerRange range: ranges) {
            ir.from = Math.max(ir.from, range.from);
            ir.to = Math.min(ir.to, range.to);
        }
        if (ir.from > ir.to)
            return null;

        return ir;
    }

    public WiringValidationUtil() {
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
