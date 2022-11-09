/*
 * Copyright (c) 2020 by Delphix. All rights reserved.
 */
package sample.masking.algorithm;

import com.delphix.masking.api.plugin.MaskingAlgorithm;
import com.delphix.masking.api.plugin.exception.MaskingException;
import com.delphix.masking.api.plugin.exception.NonConformantDataException;
import com.delphix.masking.api.plugin.referenceType.KeyReference;
import com.delphix.masking.api.provider.ComponentService;
import com.delphix.masking.api.provider.CryptoService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This algorithm masks numerical data by breaking it down into SEGMENT_SIZE segments, then uses
 * lookup tables to replace each segment value. For those familiar with the Delphix segment mapping
 * algorithm, this is the equivalent of a segment mapping algorithm with 64x 4-digit segments, with
 * no provision for ignore position/character or any other features. The replacement table values
 * are based on the algorithm's key, and remain consistent unless the algorithm's key is changed.
 * The tables are generated as needed, so the total memory requirement will vary by the longest
 * value encountered by the algorithm.
 *
 * <p>This algorithm demonstrates two features of the Masking Algorithm API: 1. Use of the
 * cryptographic provider to shuffle a collection of replacement values consistently based on the
 * algorithm key - see the method getReplacementSet() for details. 2. Handling of non-conformant
 * data by the algorithm. This algorithm only processes numerical digits, but it takes string input,
 * so it cannot mask strings with non-digit content. It also limits the total length of input to
 * avoid consuming too much memory with replacement tables.
 */
public final class NumericMapping implements MaskingAlgorithm<String> {
    private static final int LENGTH_LIMIT = 256;
    private static final int SEGMENT_SIZE = 4;
    private static final int SEGMENT_VALUE_LIMIT = 9999;
    private static final String VALUE_FORMAT = "%0" + SEGMENT_SIZE + "d";

    private CryptoService crypto;
    private Map<Integer, List<String>> replacementLists = new HashMap<>();

    /*
     * The key reference should always be public even if its not configurable. This makes it visible during
     * dependency discovery on the masking engine.
     */
    public KeyReference key = new KeyReference();

    @Override
    public String getName() {
        return "Numeric Mapping";
    }

    @Override
    public void setup(@Nonnull ComponentService serviceProvider) {
        crypto = serviceProvider.getCryptoService(key);
    }

    @Override
    public String mask(@Nullable String s) throws MaskingException {
        if (s == null) {
            return null;
        }

        if (s.length() > LENGTH_LIMIT) {
            throw new NonConformantDataException("String too long to process");
        }

        StringBuilder result = new StringBuilder(); // We'll build the result string here
        int segment = 0; // Which segment we're on, starting with 0
        int lookup = 0; // The integer value we're going to replace

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (!Character.isDigit(c)) {
                throw new NonConformantDataException("Non digit character encountered");
            }

            lookup *= 10;
            lookup += Character.digit(c, 10);

            if (i % SEGMENT_SIZE == SEGMENT_SIZE - 1) {
                // We have a complete SEGMENT_SIZE segment, add the replacement value and reset
                // lookup.
                result.append(getReplacementList(segment).get(lookup));
                lookup = 0;
                segment++;
            }
        }

        /*
         * We've handled each 4-digit segment above, but there may have been a few extra digits that didn't constitute
         * a whole segment. Their value has already been saved in lookup.
         */
        int remainder = s.length() % SEGMENT_SIZE;
        if (remainder > 0) {
            result.append(getReplacementList(segment).get(lookup).substring(0, remainder));
        }

        return result.toString();
    }

    private List<String> getReplacementList(int segment) {
        List<String> result = replacementLists.get(segment);

        if (result != null) {
            // We have already generated and cached the replacement list for this segment
            return result;
        }

        result = new ArrayList<>(SEGMENT_VALUE_LIMIT + 1);

        // Add values "0000" through "9999" to the list
        for (int i = 0; i <= SEGMENT_VALUE_LIMIT; i++) {
            result.add(String.format(VALUE_FORMAT, i));
        }

        // Since we derive a new key with segment number as the salt, each replacement list will be
        // shuffled differently
        crypto.deriveNewKey(String.valueOf(segment)).shuffleList(result);
        // Cache this list so we only have to compute it once
        replacementLists.put(segment, result);

        return result;
    }
}
