package org.tallison.tika.app.fx.sax;

import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class DomWriter {

    private final Document document;

    public DomWriter(Document document) {
        this.document = document;
    }

    public void appendMap(Element parent, String mappingsElementName,
                           String mappingElementName, Map<String, String> map, String... attrs) {
        Element mappings = createAndGetElement(parent, mappingsElementName, attrs);
        for (Map.Entry<String, String> e : map.entrySet()) {
            appendLeafElement(mappings, mappingElementName, "from", e.getKey(), "to",
                    e.getValue());
        }
    }

    public void appendTextElement(Element parent, String itemName, String text,
                                   String... attrs) {
        Element el = createAndGetElement(parent, itemName, attrs);
        el.setTextContent(text);
    }

    public Element createAndGetElement(Element parent, String elementName,
                                        String... attrs) {
        Element el = document.createElement(elementName);
        parent.appendChild(el);
        for (int i = 0; i < attrs.length; i += 2) {
            el.setAttribute(attrs[i], attrs[i + 1]);
        }
        return el;
    }

    public void appendLeafElement(Element parent, String elementName,
                                   String... attrs) {
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
