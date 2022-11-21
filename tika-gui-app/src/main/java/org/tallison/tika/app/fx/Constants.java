/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tallison.tika.app.fx;

import org.apache.tika.pipes.fetcher.fs.FileSystemFetcher;

public class Constants {
    public static final String FS_EMITTER_CLASS =
            "org.apache.tika.pipes.emitter.fs.FileSystemEmitter";
    public static final String FS_FETCHER_CLASS = FileSystemFetcher.class.getName();

    //an imaginary class
    public static final String CSV_EMITTER_CLASS = "org.apache.tika.pipes.emitter.csv.CSVEmitter";

    public static final String OPEN_SEARCH_EMITTER_CLASS =
            "org.apache.tika.pipes.emitter.opensearch.OpenSearchEmitter";
    public static final String JDBC_EMITTER_CLASS =
            "org.apache.tika.pipes.emitter.jdbc.JDBCEmitter";

    public static final String NO_DIGEST = "No Digest";

    //JDBC emitter

    // if the table doesn't exist, we create it via the dialog
    // public static final String JDBC_TABLE_CREATED = "tableCreated";
    public static final String JDBC_CONNECTION_STRING = "jdbcConnection";
    public static final String JDBC_TABLE_NAME = "jdbcTableName";

    public static final String JDBC_INSERT_SQL = "jdbcInsertString";
    public static final String CSV_JDBC_INSERT_SQL = "csvInsertString";


    //AbstractEmitterController
    public static final String CSV_METADATA_PATH = "csvMetadataPath";

    public static final String CSV_JDBC_CONNECTION_STRING = "csvSQLConnectionString";

    public static final String CSV_DB_DIRECTORY = "csvDBDirectory";

    //Used by filesystem emitter, fetcher and pipes iterator
    //used as working directory for csv emitter
    public static final String BASE_PATH = "basePath";


    //opensearch
    public static final String OPEN_SEARCH_URL = "openSearchUrl";
    public static final String OPEN_SEARCH_USER = "userName";
    public static final String OPEN_SEARCH_PW = "password";
    public static final String OPEN_SEARCH_UPDATE_STRATEGY = "updateStrategy";

    //csv emitter
    public static final String CSV_DB_TABLE_NAME = "tika_table";
    public static final String CSV_FILE_NAME = "csvFileName";

}
