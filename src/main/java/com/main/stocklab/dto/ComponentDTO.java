package com.main.stocklab.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ComponentDTO(
        Long id,
        String partNumber,
        String name,
        String description,
        String footprint,
        String category,
        Integer quantityInStock,
        Integer minimumQuantity,
        String drawerAddress,
        BigDecimal unitCost,
        String supplier,
        String datasheetUrl,
        String notes,
        Boolean active,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
