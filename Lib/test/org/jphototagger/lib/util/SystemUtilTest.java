package org.jphototagger.lib.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class SystemUtilTest {

    @ParameterizedTest
    @CsvSource({
        "1.7.0_80, 1, 7",
        "1.8.0_292, 1, 8",
        "9.0.4, 9, 0",
        "11.0.11, 11, 0",
        "17.0.1, 17, 0",
        "21, 21, 0",
        "21.0.1, 21, 0"
    })
    void parseJavaVersion_handlesAllFormats(String versionString, int expectedMajor, int expectedMinor) {
        Version version = SystemUtil.parseJavaVersion(versionString);

        assertThat(version).isNotNull();
        assertThat(version.getMajor()).isEqualTo(expectedMajor);
        assertThat(version.getMinor1()).isEqualTo(expectedMinor);
    }

    @Test
    void getJavaVersion_returnsNonNull() {
        Version version = SystemUtil.getJavaVersion();
        assertThat(version).isNotNull();
        assertThat(version.getMajor()).isGreaterThanOrEqualTo(1);
    }
}
