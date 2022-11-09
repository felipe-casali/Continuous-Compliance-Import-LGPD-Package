/*
 * Copyright (c) 2021 by Delphix. All rights reserved.
 */
package sample.masking.driverSupport.tasks;

import com.delphix.masking.api.driverSupport.jobInfo.ColumnInfo;
import com.delphix.masking.api.driverSupport.jobInfo.TableInfo;
import com.delphix.masking.api.plugin.exception.MaskingException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/** This class represents a task to drop indexes on MSSQL columns. */
public class DropIndexes extends BaseTask {
    private Map<String, IndexMetadata> indexes = new HashMap<>();
    private static final String TABLE_NAME_ALIAS = "TABLE_NAME";
    private static final String INDEX_NAME_ALIAS = "INDEX_NAME";
    private static final String INDEX_TYPE_ALIAS = "INDEX_TYPE";
    private static final String COLUMN_NAME_ALIAS = "COLUMN_NAME";
    private static final String IS_UNIQUE_ALIAS = "IS_UNIQUE";
    private static final String IS_PADDED_ALIAS = "IS_PADDED";
    private static final String IS_INCLUDED_COLUMN_ALIAS = "IS_INCLUDED_COLUMN_ALIAS";
    private static final String ALLOW_PAGE_LOCKS_ALIAS = "ALLOW_PAGE_LOCKS";
    private static final String ALLOW_ROW_LOCKS_ALIAS = "ALLOW_ROW_LOCKS";
    private static final String IS_DESCENDING_KEY_ALIAS = "IS_DESCENDING_KEY";
    private static final String FILE_GROUP_ALIAS = "FILE_GROUP";
    /*
     * Single quotes are escaped by single quotes in SQL Server, so the below string will be '' in SQL.
     */
    private static final String ESCAPED_EMPTY_SINGLE_QUOTE_STRING = "''''";

    @Override
    public String getTaskName() {
        return "Drop Indexes";
    }

