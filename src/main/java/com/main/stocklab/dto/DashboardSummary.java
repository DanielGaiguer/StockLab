package com.main.stocklab.dto;

import java.util.Map;

public record DashboardSummary(
        long totalComponents,
        long criticalCount,
        long lowCount,
        long okCount,
        Map<String, Long> movementChart7d
) {}
