package com.nitrotech.api.application.brand.request;

import com.nitrotech.api.domain.brand.dto.BrandFilter;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BrandListRequest {
    @Size(max = 100, message = "Search query must not exceed 100 characters")
    private String search;
    
    private Boolean active;
    private Boolean deleted;
    
    public BrandFilter toFilter() {
        return new BrandFilter(search, active, deleted);
    }
}
