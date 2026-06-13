package com.nitrotech.api.infrastructure.shipping.ghtk;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

@Component
public class GhtkAddressNormalizer {

    public static final String DEFAULT_HAMLET = "Khác";

    private static final Map<String, String> PROVINCE_ALIASES = Map.of(
            "hcm", "TP. Hồ Chí Minh",
            "tp hcm", "TP. Hồ Chí Minh",
            "ho chi minh", "TP. Hồ Chí Minh",
            "hồ chí minh", "TP. Hồ Chí Minh",
            "tphcm", "TP. Hồ Chí Minh",
            "ha noi", "Hà Nội",
            "hà nội", "Hà Nội"
    );

    public String normalizeAddress(String value) {
        return normalizeWhitespace(value);
    }

    public String normalizeProvince(String value) {
        String normalized = normalizeWhitespace(value);
        if (normalized == null) {
            return null;
        }
        return PROVINCE_ALIASES.getOrDefault(key(normalized), normalized);
    }

    public String normalizeDistrict(String value) {
        String normalized = normalizeWhitespace(value);
        if (normalized == null) {
            return null;
        }
        String key = key(normalized);
        if (key.matches("q\\d+")) {
            return "Quận " + key.substring(1);
        }
        if (key.matches("quan\\s*\\d+")) {
            return "Quận " + key.replaceAll("\\D+", "");
        }
        return normalized;
    }

    public String normalizeWard(String value) {
        String normalized = normalizeWhitespace(value);
        if (normalized == null) {
            return null;
        }
        String key = key(normalized);
        if (key.matches("p\\d+")) {
            return "Phường " + key.substring(1);
        }
        if (key.matches("phuong\\s*\\d+")) {
            return "Phường " + key.replaceAll("\\D+", "");
        }
        return normalized;
    }

    private String normalizeWhitespace(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private String key(String value) {
        return normalizeWhitespace(value)
                .toLowerCase(Locale.ROOT)
                .replace(".", "");
    }
}
