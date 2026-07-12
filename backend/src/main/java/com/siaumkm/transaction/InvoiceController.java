package com.siaumkm.transaction;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

/** SRS-B3-05: CRUD invoice — pola akses identik ProductController. */
@RestController
@RequestMapping("/app/transaksi/invoices")
public class InvoiceController {

    private final InvoiceRepository repository;
    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceRepository repository, InvoiceService invoiceService) {
        this.repository = repository;
        this.invoiceService = invoiceService;
    }

    @GetMapping
    public List<Invoice> list() {
        return repository.findAll();
    }

    @PostMapping
    public ResponseEntity<Invoice> create(@Valid @RequestBody Invoice invoice) {
        return ResponseEntity.ok(repository.save(invoiceService.siapkan(invoice)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Invoice> update(@PathVariable UUID id, @Valid @RequestBody Invoice invoice) {
        if (!repository.existsById(id)) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(repository.save(invoiceService.siapkan(invoice)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
