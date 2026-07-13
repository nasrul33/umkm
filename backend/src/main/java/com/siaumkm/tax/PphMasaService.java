package com.siaumkm.tax;

import com.siaumkm.identity.BusinessEntity;
import com.siaumkm.identity.BusinessEntityRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * BR-B5-01 / SRS-B5-01: kalkulasi PPh Final per masa pajak (bulanan) —
 * omzet dari pembukuan (OmzetUsahaQuery, definisi sama dgn agregasi Pasal 58),
 * rumus DPP di TaxCalculationEngine (parameter dari tax_rule), hasil dicatat
 * append-only ke tax_calculation_log + entri kalender jatuh tempo setor
 * (tanggal 15 bulan berikutnya utk setor sendiri, PMK 164/2023 — hari
 * configurable, bukan konstanta tersebar).
 */
@Service
public class PphMasaService {

    public record HasilMasa(int tahun, int bulan,
                             BigDecimal omzetBruto,
                             BigDecimal omzetKumulatifTahunan,
                             BigDecimal omzetKenaPajak,
                             BigDecimal pajakTerhitung,
                             String regulasiAcuan,
                             LocalDate jatuhTempoSetor) {}

    private final OmzetUsahaQuery omzetUsahaQuery;
    private final TaxCalculationEngine engine;
    private final BusinessEntityRepository businessEntityRepository;
    private final TaxCalculationLogRepository logRepository;
    private final JdbcTemplate jdbcTemplate;
    private final int jatuhTempoHari;

    public PphMasaService(OmzetUsahaQuery omzetUsahaQuery,
                          TaxCalculationEngine engine,
                          BusinessEntityRepository businessEntityRepository,
                          TaxCalculationLogRepository logRepository,
                          JdbcTemplate jdbcTemplate,
                          @Value("${siaumkm.tax.jatuh-tempo-setor-hari:15}") int jatuhTempoHari) {
        this.omzetUsahaQuery = omzetUsahaQuery;
        this.engine = engine;
        this.businessEntityRepository = businessEntityRepository;
        this.logRepository = logRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.jatuhTempoHari = jatuhTempoHari;
    }

    /** Varian single-tenant: satu-satunya business_entity terdaftar. */
    @Transactional
    public HasilMasa hitungMasa(int tahun, int bulan) {
        BusinessEntity utama = businessEntityRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new NoSuchElementException(
                    "Identitas usaha belum diisi — lengkapi Modul B1 terlebih dahulu."));
        return hitungMasa(utama.getId(), tahun, bulan);
    }

    @Transactional
    public HasilMasa hitungMasa(UUID businessEntityId, int tahun, int bulan) {
        BusinessEntity utama = businessEntityRepository.findById(businessEntityId)
                .orElseThrow(() -> new NoSuchElementException(
                    "business_entity " + businessEntityId + " tidak ditemukan."));

        LocalDate awalMasa = LocalDate.of(tahun, bulan, 1);
        LocalDate awalMasaBerikut = awalMasa.plusMonths(1);
        LocalDate akhirMasa = awalMasaBerikut.minusDays(1);

        BigDecimal omzetBulan = omzetUsahaQuery.hitung(awalMasa, awalMasaBerikut);
        // Kumulatif SESUDAH bulan berjalan — semantik yang dituntut engine
        // utk rumus pengecualian Rp500jt (PMK 164/2023).
        BigDecimal omzetKumulatif = omzetUsahaQuery.hitung(LocalDate.of(tahun, 1, 1), awalMasaBerikut);

        // Aturan aktif dipilih per akhir masa pajak; exception engine (koperasi
        // lewat batas, CV, data kurang) menggagalkan seluruh transaksi — tidak
        // ada baris log yang tertulis.
        var hasil = engine.hitungPphFinal(utama.getBentukBadan(), omzetBulan,
                omzetKumulatif, akhirMasa, utama.getTanggalTerdaftarPajak());

        logRepository.save(new TaxCalculationLog(businessEntityId, bulan, tahun,
                omzetBulan, hasil.omzetKenaPajak(), hasil.taxRuleId(), hasil.pajakTerhitung()));

        LocalDate jatuhTempo = awalMasaBerikut.withDayOfMonth(jatuhTempoHari);
        pastikanKalender(businessEntityId, tahun, bulan, jatuhTempo);

        return new HasilMasa(tahun, bulan, omzetBulan, omzetKumulatif,
                hasil.omzetKenaPajak(), hasil.pajakTerhitung(), hasil.regulasiAcuan(), jatuhTempo);
    }

    /** BR-B5-09: entri kalender kepatuhan per masa, idempoten. */
    private void pastikanKalender(UUID businessEntityId, int tahun, int bulan, LocalDate jatuhTempo) {
        String nama = String.format("Setor PPh Final masa %02d/%d", bulan, tahun);
        jdbcTemplate.update("""
            INSERT INTO tax_calendar (nama_kewajiban, jatuh_tempo, business_entity_id)
            SELECT ?, ?, ?
            WHERE NOT EXISTS (
                SELECT 1 FROM tax_calendar
                WHERE nama_kewajiban = ? AND business_entity_id = ?
            )
            """, nama, jatuhTempo, businessEntityId, nama, businessEntityId);
    }
}
