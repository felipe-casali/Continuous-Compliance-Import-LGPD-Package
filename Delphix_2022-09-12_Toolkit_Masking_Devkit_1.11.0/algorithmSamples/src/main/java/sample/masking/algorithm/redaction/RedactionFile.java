/*
 * Copyright (c) 2019, 2020 by Delphix. All rights reserved.
 */
package sample.masking.algorithm.redaction;

import com.delphix.masking.api.plugin.MaskingAlgorithm;
import com.delphix.masking.api.plugin.MaskingComponent;
import com.delphix.masking.api.plugin.exception.ComponentConfigurationException;
import com.delphix.masking.api.plugin.exception.MaskingException;
import com.delphix.masking.api.plugin.referenceType.FileReference;
import com.delphix.masking.api.plugin.referenceType.GenericReference;
import com.delphix.masking.api.provider.ComponentService;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.Scanner;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class RedactionFile implements MaskingAlgorithm<String> {
    private String redactionCharacter = null;

    @JsonProperty(value = "file", required = true)
    @JsonPropertyDescription(
            "A reference to a file containing a single character to be used for redaction")
    public FileReference file;

    @Override
    public Collection<MaskingComponent> getDefaultInstances() {
        RedactionFile instance = new RedactionFile();
        // This is a reference to src/main/resources/x.txt in this project
        instance.file = new FileReference("jar://file/x.txt");
        return Collections.singletonList(instance);
    }

    @Override
    public boolean getAllowFurtherInstances() {
        return true;
    }

    @Override
    public String mask(@Nullable String input) throws MaskingException {
        if (input == null) {
            return null;
        }
        StringBuilder returnVal = new StringBuilder();

        for (int i = 0; i < input.length(); i++) {
            returnVal.append(redactionCharacter);
        }

        return returnVal.toString();
    }

    @Override
    public void validate() throws ComponentConfigurationException {
        GenericReference.checkRequiredReference(file, "file");
    }

    @Override
    public void setup(@Nonnull ComponentService serviceProvider) {
        try (InputStream inputStream = serviceProvider.openInputFile(file);
                Scanner scanner = new Scanner(inputStream, Charset.defaultCharset().name())) {
            redactionCharacter = scanner.nextLine().trim();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Unable to parse input file", e);
        }
    }

    @Override
    public String getName() {
        return "RedactionFile";
    }
}
