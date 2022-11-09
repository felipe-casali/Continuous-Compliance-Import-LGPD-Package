/*
 * Copyright (c) 2019, 2020 by Delphix. All rights reserved.
 */
package sample.masking.algorithm.redaction;

import com.delphix.masking.api.plugin.MaskingAlgorithm;
import java.math.BigDecimal;
import javax.annotation.Nullable;

/** Redact a number by replacing it with the value 42. */
public class NumberRedaction implements MaskingAlgorithm<BigDecimal> {
    private static final BigDecimal REDACTION_VALUE = new BigDecimal(42);

    @Override
    public String getName() {
        return "Number Redaction";
    }

    @Override
    public String getDescription() {
        return "Redact number by replacing it with 42";
    }

    @Override
    public BigDecimal mask(@Nullable BigDecimal input) {
        if (input == null) {
            return null;
        }

        return REDACTION_VALUE;
    }
}
