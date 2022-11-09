/*
 * Copyright (c) 2019, 2020 by Delphix. All rights reserved.
 */
package sample.masking.algorithm.redaction;

import com.delphix.masking.api.plugin.MaskingAlgorithm;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.annotation.Nullable;

/** Redact data values by overwriting with a static replacement value. */
public class DateRedaction implements MaskingAlgorithm<LocalDateTime> {
    private static final String REDACTION_VALUE = "1990-02-12T10:00:00";

    @Override
    public String getName() {
        return "Date Redaction";
    }

    @Override
    public String getDescription() {
        return "Redact Date by replacing it with February 2nd, 1990 at 10:00AM";
    }

    @Override
    public LocalDateTime mask(@Nullable LocalDateTime input) {
        if (input == null) {
            return null;
        }

        return LocalDateTime.parse(REDACTION_VALUE, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
