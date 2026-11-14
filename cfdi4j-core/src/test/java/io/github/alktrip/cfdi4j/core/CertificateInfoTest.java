package io.github.alktrip.cfdi4j.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class CertificateInfoTest {

    private static final Instant FROM = Instant.parse("2023-05-18T11:43:51Z");
    private static final Instant UNTIL = Instant.parse("2027-05-18T11:43:51Z");
    private static final CertificateInfo INFO = new CertificateInfo("30001000000500003416", "EKU9003173C9",
            "ESCUELA KEMPER URGATE SA DE CV", FROM, UNTIL, CertificateType.CSD, "CN=AC UAT");

    @Test
    void validityIncludesBothEnds() {
        assertThat(INFO.isValidAt(FROM)).isTrue();
        assertThat(INFO.isValidAt(UNTIL)).isTrue();
        assertThat(INFO.isValidAt(FROM.minusSeconds(1))).isFalse();
        assertThat(INFO.isValidAt(UNTIL.plusSeconds(1))).isFalse();
    }
}
