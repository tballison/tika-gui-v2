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
package org.tallison.tika.app.fx.sax;

import java.io.IOException;
import java.io.InputStream;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import org.apache.tika.exception.TikaException;
import org.apache.tika.utils.XMLReaderUtils;

public class XMLStringToDOM {

    public static void write(DomWriter writer, Element writerRoot, InputStream is)
            throws TikaException, IOException, SAXException {
        Document document = XMLReaderUtils.buildDOM(is);
        Element documentRoot = document.getDocumentElement();
        writer.appendChild(writerRoot, documentRoot);
    }
}
