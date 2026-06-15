package com.main.stocklab.repository;

import com.main.stocklab.model.BomImportItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BomImportItemRepository extends JpaRepository<BomImportItem, Long> {
}
