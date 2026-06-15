package com.main.stocklab.repository;

import com.main.stocklab.model.BomImport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BomImportRepository extends JpaRepository<BomImport, Long> {
}