    /**
     * This method finds all indexes on tables belonging to the jobInfo and sets them on the member
     * variable "indexes"
     */
    private Map<String, IndexMetadata> findAllIndexes() throws MaskingException {
        String ruleSetTableNamesList = getCommaSeparatedTableNames();
        String selectIndexesQuery =
                "SELECT "
                        + TABLE_NAME_ALIAS
                        + " = t.name, "
                        + INDEX_NAME_ALIAS
                        + " = ind.name, "
                        + INDEX_TYPE_ALIAS
                        + " = ind.type_desc, "
                        + COLUMN_NAME_ALIAS
                        + " = col.name, "
                        + IS_UNIQUE_ALIAS
                        + " = ind.is_unique, "
                        + IS_PADDED_ALIAS
                        + " = ind.is_padded, "
                        + ALLOW_PAGE_LOCKS_ALIAS
                        + " = ind.allow_page_locks, "
                        + ALLOW_ROW_LOCKS_ALIAS
                        + " = ind.allow_row_locks, "
                        + IS_DESCENDING_KEY_ALIAS
                        + " = ic.is_descending_key, "
                        + FILE_GROUP_ALIAS
                        + " = f.name, "
                        + IS_INCLUDED_COLUMN_ALIAS
                        + " = ic.is_included_column"
                        + " FROM sys.indexes ind INNER JOIN sys.index_columns ic ON  ind.object_id = ic.object_id and ind.index_id = ic.index_id "
                        + " INNER JOIN sys.columns col ON ic.object_id = col.object_id and ic.column_id = col.column_id "
                        + " INNER JOIN sys.tables t ON ind.object_id = t.object_id"
                        + " INNER JOIN sys.filegroups f "
                        + " ON ind.data_space_id = f.data_space_id "
                        + " WHERE ind.is_primary_key = 0 AND ind.is_unique = 0 AND ind.is_unique_constraint = 0 AND t.is_ms_shipped = 0 AND"
                        + " t.name in ("
                        + ruleSetTableNamesList
                        + ") "
                        + " and ind.name in "
                        + "(select INDEX_NAME = ind.name  FROM sys.indexes ind INNER JOIN sys.index_columns ic ON ind.object_id = ic.object_id "
                        + " and ind.index_id = ic.index_id  INNER JOIN sys.columns col ON ic.object_id = col.object_id and ic.column_id = col.column_id  "
                        + " INNER JOIN sys.tables t ON ind.object_id = t.object_id where t.name in ("
                        + ruleSetTableNamesList
                        + ") and col.name in ("
                        + getCommaSeparatedColumnNames()
                        + ")) "
                        + " ORDER BY ind.name, ind.index_id, ic.index_column_id";
        try {
            Statement statement = targetConnection.createStatement();
            ResultSet rs = statement.executeQuery(selectIndexesQuery);
            while (rs.next()) {
                IndexMetadata indexMetadata =
                        new IndexMetadata(
                                rs.getBoolean(ALLOW_PAGE_LOCKS_ALIAS),
                                rs.getBoolean(ALLOW_ROW_LOCKS_ALIAS),
                                rs.getString(COLUMN_NAME_ALIAS),
                                rs.getString(FILE_GROUP_ALIAS),
                                rs.getString(INDEX_TYPE_ALIAS),
                                rs.getBoolean(IS_INCLUDED_COLUMN_ALIAS),
                                rs.getBoolean(IS_PADDED_ALIAS),
                                rs.getBoolean(IS_DESCENDING_KEY_ALIAS),
                                rs.getString(TABLE_NAME_ALIAS),
                                rs.getString(IS_UNIQUE_ALIAS));
                logService.info(
                        jobInfo.getJobId(),
                        jobInfo.getExecutionId(),
                        "Found index to drop: " + rs.getString(INDEX_NAME_ALIAS));
                indexes.put(rs.getString(INDEX_NAME_ALIAS), indexMetadata);
            }
        } catch (SQLException e) {
            throw new MaskingException("Error selecting indexes: ", e);
        }
        if (indexes.isEmpty()) {
            logService.info(
                    jobInfo.getJobId(),
                    jobInfo.getExecutionId(),
                    "Did not find any indexes to drop.");
        }
        return indexes;
    }

    private String singleQuoted(String s) {
        return "'" + s + "'";
    }

    /**
     * This method is to structure all of the tables belonging to the jobInfo.
     *
     * @return A String of comma separated table names.
     */
    private String getCommaSeparatedTableNames() {
        if (!jobInfo.getTables().isEmpty()) {
            return jobInfo.getTables().stream()
                    .map(TableInfo::getName)
                    .map(this::singleQuoted)
                    .collect(Collectors.joining(","));
        }

        /*
         * If there are no tables in the ruleset, we return an empty string quoted by single quotes to avoid an empty
         * IN clause, which would raise a SQL Server syntax exception. Since empty column names are not allowed, nothing
         * will erroneously match the empty string.
         */
        return ESCAPED_EMPTY_SINGLE_QUOTE_STRING;
    }

    /**
     * This method is to structure all of the columns belonging to the jobInfo.
     *
     * @return A String of comma separated column names.
     */
    private String getCommaSeparatedColumnNames() {
        StringBuilder resultStringBuffer = new StringBuilder();
        for (TableInfo table : jobInfo.getTables()) {
            for (ColumnInfo column : table.getColumns()) {
                resultStringBuffer.append(singleQuoted(column.getName()));
                resultStringBuffer.append(",");
            }
        }

        if (resultStringBuffer.length() > 0) {
            String commaSeparatedResult = resultStringBuffer.toString();
            return commaSeparatedResult.substring(0, commaSeparatedResult.length() - 1);
        }

        /*
         * If there are no columns in the inventory, we return an empty string quoted by single quotes to avoid an empty
         * IN clause, which would raise a SQL Server syntax exception. Since empty column names are not allowed,
         * nothing will erroneously match the empty string.
         */
        return ESCAPED_EMPTY_SINGLE_QUOTE_STRING;
    }

