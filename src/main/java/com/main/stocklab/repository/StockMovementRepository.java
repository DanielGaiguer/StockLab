package com.main.stocklab.repository;

import com.main.stocklab.model.StockMovement;
import com.main.stocklab.model.enums.MovementType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    Page<StockMovement> findByComponentIdOrderByCreatedAtDesc(Long componentId, Pageable pageable);

    Page<StockMovement> findByMovementType(MovementType movementType, Pageable pageable);

    Page<StockMovement> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to, Pageable pageable);

    Page<StockMovement> findByMovementTypeAndCreatedAtBetween(MovementType movementType, LocalDateTime from, LocalDateTime to, Pageable pageable);

    List<StockMovement> findByCreatedAtBetweenOrderByCreatedAtAsc(LocalDateTime from, LocalDateTime to);
}
