package com.main.stocklab.repository;

import com.main.stocklab.model.Component;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ComponentRepository extends JpaRepository<Component, Long> {

    Optional<Component> findByPartNumber(String partNumber);

    Page<Component> findByPartNumberContainingIgnoreCaseOrNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String partNumber, String name, String description, Pageable pageable);

    Page<Component> findByActiveTrue(Pageable pageable);

    List<Component> findByActiveTrue();

    List<Component> findByActiveTrueOrderByPartNumber();

    @Query("SELECT COUNT(c) FROM Component c WHERE c.active = true AND c.quantityInStock <= 0")
    long countCritical();

    @Query("SELECT COUNT(c) FROM Component c WHERE c.active = true AND c.quantityInStock > 0 AND c.quantityInStock <= c.minimumQuantity")
    long countLow();

    @Query("SELECT COUNT(c) FROM Component c WHERE c.active = true AND (c.quantityInStock IS NULL OR c.quantityInStock > c.minimumQuantity OR c.minimumQuantity IS NULL)")
    long countOk();

    @Query("SELECT c FROM Component c WHERE c.active = true AND c.quantityInStock IS NOT NULL AND c.minimumQuantity IS NOT NULL AND c.quantityInStock <= c.minimumQuantity ORDER BY c.partNumber")
    List<Component> findLowStock();
}
