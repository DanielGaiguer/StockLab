package com.main.stocklab.service;

import com.main.stocklab.dto.DashboardSummary;
import com.main.stocklab.model.StockMovement;
import com.main.stocklab.repository.ComponentRepository;
import com.main.stocklab.repository.StockMovementRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    @Autowired
    private ComponentRepository componentRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    public DashboardSummary getSummary() {
        long total = componentRepository.count();
        long critical = componentRepository.countCritical();
        long low = componentRepository.countLow();
        long ok = componentRepository.countOk();

        LocalDate today = LocalDate.now();
        Map<String, Long> chart = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.atTime(LocalTime.MAX);
            List<StockMovement> movements = stockMovementRepository
                    .findByCreatedAtBetweenOrderByCreatedAtAsc(start, end);
            chart.put(date.toString(), (long) movements.size());
        }

        return new DashboardSummary(total, critical, low, ok, chart);
    }
}
