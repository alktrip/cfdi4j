package io.github.alktrip.cfdi4j.xml.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import javax.xml.stream.XMLStreamWriter;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

class FragmentsTest {

    private static final String DOC = """
            <root xmlns:a="http://example.com/a" xmlns:b="http://example.com/b" xmlns="http://example.com/default">
              <a:item b:attr="1" xml:lang="es" plain="x">
                <!-- comment --><?pi data?><![CDATA[<raw>]]>text<inner>default ns</inner>
                <b:deep a:ref="2"/>
              </a:item>
            </root>
            """;

    @Test
    void fragmentDeclaresEveryNamespaceDeclaredOnlyOnAncestors() throws Exception {
        Element item = firstChildElement(parse(DOC).getDocumentElement());

        String xml = Fragments.toXml(item);

        assertThat(xml).startsWith("<a:item")
                .contains("xmlns:a=\"http://example.com/a\"")
                .contains("xmlns:b=\"http://example.com/b\"")
                .contains("xmlns=\"http://example.com/default\"")
                .doesNotContain("xmlns:xml")
                .contains("xml:lang=\"es\"")
                .contains("<!-- comment -->")
                .contains("<?pi data?>");
        // The fragment is well-formed on its own and means the same thing.
        Element reparsed = parse(xml).getDocumentElement();
        assertThat(reparsed.getNamespaceURI()).isEqualTo("http://example.com/a");
        assertThat(reparsed.getAttributeNS("http://example.com/b", "attr")).isEqualTo("1");
    }

    @Test
    void writeReplaysElementsAttributesTextAndComments() throws Exception {
        String fragment = Fragments.toXml(firstChildElement(parse(DOC).getDocumentElement()));
        StringWriter out = new StringWriter();
        XMLStreamWriter writer = SecureXml.outputFactory().createXMLStreamWriter(out);

        writer.writeStartElement("wrapper");
        Fragments.write(fragment + "<b:second xmlns:b=\"http://example.com/b\"/>", writer);
        writer.writeEndElement();
        writer.close();

        assertThat(out.toString())
                .startsWith("<wrapper><a:item")
                .contains("b:attr=\"1\"")
                .contains("plain=\"x\"")
                .contains("<!-- comment -->")
                .contains("<?pi data?>")
                .contains("<inner>default ns</inner>")
                .contains("a:ref=\"2\"")
                .contains("<b:second xmlns:b=\"http://example.com/b\">")
                // CDATA content is kept, escaped instead of wrapped: the same character data.
                .contains("&lt;raw&gt;text")
                .endsWith("</wrapper>");
        assertThat(parse(out.toString()).getDocumentElement().getLocalName()).isEqualTo("wrapper");
    }

    private static Document parse(String xml) throws Exception {
        return SecureXml.documentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private static Element firstChildElement(Element parent) {
        for (var n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element e) {
                return e;
            }
        }
        throw new AssertionError("no child element");
    }
}
