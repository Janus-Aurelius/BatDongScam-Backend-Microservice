package com.se.bds.core.transaction.internal.support;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * AES-256 converter for sensitive fields like bankAccountNumber (US-028).
 * Note: In a real production system, the key should be managed by a KMS.
 */
@Converter
@Component
public class AesAttributeConverter implements AttributeConverter<String, String> {

    private static final String AES = "AES";
    private final SecretKeySpec secretKey;

    public AesAttributeConverter(@Value("${encryption.key:default-encryption-key-32-chars!!!}") String key) {
        // Ensure key is 32 chars for AES-256
        String paddedKey = (key + "00000000000000000000000000000000").substring(0, 32);
        this.secretKey = new SecretKeySpec(paddedKey.getBytes(), AES);
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        try {
            Cipher cipher = Cipher.getInstance(AES);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return Base64.getEncoder().encodeToString(cipher.doFinal(attribute.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        try {
            Cipher cipher = Cipher.getInstance(AES);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            return new String(cipher.doFinal(Base64.getDecoder().decode(dbData)));
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
