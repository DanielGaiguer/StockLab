package com.main.stocklab.dto;

import java.util.List;

public record BomImportRequestDTO(
        String projectName,
        List<BomImportItemDTO> items
) {}
