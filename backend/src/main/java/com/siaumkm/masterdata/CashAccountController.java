package com.siaumkm.masterdata;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

/** SRS-B2-07: CRUD rekening kas/bank/e-wallet — pola identik ProductController. */
@RestController
@RequestMapping("/app/master-data/cash-accounts")
public class CashAccountController {

    private final CashAccountRepository repository;

    public CashAccountController(CashAccountRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<CashAccount> list() {
        return repository.findAll();
    }

    @PostMapping
    public ResponseEntity<CashAccount> create(@Valid @RequestBody CashAccount cashAccount) {
        return ResponseEntity.ok(repository.save(cashAccount));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CashAccount> update(@PathVariable UUID id, @Valid @RequestBody CashAccount cashAccount) {
        if (!repository.existsById(id)) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(repository.save(cashAccount));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
