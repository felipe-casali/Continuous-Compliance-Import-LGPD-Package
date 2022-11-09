/*
 * Copyright (c) 2022 by Delphix. All rights reserved.
 */
package sample.masking.algorithm.redaction;

import com.delphix.masking.api.plugin.AlgorithmLogicalField;
import com.delphix.masking.api.plugin.MaskingAlgorithm;
import com.delphix.masking.api.plugin.MaskingComponent;
import com.delphix.masking.api.plugin.exception.ComponentConfigurationException;
import com.delphix.masking.api.plugin.exception.MaskingException;
import com.delphix.masking.api.plugin.utils.GenericData;
import com.delphix.masking.api.plugin.utils.GenericDataRow;
import com.delphix.masking.api.provider.ComponentService;
import com.delphix.masking.api.provider.LogService;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This class demonstrates the use of read-only and optional fields for multi-column masking. It
 * masks one or more target fields only if the field call "indicator" matches (or doesn't match,
 * depending on the allow/deny setting) a value in the configured indicatorValue set.
 */
public class MultiColumnRedaction implements MaskingAlgorithm<GenericDataRow> {
    @JsonProperty(value = "redactionCharacter", defaultValue = "X")
    @JsonPropertyDescription("The character to redact with")
    public String redactionCharacter = "X";

    @JsonProperty(value = "indicatorValues", required = true)
    @JsonPropertyDescription("The set of values to look for in the indicator column")
    public Set<String> indicatorValues;

    @JsonProperty(value = "reverseIndicatorMatching", defaultValue = "false")
    @JsonPropertyDescription(
            "When set to true, a row is masked when the indicator field value does *not* match any value in the "
                    + "indicatorValues list.")
    public boolean reverseIndicatorMatching = false;

    private static final String REDACT_FIELD_PREFIX = "target-";
    private static final int REDACT_FIELD_COUNT = 8;
    private static final String INDICATOR_FIELD_NAME = "indicator";

    private String name = "MultiColumnRedaction";
    private String description =
            "This multi-column algorithm requires two inputs: target and indicator. Depending on the value of "
                    + "the indicator field, up to "
                    + REDACT_FIELD_COUNT
                    + " other field(s) will be overwritten with the character specified in redactionCharacter. "
                    + "The property indicatorValues should be a list of all indicator values for which the target "
                    + "field(s) should be redacted. These values are case-sensitive. The matching logic may be "
                    + "reversed by setting reverseIndicatorMatching to true.";

    private LogService logger;
    private String instanceName;
    private boolean isRedactFieldNamesSet = false;
    private final String[] redactFieldNames = new String[REDACT_FIELD_COUNT];
    private long emptyValueCount;
    private long maskValueCount;
    private long totalRunCount;
    private long rowMatchCount;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public GenericDataRow mask(@Nullable GenericDataRow genericDataRow) throws MaskingException {
        totalRunCount++;
        if (genericDataRow == null) {
            return null;
        }

        // The presence of each field will be consistent across all usage of the algorithm instance
        if (!isRedactFieldNamesSet) {
            for (int i = 0; i < REDACT_FIELD_COUNT; i++) {
                String fieldName = REDACT_FIELD_PREFIX + (i + 1);
                if (genericDataRow.get(fieldName) != null) {
                    redactFieldNames[i] = fieldName;
                }
            }
            isRedactFieldNamesSet = true;
        }

        // read the column values from GenericDataRow
        GenericData indicatorData = genericDataRow.get(INDICATOR_FIELD_NAME);
        String indicator = indicatorData.getStringValue();

        // Determine whether the indicator matches
        boolean indicatorMatch = false;
        if (indicator != null) {
            indicatorMatch = indicatorValues.contains(indicator.trim());
        }

        // If so, redact all fields
        if (reverseIndicatorMatching != indicatorMatch) {
            rowMatchCount++;
            for (int i = 0; i < REDACT_FIELD_COUNT; i++) {
                if (redactFieldNames[i] == null) {
                    continue;
                }
                GenericData data = genericDataRow.get(redactFieldNames[i]);
                String toRedact = data.getStringValue();
                if (toRedact == null || toRedact.isEmpty()) {
                    emptyValueCount++;
                    continue;
                }

                StringBuilder sb = new StringBuilder();
                for (int c = 0; c < toRedact.length(); c++) {
                    sb.append(redactionCharacter);
                }
                data.setValue(sb.toString());
                maskValueCount++;
            }
        }

        // return the genericDataRow, now with updated member objects containing the masked values
        return genericDataRow;
    }

    @Override
    public List<AlgorithmLogicalField> listMultiColumnFields() {
        final ArrayList<AlgorithmLogicalField> fields = new ArrayList<>();

        fields.add(
                new AlgorithmLogicalField(
                        INDICATOR_FIELD_NAME,
                        MaskingType.STRING,
                        "The value of this field determines whether to redact the target field(s)",
                        true,
                        false));
        for (int i = 1; i <= REDACT_FIELD_COUNT; i++) {
            fields.add(
                    new AlgorithmLogicalField(
                            REDACT_FIELD_PREFIX + i,
                            MaskingType.STRING,
                            "A field to redact",
                            false,
                            i != 1));
        }

        return ImmutableList.copyOf(fields);
    }

    @Override
    public Collection<MaskingComponent> getDefaultInstances() {
        MultiColumnRedaction instance = new MultiColumnRedaction();
        instance.name = "MC Redaction - boolean indicator";
        instance.indicatorValues = new HashSet<>();
        instance.indicatorValues.addAll(
                Arrays.asList("true", "TRUE", "True", "1", "y", "Y", "t", "T"));
        instance.description =
                "Redact the target field(s) when the indicator field value is equivalent is true. "
                        + "The following values are considered true: '"
                        + Joiner.on("', '").join(instance.indicatorValues)
                        + "'.";
        instance.reverseIndicatorMatching = false;
        return Collections.singletonList(instance);
    }

    @Override
    public boolean getAllowFurtherInstances() {
        return true;
    }

    @Override
    public void setup(@Nonnull ComponentService serviceProvider) {
        logger = serviceProvider.getLogService();
        instanceName = serviceProvider.getInstanceName();
        if (indicatorValues == null) {
            indicatorValues = Collections.emptySet();
            return;
        }

        // Performance
        if (!(indicatorValues instanceof HashSet)) {
            indicatorValues = new HashSet<>(indicatorValues);
        }
    }

    @Override
    public void validate() throws ComponentConfigurationException {
        if (redactionCharacter == null || redactionCharacter.length() != 1) {
            throw new ComponentConfigurationException("Invalid value for redactionCharacter");
        }
    }

    @Override
    public void tearDown() {
        if (logger == null) {
            return;
        }
        logger.info(
                instanceName
                        + " STATS: redactionRowMatches="
                        + rowMatchCount
                        + "/"
                        + totalRunCount
                        + " valueRedacted="
                        + maskValueCount
                        + " emptyValues="
                        + emptyValueCount);
    }
}
