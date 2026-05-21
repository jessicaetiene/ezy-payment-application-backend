package br.com.ezy.ezypaymentapplicationbackend.domain.service;

import org.junit.jupiter.api.Test;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CardEncryptorServiceTest {

    private static final String BASE64_KEY = "V4YfXK7M7DpAeLq5j1mJ3eXxJq+zWvHcYz3mR8N9pQ0=";

    @Test
    void shouldEncryptCardNumberRemovingWhitespace() throws Exception {
        var service = new CardEncryptorService(BASE64_KEY);

        var encryptedCardNumber = service.encrypt("4111 1111 1111 1111");

        assertThat(encryptedCardNumber).isNotBlank();
        assertThat(encryptedCardNumber).doesNotContain("4111");
        assertThat(decrypt(encryptedCardNumber)).isEqualTo("4111111111111111");
    }

    @Test
    void shouldGenerateDifferentCipherTextsForTheSameCardNumber() {
        var service = new CardEncryptorService(BASE64_KEY);

        var firstEncryptedCardNumber = service.encrypt("4111111111111111");
        var secondEncryptedCardNumber = service.encrypt("4111111111111111");

        assertThat(firstEncryptedCardNumber).isNotEqualTo(secondEncryptedCardNumber);
    }

    @Test
    void shouldThrowWhenKeyIsNotBase64() {
        assertThatThrownBy(() -> new CardEncryptorService("not-a-base64-key"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldThrowWhenKeySizeIsInvalid() {
        var service = new CardEncryptorService(Base64.getEncoder().encodeToString("short".getBytes(StandardCharsets.UTF_8)));

        assertThatThrownBy(() -> service.encrypt("4111111111111111"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Error encrypting card number");
    }

    private String decrypt(String encryptedCardNumber) throws Exception {
        byte[] encryptedWithIv = Base64.getDecoder().decode(encryptedCardNumber);
        byte[] iv = Arrays.copyOfRange(encryptedWithIv, 0, 12);
        byte[] encryptedBytes = Arrays.copyOfRange(encryptedWithIv, 12, encryptedWithIv.length);
        byte[] keyBytes = Base64.getDecoder().decode(BASE64_KEY);

        var cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(128, iv));

        return new String(cipher.doFinal(encryptedBytes), StandardCharsets.UTF_8);
    }
}