    @Override
    public void preJobExecute() throws MaskingException {
        this.indexes = findAllIndexes();
        try {
            Statement statement = targetConnection.createStatement();
            for (Map.Entry<String, IndexMetadata> index : indexes.entrySet()) {
                String dropIndexStatement =
                        "drop INDEX "
                                + wrapInDelimiter(index.getKey())
                                + " ON "
                                + wrapInDelimiter(targetConnection.getCatalog())
                                + "."
                                + getQualifiedTableName(index.getValue().getTableName())
                                + ";\n";
                logService.info(
                        jobInfo.getJobId(),
                        jobInfo.getExecutionId(),
                        "Query to drop index: " + dropIndexStatement);
                statement.execute(dropIndexStatement);
                logService.info(
                        jobInfo.getJobId(),
                        jobInfo.getExecutionId(),
                        "Successfully dropped index: " + index.getKey());
            }
        } catch (SQLException e) {
            throw new MaskingException("Error dropping indexes: ", e);
        }
    }

    @Override
    public void postJobExecute() throws MaskingException {
        String orderType;
        String padded = "";
        String pageLock = "";
        String rowLock = "";
        String indexUnique = "";
        String fileGroup = "";
        String indexType = "";
        String tableName = "";
        String tempIndex;
        String columnName;
        Boolean isIncludedColumn;
        String column = null;
        String include = null;
        String indexToCreate = null;

        for (Map.Entry<String, IndexMetadata> indexEntry : indexes.entrySet()) {
            IndexMetadata indexMetadata = indexEntry.getValue();
            orderType = indexMetadata.getOrderType();
            padded = indexMetadata.getIsPadded();
            pageLock = indexMetadata.getAllowPageLocks();
            rowLock = indexMetadata.getAllowRowLocks();
            indexUnique = indexMetadata.getUnique();
            fileGroup = indexMetadata.getFileGroup();
            indexType = indexMetadata.getIndexType();
            tableName = indexMetadata.getTableName();
            tempIndex = indexEntry.getKey();
            columnName = indexMetadata.getColumnName();
            isIncludedColumn = indexMetadata.getIsIncludedColumn();

            if (indexToCreate == null) {
                indexToCreate = tempIndex;
                /*
                 * There are two types of columns that exist in an MSSQL index. One type is columns that are
                 * actually used for the index. The second type is columns included with the index for
                 * convenience. We need to separate these columns out so that when we recreate the index we
                 * recreate it the same way that it was initially built.
                 */
                if (isIncludedColumn) {
                    include = String.format("[%s]", columnName);
                } else {
                    column = String.format("[%s]%s", columnName, orderType);
                }
            } else {
                /*
                 * This will only be true when a table has multiple indexes that need to be dropped. It
                 * signifies that the result set has started providing columns for the next index so
                 * the previous index has been completely read in.
                 */
                if (!indexToCreate.equalsIgnoreCase(tempIndex)) {
                    createIndex(
                            include,
                            indexUnique,
                            indexType,
                            indexToCreate,
                            tableName,
                            column,
                            padded,
                            rowLock,
                            pageLock,
                            fileGroup);
                    include = null;
                    column = null;
                    if (isIncludedColumn) {
                        include = String.format("[%s]", columnName);
                    } else {
                        column = String.format("[%s]%s", columnName, orderType);
                    }
                    indexToCreate = tempIndex;
                } else {
                    /*
                     * Depending on what the first column consisted of either the "include" or "column" string
                     * may be null still so we need to double check quickly and handle both cases.
                     */
                    if (isIncludedColumn) {
                        if (include == null) {
                            include = String.format("[%s]", columnName);
                        } else {
                            include = include + String.format(",[%s]", columnName);
                        }
                    } else {
                        if (column == null) {
                            column = String.format("[%s]%s", columnName, orderType);
                        } else {
                            column = column + String.format(",[%s]%s", columnName, orderType);
                        }
                    }
                }
            }
        }
        if (indexToCreate != null && indexToCreate.trim().length() > 0) {
            createIndex(
                    include,
                    indexUnique,
                    indexType,
                    indexToCreate,
                    tableName,
                    column,
                    padded,
                    rowLock,
                    pageLock,
                    fileGroup);
        }
    }

