/*
 * Copyright (c) 2021 by Delphix. All rights reserved.
 */
package sample.masking.algorithm;

import com.delphix.masking.api.plugin.MaskingAlgorithm;
import com.delphix.masking.api.plugin.exception.MaskingException;
import com.delphix.masking.api.plugin.referenceType.KeyReference;
import com.delphix.masking.api.provider.ComponentService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This class implements tokenization using a simple substitution cipher across [0-9A-Za-z]. In
 * supports masking, tokenization and substitution. This sort of cipher can conceal data from casual
 * snooping but is *NOT* cryptographically secure. This class is only intended to serve as an
 * example of how to implement a tokenization algorithm.
 */
public class SubstitutionTokenization implements MaskingAlgorithm<String> {

    Map<Character, Character> substitutions = new HashMap<>();
    boolean reverse = false;
    public KeyReference key = new KeyReference();

    @Override
    public String getName() {
        return "Substitution Cipher Tokenization";
    }

    @Override
    public String getDescription() {
        return "This sample algorithm illustrates tokenization using a simple substitution cipher. This is not a"
                + " secure tokenization mechanism and should *never* be used on real sensitive data.";
    }

    @Override
    public boolean getAllowFurtherInstances() {
        return false;
    }

    @Override
    public String mask(@Nullable String input) throws MaskingException {
        if (input == null || input.isEmpty()) {
            return input;
        }

        StringBuilder result = new StringBuilder(input);

        // Replace each character if a substitution is defined
        for (int i = 0; i < result.length(); i++) {
            Character replacement = substitutions.get(result.charAt(i));
            if (replacement != null) {
                result.setCharAt(i, replacement);
            }
        }

        return result.toString();
    }

    @Override
    public void setup(@Nonnull ComponentService serviceProvider) {
        List<Character> charsIn = new ArrayList<>();

        // Add all the letters we want to mask
        for (char c = '0'; c <= '9'; c++) {
            charsIn.add(c);
        }
        for (char c = 'A'; c <= 'Z'; c++) {
            charsIn.add(c);
        }
        for (char c = 'a'; c <= 'z'; c++) {
            charsIn.add(c);
        }

        // Build a map of character substitutions
        List<Character> charsOut = new ArrayList<>(charsIn);
        serviceProvider.getCryptoService(key).shuffleListNoCollisions(charsOut);

        if (reverse) {
            for (int i = 0; i < charsOut.size(); i++) {
                substitutions.put(charsOut.get(i), charsIn.get(i));
            }
        } else {
            for (int i = 0; i < charsOut.size(); i++) {
                substitutions.put(charsIn.get(i), charsOut.get(i));
            }
        }
    }

    /**
     * Overriding this class allows us to build the substitution map in the correct direction and
     * indicates to the framework that this algorithm supports tokenization and reidentification
     * uses.
     *
     * @param mode The masking mode in which this algorithm should operate.
     */
    @Override
    public void setMaskingMode(MaskingAlgorithm.MaskingMode mode) {
        switch (mode) {
            case MASK:
            case TOKENIZE:
                return;

            case REIDENTIFY:
                reverse = true;
                return;

            default:
                throw new RuntimeException("Unsupported mask mode: " + mode);
        }
    }
}
