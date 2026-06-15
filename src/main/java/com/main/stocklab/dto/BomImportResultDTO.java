package com.main.stocklab.dto;

import java.util.List;

public record BomImportResultDTO(
        Integer processed,
        Integer matched,
        Integer notFound,
        List<String> lowStockComponents
) {}
