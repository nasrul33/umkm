package com.siaumkm.masterdata;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

/**
 * SRS-B2-03: CRUD karyawan. Berbeda dari master data lain, seluruh controller
 * OWNER-only (temuan audit NFR-05): respons memuat NPWP dan gaji karyawan —
 * data sensitif yang tidak boleh terbaca role STAFF.
 */
@RestController
@RequestMapping("/app/master-data/employees")
@PreAuthorize("hasRole('OWNER')")
public class EmployeeController {

    private final EmployeeRepository repository;

    public EmployeeController(EmployeeRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Employee> list() {
        return repository.findAll();
    }

    @PostMapping
    public ResponseEntity<Employee> create(@Valid @RequestBody Employee employee) {
        return ResponseEntity.ok(repository.save(employee));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Employee> update(@PathVariable UUID id, @Valid @RequestBody Employee employee) {
        if (!repository.existsById(id)) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(repository.save(employee));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
