package com.nitrotech.api.shared.config;

import com.nitrotech.api.shared.response.ApiResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class SystemConfigController {

    @Value("${app.shipping.free-threshold:500000}")
    private BigDecimal freeShippingThreshold;

    @Value("${app.shipping.flat-fee:30000}")
    private BigDecimal flatShippingFee;

    @GetMapping
    public ResponseEntity<ApiResult<Map<String, Object>>> getConfig() {
        Map<String, Object> shippingConfig = Map.of(
                "freeThreshold", freeShippingThreshold,
                "flatFee", flatShippingFee
        );
        
        Map<String, Object> config = Map.of(
                "shipping", shippingConfig
        );
        
        return ResponseEntity.ok(ApiResult.ok(config));
    }
}
