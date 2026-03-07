package com.example.bankcards.util;

import io.jsonwebtoken.io.Decoders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class CardCryptoService {

    @Value("${security.card.encryption-key}")
    private String encryptionKey;

    public String encrypt(String plaintext) {
        try {
            byte[] key = Decoders.BASE64.decode(encryptionKey);
            if (key.length != 16 && key.length != 24 && key.length != 32) {
                throw new IllegalStateException("Invalid AES key length for card encryption: " + key.length + " bytes");
            }
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new IllegalStateException("Card encryption failed", e);
        }
    }
}
