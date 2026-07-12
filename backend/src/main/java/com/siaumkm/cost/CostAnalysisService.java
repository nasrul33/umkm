package com.siaumkm.cost;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** SRS-B6-01/02: margin per produk dari vw_product_margin (tervalidasi di schema.sql). */
@Service
public class CostAnalysisService {

    private final JdbcTemplate jdbcTemplate;

    public CostAnalysisService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public record ProductMarginRow(UUID productId, String nama, BigDecimal hargaJual,
                                    BigDecimal hppDasar, BigDecimal marginKotor, BigDecimal marginPersen) {}

    public List<ProductMarginRow> getProductMargins() {
        return jdbcTemplate.query(
            "SELECT product_id, nama, harga_jual, hpp_dasar, margin_kotor, margin_persen FROM vw_product_margin",
            (rs, i) -> new ProductMarginRow(
                UUID.fromString(rs.getString("product_id")), rs.getString("nama"),
                rs.getBigDecimal("harga_jual"), rs.getBigDecimal("hpp_dasar"),
                rs.getBigDecimal("margin_kotor"), rs.getBigDecimal("margin_persen")));
    }
}
