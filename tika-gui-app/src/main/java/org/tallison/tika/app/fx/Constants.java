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

    public static final String FS_FETCHER_CLASS = FileSystemFetcher.class.getName();

    public static final String NO_DIGEST = "No Digest";
    //Used by filesystem emitter, fetcher and pipes iterator
    //used as working directory for csv emitter
    public static final String BASE_PATH = "basePath";

    public static final String DEFAULT_METADATA_MAPPINGS = "default-metadata-mappings.csv";

    // TODO -- put this into the fs fetcher class once we refactor to add that
    public static final String EXTRACT_FILE_SYSTEM_METADATA = "extractFileSystemMetadata";
}
