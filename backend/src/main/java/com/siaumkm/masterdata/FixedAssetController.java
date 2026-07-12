package com.siaumkm.masterdata;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

/** SRS-B2-04: CRUD aset tetap — pola identik ProductController. */
@RestController
@RequestMapping("/app/master-data/fixed-assets")
public class FixedAssetController {

    private final FixedAssetRepository repository;

    public FixedAssetController(FixedAssetRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<FixedAsset> list() {
        return repository.findAll();
    }

    @PostMapping
    public ResponseEntity<FixedAsset> create(@Valid @RequestBody FixedAsset asset) {
        return ResponseEntity.ok(repository.save(asset));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FixedAsset> update(@PathVariable UUID id, @Valid @RequestBody FixedAsset asset) {
        if (!repository.existsById(id)) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(repository.save(asset));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
