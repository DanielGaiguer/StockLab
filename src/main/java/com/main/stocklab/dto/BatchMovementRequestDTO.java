package com.main.stocklab.dto;

import com.main.stocklab.model.enums.MovementType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record BatchMovementRequestDTO(
        @NotNull(message = "Movement type is required")
        MovementType movementType,

        @NotBlank(message = "Performed by is required")
        String performedBy,

        @NotEmpty(message = "At least one item is required")
        @Valid
        List<BatchMovementItemDTO> items
) {}
