package com.main.stocklab.dto;

import java.math.BigDecimal;

public record CreateComponentDTO(
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
        Boolean active
) {}
