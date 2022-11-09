/*
 * Copyright (c) 2019, 2020 by Delphix. All rights reserved.
 */
package sample.masking.algorithm.redaction;

import com.delphix.masking.api.plugin.MaskingAlgorithm;
import java.nio.ByteBuffer;
import javax.annotation.Nullable;

/** Redact binary data by overwriting with an ascending cycle of byte values from 0 to 255. */
public class BytesRedaction implements MaskingAlgorithm<ByteBuffer> {
    @Override
    public String getName() {
        return "Byte Array Redaction";
    }

    @Override
    public String getDescription() {
        return "Redact a byte array by overwriting with a cycle of values from 0 to 255";
    }

    @Override
    public ByteBuffer mask(@Nullable ByteBuffer input) {
        if (input == null) {
            return null;
        }

        int limit = input.limit();
        for (int i = 0; i < limit; i++) {
            input.put(i, (byte) (i & 0xFF));
        }

        return input;
    }
}
