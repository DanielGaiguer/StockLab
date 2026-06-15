package com.main.stocklab.dto;

import com.main.stocklab.model.enums.MovementType;

public record UpdateStockDTO(
        MovementType movementType,
        Integer quantity,
        String projectName,
        String performedBy,
        String notes
) {}
