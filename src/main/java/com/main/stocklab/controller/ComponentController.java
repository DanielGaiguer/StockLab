package com.main.stocklab.controller;

import com.main.stocklab.dto.ComponentDTO;
import com.main.stocklab.dto.CreateComponentDTO;
import com.main.stocklab.model.Component;
import com.main.stocklab.service.ComponentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/components")
public class ComponentController {

    @Autowired
    private ComponentService componentService;

    @GetMapping
    public ResponseEntity<Page<ComponentDTO>> list(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<Component> result = componentService.findAll(search,
                PageRequest.of(page, size, Sort.by("partNumber")));

        return ResponseEntity.ok(result.map(this::toDTO));
    }

    @GetMapping("/{partNumber}")
    public ResponseEntity<ComponentDTO> getByPartNumber(@PathVariable String partNumber) {
        return componentService.findByPartNumber(partNumber)
                .map(c -> ResponseEntity.ok(toDTO(c)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ComponentDTO> create(@RequestBody CreateComponentDTO request) {
        Component component = componentService.createFromDTO(request);
        return ResponseEntity.ok(toDTO(component));
    }

    @PatchMapping("/{partNumber}")
    public ResponseEntity<ComponentDTO> update(
            @PathVariable String partNumber,
            @RequestBody CreateComponentDTO request) {
        Component component = componentService.update(partNumber, request);
        return ResponseEntity.ok(toDTO(component));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<java.util.List<ComponentDTO>> lowStock() {
        return ResponseEntity.ok(
                componentService.findLowStock()
                        .stream()
                        .map(this::toDTO)
                        .toList()
        );
    }

    private ComponentDTO toDTO(Component c) {
        String status;
        if (c.getQuantityInStock() == null || c.getMinimumQuantity() == null) {
            status = "OK";
        } else if (c.getQuantityInStock() <= 0) {
            status = "CRITICAL";
        } else if (c.getQuantityInStock() <= c.getMinimumQuantity()) {
            status = "LOW";
        } else {
            status = "OK";
        }

        return new ComponentDTO(
                c.getId(),
                c.getPartNumber(),
                c.getName(),
                c.getDescription(),
                c.getFootprint(),
                c.getCategory(),
                c.getQuantityInStock(),
                c.getMinimumQuantity(),
                c.getDrawerAddress(),
                c.getUnitCost(),
                c.getSupplier(),
                c.getDatasheetUrl(),
                c.getNotes(),
                c.getActive(),
                status,
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
