package com.main.stocklab.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BatchMovementItemDTO(
        @NotNull(message = "Component ID is required")
        Long componentId,

        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be positive")
        Integer quantity
) {}
