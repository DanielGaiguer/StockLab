package com.main.stocklab.model;

import com.main.stocklab.model.enums.BomImportItemStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_bom_import_item")
public class BomImportItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "bom_import_id")
    private BomImport bomImport;

    private String partNumber;

    private String designator;

    private Integer quantity;

    @Enumerated(EnumType.STRING)
    private BomImportItemStatus status;

    @ManyToOne
    @JoinColumn(name = "component_id")
    private Component component;

    private String notes;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public BomImport getBomImport() { return bomImport; }
    public void setBomImport(BomImport bomImport) { this.bomImport = bomImport; }

    public String getPartNumber() { return partNumber; }
    public void setPartNumber(String partNumber) { this.partNumber = partNumber; }

    public String getDesignator() { return designator; }
    public void setDesignator(String designator) { this.designator = designator; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public BomImportItemStatus getStatus() { return status; }
    public void setStatus(BomImportItemStatus status) { this.status = status; }

    public Component getComponent() { return component; }
    public void setComponent(Component component) { this.component = component; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
