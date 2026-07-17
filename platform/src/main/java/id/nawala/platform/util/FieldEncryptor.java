package id.nawala.platform.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Base64;

/**
 * JPA AttributeConverter that auto-encrypts/decrypts string fields
 * annotated with @Convert(converter = FieldEncryptor.class).
 * Handles legacy unencrypted data gracefully.
 */
@Converter
@Component
public class FieldEncryptor implements AttributeConverter<String, String> {

    private static EncryptionUtils encryptionUtils;

    @Autowired
    public void setEncryptionUtils(EncryptionUtils utils) {
        FieldEncryptor.encryptionUtils = utils;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isEmpty()) return attribute;
        return encryptionUtils.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) return dbData;
        // Check if data looks like encrypted (valid base64 with minimum length for IV + ciphertext)
        if (isLikelyEncrypted(dbData)) {
            try {
                return encryptionUtils.decrypt(dbData);
            } catch (Exception e) {
                // If decryption fails, return raw data (legacy unencrypted)
                return dbData;
            }
        }
        return dbData;
    }

    private boolean isLikelyEncrypted(String data) {
        if (data.length() < 20) return false; // Too short to be encrypted (IV + tag alone is 28+ base64 chars)
        try {
            byte[] decoded = Base64.getDecoder().decode(data);
            return decoded.length > 12; // At minimum IV (12 bytes) + some ciphertext
        } catch (IllegalArgumentException e) {
            return false; // Not valid base64, so not encrypted
        }
    }
}
