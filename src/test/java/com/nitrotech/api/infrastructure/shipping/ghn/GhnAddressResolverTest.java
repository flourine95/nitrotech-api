package com.nitrotech.api.infrastructure.shipping.ghn;

import com.nitrotech.api.infrastructure.shipping.ghn.dto.GhnDistrictResponse;
import com.nitrotech.api.infrastructure.shipping.ghn.dto.GhnProvinceResponse;
import com.nitrotech.api.infrastructure.shipping.ghn.dto.GhnWardResponse;
import com.nitrotech.api.shared.exception.ShippingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class GhnAddressResolverTest {

    private GhnClient ghnClient;
    private GhnAddressResolver resolver;

    @BeforeEach
    void setUp() {
        ghnClient = mock(GhnClient.class);
        resolver = new GhnAddressResolver(ghnClient);
    }

    @Test
    void getProvinceIdMatchesByGsoCode() {
        GhnProvinceResponse response = new GhnProvinceResponse(200, "Success", List.of(
                new GhnProvinceResponse.ProvinceData(10, "Hồ Chí Minh", "79"),
                new GhnProvinceResponse.ProvinceData(20, "Hà Nội", "01")
        ));
        when(ghnClient.getProvinces()).thenReturn(response);

        // Exact match
        Integer id1 = resolver.getProvinceId("79", "Hồ Chí Minh");
        assertThat(id1).isEqualTo(10);

        // Numeric match (padding check)
        Integer id2 = resolver.getProvinceId("079", "Hồ Chí Minh");
        assertThat(id2).isEqualTo(10);

        // Second call should hit cache, not the client
        Integer id3 = resolver.getProvinceId("79", "Hồ Chí Minh");
        assertThat(id3).isEqualTo(10);

        verify(ghnClient, times(1)).getProvinces();
    }

    @Test
    void getProvinceIdMatchesByNameWhenCodeMatchFails() {
        GhnProvinceResponse response = new GhnProvinceResponse(200, "Success", List.of(
                new GhnProvinceResponse.ProvinceData(10, "Thành phố Hồ Chí Minh", "79000") // Different GSO code
        ));
        when(ghnClient.getProvinces()).thenReturn(response);

        Integer id = resolver.getProvinceId("79", "Hồ Chí Minh");
        assertThat(id).isEqualTo(10);
    }

    @Test
    void getProvinceIdThrowsExceptionWhenNotFound() {
        GhnProvinceResponse response = new GhnProvinceResponse(200, "Success", List.of(
                new GhnProvinceResponse.ProvinceData(10, "Hà Nội", "01")
        ));
        when(ghnClient.getProvinces()).thenReturn(response);

        assertThatThrownBy(() -> resolver.getProvinceId("79", "Hồ Chí Minh"))
                .isInstanceOf(ShippingException.class)
                .hasMessageContaining("Cannot map province GSO code: 79")
                .extracting("code").isEqualTo("INVALID_ADDRESS_CODE");
    }

    @Test
    void getDistrictIdMatchesByGsoCode() {
        GhnDistrictResponse response = new GhnDistrictResponse(200, "Success", List.of(
                new GhnDistrictResponse.DistrictData(101, 10, "Quận 1", "760"),
                new GhnDistrictResponse.DistrictData(102, 10, "Quận 3", "762")
        ));
        when(ghnClient.getDistricts(10)).thenReturn(response);

        // Exact match
        Integer id1 = resolver.getDistrictId(10, "760", "Quận 1");
        assertThat(id1).isEqualTo(101);

        // Numeric match
        Integer id2 = resolver.getDistrictId(10, "0760", "Quận 1");
        assertThat(id2).isEqualTo(101);

        // Cache hit
        Integer id3 = resolver.getDistrictId(10, "760", "Quận 1");
        assertThat(id3).isEqualTo(101);

        verify(ghnClient, times(1)).getDistricts(10);
    }

    @Test
    void getDistrictIdMatchesByNameWhenCodeMatchFails() {
        GhnDistrictResponse response = new GhnDistrictResponse(200, "Success", List.of(
                new GhnDistrictResponse.DistrictData(101, 10, "Quận 1", "999")
        ));
        when(ghnClient.getDistricts(10)).thenReturn(response);

        Integer id = resolver.getDistrictId(10, "760", "Quận 1");
        assertThat(id).isEqualTo(101);
    }

    @Test
    void getDistrictIdThrowsExceptionWhenNotFound() {
        GhnDistrictResponse response = new GhnDistrictResponse(200, "Success", List.of(
                new GhnDistrictResponse.DistrictData(102, 10, "Quận 3", "762")
        ));
        when(ghnClient.getDistricts(10)).thenReturn(response);

        assertThatThrownBy(() -> resolver.getDistrictId(10, "760", "Quận 1"))
                .isInstanceOf(ShippingException.class)
                .hasMessageContaining("Cannot map district GSO code: 760 in province ID 10")
                .extracting("code").isEqualTo("INVALID_ADDRESS_CODE");
    }

    @Test
    void getWardCodeMatchesByNameExactAndPartial() {
        GhnWardResponse response = new GhnWardResponse(200, "Success", List.of(
                new GhnWardResponse.WardData("W_BEN_NGHE", 101, "Phường Bến Nghé"),
                new GhnWardResponse.WardData("W_DA_KAO", 101, "Phường Đa Kao")
        ));
        when(ghnClient.getWards(101)).thenReturn(response);

        // Exact match (after normalize)
        String code1 = resolver.getWardCode(101, "Phường Bến Nghé");
        assertThat(code1).isEqualTo("W_BEN_NGHE");

        // Input standardizing check (remove prefixes, accents)
        String code2 = resolver.getWardCode(101, "Ben Nghe");
        assertThat(code2).isEqualTo("W_BEN_NGHE");

        // Substring / partial match
        String code3 = resolver.getWardCode(101, "Đa Kao");
        assertThat(code3).isEqualTo("W_DA_KAO");

        // Cache hit
        String code4 = resolver.getWardCode(101, "Ben Nghe");
        assertThat(code4).isEqualTo("W_BEN_NGHE");

        verify(ghnClient, times(1)).getWards(101);
    }

    @Test
    void getWardCodeThrowsExceptionWhenNotFound() {
        GhnWardResponse response = new GhnWardResponse(200, "Success", List.of(
                new GhnWardResponse.WardData("W_DA_KAO", 101, "Phường Đa Kao")
        ));
        when(ghnClient.getWards(101)).thenReturn(response);

        assertThatThrownBy(() -> resolver.getWardCode(101, "Bến Nghé"))
                .isInstanceOf(ShippingException.class)
                .hasMessageContaining("Cannot resolve ward name 'Bến Nghé' in GHN district ID 101")
                .extracting("code").isEqualTo("GHN_WARD_NOT_FOUND");
    }

    @Test
    void cachesAreThreadSafeAndClientCalledOnce() throws Exception {
        GhnProvinceResponse response = new GhnProvinceResponse(200, "Success", List.of(
                new GhnProvinceResponse.ProvinceData(10, "Hồ Chí Minh", "79")
        ));
        when(ghnClient.getProvinces()).thenAnswer(invocation -> {
            // Simulate API latency
            Thread.sleep(100);
            return response;
        });

        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<Integer>> futures = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            futures.add(executor.submit(() -> resolver.getProvinceId("79", "Hồ Chí Minh")));
        }

        for (var f : futures) {
            assertThat(f.get()).isEqualTo(10);
        }
        executor.shutdown();

        // Ensure getProvinces was called exactly once despite 10 concurrent requests
        verify(ghnClient, times(1)).getProvinces();
    }
}
