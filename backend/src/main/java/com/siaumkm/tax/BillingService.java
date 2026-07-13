package com.siaumkm.tax;

import com.siaumkm.identity.BusinessEntity;
import com.siaumkm.identity.BusinessEntityRepository;
import com.siaumkm.tax.PjapClient.BillingOrder;
import com.siaumkm.tax.PjapClient.BillingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * BR-B5-07 / NFR-09: pembuatan kode billing atas hasil kalkulasi masa.
 * Aturan Emas #6: kegagalan PJAP TIDAK menghentikan apa pun — billing_request
 * jatuh ke PENDING_MANUAL (operator membuat kode di DJP Online lalu
 * mencatatkannya via catatManual), pencatatan internal jalan terus.
 *
 * SENGAJA TIDAK @Transactional di method utama: baris REQUESTED harus
 * ter-commit SEBELUM panggilan HTTP (idempotensi; koneksi DB tidak ditahan
 * selama retry HTTP). Retry hidup di PjapClient, bukan di sini.
 */
@Service
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);

    private final BillingRequestRepository billingRequestRepository;
    private final TaxCalculationLogRepository taxCalculationLogRepository;
    private final TaxRuleRepository taxRuleRepository;
    private final BusinessEntityRepository businessEntityRepository;
    private final ObjectProvider<PjapClient> pjapClientProvider;
    private final PjapProperties properties;

    public BillingService(BillingRequestRepository billingRequestRepository,
                          TaxCalculationLogRepository taxCalculationLogRepository,
                          TaxRuleRepository taxRuleRepository,
                          BusinessEntityRepository businessEntityRepository,
                          ObjectProvider<PjapClient> pjapClientProvider,
                          PjapProperties properties) {
        this.billingRequestRepository = billingRequestRepository;
        this.taxCalculationLogRepository = taxCalculationLogRepository;
        this.taxRuleRepository = taxRuleRepository;
        this.businessEntityRepository = businessEntityRepository;
        this.pjapClientProvider = pjapClientProvider;
        this.properties = properties;
    }

    public BillingRequest buatKodeBilling(UUID taxCalculationLogId) {
        TaxCalculationLog kalkulasi = taxCalculationLogRepository.findById(taxCalculationLogId)
                .orElseThrow(() -> new NoSuchElementException(
                    "tax_calculation_log " + taxCalculationLogId + " tidak ditemukan."));

        if (kalkulasi.getPajakTerhitung().signum() <= 0) {
            throw new IllegalStateException(
                "Tidak ada pajak terutang pada masa ini — kode billing tidak diperlukan.");
        }
        // Pembulatan adalah keputusan pajak di engine, BUKAN di lapisan ini —
        // nilai berdesimal berarti rumus pembulatan belum diterapkan: fail fast.
        if (kalkulasi.getPajakTerhitung().stripTrailingZeros().scale() > 0) {
            throw new IllegalStateException(
                "pajak_terhitung " + kalkulasi.getPajakTerhitung() + " bukan rupiah utuh — "
                + "kalkulasi ulang masa ini diperlukan sebelum membuat kode billing.");
        }

        TaxRule rule = taxRuleRepository.findById(kalkulasi.getTaxRuleId()).orElseThrow();
        if (rule.getKodeAkunPajak() == null || rule.getKodeJenisSetoran() == null) {
            throw new IllegalStateException(
                "KAP/KJS belum terisi pada tax_rule " + rule.getKodeAturan() + " — lengkapi via /add-tax-rule.");
        }

        BusinessEntity entitas = businessEntityRepository.findById(kalkulasi.getBusinessEntityId())
                .orElseThrow();
        if (entitas.getNpwp() == null) {
            throw new IllegalStateException("NPWP belum diisi di identitas usaha (Modul B1).");
        }

        // TX-1: klaim idempoten — REQUESTED ter-commit sebelum HTTP.
        BillingRequest request = new BillingRequest();
        request.setBusinessEntityId(entitas.getId());
        request.setTaxCalculationLogId(taxCalculationLogId);
        request.setPjapProvider(properties.provider() != null ? properties.provider() : "UNKNOWN");
        request.setStatus(BillingRequest.Status.REQUESTED);

        long jumlah = kalkulasi.getPajakTerhitung().longValueExact();
        String npwp = entitas.getNpwp();
        request.setRequestPayload(payloadMasked(npwp, rule, kalkulasi, jumlah));

        try {
            request = billingRequestRepository.saveAndFlush(request);
        } catch (DataIntegrityViolationException e) {
            // Sudah ada request hidup utk kalkulasi ini — kembalikan yang ada.
            return billingRequestRepository
                    .findFirstByTaxCalculationLogIdAndStatusNotOrderByRequestedAtDesc(
                            taxCalculationLogId, BillingRequest.Status.FAILED)
                    .orElseThrow(() -> e);
        }

        // TX-2 dst.: panggilan HTTP di luar transaksi DB.
        PjapClient pjapClient = pjapClientProvider.getIfAvailable();
        if (pjapClient == null) {
            // Aturan Emas #6: tanpa mitra PJAP terkonfigurasi, sistem tetap
            // hidup — operator membuat kode billing manual di DJP Online.
            request.setResponsePayload(jsonError(
                "PJAP belum dikonfigurasi (PJAP_BASE_URL kosong) — buat kode billing manual di DJP Online lalu catat via /manual."));
            request.setStatus(BillingRequest.Status.PENDING_MANUAL);
            return billingRequestRepository.saveAndFlush(request);
        }

        BillingOrder order = new BillingOrder(request.getId().toString(), npwp,
                rule.getKodeAkunPajak(), rule.getKodeJenisSetoran(),
                kalkulasi.getPeriodeBulan(), kalkulasi.getPeriodeBulan(),
                kalkulasi.getPeriodeTahun(), jumlah);

        try {
            BillingResult hasil = pjapClient.buatKodeBilling(order);
            request.setKodeBilling(hasil.kodeBilling());
            request.setResponsePayload(mask(hasil.rawResponseBody(), npwp));
            request.setStatus(BillingRequest.Status.ISSUED);
        } catch (PjapClient.PjapPermanentException e) {
            request.setResponsePayload(jsonError(e.getMessage()));
            request.setStatus(BillingRequest.Status.FAILED);
        } catch (PjapClient.PjapTransientException | PjapClient.PjapAmbiguousResponseException e) {
            request.setResponsePayload(jsonError(e.getMessage()));
            request.setStatus(BillingRequest.Status.PENDING_MANUAL);
            log.warn("Billing ref={} jatuh ke PENDING_MANUAL: {}", request.getId(), e.getMessage());
        }
        return billingRequestRepository.saveAndFlush(request);
    }

    /** SRS-B5-05: operator mencatat kode billing yang dibuat manual di DJP Online. */
    public BillingRequest catatManual(UUID billingRequestId, String kodeBilling) {
        if (kodeBilling == null || !kodeBilling.matches("^[0-9]{15}$")) {
            throw new IllegalArgumentException("Kode billing harus 15 digit angka.");
        }
        BillingRequest request = billingRequestRepository.findById(billingRequestId)
                .orElseThrow(() -> new NoSuchElementException(
                    "billing_request " + billingRequestId + " tidak ditemukan."));
        if (request.getStatus() == BillingRequest.Status.ISSUED) {
            throw new IllegalStateException(
                "Kode billing sudah terbit untuk request ini — tidak dapat ditimpa manual.");
        }
        request.setKodeBilling(kodeBilling);
        request.setStatus(BillingRequest.Status.ISSUED);
        return billingRequestRepository.saveAndFlush(request);
    }

    // NFR-05: NPWP tidak pernah tersimpan utuh di payload JSONB — 4 digit awal
    // + 4 akhir cukup untuk debugging; nilai utuh selalu ada di business_entity.
    private String maskNpwp(String npwp) {
        return npwp.substring(0, 4) + "********" + npwp.substring(12);
    }

    private String mask(String teks, String npwp) {
        return teks == null ? null : teks.replace(npwp, maskNpwp(npwp));
    }

    private String payloadMasked(String npwp, TaxRule rule, TaxCalculationLog kalkulasi, long jumlah) {
        return String.format(
            "{\"npwp\":\"%s\",\"kodeAkunPajak\":\"%s\",\"kodeJenisSetoran\":\"%s\","
            + "\"masa\":\"%02d/%d\",\"jumlah\":%d}",
            maskNpwp(npwp), rule.getKodeAkunPajak(), rule.getKodeJenisSetoran(),
            kalkulasi.getPeriodeBulan(), kalkulasi.getPeriodeTahun(), jumlah);
    }

    private String jsonError(String pesan) {
        return "{\"error\":" + toJsonString(pesan) + "}";
    }

    private String toJsonString(String s) {
        return "\"" + (s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"")) + "\"";
    }
}
