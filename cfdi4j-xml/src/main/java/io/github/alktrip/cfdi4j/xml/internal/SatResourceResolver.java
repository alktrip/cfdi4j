package io.github.alktrip.cfdi4j.xml.internal;

import java.io.InputStream;
import java.io.Reader;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

/**
 * Resolves {@code xs:import} and {@code xs:include} of the SAT schemas to bundled resources
 * (ADR 0007). Any other location is an error, never a download.
 */
public final class SatResourceResolver implements LSResourceResolver {

    @Override
    public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
        return new Input(publicId, systemId, baseURI, SatResources.open(systemId));
    }

    /** Minimal {@link LSInput} over a byte stream. */
    private static final class Input implements LSInput {
        private String publicId;
        private String systemId;
        private String baseURI;
        private InputStream byteStream;

        Input(String publicId, String systemId, String baseURI, InputStream byteStream) {
            this.publicId = publicId;
            this.systemId = systemId;
            this.baseURI = baseURI;
            this.byteStream = byteStream;
        }

        @Override
        public Reader getCharacterStream() {
            return null;
        }

        @Override
        public void setCharacterStream(Reader characterStream) {
            throw new UnsupportedOperationException();
        }

        @Override
        public InputStream getByteStream() {
            return byteStream;
        }

        @Override
        public void setByteStream(InputStream byteStream) {
            this.byteStream = byteStream;
        }

        @Override
        public String getStringData() {
            return null;
        }

        @Override
        public void setStringData(String stringData) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getSystemId() {
            return systemId;
        }

        @Override
        public void setSystemId(String systemId) {
            this.systemId = systemId;
        }

        @Override
        public String getPublicId() {
            return publicId;
        }

        @Override
        public void setPublicId(String publicId) {
            this.publicId = publicId;
        }

        @Override
        public String getBaseURI() {
            return baseURI;
        }

        @Override
        public void setBaseURI(String baseURI) {
            this.baseURI = baseURI;
        }

        @Override
        public String getEncoding() {
            return null;
        }

        @Override
        public void setEncoding(String encoding) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean getCertifiedText() {
            return false;
        }

        @Override
        public void setCertifiedText(boolean certifiedText) {
            throw new UnsupportedOperationException();
        }
    }
}
