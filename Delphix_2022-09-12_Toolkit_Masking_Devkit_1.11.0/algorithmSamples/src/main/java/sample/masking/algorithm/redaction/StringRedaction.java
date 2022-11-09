/*
 * Copyright (c) 2019, 2020 by Delphix. All rights reserved.
 */
package sample.masking.algorithm.redaction;

import com.delphix.masking.api.plugin.MaskingAlgorithm;
import com.delphix.masking.api.plugin.MaskingComponent;
import com.delphix.masking.api.plugin.exception.ComponentConfigurationException;
import com.delphix.masking.api.plugin.exception.MaskingException;
import com.delphix.masking.api.provider.ComponentService;
import com.delphix.masking.api.provider.LogService;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This is a simple redaction algorithm with several real implementations that redact with different
 * letters. These exist for test and demonstration purposes, and will be enhanced prior to making
 * the Masking SDK public.
 */
public class StringRedaction implements MaskingAlgorithm<String> {
    // These are the name for the framework. The default instance names are set in the
    // getDefaultInstances method.
    private String name = "StringRedaction";

    @JsonProperty(value = "redactionCharacter", required = true)
    public String redactionCharacter = "specified";

    private LogService logger;

    private int count = 0;

    private Random random = new Random(LocalDateTime.now().getNano());

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Collection<MaskingComponent> getDefaultInstances() {
        StringRedaction instanceX = new StringRedaction();
        instanceX.name = "Redaction X";
        instanceX.redactionCharacter = "X";

        StringRedaction instanceY = new StringRedaction();
        instanceY.name = "Redaction Y";
        instanceY.redactionCharacter = "Y";

        StringRedaction instanceZ = new StringRedaction();
        instanceZ.name = "Redaction Z";
        instanceZ.redactionCharacter = "Z";

        return Arrays.asList(instanceX, instanceY, instanceZ);
    }

    @Override
    public boolean getAllowFurtherInstances() {
        return true;
    }

    @Override
    public String getDescription() {
        return String.format(
                "Redact String by overwriting with '%s' character", redactionCharacter);
    }

    @Override
    public String mask(@Nullable String input) throws MaskingException {
        if (input == null) {
            return null;
        }

        if (random.nextDouble() < 0.1) {
            logger.info("{0}: Masked {1} values", getName(), count);
        }

        StringBuilder returnVal = new StringBuilder();

        for (int i = 0; i < input.length(); i++) {
            returnVal.append(redactionCharacter);
        }
        count++;
        return returnVal.toString();
    }

    @Override
    public void validate() throws ComponentConfigurationException {
        if (redactionCharacter == null || redactionCharacter.length() != 1) {
            throw new ComponentConfigurationException(
                    "redactionCharacter must be a single character");
        }
    }

    @Override
    public void setup(@Nonnull ComponentService serviceProvider) {
        logger = serviceProvider.getLogService();
    }

    @Override
    public void tearDown() {
        logger.info("{0}: Masked a total of {1} values", getName(), count);
        count = 0;
    }
}
