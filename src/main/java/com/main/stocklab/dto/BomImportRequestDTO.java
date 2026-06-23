package com.main.stocklab.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record BomImportRequestDTO(
        @NotBlank(message = "Project name is required")
        String projectName,

        @NotEmpty(message = "Items list cannot be empty")
        @Valid
        List<BomImportItemDTO> items
) {}
