package com.nitrotech.api.infrastructure.shipping.ghn;

import com.nitrotech.api.domain.order.dto.ShippingAddressSnapshot;
import com.nitrotech.api.infrastructure.persistence.entity.CarrierAddressMappingEntity;
import com.nitrotech.api.infrastructure.persistence.repository.CarrierAddressMappingJpaRepository;
import com.nitrotech.api.infrastructure.shipping.ghn.dto.GhnDistrictResponse;
import com.nitrotech.api.infrastructure.shipping.ghn.dto.GhnProvinceResponse;
import com.nitrotech.api.infrastructure.shipping.ghn.dto.GhnWardResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GhnCarrierAddressResolverTest {

    private CarrierAddressMappingJpaRepository mappingJpa;
    private GhnClient ghnClient;
    private GhnCarrierAddressResolver resolver;

    @BeforeEach
    void setUp() {
        mappingJpa = mock(CarrierAddressMappingJpaRepository.class);
        ghnClient = mock(GhnClient.class);
        resolver = new GhnCarrierAddressResolver(mappingJpa, ghnClient);
    }

    @Test
    void returnsExistingMappingWithoutCallingGhn() {
        ShippingAddressSnapshot address = address();
        CarrierAddressMappingEntity mapping = new CarrierAddressMappingEntity();
        mapping.setCarrierProvinceId("202");
        mapping.setCarrierDistrictId("1442");
        mapping.setCarrierWardCode("20101");

        when(mappingJpa.findByProviderIgnoreCaseAndProvinceCodeAndDistrictCodeAndWardCodeAndActiveTrue(
                "ghn", "79", "760", "26734"
        )).thenReturn(Optional.of(mapping));

        GhnCarrierAddress result = resolver.resolve(address);

        assertThat(result.provinceId()).isEqualTo(202);
        assertThat(result.districtId()).isEqualTo(1442);
        assertThat(result.wardCode()).isEqualTo("20101");
        verifyNoInteractions(ghnClient);
    }

    @Test
    void autoMatchesByNameBeforeFallingBackToMismatchedCode() {
        ShippingAddressSnapshot address = address();

        when(mappingJpa.findByProviderIgnoreCaseAndProvinceCodeAndDistrictCodeAndWardCodeAndActiveTrue(
                "ghn", "79", "760", "26734"
        )).thenReturn(Optional.empty());

        when(ghnClient.getProvinces()).thenReturn(new GhnProvinceResponse(200, "Success", List.of(
                new GhnProvinceResponse.ProvinceData(218, "Sóc Trăng", "79"),
                new GhnProvinceResponse.ProvinceData(202, "Hồ Chí Minh", "8")
        )));
        when(ghnClient.getDistricts(202)).thenReturn(new GhnDistrictResponse(200, "Success", List.of(
                new GhnDistrictResponse.DistrictData(1442, 202, "Quận 1", "0201")
        )));
        when(ghnClient.getWards(1442)).thenReturn(new GhnWardResponse(200, "Success", List.of(
                new GhnWardResponse.WardData("20101", 1442, "Phường Bến Nghé")
        )));

        when(mappingJpa.save(any(CarrierAddressMappingEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GhnCarrierAddress result = resolver.resolve(address);

        assertThat(result.provinceId()).isEqualTo(202);
        assertThat(result.districtId()).isEqualTo(1442);
        assertThat(result.wardCode()).isEqualTo("20101");

        ArgumentCaptor<CarrierAddressMappingEntity> captor = ArgumentCaptor.forClass(CarrierAddressMappingEntity.class);
        verify(mappingJpa).save(captor.capture());
        CarrierAddressMappingEntity saved = captor.getValue();
        assertThat(saved.getProvinceCode()).isEqualTo("79");
        assertThat(saved.getCarrierProvinceId()).isEqualTo("202");
        assertThat(saved.getCarrierDistrictId()).isEqualTo("1442");
        assertThat(saved.getCarrierWardCode()).isEqualTo("20101");
        assertThat(saved.getConfidence()).isEqualTo("auto_matched");
    }

    private ShippingAddressSnapshot address() {
        return new ShippingAddressSnapshot(
                "Nguyen Van A",
                "0909123456",
                "Thành phố Hồ Chí Minh",
                "79",
                "Quận 1",
                "760",
                "Phường Bến Nghé",
                "26734",
                "123 Nguyen Hue"
        );
    }
}
