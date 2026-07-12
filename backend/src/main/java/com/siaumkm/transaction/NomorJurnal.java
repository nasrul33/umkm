package com.siaumkm.transaction;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Generator nomor jurnal: prefix + epoch millis + 4 digit acak (21 karakter,
 * muat VARCHAR(30)). Suffix acak mencegah tabrakan unique constraint saat dua
 * transaksi tercatat pada milidetik yang sama.
 */
final class NomorJurnal {

    private NomorJurnal() {}

    static String generate(String prefix) {
        return prefix + "-" + System.currentTimeMillis() + "-"
                + ThreadLocalRandom.current().nextInt(1000, 10000);
    }
}
