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

import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class DomWriter {

    private final Document document;

    public DomWriter(Document document) {
        this.document = document;
    }

    public void appendChild(Element rootElement, Element child) {
        Node newNode = document.importNode(child, true);
        rootElement.appendChild(newNode);
    }

    public void appendMap(Element parent, String mappingsElementName, String mappingElementName,
                          Map<String, String> map, String... attrs) {
        Element mappings = createAndGetElement(parent, mappingsElementName, attrs);
        for (Map.Entry<String, String> e : map.entrySet()) {
            appendLeafElement(mappings, mappingElementName, "from", e.getKey(), "to", e.getValue());
        }

    }

    public void appendTextElement(Element parent, String itemName, String text, String... attrs) {
        Element el = createAndGetElement(parent, itemName, attrs);
        el.setTextContent(text);
    }

    public Element createAndGetElement(Element parent, String elementName, String... attrs) {
        Element el = document.createElement(elementName);
        parent.appendChild(el);
        for (int i = 0; i < attrs.length; i += 2) {
            el.setAttribute(attrs[i], attrs[i + 1]);
        }
        return el;
    }

    public void appendLeafElement(Element parent, String elementName, String... attrs) {
        createAndGetElement(parent, elementName, attrs);
    }

    public void appendListElement(Element parent, String itemNames, String itemName,
                                  String... elements) {
        Element items = createAndGetElement(parent, itemNames);
        for (String element : elements) {
            Element item = createAndGetElement(items, itemName);
            item.setTextContent(element);
        }
    }
}
