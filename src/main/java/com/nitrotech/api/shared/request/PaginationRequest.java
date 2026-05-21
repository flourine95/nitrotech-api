package com.nitrotech.api.shared.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.util.List;

@Data
public class PaginationRequest {
    @Min(value = 0, message = "Page must be >= 0")
    private Integer page = 0;
    
    @Min(value = 1, message = "Size must be >= 1")
    @Max(value = 100, message = "Size must be <= 100")
    private Integer size = 20;
    
    private List<String> sort;
}
