/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.rm.datasource.undo.postgresql;

import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.rm.datasource.sql.struct.Field;
import io.seata.rm.datasource.sql.struct.KeyType;
import io.seata.rm.datasource.sql.struct.Row;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.rm.datasource.undo.AbstractUndoExecutor;
import io.seata.rm.datasource.undo.KeywordChecker;
import io.seata.rm.datasource.undo.KeywordCheckerFactory;
import io.seata.rm.datasource.undo.SQLUndoLog;
import io.seata.sqlparser.util.JdbcConstants;
import java.util.List;

/**
 * The type postgresql undo delete executor.
 *
 * @author japsercloud
 */
public class PostgresqlUndoDeleteExecutor extends AbstractUndoExecutor {

    /**
     * Instantiates a new postgresql undo delete executor.
     *
     * @param sqlUndoLog the sql undo log
     */
    public PostgresqlUndoDeleteExecutor(SQLUndoLog sqlUndoLog) {
        super(sqlUndoLog);
    }

    @Override
    protected String buildUndoSQL() {
        KeywordChecker keywordChecker = KeywordCheckerFactory.getKeywordChecker(JdbcConstants.POSTGRESQL);
        TableRecords beforeImage = sqlUndoLog.getBeforeImage();
        List<Row> beforeImageRows = beforeImage.getRows();
        if (beforeImageRows == null || beforeImageRows.size() == 0) {
            throw new ShouldNeverHappenException("Invalid UNDO LOG");
        }
        Row row = beforeImageRows.get(0);

        StringBuilder insertColumns = new StringBuilder();
        StringBuilder insertValues = new StringBuilder();
        Field pkField = null;
        boolean first = true;
        for (Field field : row.getFields()) {
            if (field.getKeyType() == KeyType.PRIMARY_KEY) {
                pkField = field;
                continue;
            } else {
                if (first) {
                    first = false;
                } else {
                    insertColumns.append(", ");
                    insertValues.append(", ");
                }
                insertColumns.append(keywordChecker.checkAndReplace(field.getName()));
                insertValues.append("?");
            }

        }
        if (!first) {
            insertColumns.append(", ");
            insertValues.append(", ");
        }
        insertColumns.append(keywordChecker.checkAndReplace(pkField.getName()));
        insertValues.append("?");

        return "INSERT INTO " + keywordChecker.checkAndReplace(sqlUndoLog.getTableName()) + "(" + insertColumns.toString() + ") VALUES (" + insertValues.toString() + ")";
    }

    @Override
    protected TableRecords getUndoRows() {
        return sqlUndoLog.getBeforeImage();
    }
}