    private void createIndex(
            String include,
            String indexUnique,
            String indexType,
            String indexToCreate,
            String tableName,
            String column,
            String padded,
            String rowLock,
            String pageLock,
            String fileGroup)
            throws MaskingException {
        try (Statement statement = targetConnection.createStatement()) {
            String includesStatement =
                    include != null ? String.format("INCLUDE (%s)", include) : "";
            String createIndexScripts =
                    "create "
                            + indexUnique
                            + " "
                            + indexType
                            + " INDEX "
                            + wrapInDelimiter(indexToCreate)
                            + " ON "
                            + wrapInDelimiter(targetConnection.getCatalog())
                            + "."
                            + getQualifiedTableName(tableName)
                            + " ( "
                            + column
                            + " ) "
                            + includesStatement
                            + " with ( PAD_INDEX  = "
                            + padded
                            + " , STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = "
                            + rowLock
                            + " , ALLOW_PAGE_LOCKS = "
                            + pageLock
                            + ")"
                            + " ON "
                            + wrapInDelimiter(fileGroup)
                            + " ;\n";
            logService.info(
                    jobInfo.getJobId(),
                    jobInfo.getExecutionId(),
                    "Statement to create index: " + createIndexScripts);
            statement.execute(createIndexScripts);
            logService.info(
                    jobInfo.getJobId(),
                    jobInfo.getExecutionId(),
                    "Successfully recreated index: " + indexToCreate);
        } catch (SQLException e) {
            throw new MaskingException("Failed to recreate indexes: " + e);
        }
    }

    private class IndexMetadata {
        private String allowPageLocks;
        private String allowRowLocks;
        private String columnName;
        private String fileGroup;
        private String indexType;
        private Boolean isIncludedColumn;
        private String isPadded;
        private String orderType;
        private String tableName;
        private String unique;

        public IndexMetadata(
                Boolean allowPageLocks,
                Boolean allowRowLocks,
                String columnName,
                String fileGroup,
                String indexType,
                Boolean isIncludedColumn,
                Boolean isPadded,
                Boolean orderType,
                String tableName,
                String unique) {
            this.allowPageLocks = allowPageLocks ? "ON" : "OFF";
            this.allowRowLocks = allowRowLocks ? "ON" : "OFF";
            this.columnName = columnName;
            this.fileGroup = fileGroup != null && fileGroup.trim().length() > 0 ? fileGroup : "";
            this.indexType = indexType;
            this.isIncludedColumn = isIncludedColumn;
            this.isPadded = isPadded ? "ON" : "OFF";
            this.orderType = orderType ? "DESC" : "ASC";
            this.tableName = tableName;
            this.unique = unique != null && unique.equalsIgnoreCase("1") ? "UNIQUE" : "";
        }

        String getAllowPageLocks() {
            return allowPageLocks;
        }

        String getAllowRowLocks() {
            return allowRowLocks;
        }

        String getColumnName() {
            return columnName;
        }

        String getFileGroup() {
            return fileGroup;
        }

        String getIndexType() {
            return indexType;
        }

        Boolean getIsIncludedColumn() {
            return isIncludedColumn;
        }

        String getIsPadded() {
            return isPadded;
        }

        String getOrderType() {
            return orderType;
        }

        String getTableName() {
            return tableName;
        }

        String getUnique() {
            return unique;
        }
    }
}
