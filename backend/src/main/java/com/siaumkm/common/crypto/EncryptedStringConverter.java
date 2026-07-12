package com.siaumkm.common.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

/**
 * NFR-05: pasang dengan @Convert pada field entity yang menyimpan data sensitif
 * (NPWP, NIK, no. rekening). Validasi format (@Pattern) tetap berjalan pada
 * plaintext di memori — konversi hanya terjadi saat tulis/baca database.
 * DI berfungsi karena Spring Boot mendaftarkan SpringBeanContainer ke Hibernate.
 */
@Component
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private final FieldEncryptionService crypto;

    public EncryptedStringConverter(FieldEncryptionService crypto) {
        this.crypto = crypto;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return crypto.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return crypto.decrypt(dbData);
    }
}
