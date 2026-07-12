package com.siaumkm.masterdata;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** SRS-B2-03: master karyawan, dasar perhitungan beban gaji & PPh 21 (B5). */
@Entity
@Table(name = "employee")
public class Employee {

    @Id @GeneratedValue private UUID id;

    @Column(nullable = false) private String nama;

    // TODO NFR: NPWP wajib dienkripsi di application layer sebelum rilis —
    // konsisten dengan penanganan npwp/nik di BusinessEntity (belum diimplementasi).
    @Column(length = 16) private String npwp;

    @Column(name = "gaji_bulanan", precision = 19, scale = 2)
    private BigDecimal gajiBulanan;

    @Column(name = "tanggal_mulai") private LocalDate tanggalMulai;

    @Column(name = "is_active", nullable = false) private boolean isActive = true;

    public UUID getId() { return id; }
    public String getNama() { return nama; }
    public void setNama(String v) { this.nama = v; }
    public String getNpwp() { return npwp; }
    public void setNpwp(String v) { this.npwp = v; }
    public BigDecimal getGajiBulanan() { return gajiBulanan; }
    public void setGajiBulanan(BigDecimal v) { this.gajiBulanan = v; }
    public LocalDate getTanggalMulai() { return tanggalMulai; }
    public void setTanggalMulai(LocalDate v) { this.tanggalMulai = v; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean v) { this.isActive = v; }
}
