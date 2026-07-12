package com.siaumkm.identity;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

/**
 * SRS-B1-02: entitas usaha lain lingkaran Pasal 58. OWNER-only — memuat NPWP
 * dan omzet entitas lain; omzet_tahunan_diketahui memengaruhi angka pajak dan
 * perubahannya terekam audit hash-chain (trg_audit_related_entity).
 */
@RestController
@RequestMapping("/app/identity/related-entities")
@PreAuthorize("hasRole('OWNER')")
public class RelatedEntityController {

    private final RelatedEntityRepository repository;

    public RelatedEntityController(RelatedEntityRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<RelatedEntity> list() {
        return repository.findAll();
    }

    @PostMapping
    public ResponseEntity<RelatedEntity> create(@Valid @RequestBody RelatedEntity entity) {
        return ResponseEntity.ok(repository.save(entity));
    }

    @PutMapping("/{id}")
    public ResponseEntity<RelatedEntity> update(@PathVariable UUID id, @Valid @RequestBody RelatedEntity entity) {
        if (!repository.existsById(id)) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(repository.save(entity));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
