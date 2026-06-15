package com.main.stocklab.model;

import com.main.stocklab.model.enums.BomImportStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_bom_import")
public class BomImport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String projectName;

    @Column(name = "file_name")
    private String fileName;

    @Enumerated(EnumType.STRING)
    private BomImportStatus status;

    @Column(name = "imported_by")
    private String importedBy;

    @Column(name = "total_items")
    private Integer totalItems;

    private Integer matched;

    @Column(name = "not_found")
    private Integer notFound;

    @OneToMany(mappedBy = "bomImport", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BomImportItem> items = new ArrayList<>();

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public BomImportStatus getStatus() { return status; }
    public void setStatus(BomImportStatus status) { this.status = status; }

    public String getImportedBy() { return importedBy; }
    public void setImportedBy(String importedBy) { this.importedBy = importedBy; }

    public Integer getTotalItems() { return totalItems; }
    public void setTotalItems(Integer totalItems) { this.totalItems = totalItems; }

    public Integer getMatched() { return matched; }
    public void setMatched(Integer matched) { this.matched = matched; }

    public Integer getNotFound() { return notFound; }
    public void setNotFound(Integer notFound) { this.notFound = notFound; }

    public List<BomImportItem> getItems() { return items; }
    public void setItems(List<BomImportItem> items) { this.items = items; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
