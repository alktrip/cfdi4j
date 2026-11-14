package io.github.alktrip.cfdi4j.crypto.internal;

import static io.github.alktrip.cfdi4j.crypto.internal.PrivateKeysTest.oid;
import static io.github.alktrip.cfdi4j.crypto.internal.PrivateKeysTest.seq;
import static io.github.alktrip.cfdi4j.crypto.internal.PrivateKeysTest.tlv;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class DerTest {

    @Test
    void decodesOids() {
        assertThat(Der.parse(oid("1.2.840.113549.1.5.13")).oid()).isEqualTo("1.2.840.113549.1.5.13");
        assertThat(Der.parse(oid("2.5.4.45")).oid()).isEqualTo("2.5.4.45");
        assertThat(Der.parse(oid("2.999.3")).oid()).isEqualTo("2.999.3");
        assertThatIllegalArgumentException().isThrownBy(() -> Der.parse(tlv(Der.OID, new byte[0])).oid());
    }

    @Test
    void decodesStringTypes() {
        assertThat(Der.parse(tlv(Der.UTF8_STRING, "Ñandú".getBytes(StandardCharsets.UTF_8))).text()).isEqualTo("Ñandú");
        assertThat(Der.parse(tlv(Der.PRINTABLE_STRING, "EKU9003173C9".getBytes(StandardCharsets.US_ASCII))).text()).isEqualTo("EKU9003173C9");
        assertThat(Der.parse(tlv(Der.IA5_STRING, "a@b".getBytes(StandardCharsets.US_ASCII))).text()).isEqualTo("a@b");
        assertThat(Der.parse(tlv(Der.TELETEX_STRING, "é".getBytes(StandardCharsets.ISO_8859_1))).text()).isEqualTo("é");
        assertThat(Der.parse(tlv(Der.BMP_STRING, "Ω".getBytes(StandardCharsets.UTF_16BE))).text()).isEqualTo("Ω");
        assertThatIllegalArgumentException().isThrownBy(() -> Der.parse(tlv(Der.INTEGER, new byte[] {1})).text());
    }

    @Test
    void readsLongLengthsAndChildren() {
        byte[] big = tlv(Der.OCTET_STRING, new byte[300]);

        assertThat(Der.parse(big).octets()).hasSize(300);
        assertThat(Der.parse(seq(big, tlv(Der.INTEGER, new byte[] {5}))).children()).hasSize(2);
        assertThat(Der.parse(seq(big)).children().get(0).tag()).isEqualTo(Der.OCTET_STRING);
    }

    @Test
    void rejectsMalformedInput() {
        assertThatIllegalArgumentException().isThrownBy(() -> Der.parse(new byte[] {0x30})).withMessageContaining("truncated");
        assertThatIllegalArgumentException().isThrownBy(() -> Der.parse(new byte[] {0x04, 0x05, 1})).withMessageContaining("truncated");
        assertThatIllegalArgumentException().isThrownBy(() -> Der.parse(new byte[] {0x04, 0x01, 1, 2})).withMessageContaining("trailing");
        assertThatIllegalArgumentException().isThrownBy(() -> Der.parse(new byte[] {0x1f, 0x01, 1})).withMessageContaining("multi-byte");
        assertThatIllegalArgumentException().isThrownBy(() -> Der.parse(new byte[] {0x04, (byte) 0x80})).withMessageContaining("unsupported DER length");
        assertThatIllegalArgumentException().isThrownBy(() -> Der.parse(new byte[] {0x04, (byte) 0x84, 0, 0, 0, 1})).withMessageContaining("unsupported DER length");
        assertThatIllegalArgumentException().isThrownBy(() -> Der.parse(tlv(Der.INTEGER, new byte[] {1})).expect(Der.SEQUENCE))
                .withMessageContaining("expected DER tag 0x30");
    }
}
