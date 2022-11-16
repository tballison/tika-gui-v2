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
package org.tallison.tika.gui.tools.deprecated;

import org.junit.jupiter.api.Test;
import org.tallison.tika.gui.tools.deprecated.SnapshotParser;
import org.tallison.tika.gui.tools.deprecated.SnapshotResult;

public class SnapshotParserTest {

    @Test
    public void testBasic() throws Exception {

        SnapshotParser p = new SnapshotParser();
        String base = "https://repository.apache.org/content/groups/snapshots/";
        SnapshotResult result = p.parse(base + "org/apache/tika/tika-core");
        System.out.println(result);
    }
}
