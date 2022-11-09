/*
 * Copyright (c) 2019, 2021 by Delphix. All rights reserved.
 */
package sample.masking.algorithm;

import com.delphix.masking.api.plugin.MaskingAlgorithm;
import com.delphix.masking.api.plugin.MaskingComponent;
import com.delphix.masking.api.plugin.exception.ComponentConfigurationException;
import com.delphix.masking.api.plugin.referenceType.FileReference;
import com.delphix.masking.api.plugin.referenceType.GenericReference;
import com.delphix.masking.api.plugin.referenceType.KeyReference;
import com.delphix.masking.api.provider.ComponentService;
import com.delphix.masking.api.provider.CryptoService;
import com.delphix.masking.api.provider.LogService;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class StringHashedLookup implements MaskingAlgorithm<String> {
    private List<String> replacements;
    private CryptoService crypto;

    // These are the name and description for the framework. The default instance name and
    // description are set in the getDefaultInstances method.
    private String name = "StringHashedLookup";
    private String description =
            "This algorithm masks by selecting a stable replacement value from the list of provided replacements.";

    private LogService logger;

    public KeyReference key = new KeyReference();

    @JsonProperty(value = "caseInsensitive", defaultValue = "false")
    public Boolean caseInsensitive = false;

    @JsonProperty("replacementFile")
    @JsonPropertyDescription(
            "Reference to a UTF-8 encoded file containing newline separated replacement values")
    public FileReference replacementFile;

    @JsonProperty("replacementList")
    @JsonPropertyDescription("List of replacement values to use")
    public List<String> replacementList;

    @JsonProperty("filterLookupsByFieldLength")
    @JsonPropertyDescription(
            "Whether lookup values longer than the intended field length should be filtered out")
    public boolean filterLookupsByFieldLength = false;

    /**
     * Mask a string by choosing a replacement from a list based on the hash of the original value.
     *
     * @param input The String object to be masked.
     * @return Returns the masked value.
     */
    @Override
    public String mask(@Nullable String input) {
        if (input == null || input.length() == 0) {
            return input;
        }

        if (caseInsensitive) {
            input = input.toLowerCase();
        }

        return replacements.get((int) crypto.computeHashedLookupIndex(input, replacements.size()));
    }

    /**
     * Get the recommended name of this Algorithm.
     *
     * @return The name of this algorithm
     */
    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Collection<MaskingComponent> getDefaultInstances() {
        StringHashedLookup myInstance = new StringHashedLookup();
        myInstance.name = "Replacement A to Z";
        myInstance.description = "Replace with letters A-Z based on input hash";
        myInstance.replacementList =
                IntStream.rangeClosed('A', 'Z')
                        .mapToObj(c -> String.valueOf((char) c))
                        .collect(Collectors.toList());

        return Collections.singletonList(myInstance);
    }

    @Override
    public boolean getAllowFurtherInstances() {
        return true;
    }

    @Override
    public void validate() throws ComponentConfigurationException {
        if ((replacementFile == null) && (replacementList == null || replacementList.size() == 0)) {
            throw new ComponentConfigurationException(
                    "No replacement values provided: "
                            + "both replacementFile and replacementList configuration values were missing or empty");
        }
        GenericReference.checkOptionalReference(replacementFile, "replacementFile");
    }

    @Override
    public void setup(@Nonnull ComponentService serviceProvider) {
        replacements = new ArrayList<>();
        logger = serviceProvider.getLogService();
        final int valueLengthLimit =
                serviceProvider.getMaskValueMetadata() != null
                        ? serviceProvider.getMaskValueMetadata().getStringMaxLength()
                        : Integer.MAX_VALUE;

        if (replacementFile != null) {
            String line;
            try (InputStream is = serviceProvider.openInputFile(replacementFile);
                    BufferedReader reader =
                            new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                while ((line = reader.readLine()) != null) {
                    if (line.length() <= valueLengthLimit) {
                        replacements.add(line);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        if (replacementList != null) {
            replacementList.stream()
                    .filter(v -> !filterLookupsByFieldLength || v.length() <= valueLengthLimit)
                    .forEach(v -> replacements.add(v));
        }

        crypto = serviceProvider.getCryptoService(key);

        logger.info(
                "Max String length is: "
                        + valueLengthLimit
                        + "  Lookup Count: "
                        + replacements.size());
    }
}
