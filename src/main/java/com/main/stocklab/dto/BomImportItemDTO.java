package com.main.stocklab.dto;

public record BomImportItemDTO(
        String partNumber,
        Integer quantity,
        String designator
) {}
