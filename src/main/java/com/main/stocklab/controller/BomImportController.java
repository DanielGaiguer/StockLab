package com.main.stocklab.controller;

import com.main.stocklab.dto.BomImportRequestDTO;
import com.main.stocklab.dto.BomImportResultDTO;
import com.main.stocklab.service.BomImportService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/bom")
public class BomImportController {

    @Autowired
    private BomImportService service;

    @PostMapping("/import")
    public ResponseEntity<BomImportResultDTO> importBom(
            @Valid @RequestBody BomImportRequestDTO request) {
        return ResponseEntity.ok(service.importBom(request));
    }

    @PostMapping("/upload")
    public ResponseEntity<BomImportResultDTO> uploadBom(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "projectName", required = false) String projectName) {
        return ResponseEntity.ok(service.uploadBom(file, projectName));
    }
}
