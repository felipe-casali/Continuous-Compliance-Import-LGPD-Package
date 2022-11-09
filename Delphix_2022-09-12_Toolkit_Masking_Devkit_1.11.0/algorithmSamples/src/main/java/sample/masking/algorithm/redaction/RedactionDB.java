/*
 * Copyright (c) 2020 by Delphix. All rights reserved.
 */
package sample.masking.algorithm.redaction;

import com.delphix.masking.api.plugin.MaskingAlgorithm;
import com.delphix.masking.api.plugin.MaskingComponent;
import com.delphix.masking.api.plugin.exception.ComponentConfigurationException;
import com.delphix.masking.api.plugin.exception.MaskingException;
import com.delphix.masking.api.plugin.referenceType.GenericReference;
import com.delphix.masking.api.plugin.referenceType.JdbcReference;
import com.delphix.masking.api.provider.ComponentService;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class RedactionDB implements MaskingAlgorithm<String> {
    private String redactionCharacter = null;

    @JsonProperty(value = "jdbc", required = true)
    @JsonPropertyDescription("A reference to a database containing a table redaction_character")
    public JdbcReference jdbc;

    private static final String GET_REDACTION_CHARACTER = "SELECT redact FROM redaction_character";

    @Override
    public Collection<MaskingComponent> getDefaultInstances() {
        return null;
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
        GenericReference.checkRequiredReference(jdbc, "jdbc");
    }

    @Override
    public void setup(@Nonnull ComponentService serviceProvider) {
        try (Connection conn = serviceProvider.openJdbcConnection(jdbc);
                PreparedStatement stmt = conn.prepareStatement(GET_REDACTION_CHARACTER)) {
            ResultSet resultSet = stmt.executeQuery();
            List<String> redactionChars = new ArrayList<>();
            while (resultSet.next()) {
                redactionChars.add(resultSet.getString("redact"));
            }
            if (redactionChars.size() > 0) {
                Random random = new Random();
                int randInt = random.ints(0, redactionChars.size()).findAny().getAsInt();
                redactionCharacter = redactionChars.get(randInt);
            } else {
                throw new RuntimeException("Couldn't find redaction character");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getName() {
        return "RedactionDB";
    }
}
