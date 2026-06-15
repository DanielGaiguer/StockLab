package com.main.stocklab.dto;

import com.main.stocklab.model.enums.MovementType;
import java.time.LocalDateTime;

public record StockMovementDTO(
        Long id,
        Long componentId,
        String componentPartNumber,
        String componentName,
        MovementType movementType,
        Integer quantity,
        Integer quantityBefore,
        Integer quantityAfter,
        String projectName,
        String performedBy,
        String notes,
        LocalDateTime createdAt
) {}
