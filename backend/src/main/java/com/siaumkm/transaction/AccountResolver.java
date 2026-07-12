package com.siaumkm.transaction;

import com.siaumkm.masterdata.ChartOfAccountRepository;
import org.springframework.stereotype.Component;
import java.util.UUID;

/**
 * Memetakan kode akun standar UMKM (seed Flyway V1__init_schema.sql) ke UUID.
 * Kode akun di sini BUKAN hardcode tarif/logika pajak (Aturan Emas #3 tidak
 * dilanggar) — ini rujukan struktur CoA standar yang di-seed saat provisioning.
 */
@Component
public class AccountResolver {

    public static final String KAS               = "1000";
    public static final String BANK              = "1100";
    public static final String PIUTANG_USAHA     = "1200";
    public static final String PERSEDIAAN        = "1300";
    public static final String HUTANG_USAHA      = "2000";
    public static final String MODAL_PEMILIK     = "3000";
    public static final String PENDAPATAN_USAHA  = "4000";
    public static final String BIAYA_OPERASIONAL = "5100";

    private final ChartOfAccountRepository repository;

    public AccountResolver(ChartOfAccountRepository repository) {
        this.repository = repository;
    }

    public UUID idByKode(String kodeAkun) {
        return repository.findByKodeAkun(kodeAkun)
                .orElseThrow(() -> new IllegalStateException(
                    "Akun " + kodeAkun + " belum ter-seed di chart_of_account"))
                .getId();
    }

    /** Akun tempat dana masuk/keluar sesuai pilihan "Bayar dengan" di wizard. */
    public UUID akunDana(MetodePembayaran metode) {
        return switch (metode) {
            case CASH -> idByKode(KAS);
            case TRANSFER, QRIS -> idByKode(BANK);
            case RECEIVABLE, PAYABLE -> throw new IllegalArgumentException(
                "Metode " + metode + " bukan akun dana — piutang/hutang ditangani di mapper masing-masing.");
        };
    }
}
