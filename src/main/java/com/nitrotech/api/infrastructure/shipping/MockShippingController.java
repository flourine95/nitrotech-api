package com.nitrotech.api.infrastructure.shipping;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@Profile("dev")
public class MockShippingController {

    @PostMapping("/mock-ghtk/services/shipment/order")
    public ResponseEntity<Map<String, Object>> mockGhtkCreateOrder(@RequestBody Map<String, Object> request) {
        Map<String, Object> orderDetails = Map.of(
                "label", "GHTK-MOCK-" + (100000 + (int)(Math.random() * 900000)),
                "fee", 35000,
                "estimated_deliver_time", "2026-06-14 15:30:00"
        );
        Map<String, Object> response = Map.of(
                "success", true,
                "message", "Vận đơn được khởi tạo thành công (MOCK GHTK)",
                "order", orderDetails
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/mock-ghn/master-data/province")
    public ResponseEntity<Map<String, Object>> mockGhnProvinces() {
        List<Map<String, Object>> provinces = List.of(
                Map.of("ProvinceID", 77, "ProvinceName", "Tỉnh Bà Rịa - Vũng Tàu", "Code", "77"),
                Map.of("ProvinceID", 46, "ProvinceName", "Thành phố Huế", "Code", "46"),
                Map.of("ProvinceID", 8, "ProvinceName", "Tỉnh Tuyên Quang", "Code", "8")
        );
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "Success",
                "data", provinces
        ));
    }

    @PostMapping("/mock-ghn/master-data/district")
    public ResponseEntity<Map<String, Object>> mockGhnDistricts(@RequestBody Map<String, Object> request) {
        Integer provinceId = (Integer) request.get("province_id");
        List<Map<String, Object>> districts;
        if (provinceId == null) {
            districts = List.of();
        } else if (provinceId == 77) {
            districts = List.of(
                    Map.of("DistrictID", 751, "ProvinceID", 77, "DistrictName", "Huyện Xuyên Mộc", "Code", "751")
            );
        } else if (provinceId == 46) {
            districts = List.of(
                    Map.of("DistrictID", 460, "ProvinceID", 46, "DistrictName", "Thành phố Huế", "Code", "460")
            );
        } else if (provinceId == 8) {
            districts = List.of(
                    Map.of("DistrictID", 80, "ProvinceID", 8, "DistrictName", "Không áp dụng", "Code", "8")
            );
        } else {
            districts = List.of();
        }
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "Success",
                "data", districts
        ));
    }

    @PostMapping("/mock-ghn/master-data/ward")
    public ResponseEntity<Map<String, Object>> mockGhnWards(@RequestBody Map<String, Object> request) {
        Integer districtId = (Integer) request.get("district_id");
        List<Map<String, Object>> wards;
        if (districtId == null) {
            wards = List.of();
        } else if (districtId == 751) {
            wards = List.of(
                    Map.of("WardCode", "26641", "DistrictID", 751, "WardName", "Xã Hòa Bình")
            );
        } else if (districtId == 460) {
            wards = List.of(
                    Map.of("WardCode", "19918", "DistrictID", 460, "WardName", "Xã Phú Hồ")
            );
        } else if (districtId == 80) {
            wards = List.of(
                    Map.of("WardCode", "778", "DistrictID", 80, "WardName", "Xã Sơn Vĩ")
            );
        } else {
            wards = List.of();
        }
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "Success",
                "data", wards
        ));
    }

    @PostMapping("/mock-ghn/v2/shipping-order/create")
    public ResponseEntity<Map<String, Object>> mockGhnCreateOrder(@RequestBody Map<String, Object> request) {
        Map<String, Object> responseData = Map.of(
                "order_code", "GHN-MOCK-" + (100000 + (int)(Math.random() * 900000)),
                "total_fee", 28000,
                "expected_delivery_time", "2026-06-14T18:00:00Z"
        );
        return ResponseEntity.ok(Map.of(
                "code", 200,
                "message", "Success",
                "data", responseData
        ));
    }
}
