package com.udbmanager.util;

import org.jasypt.util.text.BasicTextEncryptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EncryptionUtil {

    private final BasicTextEncryptor textEncryptor;

    public EncryptionUtil(@Value("${app.encryption.secret-key}") String secretKey) {
        this.textEncryptor = new BasicTextEncryptor();
        this.textEncryptor.setPassword(secretKey);
    }

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        return textEncryptor.encrypt(plainText);
    }

    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }
        return textEncryptor.decrypt(encryptedText);
    }
}
