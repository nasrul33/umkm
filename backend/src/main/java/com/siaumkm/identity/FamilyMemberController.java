package com.siaumkm.identity;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

/**
 * SRS-B1-02: registri lingkaran keluarga Pasal 58. OWNER-only — memuat
 * NIK/NPWP anggota keluarga (data pribadi, NFR-05/NFR-10).
 */
@RestController
@RequestMapping("/app/identity/family-members")
@PreAuthorize("hasRole('OWNER')")
public class FamilyMemberController {

    private final FamilyMemberRepository repository;

    public FamilyMemberController(FamilyMemberRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<FamilyMember> list() {
        return repository.findAll();
    }

    @PostMapping
    public ResponseEntity<FamilyMember> create(@Valid @RequestBody FamilyMember member) {
        return ResponseEntity.ok(repository.save(member));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FamilyMember> update(@PathVariable UUID id, @Valid @RequestBody FamilyMember member) {
        if (!repository.existsById(id)) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(repository.save(member));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
