/*
 * Copyright (c) 2021 by Delphix. All rights reserved.
 */
package sample.masking.driverSupport.tasks;

import com.delphix.masking.api.driverSupport.Task;
import com.delphix.masking.api.driverSupport.jobInfo.JobInfo;
import com.delphix.masking.api.plugin.exception.MaskingException;
import com.delphix.masking.api.provider.ComponentService;
import com.delphix.masking.api.provider.LogService;
import java.sql.Connection;

public abstract class BaseTask implements Task {
    protected JobInfo jobInfo;
    protected LogService logService;
    protected Connection targetConnection;

    @Override
    public void setup(ComponentService serviceProvider) {
        this.jobInfo = serviceProvider.getJobInfo();
        this.targetConnection = serviceProvider.getTargetConnection();
        this.logService = serviceProvider.getLogService();
    }

    /**
     * Method to resolve schema name through the following APIs. 1. Delphix Masking JobInfo API 2.
     * java.sql.Connection#getSchema API 3. java.sql.DatabaseMetadata#getUsername API
     *
     * @return schema to use for MSSQL target connection
     */
    protected String getSchema() throws MaskingException {
        if (jobInfo != null
                && !jobInfo.getTables().isEmpty()
                && jobInfo.getTables().get(0).getSchema() != null
                && jobInfo.getTables().get(0).getSchema().getName() != null
                && !jobInfo.getTables().get(0).getSchema().getName().isEmpty()) {
            return jobInfo.getTables().get(0).getSchema().getName();
        }
        try {
            try {
                return targetConnection.getSchema();
            } catch (AbstractMethodError e) {
                logService.info(
                        jobInfo.getJobId(),
                        jobInfo.getExecutionId(),
                        String.format(
                                "Connection::getSchema() method implementation is not supported by %s",
                                targetConnection.getClass().getCanonicalName()));
            }
            return targetConnection.getMetaData().getUserName();
        } catch (Exception e) {
            String errorMessage =
                    "Failed to retrieve schema from connection: "
                            + e.getMessage().replace("\n", "");
            logService.warn(jobInfo.getJobId(), jobInfo.getExecutionId(), errorMessage);
        }

        throw new MaskingException("Failed to retrieve schema for target connection.");
    }

    protected String getQualifiedTableName(String tableName) {
        try {
            return wrapInDelimiter(getSchema()) + "." + wrapInDelimiter(tableName);
        } catch (MaskingException e) {
            return tableName;
        }
    }

    protected String wrapInDelimiter(String identifier) {
        return getOpenDelimiter() + identifier + getCloseDelimiter();
    }

    private String getOpenDelimiter() {
        return "[";
    }

    private String getCloseDelimiter() {
        return "]";
    }
}
