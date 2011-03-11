package org.pillarone.riskanalytics.graph.core.graph.util

import java.lang.reflect.Field
import org.pillarone.riskanalytics.graph.core.palette.annotations.WiringValidation

public class WiringValidationUtil {

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
}
