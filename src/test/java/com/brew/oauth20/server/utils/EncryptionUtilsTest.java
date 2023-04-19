package com.brew.oauth20.server.utils;

import com.github.javafaker.Faker;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class EncryptionUtilsTest {

    private static final String ALGORITHM = "AES";
    private Faker faker;

    @BeforeAll
    void Setup() {
        this.faker = new Faker();
    }

    @Test
    public void should_encrypt_and_decrypt_properly() throws Exception {
        // Arrange
        var testData = faker.lordOfTheRings().location();
        var key = faker.regexify("[A-Za-z0-9]{16}");

        // Act
        String encryptedData = EncryptionUtils.encrypt(testData, ALGORITHM, key);
        String decryptedData = EncryptionUtils.decrypt(encryptedData, ALGORITHM, key);

        // Assert
        assertThat(decryptedData).isEqualTo(testData);
    }

    @Test
    public void should_not_encrypt_null_data() {
        assertThrows(Exception.class, () -> {
            EncryptionUtils.encrypt(null, ALGORITHM, key);
        });
    }

    @Test
    public void should_not_decrypt_null_data() {
        // Arrange
        var key = faker.regexify("[A-Za-z0-9]{16}");

        // Assert
        assertThrows(Exception.class, () -> {
            EncryptionUtils.decrypt(null, ALGORITHM, key);
        });
    }

    @Test
    public void should_not_decrypt_invalid_data() {
        // Arrange
        var key = faker.regexify("[A-Za-z0-9]{16}");

        // Assert
        assertThrows(Exception.class, () -> {
            EncryptionUtils.decrypt("invalid-encrypted-data", ALGORITHM, key);
        });
    }
}
