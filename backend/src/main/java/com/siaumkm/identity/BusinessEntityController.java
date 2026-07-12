package com.siaumkm.identity;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

/**
 * SRS-B1: endpoint privat (di balik /app/**, lihat SecurityConfig).
 * Seluruh controller OWNER-only (temuan audit NFR-05): respons memuat
 * NPWP/NIK pemilik plaintext — data pribadi (UU PDP) yang tidak boleh
 * terbaca role STAFF meski tersimpan terenkripsi di database.
 */
@RestController
@RequestMapping("/app/identity")
@PreAuthorize("hasRole('OWNER')")
public class BusinessEntityController {

    private final BusinessEntityRepository repository;

    public BusinessEntityController(BusinessEntityRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<BusinessEntity> get(@PathVariable UUID id) {
        return repository.findById(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<BusinessEntity> create(@Valid @RequestBody BusinessEntity entity) {
        return ResponseEntity.ok(repository.save(entity));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<BusinessEntity> update(@PathVariable UUID id, @Valid @RequestBody BusinessEntity entity) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(repository.save(entity));
    }
}
