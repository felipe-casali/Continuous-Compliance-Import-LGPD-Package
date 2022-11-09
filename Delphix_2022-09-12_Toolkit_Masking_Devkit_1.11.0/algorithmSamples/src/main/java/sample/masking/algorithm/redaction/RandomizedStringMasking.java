/*
 * Copyright (c) 2019, 2020 by Delphix. All rights reserved.
 */
package sample.masking.algorithm.redaction;

import com.delphix.masking.api.plugin.MaskingAlgorithm;
import com.delphix.masking.api.plugin.MaskingComponent;
import com.delphix.masking.api.plugin.exception.ComponentConfigurationException;
import com.delphix.masking.api.plugin.exception.MaskingException;
import com.delphix.masking.api.plugin.referenceType.AlgorithmInstanceReference;
import com.delphix.masking.api.plugin.referenceType.GenericReference;
import com.delphix.masking.api.provider.ComponentService;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This algorithm selects a another string based masking algorithm at random and applies it to the
 * data. It is configured with a list of algorithms to choose from. This serves to demonstrate
 * invoking one algorithm from another.
 */
public class RandomizedStringMasking implements MaskingAlgorithm<String> {
    private List<MaskingAlgorithm<String>> algorithmList = new ArrayList<>();
    private Iterator<Integer> randomStream;

    @JsonProperty(value = "algorithmNames", required = true)
    @JsonPropertyDescription("List of references to string masking algorithms to apply randomly")
    public List<AlgorithmInstanceReference> algorithms;

    @Override
    public String getName() {
        return "Randomized Masking";
    }

    @Override
    public String getDescription() {
        return "Randomly apply one of several string masking algorithms";
    }

    @Override
    public Collection<MaskingComponent> getDefaultInstances() {
        RandomizedStringMasking myInstance =
                new RandomizedStringMasking() {
                    @Override
                    public String getName() {
                        return "Randomized Redaction";
                    }

                    @Override
                    public String getDescription() {
                        return "Apply a random redaction algorithm from { X, Y, Z }";
                    }
                };
        myInstance.algorithms =
                Arrays.asList(
                        new AlgorithmInstanceReference(":Redaction X"),
                        new AlgorithmInstanceReference(":Redaction Y"),
                        new AlgorithmInstanceReference(":Redaction Z"));

        return Collections.singletonList(myInstance);
    }

    @Override
    public boolean getAllowFurtherInstances() {
        return true;
    }

    @Override
    public void validate() throws ComponentConfigurationException {
        if (algorithms == null || algorithms.isEmpty()) {
            throw new ComponentConfigurationException(
                    "Value for field algorithmNames is missing or empty");
        }
        for (AlgorithmInstanceReference ref : algorithms) {
            GenericReference.checkRequiredReference(ref, "algorithms");
        }
    }

    @Override
    public void setup(@Nonnull ComponentService serviceProvider) {
        for (AlgorithmInstanceReference algorithm : algorithms) {
            algorithmList.add(serviceProvider.getAlgorithmByName(algorithm, MaskingType.STRING));
        }
        randomStream = new Random().ints(0, algorithmList.size()).iterator();
    }

    @Override
    public String mask(@Nullable String s) throws MaskingException {
        return algorithmList.get(randomStream.next()).mask(s);
    }
}
