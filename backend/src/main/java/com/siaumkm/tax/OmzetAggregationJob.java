package com.siaumkm.tax;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * SRS-B5-02: rekalkulasi terjadwal agregasi omzet.
 * - Bulanan (tanggal 1, 00:05 WIB): tahun berjalan.
 * - Khusus Januari: juga FINALISASI tahun T-1 — angka inilah yang menentukan
 *   kelayakan PPh Final tahun berjalan (konsekuensi Pasal 58 berlaku untuk
 *   tahun pajak BERIKUTNYA setelah ambang terlampaui).
 * Zona Asia/Jakarta eksplisit — tahun agregasi mengikuti kalender WIB, bukan UTC.
 */
@Component
public class OmzetAggregationJob {

    private static final Logger log = LoggerFactory.getLogger(OmzetAggregationJob.class);
    static final ZoneId ZONA_WIB = ZoneId.of("Asia/Jakarta");

    private final OmzetAggregationService aggregationService;

    public OmzetAggregationJob(OmzetAggregationService aggregationService) {
        this.aggregationService = aggregationService;
    }

    @Scheduled(cron = "0 5 0 1 * *", zone = "Asia/Jakarta")
    public void jalankanBulanan() {
        LocalDate hariIni = LocalDate.now(ZONA_WIB);
        try {
            if (hariIni.getMonthValue() == 1) {
                aggregationService.hitungUlang(hariIni.getYear() - 1); // finalisasi T-1
            }
            var hasil = aggregationService.hitungUlang(hariIni.getYear());
            log.info("Agregasi omzet {} selesai: gabungan={} status={}",
                    hasil.tahun(), hasil.omzetGabungan(), hasil.status());
        } catch (java.util.NoSuchElementException e) {
            // Instalasi baru yang identitas usahanya belum diisi — bukan error.
            log.info("Agregasi omzet dilewati: {}", e.getMessage());
        }
    }
}
