package com.nitrotech.api.infrastructure.shipping.ghn;

import com.nitrotech.api.domain.order.dto.ShippingAddressSnapshot;
import com.nitrotech.api.infrastructure.persistence.entity.CarrierAddressMappingEntity;
import com.nitrotech.api.infrastructure.persistence.repository.CarrierAddressMappingJpaRepository;
import com.nitrotech.api.infrastructure.shipping.ghn.dto.GhnDistrictResponse;
import com.nitrotech.api.infrastructure.shipping.ghn.dto.GhnProvinceResponse;
import com.nitrotech.api.infrastructure.shipping.ghn.dto.GhnWardResponse;
import com.nitrotech.api.shared.exception.ShippingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class GhnCarrierAddressResolver {

    private static final String PROVIDER = "ghn";

    private final CarrierAddressMappingJpaRepository mappingJpa;
    private final GhnClient ghnClient;

    @Transactional
    public GhnCarrierAddress resolve(ShippingAddressSnapshot address) {
        if (address == null) {
            throw new ShippingException("INVALID_ADDRESS_CODE", "Shipping address is missing");
        }

        String provinceCode = clean(address.provinceCode());
        String districtCode = clean(address.districtCode());
        String wardCode = clean(address.wardCode());

        return mappingJpa.findByProviderIgnoreCaseAndProvinceCodeAndDistrictCodeAndWardCodeAndActiveTrue(
                        PROVIDER, provinceCode, districtCode, wardCode
                )
                .map(this::toCarrierAddress)
                .orElseGet(() -> autoMatchAndSave(address, provinceCode, districtCode, wardCode));
    }

    private GhnCarrierAddress autoMatchAndSave(
            ShippingAddressSnapshot address,
            String provinceCode,
            String districtCode,
            String wardCode
    ) {
        GhnProvinceResponse.ProvinceData province = matchProvince(address);
        GhnDistrictResponse.DistrictData district = matchDistrict(province.getProvinceID(), address);
        GhnWardResponse.WardData ward = matchWard(district.getDistrictID(), address);

        CarrierAddressMappingEntity mapping = new CarrierAddressMappingEntity();
        mapping.setProvider(PROVIDER);
        mapping.setProvinceCode(provinceCode);
        mapping.setDistrictCode(districtCode);
        mapping.setWardCode(wardCode);
        mapping.setProvinceName(clean(address.province()));
        mapping.setDistrictName(clean(address.district()));
        mapping.setWardName(clean(address.ward()));
        mapping.setCarrierProvinceId(String.valueOf(province.getProvinceID()));
        mapping.setCarrierDistrictId(String.valueOf(district.getDistrictID()));
        mapping.setCarrierWardCode(ward.getWardCode());
        mapping.setCarrierProvinceName(province.getProvinceName());
        mapping.setCarrierDistrictName(district.getDistrictName());
        mapping.setCarrierWardName(ward.getWardName());
        mapping.setConfidence("auto_matched");
        mapping.setActive(true);

        CarrierAddressMappingEntity saved = mappingJpa.save(mapping);
        return toCarrierAddress(saved);
    }

    private GhnProvinceResponse.ProvinceData matchProvince(ShippingAddressSnapshot address) {
        GhnProvinceResponse response = ghnClient.getProvinces();
        if (response == null || response.getData() == null) {
            throw new ShippingException("GHN_ADDRESS_ERROR", "Failed to fetch provinces from GHN");
        }
        return matchOne(
                response.getData(),
                address.province(),
                address.provinceCode(),
                GhnProvinceResponse.ProvinceData::getProvinceName,
                GhnProvinceResponse.ProvinceData::getCode,
                "province"
        );
    }

    private GhnDistrictResponse.DistrictData matchDistrict(Integer provinceId, ShippingAddressSnapshot address) {
        GhnDistrictResponse response = ghnClient.getDistricts(provinceId);
        if (response == null || response.getData() == null) {
            throw new ShippingException("GHN_ADDRESS_ERROR", "Failed to fetch districts from GHN");
        }
        return matchOne(
                response.getData(),
                address.district(),
                address.districtCode(),
                GhnDistrictResponse.DistrictData::getDistrictName,
                GhnDistrictResponse.DistrictData::getCode,
                "district"
        );
    }

    private GhnWardResponse.WardData matchWard(Integer districtId, ShippingAddressSnapshot address) {
        GhnWardResponse response = ghnClient.getWards(districtId);
        if (response == null || response.getData() == null) {
            throw new ShippingException("GHN_ADDRESS_ERROR", "Failed to fetch wards from GHN");
        }
        return matchOne(
                response.getData(),
                address.ward(),
                address.wardCode(),
                GhnWardResponse.WardData::getWardName,
                GhnWardResponse.WardData::getWardCode,
                "ward"
        );
    }

    private <T> T matchOne(
            List<T> candidates,
            String internalName,
            String internalCode,
            Function<T, String> nameGetter,
            Function<T, String> codeGetter,
            String level
    ) {
        String normalizedName = normalize(internalName);
        List<T> exactNameMatches = candidates.stream()
                .filter(item -> normalize(nameGetter.apply(item)).equals(normalizedName))
                .toList();
        if (exactNameMatches.size() == 1) {
            return exactNameMatches.get(0);
        }
        if (exactNameMatches.size() > 1) {
            throw ambiguous(level, internalName);
        }

        List<T> partialNameMatches = candidates.stream()
                .filter(item -> {
                    String candidate = normalize(nameGetter.apply(item));
                    return !candidate.isBlank()
                            && !normalizedName.isBlank()
                            && (candidate.contains(normalizedName) || normalizedName.contains(candidate));
                })
                .toList();
        if (partialNameMatches.size() == 1) {
            return partialNameMatches.get(0);
        }
        if (partialNameMatches.size() > 1) {
            throw ambiguous(level, internalName);
        }

        String cleanCode = clean(internalCode);
        if (!cleanCode.isBlank()) {
            List<T> codeMatches = candidates.stream()
                    .filter(item -> matchCode(codeGetter.apply(item), cleanCode))
                    .toList();
            if (codeMatches.size() == 1) {
                return codeMatches.get(0);
            }
            if (codeMatches.size() > 1) {
                throw ambiguous(level, internalCode);
            }
        }

        throw new ShippingException("CARRIER_ADDRESS_NOT_MAPPED",
                "Cannot map " + level + " '" + internalName + "' to GHN address data");
    }

    private GhnCarrierAddress toCarrierAddress(CarrierAddressMappingEntity mapping) {
        return new GhnCarrierAddress(
                Integer.valueOf(mapping.getCarrierProvinceId()),
                Integer.valueOf(mapping.getCarrierDistrictId()),
                mapping.getCarrierWardCode()
        );
    }

    private ShippingException ambiguous(String level, String value) {
        return new ShippingException("CARRIER_ADDRESS_AMBIGUOUS",
                "Multiple GHN " + level + " candidates matched '" + value + "'");
    }

    private boolean matchCode(String candidate, String input) {
        if (candidate == null || input == null) return false;
        try {
            return Integer.parseInt(candidate.trim()) == Integer.parseInt(input.trim());
        } catch (NumberFormatException e) {
            return candidate.trim().equalsIgnoreCase(input.trim());
        }
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalize(String value) {
        if (value == null) return "";
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
        normalized = Pattern.compile("\\p{InCombiningDiacriticalMarks}+").matcher(normalized).replaceAll("")
                .toLowerCase()
                .replace("đ", "d")
                .replaceAll("\\btp\\.?\\b", "thanh pho")
                .replaceAll("\\bq\\.?\\b", "quan")
                .replaceAll("[^a-z0-9]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        normalized = normalized.replaceAll("^(tinh|thanh pho|quan|huyen|thi xa|phuong|xa|thi tran)\\s+", "");
        if (Objects.equals(normalized, "hcm")) {
            return "ho chi minh";
        }
        return normalized;
    }
}
