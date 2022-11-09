/*
 * Copyright (c) 2021 by Delphix. All rights reserved.
 */
package sample.masking.driverSupport.tasks;

import com.delphix.masking.api.driverSupport.jobInfo.TableInfo;
import com.delphix.masking.api.plugin.exception.MaskingException;
import com.google.common.base.Joiner;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class represents a task to disable and re-enable all foreign key and check constraints on
 * MSSQL columns.
 */
public class DisableConstraints extends BaseTask {
    private Map<String, ConstraintMetadata> enabledConstraints;
    private static final String CHECK_CONSTRAINT_TYPE = "C";
    private static final String FOREIGN_KEY_CONSTRAINT_TYPE = "FK";
    private static final String ALTER_CONSTRAINT_STATEMENT = "ALTER TABLE %s %s CONSTRAINT %s";
    private static final String MASKED_TABLE = "MASKED_TABLE";
    private static final String PK_TABLE = "PK_TABLE";
    private static final String FK_TABLE = "FK_TABLE";

    @Override
    public String getTaskName() {
        return "Disable Constraints";
    }

    private static String singleQuoted(String s) {
        return "'" + s + "'";
    }

    private static String doubleQuoted(String s) {
        return "\"" + s + "\"";
    }

    private static String listOutConstraintColumns(List<String> columnNames) {
        return Joiner.on(",")
                .join(
                        columnNames.stream()
                                .map(columnName -> doubleQuoted(columnName))
                                .collect(Collectors.toList()));
    }

    private String getSchemaClause() {
        try {
            return " and tc.table_schema = " + singleQuoted(getSchema());
        } catch (MaskingException e) {
            return "";
        }
    }

