package com.nitrotech.api.infrastructure.shipping.ghn;

import com.nitrotech.api.infrastructure.shipping.ghn.dto.GhnDistrictResponse;
import com.nitrotech.api.infrastructure.shipping.ghn.dto.GhnProvinceResponse;
import com.nitrotech.api.infrastructure.shipping.ghn.dto.GhnWardResponse;
import com.nitrotech.api.shared.exception.ShippingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class GhnAddressResolver {

    private final GhnClient ghnClient;

    // Cache mappings: GSO Code -> GHN ID/Code
    private final Map<String, Integer> provinceCache = new ConcurrentHashMap<>();
    private final Map<Integer, Map<String, Integer>> districtCache = new ConcurrentHashMap<>();
    private final Map<Integer, Map<String, String>> wardCache = new ConcurrentHashMap<>();

    public Integer getProvinceId(String gsoProvinceCode, String provinceName) {
        if (gsoProvinceCode == null) return null;
        String cleanGsoCode = gsoProvinceCode.trim();
        Integer cached = provinceCache.get(cleanGsoCode);
        if (cached != null) {
            return cached;
        }

        synchronized (provinceCache) {
            cached = provinceCache.get(cleanGsoCode);
            if (cached != null) {
                return cached;
            }

            for (Map.Entry<String, Integer> entry : provinceCache.entrySet()) {
                if (matchGsoCode(entry.getKey(), cleanGsoCode)) {
                    Integer matchedId = entry.getValue();
                    provinceCache.put(cleanGsoCode, matchedId);
                    return matchedId;
                }
            }

            log.info("Fetching provinces from GHN to map GSO code: {}", cleanGsoCode);
            GhnProvinceResponse response = ghnClient.getProvinces();
            if (response == null || response.getData() == null) {
                throw new ShippingException("GHN_ADDRESS_ERROR", "Failed to fetch provinces from GHN");
            }

            for (var p : response.getData()) {
                if (p.getCode() != null) {
                    provinceCache.put(p.getCode().trim(), p.getProvinceID());
                }
            }

            Integer matchedId = null;
            for (var p : response.getData()) {
                if (p.getCode() != null && matchGsoCode(p.getCode(), cleanGsoCode)) {
                    matchedId = p.getProvinceID();
                    provinceCache.put(cleanGsoCode, matchedId);
                    break;
                }
            }

            if (matchedId == null) {
                String normSearchName = normalize(provinceName);
                for (var p : response.getData()) {
                    if (normalize(p.getProvinceName()).contains(normSearchName) || 
                        normSearchName.contains(normalize(p.getProvinceName()))) {
                        matchedId = p.getProvinceID();
                        provinceCache.put(cleanGsoCode, matchedId);
                        break;
                    }
                }
            }

            if (matchedId == null) {
                throw new ShippingException("INVALID_ADDRESS_CODE", "Cannot map province GSO code: " + gsoProvinceCode);
            }
            return matchedId;
        }
    }

    public Integer getDistrictId(Integer ghnProvinceId, String gsoDistrictCode, String districtName) {
        if (ghnProvinceId == null || gsoDistrictCode == null) return null;
        String cleanGsoCode = gsoDistrictCode.trim();
        
        var provinceDistricts = districtCache.computeIfAbsent(ghnProvinceId, k -> new ConcurrentHashMap<>());
        Integer cached = provinceDistricts.get(cleanGsoCode);
        if (cached != null) {
            return cached;
        }

        synchronized (provinceDistricts) {
            cached = provinceDistricts.get(cleanGsoCode);
            if (cached != null) {
                return cached;
            }

            for (Map.Entry<String, Integer> entry : provinceDistricts.entrySet()) {
                if (matchGsoCode(entry.getKey(), cleanGsoCode)) {
                    Integer matchedId = entry.getValue();
                    provinceDistricts.put(cleanGsoCode, matchedId);
                    return matchedId;
                }
            }

            log.info("Fetching districts for province {} from GHN to map GSO code: {}", ghnProvinceId, cleanGsoCode);
            GhnDistrictResponse response = ghnClient.getDistricts(ghnProvinceId);
            if (response == null || response.getData() == null) {
                throw new ShippingException("GHN_ADDRESS_ERROR", "Failed to fetch districts from GHN");
            }

            for (var d : response.getData()) {
                if (d.getCode() != null) {
                    provinceDistricts.put(d.getCode().trim(), d.getDistrictID());
                }
            }

            Integer matchedId = null;
            for (var d : response.getData()) {
                if (d.getCode() != null && matchGsoCode(d.getCode(), cleanGsoCode)) {
                    matchedId = d.getDistrictID();
                    provinceDistricts.put(cleanGsoCode, matchedId);
                    break;
                }
            }

            if (matchedId == null) {
                String normSearchName = normalize(districtName);
                for (var d : response.getData()) {
                    if (normalize(d.getDistrictName()).contains(normSearchName) || 
                        normSearchName.contains(normalize(d.getDistrictName()))) {
                        matchedId = d.getDistrictID();
                        provinceDistricts.put(cleanGsoCode, matchedId);
                        break;
                    }
                }
            }

            if (matchedId == null) {
                throw new ShippingException("INVALID_ADDRESS_CODE", "Cannot map district GSO code: " + gsoDistrictCode + " in province ID " + ghnProvinceId);
            }
            return matchedId;
        }
    }

    public String getWardCode(Integer ghnDistrictId, String wardName) {
        if (ghnDistrictId == null || wardName == null) return null;
        
        var districtWards = wardCache.computeIfAbsent(ghnDistrictId, k -> new ConcurrentHashMap<>());
        String normSearchName = normalize(wardName);
        String cached = districtWards.get(normSearchName);
        if (cached != null) {
            return cached;
        }

        synchronized (districtWards) {
            cached = districtWards.get(normSearchName);
            if (cached != null) {
                return cached;
            }

            log.info("Fetching wards for district {} from GHN to map ward name: {}", ghnDistrictId, wardName);
            GhnWardResponse response = ghnClient.getWards(ghnDistrictId);
            if (response == null || response.getData() == null) {
                throw new ShippingException("GHN_ADDRESS_ERROR", "Failed to fetch wards from GHN");
            }

            for (var w : response.getData()) {
                String normWardName = normalize(w.getWardName());
                districtWards.put(normWardName, w.getWardCode());
            }

            String matchedCode = districtWards.get(normSearchName);
            if (matchedCode == null) {
                for (var w : response.getData()) {
                    String normWardName = normalize(w.getWardName());
                    if (normWardName.contains(normSearchName) || normSearchName.contains(normWardName)) {
                        matchedCode = w.getWardCode();
                        districtWards.put(normSearchName, matchedCode);
                        break;
                    }
                }
            }

            if (matchedCode == null) {
                List<String> candidates = response.getData().stream()
                        .map(w -> w.getWardName() + " (" + w.getWardCode() + ")")
                        .toList();
                log.error("Failed to map ward name: '{}' (normalized: '{}') in GHN district ID: {}. Available candidates: {}",
                        wardName, normSearchName, ghnDistrictId, candidates);
                throw new ShippingException("GHN_WARD_NOT_FOUND", 
                        "Cannot resolve ward name '" + wardName + "' in GHN district ID " + ghnDistrictId);
            }
            return matchedCode;
        }
    }

    private boolean matchGsoCode(String code1, String code2) {
        if (code1 == null || code2 == null) return false;
        try {
            return Integer.parseInt(code1.trim()) == Integer.parseInt(code2.trim());
        } catch (NumberFormatException e) {
            return code1.trim().equalsIgnoreCase(code2.trim());
        }
    }

    private String normalize(String str) {
        if (str == null) return "";
        String temp = Normalizer.normalize(str, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        String normalized = pattern.matcher(temp).replaceAll("")
                .toLowerCase()
                .replace("đ", "d")
                .replaceAll("[^a-z0-9]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        normalized = normalized.replaceAll("^(tinh|thanh pho|tp|quand|quan|huyen|thi xa|phuong|xa|thi tran)\\s+", "");
        return normalized.trim();
    }
}
