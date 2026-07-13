package com.siaumkm.tax;

import com.siaumkm.tax.PphMasaService.HasilMasa;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * BR-B5-01: kalkulasi PPh Final per masa. OWNER-only (angka pajak = data
 * keuangan sensitif). Respons hanya hasil kalkulasi + rujukan aturan —
 * bukan opini/rekomendasi pajak (BR-B5-11).
 */
@RestController
@RequestMapping("/app/tax/pph-final")
@PreAuthorize("hasRole('OWNER')")
public class PphMasaController {

    private final PphMasaService pphMasaService;

    public PphMasaController(PphMasaService pphMasaService) {
        this.pphMasaService = pphMasaService;
    }

    @PostMapping("/recalculate")
    public ResponseEntity<HasilMasa> recalculate(@RequestParam(required = false) Integer tahun,
                                                  @RequestParam(required = false) Integer bulan) {
        // Default: masa pajak bulan LALU (kalkulasi masa berjalan belum final).
        LocalDate defaultMasa = LocalDate.now(OmzetAggregationJob.ZONA_WIB).minusMonths(1);
        int tahunEfektif = tahun != null ? tahun : defaultMasa.getYear();
        int bulanEfektif = bulan != null ? bulan : defaultMasa.getMonthValue();
        return ResponseEntity.ok(pphMasaService.hitungMasa(tahunEfektif, bulanEfektif));
    }
}
