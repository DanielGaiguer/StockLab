package com.main.stocklab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record BomImportItemDTO(
        @NotBlank(message = "Part number is required")
        String partNumber,

        @Positive(message = "Quantity must be positive")
        Integer quantity,

        String designator
) {}
