package com.nitrotech.api.infrastructure.shipping.ghtk;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GhtkAddressNormalizerTest {

    private final GhtkAddressNormalizer normalizer = new GhtkAddressNormalizer();

    @Test
    void normalizesCommonProvinceAliases() {
        assertThat(normalizer.normalizeProvince("HCM")).isEqualTo("TP. Hồ Chí Minh");
        assertThat(normalizer.normalizeProvince("tp. hcm")).isEqualTo("TP. Hồ Chí Minh");
        assertThat(normalizer.normalizeProvince("Ha Noi")).isEqualTo("Hà Nội");
    }

    @Test
    void expandsNumericDistrictAndWardShortcuts() {
        assertThat(normalizer.normalizeDistrict("Q1")).isEqualTo("Quận 1");
        assertThat(normalizer.normalizeDistrict("quan 3")).isEqualTo("Quận 3");
        assertThat(normalizer.normalizeWard("P1")).isEqualTo("Phường 1");
        assertThat(normalizer.normalizeWard("phuong 7")).isEqualTo("Phường 7");
    }

    @Test
    void keepsNamedWardBecauseGhtkAcceptsTextAddressNames() {
        assertThat(normalizer.normalizeWard("Ben Nghe")).isEqualTo("Ben Nghe");
    }
}
