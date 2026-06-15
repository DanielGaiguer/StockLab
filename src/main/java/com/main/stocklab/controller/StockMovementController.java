package com.main.stocklab.controller;

import com.main.stocklab.dto.StockMovementDTO;
import com.main.stocklab.dto.UpdateStockDTO;
import com.main.stocklab.service.StockMovementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/movements")
public class StockMovementController {

    @Autowired
    private StockMovementService service;

    @GetMapping
    public ResponseEntity<Page<StockMovementDTO>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {

        return ResponseEntity.ok(
                service.findAll(type, from, to,
                        PageRequest.of(page, size, Sort.by("createdAt").descending()))
        );
    }

    @GetMapping("/component/{componentId}")
    public ResponseEntity<Page<StockMovementDTO>> byComponent(
            @PathVariable Long componentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(
                service.findByComponent(componentId,
                        PageRequest.of(page, size, Sort.by("createdAt").descending()))
        );
    }

    @PostMapping("/{componentId}")
    public ResponseEntity<Void> create(
            @PathVariable Long componentId,
            @RequestBody UpdateStockDTO request) {

        service.updateStock(componentId, request);
        return ResponseEntity.ok().build();
    }
}
