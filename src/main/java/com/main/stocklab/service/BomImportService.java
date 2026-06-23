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
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class BomImportService {

    private static final Logger log = LoggerFactory.getLogger(BomImportService.class);

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
        return processImport(projectName, file.getOriginalFilename(), "BOM Import", items);
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
        int lowStock = 0;
        List<String> lowStockComponents = new ArrayList<>();
        List<BomImportItem> importItems = new ArrayList<>();
        List<StockMovement> movements = new ArrayList<>();
        List<Component> componentsToUpdate = new ArrayList<>();

        for (BomImportItemDTO item : items) {
            processed++;

            log.info("Looking up part number: '{}'", item.partNumber());
            Optional<Component> optComponent = componentRepository.findByPartNumber(item.partNumber());
            log.info("  Found: {}", optComponent.isPresent());

            BomImportItem importItem = new BomImportItem();
            importItem.setBomImport(bomImport);
            importItem.setPartNumber(item.partNumber());
            importItem.setDesignator(item.designator());
            importItem.setQuantity(item.quantity());

            if (optComponent.isPresent()) {
                Component component = optComponent.get();
                matched++;
                importItem.setComponent(component);

                int quantityBefore = component.getQuantityInStock() != null ? component.getQuantityInStock() : 0;
                int quantityAfter = quantityBefore - item.quantity();
                component.setQuantityInStock(Math.max(quantityAfter, 0));
                componentsToUpdate.add(component);

                StockMovement movement = new StockMovement();
                movement.setComponent(component);
                movement.setMovementType(MovementType.BOM_DEDUCTION);
                movement.setQuantity(item.quantity());
                movement.setQuantityBefore(quantityBefore);
                movement.setQuantityAfter(quantityAfter);
                movement.setProjectName(projectName);
                movement.setBomImportId(bomImport.getId());
                movement.setPerformedBy(importedBy != null ? importedBy : "BOM Import");
                movements.add(movement);

                if (component.getMinimumQuantity() != null && quantityAfter <= component.getMinimumQuantity()) {
                    lowStock++;
                    importItem.setStatus(BomImportItemStatus.LOW_STOCK);
                    lowStockComponents.add(component.getPartNumber() + " - " + component.getName());
                } else {
                    importItem.setStatus(BomImportItemStatus.FOUND);
                }
            } else {
                notFound++;
                importItem.setStatus(BomImportItemStatus.NOT_FOUND);
            }

            importItems.add(importItem);
        }

        componentRepository.saveAll(componentsToUpdate);
        stockMovementRepository.saveAll(movements);
        bomImportItemRepository.saveAll(importItems);

        if (notFound == 0 && lowStock == 0) {
            bomImport.setStatus(BomImportStatus.PROCESSED);
        } else if (notFound == 0 && lowStock > 0) {
            bomImport.setStatus(BomImportStatus.PARTIALLY_MATCHED);
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
        log.info("parseFile: filename='{}', size={}, partContentType='{}'", filename, file.getSize(), file.getContentType());
        try {
            log.info("parseFile: name='{}', originalFilename='{}', isEmpty={}, fileSize={}",
                    file.getName(), file.getOriginalFilename(), file.isEmpty(), file.getBytes().length);
        } catch (Exception e) {
            log.warn("Could not read file bytes: {}", e.getMessage());
        }

        if (filename != null && (filename.endsWith(".xlsx") || filename.endsWith(".xls"))) {
            log.info("Detected Excel file");
            return parseExcel(file);
        }
        try {
            byte[] content = file.getBytes();
            log.info("File bytes length: {}", content.length);
            String rawContent = new String(content, StandardCharsets.UTF_8);
            log.info("Raw file content:\n{}", rawContent);
            String firstLine = rawContent.split("\n")[0].trim();
            log.info("First line: '{}'", firstLine);

            if (isAltiumTsv(content)) {
                log.info("Detected Altium TSV");
                List<BomImportItemDTO> items = parseAltiumTsv(content);
                log.info("Parsed {} Altium items", items.size());
                for (int i = 0; i < items.size(); i++) {
                    log.info("  Item {}: partNumber='{}', qty={}", i, items.get(i).partNumber(), items.get(i).quantity());
                }
                return items;
            }
            log.info("Not Altium TSV, falling back to CSV");
            List<BomImportItemDTO> items = parseCsv(content);
            log.info("Parsed {} CSV items", items.size());
            for (int i = 0; i < items.size(); i++) {
                log.info("  Item {}: partNumber='{}', qty={}", i, items.get(i).partNumber(), items.get(i).quantity());
            }
            return items;
        } catch (Exception e) {
            log.error("Failed to read file: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to read file: " + e.getMessage());
        }
    }

    private boolean isAltiumTsv(byte[] content) {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8))) {
            String firstLine = br.readLine();
            boolean result = firstLine != null
                    && (firstLine.contains("Bill of Materials")
                    || firstLine.contains("Lista de Componentes"));
            log.debug("isAltiumTsv: firstLine='{}', result={}", firstLine, result);
            return result;
        } catch (Exception e) {
            log.warn("isAltiumTsv error: {}", e.getMessage());
            return false;
        }
    }

    private List<BomImportItemDTO> parseAltiumTsv(byte[] content) {
        List<BomImportItemDTO> items = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8))) {
            // Skip 5 metadata lines (header, project, date, empty line)
            for (int i = 0; i < 5; i++) {
                String metaLine = br.readLine();
                if (metaLine == null) return items;
            }

            // Read header row (tab-separated)
            String headerLine = br.readLine();
            if (headerLine == null) return items;
            String[] headers = headerLine.split("\t");

            // Find column indices: Part Number (5), Quantity (6), Designator (7)
            int pnIdx = indexOf(headers, "part number", "partNumber", "pn");
            int qtyIdx = indexOf(headers, "quantity", "qty", "quantidade");
            int desIdx = indexOf(headers, "designator", "refdes", "reference");

            // If header detection failed, use fixed Altium positions
            if (pnIdx < 0) pnIdx = 5;
            if (qtyIdx < 0) qtyIdx = 6;
            if (desIdx < 0) desIdx = 7;

            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank() || line.startsWith("Qntd") || line.startsWith("Quantidade")) break;
                String[] parts = line.split("\t");
                String partNumber = pnIdx < parts.length ? parts[pnIdx].trim() : "";
                if (partNumber.isEmpty()) continue;
                int quantity = qtyIdx < parts.length ? parseInt(parts[qtyIdx].trim()) : 1;
                String designator = desIdx < parts.length ? parts[desIdx].trim() : null;
                if (designator != null && designator.startsWith("***")) {
                    designator = null;
                }
                items.add(new BomImportItemDTO(partNumber, quantity, designator));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Altium BOM: " + e.getMessage());
        }
        return items;
    }

    private List<BomImportItemDTO> parseCsv(byte[] content) {
        List<BomImportItemDTO> items = new ArrayList<>();
        try (Reader reader = new InputStreamReader(new ByteArrayInputStream(content), StandardCharsets.UTF_8);
             BufferedReader br = new BufferedReader(reader)) {
            String header = br.readLine();
            String[] cols = parseCsvLine(header);
            int pnIdx = indexOf(cols, "part_number", "partNumber", "part number", "pn");
            int qtyIdx = indexOf(cols, "quantity", "qty", "quantidade");
            int desIdx = indexOf(cols, "designator", "refdes", "reference");

            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = parseCsvLine(line);
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

    private String[] parseCsvLine(String line) {
        if (line == null || line.isBlank()) return new String[0];
        List<String> fields = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder field = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(field.toString());
                field = new StringBuilder();
            } else {
                field.append(c);
            }
        }
        fields.add(field.toString());
        return fields.toArray(new String[0]);
    }

    private List<BomImportItemDTO> parseExcel(MultipartFile file) {
        List<BomImportItemDTO> items = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            log.info("Excel sheet: rows={}, firstRowNum={}, lastRowNum={}", sheet.getPhysicalNumberOfRows(), sheet.getFirstRowNum(), sheet.getLastRowNum());

            // Scan for header row (could have metadata rows before it)
            int headerRowIdx = -1;
            for (int i = sheet.getFirstRowNum(); i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                StringBuilder rowStr = new StringBuilder();
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    rowStr.append(" [").append(c).append("]=").append(getCellString(row.getCell(c)));
                }
                log.info("  Excel row {}:{}", i, rowStr);
                // Check if this row contains header keywords
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    String val = getCellString(row.getCell(c));
                    if (val.equalsIgnoreCase("part number") || val.equalsIgnoreCase("partNumber")
                            || val.equalsIgnoreCase("pn") || val.equalsIgnoreCase("quantity")
                            || val.equalsIgnoreCase("qty") || val.equalsIgnoreCase("designator")) {
                        headerRowIdx = i;
                        log.info("Found header row at index {}", i);
                        break;
                    }
                }
                if (headerRowIdx >= 0) break;
            }

            if (headerRowIdx < 0) {
                log.warn("No header row found in Excel, trying row 0 as fallback");
                Row row0 = sheet.getRow(0);
                if (row0 != null) {
                    StringBuilder rowStr = new StringBuilder();
                    for (int c = 0; c < row0.getLastCellNum(); c++) {
                        rowStr.append(" [").append(c).append("]=").append(getCellString(row0.getCell(c)));
                    }
                    log.info("  Row 0:{}", rowStr);
                }
                return items;
            }

            Row headerRow = sheet.getRow(headerRowIdx);
            int pnIdx = excelIndexOf(headerRow, "part_number", "partNumber", "part number", "pn");
            int qtyIdx = excelIndexOf(headerRow, "quantity", "qty", "quantidade");
            int desIdx = excelIndexOf(headerRow, "designator", "refdes", "reference");
            log.info("Excel column indices: pnIdx={}, qtyIdx={}, desIdx={}", pnIdx, qtyIdx, desIdx);

            for (int i = headerRowIdx + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String partNumber = pnIdx >= 0 ? getCellString(row.getCell(pnIdx)) : "";
                int quantity = qtyIdx >= 0 ? getNumericCellValue(row.getCell(qtyIdx)) : 1;
                String designator = desIdx >= 0 ? getCellString(row.getCell(desIdx)) : null;
                log.debug("  Excel row {}: partNumber='{}', qty={}, des='{}'", i, partNumber, quantity, designator);
                if (!partNumber.isEmpty()) {
                    items.add(new BomImportItemDTO(partNumber, quantity, designator));
                }
            }
            log.info("Parsed {} Excel items", items.size());
            for (int i = 0; i < items.size(); i++) {
                log.info("  Item {}: partNumber='{}', qty={}", i, items.get(i).partNumber(), items.get(i).quantity());
            }
        } catch (Exception e) {
            log.error("Failed to parse Excel: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse Excel: " + e.getMessage());
        }
        return items;
    }

    private int getNumericCellValue(Cell cell) {
        if (cell == null) return 1;
        try {
            return (int) cell.getNumericCellValue();
        } catch (Exception e) {
            String val = getCellString(cell);
            return parseInt(val);
        }
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
