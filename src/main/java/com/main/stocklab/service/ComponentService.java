package com.main.stocklab.service;

import com.main.stocklab.dto.CreateComponentDTO;
import com.main.stocklab.model.Component;
import com.main.stocklab.repository.ComponentRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ComponentService {

    @Autowired
    private ComponentRepository repository;

    public Page<Component> findAll(String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return repository.findByPartNumberContainingIgnoreCaseOrNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                    search, search, search, pageable);
        }
        return repository.findByActiveTrue(pageable);
    }

    public Component save(Component component) {
        return repository.save(component);
    }

    public Component createFromDTO(CreateComponentDTO dto) {
        Component c = new Component();
        c.setPartNumber(dto.partNumber());
        c.setName(dto.name());
        c.setDescription(dto.description());
        c.setFootprint(dto.footprint());
        c.setCategory(dto.category());
        c.setQuantityInStock(dto.quantityInStock() != null ? dto.quantityInStock() : 0);
        c.setMinimumQuantity(dto.minimumQuantity() != null ? dto.minimumQuantity() : 1);
        c.setDrawerAddress(dto.drawerAddress());
        c.setUnitCost(dto.unitCost());
        c.setSupplier(dto.supplier());
        c.setDatasheetUrl(dto.datasheetUrl());
        c.setNotes(dto.notes());
        c.setActive(dto.active() != null ? dto.active() : true);
        return repository.save(c);
    }

    public Component update(String partNumber, CreateComponentDTO dto) {
        Component c = repository.findByPartNumber(partNumber)
                .orElseThrow(() -> new RuntimeException("Component not found: " + partNumber));
        if (dto.name() != null) c.setName(dto.name());
        if (dto.description() != null) c.setDescription(dto.description());
        if (dto.footprint() != null) c.setFootprint(dto.footprint());
        if (dto.category() != null) c.setCategory(dto.category());
        if (dto.quantityInStock() != null) c.setQuantityInStock(dto.quantityInStock());
        if (dto.minimumQuantity() != null) c.setMinimumQuantity(dto.minimumQuantity());
        if (dto.drawerAddress() != null) c.setDrawerAddress(dto.drawerAddress());
        if (dto.unitCost() != null) c.setUnitCost(dto.unitCost());
        if (dto.supplier() != null) c.setSupplier(dto.supplier());
        if (dto.datasheetUrl() != null) c.setDatasheetUrl(dto.datasheetUrl());
        if (dto.notes() != null) c.setNotes(dto.notes());
        if (dto.active() != null) c.setActive(dto.active());
        return repository.save(c);
    }

    public Component findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Component not found"));
    }

    public Optional<Component> findByPartNumber(String partNumber) {
        return repository.findByPartNumber(partNumber);
    }

    public List<Component> findLowStock() {
        return repository.findAll()
                .stream()
                .filter(c -> c.getQuantityInStock() != null && c.getMinimumQuantity() != null
                        && c.getQuantityInStock() <= c.getMinimumQuantity())
                .toList();
    }
}
