package io.github.alktrip.cfdi4j.crypto.internal;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Minimal DER reader for the few structures cfdi4j needs to read itself: PBES2 parameters of an
 * encrypted PKCS#8 key and the attributes of an X.509 name. Any malformed input throws
 * {@link IllegalArgumentException}.
 */
public final class Der {

    public static final int INTEGER = 0x02;
    public static final int OCTET_STRING = 0x04;
    public static final int NULL = 0x05;
    public static final int OID = 0x06;
    public static final int UTF8_STRING = 0x0c;
    public static final int PRINTABLE_STRING = 0x13;
    public static final int TELETEX_STRING = 0x14;
    public static final int IA5_STRING = 0x16;
    public static final int BMP_STRING = 0x1e;
    public static final int SEQUENCE = 0x30;
    public static final int SET = 0x31;

    /** A decoded tag-length-value. */
    public static final class Tlv {
        private final int tag;
        private final byte[] value;

        Tlv(int tag, byte[] value) {
            this.tag = tag;
            this.value = value;
        }

        public int tag() {
            return tag;
        }

        public List<Tlv> children() {
            List<Tlv> result = new ArrayList<>();
            int pos = 0;
            while (pos < value.length) {
                int[] next = new int[1];
                result.add(read(value, pos, next));
                pos = next[0];
            }
            return result;
        }

        public Tlv expect(int expectedTag) {
            if (tag != expectedTag) {
                throw new IllegalArgumentException("expected DER tag 0x" + Integer.toHexString(expectedTag) + " but found 0x" + Integer.toHexString(tag));
            }
            return this;
        }

        public String oid() {
            expect(OID);
            if (value.length == 0) {
                throw new IllegalArgumentException("empty OID");
            }
            StringBuilder sb = new StringBuilder();
            long current = 0;
            boolean first = true;
            for (byte b : value) {
                current = (current << 7) | (b & 0x7f);
                if ((b & 0x80) == 0) {
                    if (first) {
                        long head = Math.min(current / 40, 2);
                        sb.append(head).append('.').append(current - head * 40);
                        first = false;
                    } else {
                        sb.append('.').append(current);
                    }
                    current = 0;
                }
            }
            return sb.toString();
        }

        public BigInteger integer() {
            expect(INTEGER);
            return new BigInteger(value);
        }

        public byte[] octets() {
            expect(OCTET_STRING);
            return value.clone();
        }

        /** Decodes the directory string types used in X.509 names. */
        public String text() {
            return switch (tag) {
                case UTF8_STRING -> new String(value, StandardCharsets.UTF_8);
                case PRINTABLE_STRING, IA5_STRING -> new String(value, StandardCharsets.US_ASCII);
                case TELETEX_STRING -> new String(value, StandardCharsets.ISO_8859_1);
                case BMP_STRING -> new String(value, StandardCharsets.UTF_16BE);
                default -> throw new IllegalArgumentException("not a string type: 0x" + Integer.toHexString(tag));
            };
        }
    }

    private Der() {}

    /** Reads exactly one TLV that spans the whole input. */
    public static Tlv parse(byte[] der) {
        int[] next = new int[1];
        Tlv tlv = read(der, 0, next);
        if (next[0] != der.length) {
            throw new IllegalArgumentException("trailing data after DER value");
        }
        return tlv;
    }

    static Tlv read(byte[] data, int offset, int[] next) {
        if (offset + 2 > data.length) {
            throw new IllegalArgumentException("truncated DER value");
        }
        int tag = data[offset] & 0xff;
        if ((tag & 0x1f) == 0x1f) {
            throw new IllegalArgumentException("multi-byte DER tags are not supported");
        }
        int pos = offset + 1;
        int length = data[pos++] & 0xff;
        if ((length & 0x80) != 0) {
            int count = length & 0x7f;
            if (count == 0 || count > 3 || pos + count > data.length) {
                throw new IllegalArgumentException("unsupported DER length");
            }
            length = 0;
            for (int i = 0; i < count; i++) {
                length = (length << 8) | (data[pos++] & 0xff);
            }
        }
        if (pos + length > data.length) {
            throw new IllegalArgumentException("truncated DER value");
        }
        next[0] = pos + length;
        return new Tlv(tag, Arrays.copyOfRange(data, pos, pos + length));
    }
}
