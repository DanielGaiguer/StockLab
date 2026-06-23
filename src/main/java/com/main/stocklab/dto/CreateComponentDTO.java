package com.main.stocklab.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record CreateComponentDTO(
        @NotBlank(message = "Part number is required")
        String partNumber,

        @NotBlank(message = "Name is required")
        String name,

        String description,
        String footprint,
        String category,

        @PositiveOrZero(message = "Quantity must be zero or positive")
        Integer quantityInStock,

        @PositiveOrZero(message = "Minimum quantity must be zero or positive")
        Integer minimumQuantity,

        String drawerAddress,

        @PositiveOrZero(message = "Unit cost must be zero or positive")
        BigDecimal unitCost,

        String supplier,
        String datasheetUrl,
        String notes,
        Boolean active
) {}