    /**
     * Find all enabled foreign key check constraints on tables provided in job info and save them
     * as a member of this object, so that the same list of constraints can be used to be re-enabled
     * in the postExecute method.
     *
     * @return A Map of constraint name to ConstraintMetadata of all enabled constraints.
     */
    private Map<String, ConstraintMetadata> findEnabledConstraints() throws MaskingException {
        Map<String, ConstraintMetadata> constraints = new HashMap<>();
        try {
            for (TableInfo table : jobInfo.getTables()) {
                // find all check constraints on table
                String getCheckConstraintsQuery =
                        "select distinct cast(cc.name as varchar(255)) as constraintName from sys.check_constraints cc, "
                                + "INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc where tc.constraint_type = 'CHECK' and "
                                + "tc.constraint_name = cc.\"name\" and cc.is_disabled = 0 and tc.table_name = "
                                + singleQuoted(table.getName())
                                + getSchemaClause()
                                + " and tc.table_catalog = "
                                + singleQuoted(targetConnection.getCatalog());
                try (Statement statement = targetConnection.createStatement()) {
                    try (ResultSet resultSet = statement.executeQuery(getCheckConstraintsQuery)) {
                        while (resultSet.next()) {
                            String constraintName = resultSet.getString("constraintName");
                            constraints.put(
                                    constraintName,
                                    new ConstraintMetadata(
                                            table, constraintName, CHECK_CONSTRAINT_TYPE));
                        }
                    }
                }
                // find all foreign key constraints on table
                try (ResultSet fkInfo =
                        targetConnection
                                .getMetaData()
                                .getImportedKeys(null, getSchema(), table.getName())) {
                    while (fkInfo.next()) {
                        String constraintName = fkInfo.getString("FK_NAME");
                        String pkTableName = fkInfo.getString("PKTABLE_NAME");
                        String fkTableName = fkInfo.getString("FKTABLE_NAME");
                        String pkColumnName = fkInfo.getString("PKCOLUMN_NAME");
                        String fkColumnName = fkInfo.getString("FKCOLUMN_NAME");

                        if (!constraints.containsKey(constraintName)) {
                            ConstraintMetadata fkConstraint =
                                    new ConstraintMetadata(
                                            table,
                                            constraintName,
                                            FOREIGN_KEY_CONSTRAINT_TYPE,
                                            pkTableName,
                                            fkTableName);
                            fkConstraint.addCompoundConstraintColumns(pkColumnName, fkColumnName);
                            constraints.put(constraintName, fkConstraint);
                        } else {
                            constraints
                                    .get(constraintName)
                                    .addCompoundConstraintColumns(pkColumnName, fkColumnName);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            String errorMessage = "Error retrieving constraints on tables to be masked: ";
            logService.error(jobInfo.getJobId(), jobInfo.getExecutionId(), errorMessage + e);
            throw new MaskingException(errorMessage, e);
        }
        return constraints;
    }

    /**
     * Disable all constraints
     *
     * @throws MaskingException in case of SQLException
     */
    private void disableConstraints() throws MaskingException {
        this.enabledConstraints = findEnabledConstraints();
        try (Statement statement = targetConnection.createStatement()) {
            for (ConstraintMetadata constraint : enabledConstraints.values()) {
                logService.info(
                        jobInfo.getJobId(),
                        jobInfo.getExecutionId(),
                        String.format(
                                "Starting to disable constraint: \"%s\" on table \"%s\"",
                                constraint.getName(),
                                constraint.getQualifiedTableNameByType(MASKED_TABLE)));
                try {
                    String builtSqlStatement =
                            String.format(
                                    ALTER_CONSTRAINT_STATEMENT,
                                    constraint.getQualifiedTableNameByType(MASKED_TABLE),
                                    constraint.getDisableAction(),
                                    constraint.getName(),
                                    ";");
                    logService.info(
                            jobInfo.getJobId(), jobInfo.getExecutionId(), builtSqlStatement);
                    statement.execute(builtSqlStatement);
                } catch (SQLException e) {
                    String errorMessage =
                            String.format(
                                    "Error disabling constraint: \"%s\" on table \"%s\".",
                                    constraint.getName(),
                                    constraint.getQualifiedTableNameByType(MASKED_TABLE));
                    logService.error(
                            jobInfo.getJobId(), jobInfo.getExecutionId(), errorMessage + e);
                    throw new MaskingException(errorMessage, e);
                }
                logService.info(
                        jobInfo.getJobId(),
                        jobInfo.getExecutionId(),
                        String.format(
                                "Finished disabling constraint: \"%s\" on table \"%s\".",
                                constraint.getName(),
                                constraint.getQualifiedTableNameByType(MASKED_TABLE)));
            }
        } catch (SQLException e) {
            String errorMessage =
                    String.format(
                            "Error creating statement on target connection %s: ",
                            targetConnection.getClass());
            logService.error(jobInfo.getJobId(), jobInfo.getExecutionId(), errorMessage + e);
            throw new MaskingException(errorMessage, e);
        }
    }

    @Override
    public void preJobExecute() throws MaskingException {
        disableConstraints();
    }

    /** This function enables all constraints on the target database table. */
    private void enableConstraints() throws MaskingException {
        try (Statement statement = targetConnection.createStatement()) {
            for (ConstraintMetadata constraint : enabledConstraints.values()) {
                logService.info(
                        jobInfo.getJobId(),
                        jobInfo.getExecutionId(),
                        String.format(
                                "Starting to enable constraint: \"%s\" on table \"%s\"",
                                constraint.getName(),
                                constraint.getQualifiedTableNameByType(MASKED_TABLE)));
                String enableConstraintStatement = ALTER_CONSTRAINT_STATEMENT;
                String builtSqlStatement;
                if (constraint.getType().equals(FOREIGN_KEY_CONSTRAINT_TYPE)) {
                    enableConstraintStatement += " FOREIGN KEY (%s) REFERENCES %s (%s);";
                    builtSqlStatement =
                            String.format(
                                    enableConstraintStatement,
                                    constraint.getQualifiedTableNameByType(FK_TABLE),
                                    constraint.getEnableAction(),
                                    constraint.getName(),
                                    listOutConstraintColumns(constraint.getFkColumnNames()),
                                    constraint.getQualifiedTableNameByType(PK_TABLE),
                                    listOutConstraintColumns(constraint.getPkColumnNames()));
                } else {
                    builtSqlStatement =
                            String.format(
                                    enableConstraintStatement,
                                    constraint.getQualifiedTableNameByType(MASKED_TABLE),
                                    constraint.getEnableAction(),
                                    constraint.getName(),
                                    ";");
                }
                logService.info(jobInfo.getJobId(), jobInfo.getExecutionId(), builtSqlStatement);
                statement.execute(builtSqlStatement);
                logService.info(
                        jobInfo.getJobId(),
                        jobInfo.getExecutionId(),
                        String.format(
                                "Finished enabling constraint: \"%s\" on table \"%s\"",
                                constraint.getName(),
                                constraint.getQualifiedTableNameByType(MASKED_TABLE)));
            }
        } catch (SQLException e) {
            String errorMessage =
                    "Error occurred while executing query to enable constraints on table.";
            logService.error(jobInfo.getJobId(), jobInfo.getExecutionId(), errorMessage, e);
            throw new MaskingException(errorMessage, e);
        }
    }

    @Override
    public void postJobExecute() throws MaskingException {
        enableConstraints();
    }

    /**
     * This private class is used to store constraint metadata that is required in order to recreate
     * constraints accurately.
     */
    private class ConstraintMetadata {
        private final TableInfo table;
        private final String constraintName;
        private final String type;
        private String pkTableName;
        private String fkTableName;
        private final List<String> pkColumnNames = new ArrayList<>();
        private final List<String> fkColumnNames = new ArrayList<>();
        private static final String DISABLE_CHECK_CONSTRAINT_ACTION = "NOCHECK";
        private static final String ENABLE_CHECK_CONSTRAINT_ACTION = "CHECK";
        private static final String DROP_FOREIGN_KEY_CONSTRAINT_ACTION = "DROP";
        private static final String ADD_FOREIGN_KEY_CONSTRAINT_ACTION = "ADD";

        ConstraintMetadata(TableInfo tableIn, String constraintNameIn, String type) {
            this.table = tableIn;
            this.constraintName = constraintNameIn;
            this.type = type;
        }

        ConstraintMetadata(
                TableInfo tableIn,
                String constraintNameIn,
                String type,
                String pkTableName,
                String fkTableName) {
            this(tableIn, constraintNameIn, type);
            this.pkTableName = pkTableName;
            this.fkTableName = fkTableName;
        }

        String getName() {
            return constraintName;
        }

        String getType() {
            return type;
        }

        List<String> getPkColumnNames() {
            return pkColumnNames;
        }

        List<String> getFkColumnNames() {
            return fkColumnNames;
        }

        String getQualifiedTableNameByType(String tableType) {
            String tableName;
            switch (tableType) {
                case MASKED_TABLE:
                    tableName = table.getName();
                    break;
                case PK_TABLE:
                    tableName = pkTableName;
                    break;
                case FK_TABLE:
                    tableName = fkTableName;
                    break;
                default:
                    throw new RuntimeException("Unknown table type.");
            }

            return getQualifiedTableName(tableName);
        }

        String getDisableAction() {
            return type.equals(CHECK_CONSTRAINT_TYPE)
                    ? DISABLE_CHECK_CONSTRAINT_ACTION
                    : DROP_FOREIGN_KEY_CONSTRAINT_ACTION;
        }

        String getEnableAction() {
            return type.equals(CHECK_CONSTRAINT_TYPE)
                    ? ENABLE_CHECK_CONSTRAINT_ACTION
                    : ADD_FOREIGN_KEY_CONSTRAINT_ACTION;
        }

        void addCompoundConstraintColumns(String pkColumnName, String fkColumnName) {
            this.pkColumnNames.add(pkColumnName);
            this.fkColumnNames.add(fkColumnName);
        }
    }
}
