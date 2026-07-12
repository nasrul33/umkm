package com.siaumkm.masterdata;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

/**
 * SRS-B2-02: CRUD standar produk/jasa. Controller Customer/Vendor/CashAccount
 * mengikuti pola IDENTIK ini — bukan digenerate ulang berbeda-beda, agar konsisten.
 * OWNER dan STAFF sama-sama boleh baca & tambah data master (bukan data keuangan sensitif),
 * hanya hapus yang dibatasi OWNER (lihat NFR-10).
 */
@RestController
@RequestMapping("/app/master-data/products")
public class ProductController {

    private final ProductRepository repository;

    public ProductController(ProductRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Product> list() {
        return repository.findAll();
    }

    @PostMapping
    public ResponseEntity<Product> create(@Valid @RequestBody Product product) {
        return ResponseEntity.ok(repository.save(product));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> update(@PathVariable UUID id, @Valid @RequestBody Product product) {
        if (!repository.existsById(id)) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(repository.save(product));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
