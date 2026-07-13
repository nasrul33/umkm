package com.siaumkm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point SIA-UMKM Premium.
 * Rujukan: SRS-UMKM-01 v1.0 Bagian 2 (Arsitektur Sistem).
 * Setiap instalasi bersifat single-tenant per klien — TIDAK ada logic multi-tenancy di sini.
 */
@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class SiaUmkmApplication {
    public static void main(String[] args) {
        SpringApplication.run(SiaUmkmApplication.class, args);
    }
}
