package com.siaumkm.transaction;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ThreadLocalRandom;

/**
 * SRS-B3-05: subtotal dan total invoice SELALU dihitung ulang di server dari
 * qty x harga satuan — angka kiriman client tidak dipercaya (Aturan Emas #1).
 */
@Service
public class InvoiceService {

    public Invoice siapkan(Invoice invoice) {
        if (invoice.getNomorInvoice() == null || invoice.getNomorInvoice().isBlank()) {
            invoice.setNomorInvoice(generateNomorInvoice());
        }

        BigDecimal total = BigDecimal.ZERO;
        for (InvoiceLine line : invoice.getLines()) {
            if (line.getQty() == null || line.getQty().signum() <= 0) {
                throw new IllegalArgumentException("Qty baris invoice harus lebih besar dari 0");
            }
            if (line.getHargaSatuan() == null || line.getHargaSatuan().signum() < 0) {
                throw new IllegalArgumentException("Harga satuan baris invoice tidak valid");
            }
            BigDecimal subtotal = line.getQty()
                    .multiply(line.getHargaSatuan())
                    .setScale(2, RoundingMode.HALF_UP);
            line.setSubtotal(subtotal);
            total = total.add(subtotal);
        }
        invoice.setTotal(total.setScale(2, RoundingMode.HALF_UP));
        return invoice;
    }

    private String generateNomorInvoice() {
        return "INV-" + System.currentTimeMillis() + "-" + ThreadLocalRandom.current().nextInt(1000, 10000);
    }
}
