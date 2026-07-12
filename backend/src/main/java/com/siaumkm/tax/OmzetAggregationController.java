package com.siaumkm.tax;

import com.siaumkm.tax.OmzetAggregationService.HasilAgregasi;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * SRS-B5-02/B5-03: hasil agregasi omzet + status ambang. OWNER-only —
 * angka omzet gabungan lingkaran keluarga adalah data keuangan sensitif.
 * Respons hanya kalkulasi + rujukan pasal, bukan rekomendasi (BR-B5-11).
 */
@RestController
@RequestMapping("/app/tax/omzet-aggregation")
@PreAuthorize("hasRole('OWNER')")
public class OmzetAggregationController {

    private final OmzetAggregationService aggregationService;

    public OmzetAggregationController(OmzetAggregationService aggregationService) {
        this.aggregationService = aggregationService;
    }

    @PostMapping("/recalculate")
    public ResponseEntity<HasilAgregasi> recalculate(
            @RequestParam(required = false) Integer tahun) {
        int tahunEfektif = tahun != null ? tahun
                : LocalDate.now(OmzetAggregationJob.ZONA_WIB).getYear();
        return ResponseEntity.ok(aggregationService.hitungUlang(tahunEfektif));
    }
}
