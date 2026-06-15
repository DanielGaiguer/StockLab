package com.main.stocklab.service;

import com.main.stocklab.dto.StockMovementDTO;
import com.main.stocklab.dto.UpdateStockDTO;
import com.main.stocklab.model.Component;
import com.main.stocklab.model.StockMovement;
import com.main.stocklab.model.enums.MovementType;
import com.main.stocklab.repository.ComponentRepository;
import com.main.stocklab.repository.StockMovementRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class StockMovementService {

    @Autowired
    private StockMovementRepository repository;

    @Autowired
    private ComponentRepository componentRepository;

    public Page<StockMovementDTO> findAll(String type, String from, String to, Pageable pageable) {
        Page<StockMovement> page;
        MovementType movementType = null;
        if (type != null && !type.isBlank()) {
            movementType = MovementType.valueOf(type);
        }
        LocalDateTime fromDate = from != null ? LocalDate.parse(from).atStartOfDay() : null;
        LocalDateTime toDate = to != null ? LocalDate.parse(to).atTime(LocalTime.MAX) : null;

        if (movementType != null && fromDate != null && toDate != null) {
            page = repository.findByMovementTypeAndCreatedAtBetween(movementType, fromDate, toDate, pageable);
        } else if (movementType != null) {
            page = repository.findByMovementType(movementType, pageable);
        } else if (fromDate != null && toDate != null) {
            page = repository.findByCreatedAtBetween(fromDate, toDate, pageable);
        } else {
            page = repository.findAll(pageable);
        }
        return page.map(this::toDTO);
    }

    public Page<StockMovementDTO> findByComponent(Long componentId, Pageable pageable) {
        return repository.findByComponentIdOrderByCreatedAtDesc(componentId, pageable)
                .map(this::toDTO);
    }

    @Transactional
    public void updateStock(Long componentId, UpdateStockDTO dto) {
        Component component = componentRepository.findById(componentId)
                .orElseThrow(() -> new RuntimeException("Component not found: " + componentId));

        int quantityBefore = component.getQuantityInStock() != null ? component.getQuantityInStock() : 0;
        int quantityAfter = quantityBefore;

        switch (dto.movementType()) {
            case INTAKE -> quantityAfter = quantityBefore + dto.quantity();
            case WITHDRAWAL -> quantityAfter = quantityBefore - dto.quantity();
            case CORRECTION -> quantityAfter = dto.quantity();
            case BOM_DEDUCTION -> throw new RuntimeException("Use BOM import for BOM deductions");
        }

        component.setQuantityInStock(Math.max(quantityAfter, 0));
        componentRepository.save(component);

        StockMovement movement = new StockMovement();
        movement.setComponent(component);
        movement.setMovementType(dto.movementType());
        movement.setQuantity(dto.quantity());
        movement.setQuantityBefore(quantityBefore);
        movement.setQuantityAfter(quantityAfter);
        movement.setProjectName(dto.projectName());
        movement.setPerformedBy(dto.performedBy());
        movement.setNotes(dto.notes());
        repository.save(movement);
    }

    private StockMovementDTO toDTO(StockMovement m) {
        return new StockMovementDTO(
                m.getId(),
                m.getComponent().getId(),
                m.getComponent().getPartNumber(),
                m.getComponent().getName(),
                m.getMovementType(),
                m.getQuantity(),
                m.getQuantityBefore(),
                m.getQuantityAfter(),
                m.getProjectName(),
                m.getPerformedBy(),
                m.getNotes(),
                m.getCreatedAt()
        );
    }
}
