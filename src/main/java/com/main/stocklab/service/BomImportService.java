package com.main.stocklab.service;

import com.main.stocklab.dto.BomImportItemDTO;
import com.main.stocklab.dto.BomImportRequestDTO;
import com.main.stocklab.dto.BomImportResultDTO;
import com.main.stocklab.model.BomImport;
import com.main.stocklab.model.BomImportItem;
import com.main.stocklab.model.Component;
import com.main.stocklab.model.StockMovement;
import com.main.stocklab.model.enums.BomImportItemStatus;
import com.main.stocklab.model.enums.BomImportStatus;
import com.main.stocklab.model.enums.MovementType;
import com.main.stocklab.repository.BomImportItemRepository;
import com.main.stocklab.repository.BomImportRepository;
import com.main.stocklab.repository.ComponentRepository;
import com.main.stocklab.repository.StockMovementRepository;
import jakarta.transaction.Transactional;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class BomImportService {

    @Autowired
    private BomImportRepository bomImportRepository;

    @Autowired
    private BomImportItemRepository bomImportItemRepository;

    @Autowired
    private ComponentRepository componentRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @Transactional
    public BomImportResultDTO importBom(BomImportRequestDTO request) {
        return processImport(request.projectName(), null, null, request.items());
    }

    @Transactional
    public BomImportResultDTO uploadBom(MultipartFile file, String projectName) {
        List<BomImportItemDTO> items = parseFile(file);
        return processImport(projectName, file.getOriginalFilename(), null, items);
    }

    private BomImportResultDTO processImport(String projectName, String fileName, String importedBy, List<BomImportItemDTO> items) {
        BomImport bomImport = new BomImport();
        bomImport.setProjectName(projectName);
        bomImport.setFileName(fileName);
        bomImport.setImportedBy(importedBy);
        bomImport.setStatus(BomImportStatus.PENDING);
        bomImport.setTotalItems(items.size());
        bomImport = bomImportRepository.save(bomImport);

        int processed = 0;
        int matched = 0;
        int notFound = 0;
        List<String> lowStockComponents = new ArrayList<>();

        for (BomImportItemDTO item : items) {
            processed++;

            Optional<Component> optComponent = componentRepository.findByPartNumber(item.partNumber());

            BomImportItem importItem = new BomImportItem();
            importItem.setBomImport(bomImport);
            importItem.setPartNumber(item.partNumber());
            importItem.setDesignator(item.designator());
            importItem.setQuantity(item.quantity());

            if (optComponent.isPresent()) {
                Component component = optComponent.get();
                matched++;
                importItem.setStatus(BomImportItemStatus.FOUND);
                importItem.setComponent(component);

                int quantityBefore = component.getQuantityInStock() != null ? component.getQuantityInStock() : 0;
                int quantityAfter = quantityBefore - item.quantity();
                component.setQuantityInStock(Math.max(quantityAfter, 0));
                componentRepository.save(component);

                StockMovement movement = new StockMovement();
                movement.setComponent(component);
                movement.setMovementType(MovementType.BOM_DEDUCTION);
                movement.setQuantity(item.quantity());
                movement.setQuantityBefore(quantityBefore);
                movement.setQuantityAfter(quantityAfter);
                movement.setProjectName(projectName);
                movement.setBomImportId(bomImport.getId());
                movement.setPerformedBy("BOM Import");
                stockMovementRepository.save(movement);

                if (quantityAfter <= component.getMinimumQuantity()) {
                    lowStockComponents.add(component.getPartNumber() + " - " + component.getName());
                }
            } else {
                notFound++;
                importItem.setStatus(BomImportItemStatus.NOT_FOUND);
            }

            bomImportItemRepository.save(importItem);
        }

        if (notFound == 0) {
            bomImport.setStatus(BomImportStatus.PROCESSED);
        } else if (matched > 0) {
            bomImport.setStatus(BomImportStatus.PARTIALLY_MATCHED);
        } else {
            bomImport.setStatus(BomImportStatus.FAILED);
        }
        bomImport.setMatched(matched);
        bomImport.setNotFound(notFound);
        bomImportRepository.save(bomImport);

        return new BomImportResultDTO(processed, matched, notFound, lowStockComponents);
    }

    private List<BomImportItemDTO> parseFile(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename != null && (filename.endsWith(".xlsx") || filename.endsWith(".xls"))) {
            return parseExcel(file);
        }
        return parseCsv(file);
    }

    private List<BomImportItemDTO> parseCsv(MultipartFile file) {
        List<BomImportItemDTO> items = new ArrayList<>();
        try (Reader reader = new InputStreamReader(file.getInputStream());
             BufferedReader br = new BufferedReader(reader)) {
            String header = br.readLine();
            String[] cols = header != null ? header.split(",") : new String[0];
            int pnIdx = indexOf(cols, "part_number", "partNumber", "part number", "pn");
            int qtyIdx = indexOf(cols, "quantity", "qty", "quantidade");
            int desIdx = indexOf(cols, "designator", "refdes", "reference");

            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                String partNumber = pnIdx >= 0 && pnIdx < parts.length ? parts[pnIdx].trim() : "";
                int quantity = qtyIdx >= 0 && qtyIdx < parts.length ? parseInt(parts[qtyIdx].trim()) : 1;
                String designator = desIdx >= 0 && desIdx < parts.length ? parts[desIdx].trim() : null;
                if (!partNumber.isEmpty()) {
                    items.add(new BomImportItemDTO(partNumber, quantity, designator));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse CSV: " + e.getMessage());
        }
        return items;
    }

    private List<BomImportItemDTO> parseExcel(MultipartFile file) {
        List<BomImportItemDTO> items = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            int pnIdx = excelIndexOf(headerRow, "part_number", "partNumber", "part number", "pn");
            int qtyIdx = excelIndexOf(headerRow, "quantity", "qty", "quantidade");
            int desIdx = excelIndexOf(headerRow, "designator", "refdes", "reference");

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String partNumber = pnIdx >= 0 ? getCellString(row.getCell(pnIdx)) : "";
                int quantity = qtyIdx >= 0 ? (int) row.getCell(qtyIdx).getNumericCellValue() : 1;
                String designator = desIdx >= 0 ? getCellString(row.getCell(desIdx)) : null;
                if (!partNumber.isEmpty()) {
                    items.add(new BomImportItemDTO(partNumber, quantity, designator));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Excel: " + e.getMessage());
        }
        return items;
    }

    private int indexOf(String[] arr, String... keys) {
        for (int i = 0; i < arr.length; i++) {
            for (String key : keys) {
                if (arr[i].trim().equalsIgnoreCase(key)) return i;
            }
        }
        return -1;
    }

    private int excelIndexOf(Row header, String... keys) {
        for (int i = 0; i < header.getLastCellNum(); i++) {
            Cell cell = header.getCell(i);
            if (cell == null) continue;
            String val = getCellString(cell);
            for (String key : keys) {
                if (val.equalsIgnoreCase(key)) return i;
            }
        }
        return -1;
    }

    private String getCellString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default -> "";
        };
    }

    private int parseInt(String s) {
        try { return Integer.parseInt(s.replaceAll("[^0-9]", "")); }
        catch (NumberFormatException e) { return 1; }
    }
}
