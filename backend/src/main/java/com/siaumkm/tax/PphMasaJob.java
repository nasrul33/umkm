package com.siaumkm.tax;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * BR-B5-01: finalisasi kalkulasi PPh Final masa T pada awal bulan T+1
 * (00:15 WIB — setelah OmzetAggregationJob), memberi ruang koreksi sebelum
 * jatuh tempo setor tanggal 15. Zona Asia/Jakarta eksplisit: masa yang
 * dihitung mengikuti kalender WIB, bukan UTC.
 */
@Component
public class PphMasaJob {

    private static final Logger log = LoggerFactory.getLogger(PphMasaJob.class);

    private final PphMasaService pphMasaService;

    public PphMasaJob(PphMasaService pphMasaService) {
        this.pphMasaService = pphMasaService;
    }

    @Scheduled(cron = "0 15 0 1 * *", zone = "Asia/Jakarta")
    public void finalisasiMasaLalu() {
        LocalDate masaLalu = LocalDate.now(OmzetAggregationJob.ZONA_WIB).minusMonths(1);
        try {
            var hasil = pphMasaService.hitungMasa(masaLalu.getYear(), masaLalu.getMonthValue());
            log.info("PPh Final masa {}/{} final: DPP={} pajak={} jatuh tempo setor={}",
                    hasil.bulan(), hasil.tahun(), hasil.omzetKenaPajak(),
                    hasil.pajakTerhitung(), hasil.jatuhTempoSetor());
        } catch (java.util.NoSuchElementException e) {
            log.info("Kalkulasi PPh masa dilewati: {}", e.getMessage());
        } catch (IllegalStateException e) {
            // mis. koperasi lewat batas waktu / bentuk badan di luar skema —
            // fakta yang harus terlihat operator, bukan silent failure.
            log.warn("Kalkulasi PPh masa {}/{} tidak dapat dilakukan: {}",
                    masaLalu.getMonthValue(), masaLalu.getYear(), e.getMessage());
        }
    }
}
