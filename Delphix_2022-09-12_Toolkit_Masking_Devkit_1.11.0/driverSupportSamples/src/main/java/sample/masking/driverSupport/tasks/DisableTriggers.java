/*
 * Copyright (c) 2021 by Delphix. All rights reserved.
 */
package sample.masking.driverSupport.tasks;

import com.delphix.masking.api.driverSupport.jobInfo.TableInfo;
import com.delphix.masking.api.plugin.exception.MaskingException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/** This class represents a task to disable and re-enable all triggers on MSSQL tables. */
public class DisableTriggers extends BaseTask {
    // DISABLE TRIGGER [schema.trigger name] ON [table name]
    private String MODIFY_TRIGGERS_SQL = "%s TRIGGER %s ON %s";
    // <trigger name>, <table name>
    private Map<String, String> triggersOnMaskedTables;

    @Override
    public String getTaskName() {
        return "Disable Triggers";
    }

    /**
     * Select all triggers are on tables belonging to jobInfo
     *
     * @return A Map of trigger name to the table name it belongs to.
     */
    private Map<String, String> findEnabledTriggersOnMaskedTables() throws MaskingException {
        Map<String, String> enabledTriggers = new HashMap<>();
        try (Statement statement = targetConnection.createStatement()) {
            for (TableInfo maskedTable : jobInfo.getTables()) {
                String getDisabledTriggersQuery =
                        String.format(
                                "SELECT name as TRIGGER_NAME FROM sys.triggers "
                                        + "WHERE parent_id = OBJECT_ID(N'%s') "
                                        + "AND is_disabled = 0 order by name ",
                                getQualifiedTableName(maskedTable.getName()));
                try (ResultSet resultSet = statement.executeQuery(getDisabledTriggersQuery)) {
                    if (resultSet.next()) {
                        String triggerName = resultSet.getString("TRIGGER_NAME");
                        if (triggerName != null) {
                            enabledTriggers.put(triggerName, maskedTable.getName());
                        }
                    }
                }
            }
        } catch (SQLException e) {
            String errorMessage = "Error retrieving triggers on tables to be masked: ";
            logService.error(jobInfo.getJobId(), jobInfo.getExecutionId(), errorMessage + e);
            throw new MaskingException(errorMessage, e);
        }

        return enabledTriggers;
    }

    @Override
    public void preJobExecute() throws MaskingException {
        this.triggersOnMaskedTables = findEnabledTriggersOnMaskedTables();
        try (Statement statement = targetConnection.createStatement()) {
            for (Map.Entry<String, String> entry : triggersOnMaskedTables.entrySet()) {
                String triggerName = entry.getKey();
                String tableName = entry.getValue();
                String disableTriggersStatement =
                        String.format(MODIFY_TRIGGERS_SQL, "DISABLE", triggerName, tableName);
                logService.info(
                        jobInfo.getJobId(),
                        jobInfo.getExecutionId(),
                        String.format(
                                "Starting to disable trigger: %s on table %s",
                                triggerName, tableName));
                statement.execute(disableTriggersStatement);
                logService.info(
                        jobInfo.getJobId(),
                        jobInfo.getExecutionId(),
                        String.format(
                                "Finished disabling trigger: %s on table %s.",
                                triggerName, tableName));
            }
        } catch (SQLException e) {
            String errorMessage = "Error creating a statement on target connection.";
            logService.error(jobInfo.getJobId(), jobInfo.getExecutionId(), errorMessage + e);
            throw new MaskingException(errorMessage, e);
        }
    }

    @Override
    public void postJobExecute() throws MaskingException {
        try (Statement statement = targetConnection.createStatement()) {
            for (Map.Entry<String, String> entry : triggersOnMaskedTables.entrySet()) {
                String triggerName = entry.getKey();
                String tableName = entry.getValue();
                String enableTriggersStatement =
                        String.format(MODIFY_TRIGGERS_SQL, "ENABLE", triggerName, tableName);
                logService.info(
                        jobInfo.getJobId(),
                        jobInfo.getExecutionId(),
                        String.format(
                                "Starting to re-enable trigger: %s on table %s",
                                triggerName, tableName));
                statement.execute(enableTriggersStatement);
                logService.info(
                        jobInfo.getJobId(),
                        jobInfo.getExecutionId(),
                        String.format(
                                "Finished re-enabling trigger: %s on table %s",
                                triggerName, tableName));
            }
        } catch (SQLException e) {
            String errorMessage = "Error creating a statement on target connection.";
            logService.error(jobInfo.getJobId(), jobInfo.getExecutionId(), errorMessage + e);
            throw new MaskingException(errorMessage, e);
        }
    }
}
